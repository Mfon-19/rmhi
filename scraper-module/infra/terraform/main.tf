terraform {
  required_version = ">= 1.6.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = ">= 3.5"
    }
  }
}

provider "aws" {
  region = var.region
}

locals {
  name_prefix = "${var.project}-${var.region}"
  db_username = "eureka_app"
  db_name     = "eureka"
  db_port     = 5432
}

resource "random_password" "db" {
  length  = 24
  special = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
  filter {
    name   = "default-for-az"
    values = ["true"]
  }
}

resource "aws_security_group" "ecs_tasks" {
  name        = "${local.name_prefix}-ecs-tasks"
  description = "ECS tasks egress"
  vpc_id      = data.aws_vpc.default.id
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "rds" {
  name        = "${local.name_prefix}-rds"
  description = "RDS access from ECS"
  vpc_id      = data.aws_vpc.default.id
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_subnet_group" "rds" {
  name       = "${local.name_prefix}-rds-subnets"
  subnet_ids = data.aws_subnets.default.ids
}

resource "aws_secretsmanager_secret" "db" {
  name = "${local.name_prefix}-db-credentials"
}

resource "aws_secretsmanager_secret" "google_api_key" {
  name = "${local.name_prefix}-google-api-key"
}

resource "aws_secretsmanager_secret_version" "db" {
  secret_id     = aws_secretsmanager_secret.db.id
  secret_string = "postgresql://${local.db_username}:${urlencode(random_password.db.result)}@${aws_db_instance.postgres.address}:${local.db_port}/${local.db_name}"
}

resource "aws_secretsmanager_secret_version" "google_api_key" {
  secret_id     = aws_secretsmanager_secret.google_api_key.id
  secret_string = var.google_api_key
}

resource "aws_db_instance" "postgres" {
  identifier              = "${replace(local.name_prefix, "_", "-")}-pg"
  engine                  = "postgres"
  engine_version          = "17.4"
  instance_class          = "db.t4g.small"
  allocated_storage       = 20
  storage_type            = "gp3"
  db_name                 = local.db_name
  username                = local.db_username
  password                = random_password.db.result
  vpc_security_group_ids  = [aws_security_group.rds.id]
  db_subnet_group_name    = aws_db_subnet_group.rds.name
  publicly_accessible     = false
  skip_final_snapshot     = true
  backup_retention_period = 7
}

resource "aws_ecr_repository" "repo" {
  name                 = var.project
  image_tag_mutability = "MUTABLE"
}

resource "aws_ecr_lifecycle_policy" "repo" {
  repository = aws_ecr_repository.repo.name
  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last 10 images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 10
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/${var.project}"
  retention_in_days = 14
}

data "aws_iam_policy_document" "task_execution" {
  statement {
    actions = [
      "logs:CreateLogStream",
      "logs:PutLogEvents",
      "logs:CreateLogGroup",
      "ecr:GetAuthorizationToken",
      "ecr:BatchCheckLayerAvailability",
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage"
    ]
    resources = ["*"]
  }
  statement {
    actions = ["secretsmanager:GetSecretValue"]
    resources = [
      aws_secretsmanager_secret.db.arn,
      aws_secretsmanager_secret.google_api_key.arn
    ]
  }
}

resource "aws_iam_role" "task_execution" {
  name               = "${local.name_prefix}-task-execution"
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect = "Allow",
      Principal = { Service = "ecs-tasks.amazonaws.com" },
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_policy" "task_execution" {
  name   = "${local.name_prefix}-task-execution"
  policy = data.aws_iam_policy_document.task_execution.json
}

resource "aws_iam_role_policy_attachment" "task_execution" {
  role       = aws_iam_role.task_execution.name
  policy_arn = aws_iam_policy.task_execution.arn
}

data "aws_iam_policy_document" "task" {
  statement {
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [
      aws_secretsmanager_secret.db.arn,
      aws_secretsmanager_secret.google_api_key.arn
    ]
  }
}

resource "aws_iam_role" "task" {
  name               = "${local.name_prefix}-task"
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect = "Allow",
      Principal = { Service = "ecs-tasks.amazonaws.com" },
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_policy" "task" {
  name   = "${local.name_prefix}-task"
  policy = data.aws_iam_policy_document.task.json
}

resource "aws_iam_role_policy_attachment" "task" {
  role       = aws_iam_role.task.name
  policy_arn = aws_iam_policy.task.arn
}

resource "aws_ecs_cluster" "this" {
  name = local.name_prefix
}

resource "aws_ecs_task_definition" "worker" {
  family                   = "${var.project}-worker"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "1024"
  memory                   = "2048"
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn
  container_definitions    = jsonencode([
    {
      name  = var.project
      image = "${aws_ecr_repository.repo.repository_url}:latest"
      essential = true
      environment = [
        { name = "MODE", value = "daily" },
        { name = "APP_NAME", value = "worker" }
      ]
      secrets = [
        {
          name      = "DB_DSN",
          valueFrom = aws_secretsmanager_secret.db.arn
        },
        {
          name      = "GOOGLE_API_KEY",
          valueFrom = aws_secretsmanager_secret.google_api_key.arn
        }
      ]
      logConfiguration = {
        logDriver = "awslogs",
        options = {
          awslogs-group         = aws_cloudwatch_log_group.ecs.name,
          awslogs-region        = var.region,
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_task_definition" "migrate" {
  family                   = "${var.project}-migrate"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn
  container_definitions    = jsonencode([
    {
      name  = "${var.project}-migrate"
      image = "${aws_ecr_repository.repo.repository_url}:latest"
      essential = true
      command = ["sh", "-c", "psql \"$DB_DSN\" -v ON_ERROR_STOP=1 -f /app/db/schema.sql"]
      secrets = [
        {
          name      = "DB_DSN",
          valueFrom = aws_secretsmanager_secret.db.arn
        }
      ]
      logConfiguration = {
        logDriver = "awslogs",
        options = {
          awslogs-group         = aws_cloudwatch_log_group.ecs.name,
          awslogs-region        = var.region,
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

data "aws_iam_policy_document" "events_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["events.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "events" {
  name               = "${local.name_prefix}-events"
  assume_role_policy = data.aws_iam_policy_document.events_assume.json
}

data "aws_iam_policy_document" "events" {
  statement {
    actions = ["ecs:RunTask"]
    resources = ["*"]
  }
  statement {
    actions = ["iam:PassRole"]
    resources = [
      aws_iam_role.task_execution.arn,
      aws_iam_role.task.arn
    ]
  }
}

resource "aws_iam_policy" "events" {
  name   = "${local.name_prefix}-events"
  policy = data.aws_iam_policy_document.events.json
}

resource "aws_iam_role_policy_attachment" "events" {
  role       = aws_iam_role.events.name
  policy_arn = aws_iam_policy.events.arn
}

resource "aws_cloudwatch_event_rule" "daily" {
  name                = "${local.name_prefix}-daily"
  schedule_expression = "cron(0 3 * * ? *)"
}

resource "aws_cloudwatch_event_target" "daily" {
  rule      = aws_cloudwatch_event_rule.daily.name
  target_id = "ecs"
  arn       = aws_ecs_cluster.this.arn
  role_arn  = aws_iam_role.events.arn
  ecs_target {
    task_definition_arn = aws_ecs_task_definition.worker.arn
    launch_type         = "FARGATE"
    task_count          = 1
    network_configuration {
      subnets          = data.aws_subnets.default.ids
      security_groups  = [aws_security_group.ecs_tasks.id]
      assign_public_ip = true
    }
  }
  input = jsonencode({
    containerOverrides = [
      {
        name = var.project,
        environment = [
          { name = "MODE", value = "daily" },
          { name = "APP_NAME", value = "daily" }
        ]
      }
    ]
  })
}

resource "aws_cloudwatch_event_rule" "transform" {
  name                = "${local.name_prefix}-transform"
  schedule_expression = "cron(30 3 * * ? *)"
}

resource "aws_cloudwatch_event_target" "transform" {
  rule      = aws_cloudwatch_event_rule.transform.name
  target_id = "ecs"
  arn       = aws_ecs_cluster.this.arn
  role_arn  = aws_iam_role.events.arn
  ecs_target {
    task_definition_arn = aws_ecs_task_definition.worker.arn
    launch_type         = "FARGATE"
    task_count          = 1
    network_configuration {
      subnets          = data.aws_subnets.default.ids
      security_groups  = [aws_security_group.ecs_tasks.id]
      assign_public_ip = true
    }
  }
  input = jsonencode({
    containerOverrides = [
      {
        name = var.project,
        environment = [
          { name = "MODE", value = "transform" },
          { name = "APP_NAME", value = "transform" }
        ]
      }
    ]
  })
}

resource "aws_cloudwatch_event_rule" "transform_check" {
  name                = "${local.name_prefix}-transform-check"
  schedule_expression = "rate(24 hours)"
}

resource "aws_cloudwatch_event_target" "transform_check" {
  rule      = aws_cloudwatch_event_rule.transform_check.name
  target_id = "ecs"
  arn       = aws_ecs_cluster.this.arn
  role_arn  = aws_iam_role.events.arn
  ecs_target {
    task_definition_arn = aws_ecs_task_definition.worker.arn
    launch_type         = "FARGATE"
    task_count          = 1
    network_configuration {
      subnets          = data.aws_subnets.default.ids
      security_groups  = [aws_security_group.ecs_tasks.id]
      assign_public_ip = true
    }
  }
  input = jsonencode({
    containerOverrides = [
      {
        name = var.project,
        environment = [
          { name = "MODE", value = "transform_check" },
          { name = "APP_NAME", value = "transform_check" }
        ]
      }
    ]
  })
}

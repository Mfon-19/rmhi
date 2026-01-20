locals {
  backend_name          = "${var.project}-backend"
  backend_container_port = 8080
}

resource "aws_ecr_repository" "backend" {
  name                 = local.backend_name
  image_tag_mutability = "MUTABLE"
}

resource "aws_ecr_lifecycle_policy" "backend" {
  repository = aws_ecr_repository.backend.name
  policy     = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Expire untagged images after 14 days"
      selection = {
        tagStatus   = "untagged"
        countType   = "sinceImagePushed"
        countUnit   = "days"
        countNumber = 14
      }
      action = { type = "expire" }
    }]
  })
}

resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/${local.backend_name}"
  retention_in_days = 14
}

resource "aws_security_group" "backend_alb" {
  name        = "${local.backend_name}-alb"
  description = "ALB ingress"
  vpc_id      = aws_vpc.this.id
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "backend_tasks" {
  name        = "${local.backend_name}-tasks"
  description = "Backend ECS tasks"
  vpc_id      = aws_vpc.this.id
  ingress {
    from_port       = local.backend_container_port
    to_port         = local.backend_container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.backend_alb.id]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group_rule" "rds_from_backend" {
  type                     = "ingress"
  from_port                = local.db_port
  to_port                  = local.db_port
  protocol                 = "tcp"
  security_group_id        = aws_security_group.rds.id
  source_security_group_id = aws_security_group.backend_tasks.id
}

resource "aws_lb" "backend" {
  name               = "${local.backend_name}-alb"
  load_balancer_type = "application"
  security_groups    = [aws_security_group.backend_alb.id]
  subnets            = aws_subnet.public[*].id
}

resource "aws_lb_target_group" "backend" {
  name        = "${local.backend_name}-tg"
  port        = local.backend_container_port
  protocol    = "HTTP"
  vpc_id      = aws_vpc.this.id
  target_type = "ip"
  health_check {
    path                = "/health"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_listener" "backend" {
  load_balancer_arn = aws_lb.backend.arn
  port              = 80
  protocol          = "HTTP"
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}

resource "aws_secretsmanager_secret" "backend_db" {
  name = "${local.backend_name}-db-credentials"
}

resource "aws_secretsmanager_secret_version" "backend_db" {
  secret_id     = aws_secretsmanager_secret.backend_db.id
  secret_string = jsonencode({
    url      = "jdbc:postgresql://${aws_db_instance.postgres.address}:${local.db_port}/${local.db_name}"
    username = local.db_username
    password = random_password.db.result
  })
}

data "aws_iam_policy_document" "backend_task_execution" {
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
      aws_secretsmanager_secret.backend_db.arn,
      var.backend_firebase_secret_arn
    ]
  }
}

resource "aws_iam_role" "backend_task_execution" {
  name               = "${local.backend_name}-task-execution"
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect = "Allow",
      Principal = { Service = "ecs-tasks.amazonaws.com" },
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_policy" "backend_task_execution" {
  name   = "${local.backend_name}-task-execution"
  policy = data.aws_iam_policy_document.backend_task_execution.json
}

resource "aws_iam_role_policy_attachment" "backend_task_execution" {
  role       = aws_iam_role.backend_task_execution.name
  policy_arn = aws_iam_policy.backend_task_execution.arn
}

resource "aws_iam_role" "backend_task" {
  name               = "${local.backend_name}-task"
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect = "Allow",
      Principal = { Service = "ecs-tasks.amazonaws.com" },
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_ecs_task_definition" "backend" {
  family                   = local.backend_name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = tostring(var.backend_cpu)
  memory                   = tostring(var.backend_memory)
  execution_role_arn       = aws_iam_role.backend_task_execution.arn
  task_role_arn            = aws_iam_role.backend_task.arn
  container_definitions    = jsonencode([
    {
      name  = local.backend_name
      image = "${aws_ecr_repository.backend.repository_url}:${var.backend_image_tag}"
      essential = true
      portMappings = [
        {
          containerPort = local.backend_container_port
          hostPort      = local.backend_container_port
          protocol      = "tcp"
        }
      ]
      environment = [
        { name = "CORS_ALLOWED_ORIGINS", value = var.backend_cors_allowed_origins }
      ]
      secrets = [
        {
          name      = "DB_URL",
          valueFrom = "${aws_secretsmanager_secret.backend_db.arn}:url::"
        },
        {
          name      = "DB_USERNAME",
          valueFrom = "${aws_secretsmanager_secret.backend_db.arn}:username::"
        },
        {
          name      = "DB_PASSWORD",
          valueFrom = "${aws_secretsmanager_secret.backend_db.arn}:password::"
        },
        {
          name      = "FIREBASE_SERVICE_ACCOUNT_JSON",
          valueFrom = var.backend_firebase_secret_arn
        }
      ]
      logConfiguration = {
        logDriver = "awslogs",
        options = {
          awslogs-group         = aws_cloudwatch_log_group.backend.name,
          awslogs-region        = var.region,
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "backend" {
  name            = local.backend_name
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = var.backend_desired_count
  launch_type     = "FARGATE"
  network_configuration {
    subnets         = aws_subnet.public[*].id
    security_groups = [aws_security_group.backend_tasks.id]
    assign_public_ip = true
  }
  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = local.backend_name
    container_port   = local.backend_container_port
  }
  depends_on = [aws_lb_listener.backend]
}

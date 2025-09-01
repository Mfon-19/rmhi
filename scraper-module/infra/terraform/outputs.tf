output "vpc_id" { value = aws_vpc.this.id }
output "private_subnets" { value = aws_subnet.private[*].id }
output "public_subnets" { value = aws_subnet.public[*].id }
output "ecr_repo_url" { value = aws_ecr_repository.repo.repository_url }
output "ecs_cluster_arn" { value = aws_ecs_cluster.this.arn }
output "daily_task_def_arn" { value = aws_ecs_task_definition.daily.arn }
output "backfill_task_def_arn" { value = aws_ecs_task_definition.backfill.arn }
output "lambda_name" { value = aws_lambda_function.scheduler.function_name }
output "db_endpoint" { value = aws_db_instance.postgres.address }
output "db_secret_arn" { value = aws_secretsmanager_secret.db.arn }


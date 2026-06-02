# CloudWatch para los logs del contenedor
resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${var.project_name}"
  retention_in_days = 7  # 7 días es suficiente para pruebas

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

# Cluster de ECS
resource "aws_ecs_cluster" "main" {
  name = "${var.project_name}-cluster"

  setting {
    name  = "containerInsights"
    value = "disabled"  # Habilitarlo cuesta extra, para pruebas no lo necesitas
  }

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

# Task Definition — es como el "pod spec" de Kubernetes pero para ECS
resource "aws_ecs_task_definition" "app" {
  family                   = "${var.project_name}-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name      = "${var.project_name}-container"
    image     = "${aws_ecr_repository.app.repository_url}:latest"
    essential = true

    portMappings = [{
      containerPort = var.app_port
      protocol      = "tcp"
    }]

    # Variables de entorno — las sensibles vienen de Secrets Manager
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
      { name = "DB_HOST",                value = split(":", aws_db_instance.postgres.endpoint)[0] },
      { name = "DB_PORT",                value = "5432" },
      { name = "DB_NAME",                value = var.db_name },
      { name = "DB_USER",                value = var.db_username },
      { name = "SERVER_PORT",            value = tostring(var.app_port) }
    ]

    secrets = [
      {
        name      = "DB_PASSWORD"
        valueFrom = aws_secretsmanager_secret.db_password.arn
      },
      {
        name      = "JWT_SECRET"
        valueFrom = aws_secretsmanager_secret.jwt_secret.arn
      }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.app.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }

    # Healthcheck a nivel contenedor (además del del ALB)
    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:${var.app_port}/actuator/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 90  # Spring Boot necesita ~30-60s para arrancar
    }
  }])

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

# Servicio de ECS — mantiene corriendo el número de tasks que le digas
resource "aws_ecs_service" "app" {
  name            = "${var.project_name}-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1      # 1 sola task para pruebas, en EKS tenías 2
  launch_type     = "FARGATE"

  # Sin esto Terraform destruye el servicio antes de crear el nuevo al hacer cambios
  deployment_minimum_healthy_percent = 50
  deployment_maximum_percent         = 200

  network_configuration {
    subnets          = module.vpc.private_subnets  # El contenedor en subnets privadas
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = "${var.project_name}-container"
    container_port   = var.app_port
  }

  # Ignora cambios en la imagen — el CI/CD la actualiza por su cuenta
  lifecycle {
    ignore_changes = [task_definition]
  }

  depends_on = [
    aws_lb_listener.http,
    aws_iam_role_policy_attachment.ecs_task_execution
  ]

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}
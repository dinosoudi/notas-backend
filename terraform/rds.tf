# Security group — solo acepta conexiones desde ECS
resource "aws_security_group" "rds" {
  name        = "${var.project_name}-rds-sg"
  description = "Acceso a RDS solo desde ECS"
  vpc_id      = module.vpc.vpc_id

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

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

# Subnet group — RDS debe estar en subnets privadas
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = module.vpc.private_subnets

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

resource "aws_db_instance" "postgres" {
  identifier        = "${var.project_name}-db"
  engine            = "postgres"
  engine_version    = "16"
  instance_class    = "db.t3.micro"  # El más barato, ~$13/mes
  allocated_storage = 20
  storage_type      = "gp2"

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  skip_final_snapshot     = true   # Sin esto terraform destroy falla si no haces snapshot
  deletion_protection     = false  # Para poder destruirlo fácil en pruebas
  backup_retention_period = 0      # Sin backups automáticos (ahorra dinero en pruebas)

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

output "rds_endpoint" {
  description = "Endpoint de la base de datos"
  value       = aws_db_instance.postgres.endpoint
}
output "app_url" {
  description = "URL de tu aplicación"
  value       = "http://${aws_lb.main.dns_name}"
}

output "ecr_url" {
  description = "URL del repositorio ECR para hacer push de imagenes"
  value       = aws_ecr_repository.app.repository_url
}

output "ecs_cluster_name" {
  description = "Nombre del cluster ECS"
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "Nombre del servicio ECS"
  value       = aws_ecs_service.app.name
}

output "rds_endpoint" {
  description = "Endpoint de la base de datos"
  value       = aws_db_instance.postgres.endpoint
  sensitive   = true
}
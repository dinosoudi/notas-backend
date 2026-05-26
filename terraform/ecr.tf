resource "aws_ecr_repository" "app" {
  name                 = "${var.project_name}-backend"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true  # Escanea vulnerabilidades en cada push
  }

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

# Política para limpiar imágenes viejas automáticamente
# Guarda solo las últimas 5, las demás se borran
resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Mantener solo las últimas 5 imágenes"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 5
      }
      action = {
        type = "expire"
      }
    }]
  })
}

output "ecr_repository_url" {
  description = "URL del repositorio ECR"
  value       = aws_ecr_repository.app.repository_url
}
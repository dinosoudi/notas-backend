variable "aws_region" {
  description = "Región de AWS"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Nombre del proyecto, se usa como prefijo en todos los recursos"
  type        = string
  default     = "taskflow"
}

variable "environment" {
  description = "Ambiente"
  type        = string
  default     = "prod"
}

# --- RDS ---
variable "db_name" {
  description = "Nombre de la base de datos"
  type        = string
  default     = "taskflow_db"
}

variable "db_username" {
  description = "Usuario de PostgreSQL"
  type        = string
  default     = "taskflow_user"
}

variable "db_password" {
  description = "Contraseña de PostgreSQL"
  type        = string
  sensitive   = true  # Terraform no la imprime en logs
}

# --- ECS ---
variable "app_port" {
  description = "Puerto en el que corre tu Spring Boot"
  type        = number
  default     = 8080
}

variable "app_image" {
  description = "URI de la imagen en ECR (se llena después del primer push)"
  type        = string
  default     = ""
}

variable "task_cpu" {
  description = "CPU para la task de ECS (en units, 256 = 0.25 vCPU)"
  type        = number
  default     = 512
}

variable "task_memory" {
  description = "Memoria para la task de ECS (en MB)"
  type        = number
  default     = 1024
}

variable "jwt_secret" {
  description = "Secret para firmar los JWT"
  type        = string
  sensitive   = true
}
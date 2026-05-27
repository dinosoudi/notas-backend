# Security group del ALB — acepta tráfico HTTP desde internet
resource "aws_security_group" "alb" {
  name        = "${var.project_name}-alb-sg"
  description = "Trafico publico al Load Balancer"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
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

# Security group de ECS — solo acepta tráfico desde el ALB
resource "aws_security_group" "ecs_tasks" {
  name        = "${var.project_name}-ecs-sg"
  description = "Trafico al contenedor solo desde el ALB"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port       = var.app_port
    to_port         = var.app_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
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

# El Load Balancer en sí
resource "aws_lb" "main" {
  name               = "${var.project_name}-alb"
  internal           = false  # Público, accesible desde internet
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = module.vpc.public_subnets  # En subnets públicas

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

# Target group — a dónde el ALB manda el tráfico
resource "aws_lb_target_group" "app" {
  name        = "${var.project_name}-tg"
  port        = var.app_port
  protocol    = "HTTP"
  vpc_id      = module.vpc.vpc_id
  target_type = "ip"  # ECS Fargate usa IPs, no instancias EC2

  health_check {
    enabled             = true
    path                = "/actuator/health"  # Tu endpoint de Spring Actuator
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    matcher             = "200"
  }

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

# Listener — escucha en el puerto 80 y manda al target group
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}

output "alb_dns_name" {
  description = "URL publica de tu aplicacion"
  value       = "http://${aws_lb.main.dns_name}"
}
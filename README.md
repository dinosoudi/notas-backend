# notas-backend

para levantar todo lo de terraform:
cd terraform
terraform apply -var-file="terraform.tfvars"

Flujo:
terraform apply   ← infra arriba
git push a prod   ← GitHub Actions deploya
pruebas           ← verificas que todo funciona
terraform destroy ← bajas todo


Host: postgres (nombre del servicio, no localhost)
Port: 5432
Database: taskflow
User: taskflow_user
Password: taskflow_pass


docker:
docker-compose up -d
docker-compose ps


3 horas más en norte de virginia us-east-1 que en mi casa

# Frontend se sirve desde nginx en la EC2 (AWS Academy no permite CloudFront/OAC).
# HTTPS real con Let's Encrypt + sslip.io sobre la Elastic IP.

output "frontend_url" {
  description = "URL HTTPS del frontend (nginx en EC2)."
  value       = "${aws_apigatewayv2_api.api.api_endpoint}/${aws_apigatewayv2_stage.dev.name}/web/"
}

output "frontend_http_url" {
  description = "Entrada HTTP que redirige al HTTPS de API Gateway."
  value       = "http://${aws_eip.backend.public_ip}"
}

output "artifacts_note" {
  value = "Releases privadas en S3 y despliegue SSM (scripts/deploy-ssm.sh)."
}

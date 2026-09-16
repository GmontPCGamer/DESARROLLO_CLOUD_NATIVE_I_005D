output "api_gateway_id" {
  description = "ID del API Gateway HTTP."
  value       = aws_apigatewayv2_api.api.id
}

output "api_gateway_endpoint" {
  description = "URL base del API Gateway."
  value       = aws_apigatewayv2_api.api.api_endpoint
}

output "url_deploy" {
  description = "URL del stage dev (backendApiUrl del frontend, sin /api)."
  value       = "${aws_apigatewayv2_api.api.api_endpoint}/${aws_apigatewayv2_stage.dev.name}"
}

output "authorizer_id" {
  description = "ID del authorizer JWT (Azure AD)."
  value       = aws_apigatewayv2_authorizer.jwt.id
}

output "backend_url" {
  description = "URL directa del BFF en la EC2 (debug)."
  value       = "http://${aws_eip.backend.public_ip}:8080"
}

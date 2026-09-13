output "api_gateway_id" {
  description = "ID del API Gateway HTTP."
  value       = aws_apigatewayv2_api.api.id
}

output "api_gateway_endpoint" {
  description = "URL de invocación del API Gateway (stage dev)."
  value       = aws_apigatewayv2_api.api.api_endpoint
}

output "url_deploy" {
  description = "URL completa del stage dev (la que consume el frontend Angular)."
  value       = "${aws_apigatewayv2_api.api.api_endpoint}/${aws_apigatewayv2_stage.dev.name}"
}

output "authorizer_id" {
  description = "ID del authorizer JWT (Azure AD)."
  value       = aws_apigatewayv2_authorizer.jwt.id
}

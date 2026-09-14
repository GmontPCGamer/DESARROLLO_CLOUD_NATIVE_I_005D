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

output "entra_tenant_id" {
  description = "Tenant usado para los registros de Entra ID."
  value       = var.gestionar_entra ? var.azure_tenant_id : null
  sensitive   = true
}

output "entra_api_client_id" {
  description = "Client ID de la aplicación híbrida para audiencia y apiScope."
  value       = var.gestionar_entra ? azuread_application.student[0].client_id : null
}

output "entra_spa_client_id" {
  description = "Client ID que debe usar Angular en el modo estudiante SPA + API."
  value       = var.gestionar_entra ? azuread_application.student[0].client_id : null
}

output "entra_api_scope" {
  description = "Scope delegado que debe solicitar Angular."
  value       = var.gestionar_entra ? "${var.entra_student_identifier_uri}/access_as_user" : null
  depends_on  = [azuread_application.student]
}

output "entra_student_client_id" {
  description = "Client ID de la aplicación híbrida para Azure Student."
  value       = var.gestionar_entra ? azuread_application.student[0].client_id : null
}

output "entra_student_scope" {
  description = "Scope de la aplicación híbrida para Azure Student."
  value       = var.gestionar_entra ? "${var.entra_student_identifier_uri}/access_as_user" : null
  depends_on  = [azuread_application.student]
}

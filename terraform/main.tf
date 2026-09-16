# =============================================================================
# API Gateway HTTP (v2) con Authorizer JWT de Azure AD / Entra ID (MSAL)
# =============================================================================

locals {
  backend_base = "http://${aws_eip.backend.public_ip}:8080"
  frontend_origin = "https://${replace(aws_eip.backend.public_ip, ".", "-")}.sslip.io"
  cors_origins = distinct(concat(var.cors_origenes, [local.frontend_origin]))
}

resource "aws_apigatewayv2_api" "api" {
  name          = "${var.nombre_proyecto}-api"
  description   = "API Gateway NexoTech (JWT Entra ID → BFF en EC2)."
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = local.cors_origins
    allow_methods = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
    allow_headers = ["Authorization", "Content-Type"]
    max_age       = 3600
  }
}

resource "aws_apigatewayv2_authorizer" "jwt" {
  api_id           = aws_apigatewayv2_api.api.id
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]
  name             = "azuread-jwt"

  jwt_configuration {
    issuer   = var.issuer_uri
    audience = [var.audiencia, "api://nexotech-demo-api"]
  }
}

# Integración para rutas públicas /api/public/*
resource "aws_apigatewayv2_integration" "backend_public" {
  api_id                 = aws_apigatewayv2_api.api.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "${local.backend_base}/api/public/{proxy}"
  payload_format_version = "1.0"
  timeout_milliseconds   = 29000
}

# Integración catch-all (rutas privadas /api/me, /api/cart, ...)
resource "aws_apigatewayv2_integration" "backend" {
  api_id                 = aws_apigatewayv2_api.api.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "${local.backend_base}/{proxy}"
  payload_format_version = "1.0"
  timeout_milliseconds   = 29000
}

# Públicas: sin JWT (EP2 — catálogo anónimo)
resource "aws_apigatewayv2_route" "public_api" {
  api_id    = aws_apigatewayv2_api.api.id
  route_key = "GET /api/public/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.backend_public.id}"
}

# Preflight CORS: sin JWT (si OPTIONS cae en ANY+JWT el navegador falla el CORS)
resource "aws_apigatewayv2_route" "options" {
  api_id             = aws_apigatewayv2_api.api.id
  route_key          = "OPTIONS /{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorization_type = "NONE"
}

# Protegidas: JWT authorizer (EP2 — 401 sin token / token inválido)
resource "aws_apigatewayv2_route" "protegida" {
  api_id             = aws_apigatewayv2_api.api.id
  route_key          = "ANY /{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorizer_id      = aws_apigatewayv2_authorizer.jwt.id
  authorization_type = "JWT"
}

resource "aws_apigatewayv2_stage" "dev" {
  api_id      = aws_apigatewayv2_api.api.id
  name        = "dev"
  auto_deploy = true

  default_route_settings {
    throttling_burst_limit = 100
    throttling_rate_limit  = 50
  }
}

# =============================================================================
# API Gateway HTTP (v2) con Authorizer JWT de Azure AD / Entra ID (MSAL)
# =============================================================================

resource "aws_apigatewayv2_api" "api" {
  name          = "${var.nombre_proyecto}-api"
  description   = "API Gateway del proyecto DESARROLLO_CLOUD_NATIVE_I_005D (backend Spring Boot)."
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = var.cors_origenes
    allow_methods = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
    allow_headers = ["Authorization", "Content-Type"]
    max_age       = 3600
  }
}

# ---- Authorizer JWT (Azure AD / Entra ID) -----------------------------
# Valida la firma del token contra las claves publicadas (JWKS) por el
# issuer y comprueba la audiencia (clientId del registro en Azure AD).
resource "aws_apigatewayv2_authorizer" "jwt" {
  api_id           = aws_apigatewayv2_api.api.id
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]
  name             = "azuread-jwt"

  jwt_configuration {
    issuer   = var.issuer_uri
    audience = [var.audiencia]
  }
}

# ---- Integración con el backend Spring Boot ---------------------------
resource "aws_apigatewayv2_integration" "backend" {
  api_id                 = aws_apigatewayv2_api.api.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = var.url_backend
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
}

# ---- Rutas ------------------------------------------------------------
resource "aws_apigatewayv2_route" "protegida" {
  api_id        = aws_apigatewayv2_api.api.id
  route_key     = "ANY /{proxy+}"
  target        = "integrations/${aws_apigatewayv2_integration.backend.id}"
  authorizer_id = aws_apigatewayv2_authorizer.jwt.id
}

# Rutas públicas explícitas sin authorizer.
resource "aws_apigatewayv2_route" "health" {
  api_id    = aws_apigatewayv2_api.api.id
  route_key = "GET /api/public/health"
  target    = "integrations/${aws_apigatewayv2_integration.backend.id}"
}

resource "aws_apigatewayv2_route" "products" {
  api_id    = aws_apigatewayv2_api.api.id
  route_key = "GET /api/public/products"
  target    = "integrations/${aws_apigatewayv2_integration.backend.id}"
}

resource "aws_apigatewayv2_route" "product" {
  api_id    = aws_apigatewayv2_api.api.id
  route_key = "GET /api/public/products/{id}"
  target    = "integrations/${aws_apigatewayv2_integration.backend.id}"
}

resource "aws_apigatewayv2_route" "preflight" {
  api_id    = aws_apigatewayv2_api.api.id
  route_key = "OPTIONS /{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.backend.id}"
}

# ---- Despliegue / Stage -----------------------------------------------
resource "aws_apigatewayv2_deployment" "api" {
  api_id      = aws_apigatewayv2_api.api.id
  description = "Deploy de rutas + authorizer JWT."

  lifecycle {
    create_before_destroy = true
  }

  depends_on = [
    aws_apigatewayv2_route.protegida,
    aws_apigatewayv2_route.health,
    aws_apigatewayv2_route.products,
    aws_apigatewayv2_route.product,
    aws_apigatewayv2_route.preflight,
    aws_apigatewayv2_authorizer.jwt,
  ]
}

resource "aws_apigatewayv2_stage" "dev" {
  api_id        = aws_apigatewayv2_api.api.id
  name          = "dev"
  deployment_id = aws_apigatewayv2_deployment.api.id
  auto_deploy   = true

  default_route_settings {
    throttling_burst_limit = 100
    throttling_rate_limit  = 10
  }
}

variable "aws_region" {
  description = "Región AWS donde se despliega el API Gateway."
  type        = string
  default     = "us-east-1"
}

variable "nombre_proyecto" {
  description = "Prefijo de nombres de los recursos."
  type        = string
  default     = "cloud-native-005d"
}

variable "tags" {
  description = "Etiquetas aplicadas a todos los recursos."
  type        = map(string)
  default = {
    Proyecto     = "DESARROLLO_CLOUD_NATIVE_I_005D"
    Entorno      = "desarrollo"
    Administrado = "terraform"
  }
}

variable "url_backend" {
  description = "URL base del backend (Spring Boot) al que API Gateway reenvía el tráfico."
  type        = string
  default     = "https://REEMPLAZAR-URL-BACKEND"
}

variable "cors_origenes" {
  description = "Orígenes permitidos para CORS (dominio del frontend Angular)."
  type        = list(string)
  default     = ["http://localhost:4200"]
}

variable "issuer_uri" {
  description = "Issuer del authorizer JWT (Azure AD / Entra ID v2)."
  type        = string
  default     = "https://login.microsoftonline.com/REEMPLAZAR-TENANT/v2.0"
}

variable "audiencia" {
  description = "Audiencia esperada en el token (clientId de la app registrada en Azure AD)."
  type        = string
  default     = "REEMPLAZAR-CLIENT-ID"
}

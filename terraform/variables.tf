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

variable "gestionar_entra" {
  description = "Crea los registros de aplicación de Microsoft Entra ID mediante Terraform."
  type        = bool
  default     = false
}

variable "azure_tenant_id" {
  description = "Directory (tenant) ID de Microsoft Entra."
  type        = string
  sensitive   = true
  default     = "REEMPLAZAR-TENANT-ID"
}

variable "entra_api_nombre" {
  description = "Nombre del registro de aplicación que representa la API Spring Boot."
  type        = string
  default     = "NexoTech API"
}

variable "entra_api_identifier_uri" {
  description = "URI única que identifica la API dentro del tenant de Entra."
  type        = string
  default     = "api://nexotech-api"
}

variable "entra_spa_nombre" {
  description = "Nombre del registro de aplicación que representa Angular."
  type        = string
  default     = "NexoTech SPA"
}

variable "spa_redirect_uris" {
  description = "URIs de redirección permitidas para Angular."
  type        = list(string)
  default     = ["http://localhost:4200/auth"]
}

variable "spa_logout_uris" {
  description = "URIs de cierre de sesión permitidas para Angular."
  type        = list(string)
  default     = ["http://localhost:4200"]
}

variable "entra_student_app_nombre" {
  description = "Nombre de la aplicación híbrida compatible con permisos de estudiante."
  type        = string
  default     = "NexoTech Student"
}

variable "entra_student_identifier_uri" {
  description = "URI única de la API híbrida para la cuenta estudiante."
  type        = string
  default     = "api://nexotech-student-api"
}

variable "aws_region" {
  description = "Región AWS donde se despliega el stack."
  type        = string
  default     = "us-east-1"
}

variable "nombre_proyecto" {
  description = "Prefijo de nombres de los recursos."
  type        = string
  default     = "nexotech-005d"
}

variable "tags" {
  description = "Etiquetas aplicadas a todos los recursos."
  type        = map(string)
  default = {
    Proyecto     = "DESARROLLO_CLOUD_NATIVE_I_005D"
    Entorno      = "demo-48h"
    Administrado = "terraform"
  }
}

variable "cors_origenes" {
  description = "Orígenes CORS adicionales (sslip.io se agrega solo; tunnel Cloudflare para redes con filtro)."
  type        = list(string)
  default = [
    "http://localhost:4200",
    "https://neutral-lover-realized-collaboration.trycloudflare.com",
  ]
}

variable "issuer_uri" {
  description = "Issuer JWT: tenant Duoc (usuarios @duocuc.cl). La app vive en NextTechDemo (multi-tenant)."
  type        = string
  default     = "https://login.microsoftonline.com/72fd0b5a-8a6a-4cff-89f6-bde961f7e250/v2.0"
}

variable "audiencia" {
  description = "Audiencia (clientId de NexoTech Demo SPA en NextTechDemo)."
  type        = string
  default     = "b541332a-4305-4111-84f1-b5584ade7d44"
}

variable "azure_tenant_id" {
  description = "Tenant del issuer de los access tokens (Duoc). App registration en NextTechDemo."
  type        = string
  default     = "72fd0b5a-8a6a-4cff-89f6-bde961f7e250"
}

variable "ec2_instance_type" {
  description = "Tipo de instancia EC2 (demo corta: t3.medium)."
  type        = string
  default     = "t3.medium"
}

variable "ec2_key_name" {
  description = "Key pair de AWS Academy."
  type        = string
  default     = "vockey"
}

variable "ec2_instance_profile" {
  description = "Instance profile de AWS Academy."
  type        = string
  default     = "LabInstanceProfile"
}

variable "app_admin_users" {
  description = "preferred_username que reciben ROLE_ADMIN en el BFF."
  type        = string
  default     = "fe.ardiles@duocuc.cl"
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

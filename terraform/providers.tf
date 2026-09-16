# =============================================================================
# Capa de Infraestructura: AWS API Gateway + Authorizer JWT (Azure AD / Entra ID)
# Proyecto: DESARROLLO_CLOUD_NATIVE_I_005D
# Estructura de ramas: GitFlow (feature/* -> develop -> main)
# =============================================================================

terraform {
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = var.tags
  }
}

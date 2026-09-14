# Registros de aplicación de Microsoft Entra ID.
# Se mantienen opcionales para que el despliegue AWS existente no dependa de
# permisos de Microsoft Graph hasta que gestionar_entra se active.

resource "random_uuid" "access_as_user_scope" {
  count = var.gestionar_entra ? 1 : 0
}

resource "azuread_application" "api" {
  count        = var.gestionar_entra ? 1 : 0
  display_name = var.entra_api_nombre

  identifier_uris = [var.entra_api_identifier_uri]

  api {
    requested_access_token_version = 2

    oauth2_permission_scope {
      admin_consent_description  = "Permite a NexoTech SPA usar la API en nombre del usuario."
      admin_consent_display_name = "Acceder a NexoTech API"
      enabled                    = true
      id                         = random_uuid.access_as_user_scope[0].result
      type                       = "User"
      user_consent_description   = "Permite a NexoTech SPA usar la API en tu nombre."
      user_consent_display_name  = "Acceder a NexoTech API"
      value                      = "access_as_user"
    }
  }

}

resource "random_uuid" "student_access_scope" {
  count = var.gestionar_entra ? 1 : 0
}

resource "azuread_application" "student" {
  count           = var.gestionar_entra ? 1 : 0
  display_name    = var.entra_student_app_nombre
  identifier_uris = [var.entra_student_identifier_uri]

  api {
    requested_access_token_version = 2

    oauth2_permission_scope {
      admin_consent_description  = "Permite usar NexoTech en nombre del usuario."
      admin_consent_display_name = "Acceder a NexoTech"
      enabled                    = true
      id                         = random_uuid.student_access_scope[0].result
      type                       = "User"
      user_consent_description   = "Permite usar NexoTech en tu nombre."
      user_consent_display_name  = "Acceder a NexoTech"
      value                      = "access_as_user"
    }
  }

  single_page_application {
    redirect_uris = var.spa_redirect_uris
  }

  web {
    logout_url = var.spa_logout_uris[0]
  }
}

# Registro creado durante la primera configuración. Se conserva para no
# destruirlo; el flujo actual usa NexoTech API como aplicación híbrida.
resource "azuread_application" "spa" {
  count        = var.gestionar_entra ? 1 : 0
  display_name = var.entra_spa_nombre

  single_page_application {
    redirect_uris = var.spa_redirect_uris
  }

  web {
    logout_url = var.spa_logout_uris[0]
  }
}
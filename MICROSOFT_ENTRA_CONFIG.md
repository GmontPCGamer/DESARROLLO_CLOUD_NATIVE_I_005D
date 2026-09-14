# Configurar Microsoft Entra ID

La configuración para Azure for Students usa el registro de aplicación híbrido `NexoTech Student`:

- **NexoTech Student**: representa el backend, expone el scope `access_as_user` y contiene la redirección de Angular.

No se necesita un `client secret` para Angular ni para Spring Boot. Spring valida la firma, el issuer y la audiencia del JWT mediante las claves públicas de Entra ID.

## 1. Registrar la API

En **Microsoft Entra ID > Registros de aplicaciones > Nuevo registro**:

1. Nombre: `NexoTech API`.
2. Tipo de cuenta: solo el directorio de esta organización.
3. Guarda el **Application (client) ID**. Se llamará `API_CLIENT_ID`.
4. En **Exponer una API**, acepta el URI sugerido:
   `api://API_CLIENT_ID`
5. Selecciona **Agregar un ámbito**:
   - Nombre del ámbito: `access_as_user`
   - Quién puede dar consentimiento: administradores y usuarios
   - Estado: habilitado
   - Texto de consentimiento: `Acceder a NexoTech en nombre del usuario`

El scope completo será:

```text
api://API_CLIENT_ID/access_as_user
```

## Crear las aplicaciones con Terraform

El proyecto incluye `terraform/entra.tf`. Para usarlo necesitas tener Azure CLI instalado, permisos para registrar aplicaciones y una sesión activa:

```bash
az login
az account set --subscription "TU_SUBSCRIPTION_ID"
az ad signed-in-user show
```

Copia el archivo de variables y completa el tenant:

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
```

En `terraform.tfvars`, deja `gestionar_entra = true` y reemplaza `azure_tenant_id`. Si también vas a desplegar AWS, completa `url_backend`, `cors_origenes` y los valores del authorizer.

Ejecuta:

```bash
terraform init -upgrade
terraform fmt -recursive
terraform plan -out=tfplan
terraform apply tfplan
```

Terraform conserva los registros anteriores y crea `NexoTech Student` como aplicación híbrida con el scope delegado y la plataforma SPA en una sola operación. Esta variante no intenta crear service principals ni conceder consentimiento administrativo, porque esas operaciones requieren roles que normalmente no están disponibles en Azure for Students.

Obtén los valores para configurar Angular y el authorizer:

```bash
terraform output entra_api_client_id
terraform output entra_spa_client_id
terraform output entra_api_scope
terraform output -raw entra_tenant_id
```

Usa esos outputs así:

- `entra_spa_client_id` -> `frontend/src/environments/environment.development.ts` como `msal.clientId`.
- `entra_student_scope` -> `msal.apiScope`.
- `entra_tenant_id` -> `msal.authority` y `AZURE_TENANT_ID`.
- `entra_api_client_id` -> `audiencia` de AWS API Gateway.

El proveedor AzureAD usa las credenciales de Azure CLI. No guardes client secrets ni tokens en `terraform.tfvars`.

## 2. Configurar la SPA dentro del mismo registro

En el registro `NexoTech Student`, entra a **Autenticación > Agregar una plataforma > Aplicación de una sola página** y agrega:
   - URI de redirección: `http://localhost:4200/auth`
   - URI de cierre de sesión: `http://localhost:4200`
No agregues un secreto de cliente. Angular usa el mismo **Application (client) ID** de `NexoTech Student`, y solicita `api://nexotech-student-api/access_as_user` para obtener un token destinado a esa misma aplicación.

## 3. Configurar Angular

Edita `frontend/src/environments/environment.development.ts`:

```ts
msal: {
   clientId: 'API_CLIENT_ID',
  authority: 'https://login.microsoftonline.com/TENANT_ID',
  apiScope: 'api://API_CLIENT_ID/access_as_user',
  redirectUri: 'http://localhost:4200/auth',
  postLogoutRedirectUri: 'http://localhost:4200',
},
```

`TENANT_ID` es el **Directory (tenant) ID** del mismo directorio donde registraste ambas aplicaciones. Angular solicita `access_as_user` para que las llamadas protegidas no puedan hacerse sin autorización.

## 4. Configurar Spring Boot y microservicios

En una terminal macOS/Linux, antes de iniciar cada proceso:

```bash
export SPRING_PROFILES_ACTIVE=azuread
export AZURE_TENANT_ID='TENANT_ID'
```

Después inicia los servicios con sus comandos habituales. Todos deben usar el perfil `azuread`, no `local`:

```bash
cd backend && sh ./mvnw spring-boot:run
cd services/catalog-service && mvn spring-boot:run
cd services/cart-service && mvn spring-boot:run
cd services/order-service && mvn spring-boot:run
cd services/notification-service && mvn spring-boot:run
```

El backend y cada microservicio usan el issuer:

```text
https://login.microsoftonline.com/TENANT_ID/v2.0
```

## 5. Comprobar la configuración

1. Reinicia Angular después de cambiar el environment.
2. Abre `http://localhost:4200`.
3. Entra a **Carrito**, **Compras** o **Perfil**.
4. Completa el inicio de sesión.
5. Comprueba que una llamada protegida responda, por ejemplo:

```bash
curl -i http://localhost:8080/api/me
```

Sin token debe devolver `401`. El catálogo público debe responder sin token:

```bash
curl -i http://localhost:8080/api/public/products
```

Desde Angular, MSAL debe enviar en las operaciones protegidas:

```http
Authorization: Bearer <access_token>
```

## Errores habituales

- `AADSTS50011`: la URI `http://localhost:4200/auth` no coincide exactamente con la registrada.
- `AADSTS65001`: falta consentimiento para el permiso `access_as_user`.
- `401` desde Spring: algún servicio sigue usando el perfil `local` o `TENANT_ID` es incorrecto.
- `invalid audience`: el backend espera la audiencia de `NexoTech API`; revisa `AZURE_API_AUDIENCE`.
- `CORS`: el backend debe conservar `http://localhost:4200` en `app.cors.allowed-origins`.
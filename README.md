# DESARROLLO_CLOUD_NATIVE_I_005D — Capa I (Cloud Native de referencia)

> Proyecto de la asignatura **DESARROLLO_CLOUD_NATIVE_I_005D** (Cloud Native).
> Plataforma de referencia: **SPA Angular + MSAL** → **API Gateway (AWS) con authorizer JWT de Azure AD / Entra ID** → **backend Spring Boot (OAuth2 Resource Server)**.

---

## 1. Stack

| Capa | Tecnología | Carpeta |
|------|------------|---------|
| **Frontend** | Angular 22 (standalone) + `@azure/msal-angular` / `@azure/msal-browser` | `frontend/` |
| **Infraestructura** | Terraform (AWS API Gateway HTTP v2 + authorizer JWT) | `terraform/` |
| **Backend** | Spring Boot 4 + Spring Security OAuth2 Resource Server | `backend/` |

---

## 2. Requisitos previos

- Node.js >= 22 y npm (Angular CLI)
- Java 17 (Temurin) y Maven (usar `./mvnw.cmd`)
- Terraform >= 1.6
- AWS CLI configurado (para Terraform) y cuenta AWS
- Una **app registrada en Azure AD / Entra ID** con:
  - `clientId`, `tenantId` y, si se valida por issuer, el `issuer-uri`:
    `https://login.microsoftonline.com/{tenant-id}/v2.0`
  - Permisos / scope para el backend (Resource Server)

---

## 3. Cómo levantar el proyecto

### 3.1 Backend (Spring Boot)

```bash
cd backend
./mvnw.cmd spring-boot:run
# si solo quieres compilar:  ./mvnw.cmd -DskipTests package
```

Perfiles disponibles:

| Perfil | Uso | Auth |
|--------|-----|------|
| `local` (default) | Desarrollo sin Azure; HS256 con secret local | JWT HS256 |
| `azuread` | Producción real contra Entra ID | JWT RS256 (JWKS) |

Selección de perfil con variable de entorno (dev local no usa Azure):

```bash
# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE = "local"; ./mvnw.cmd spring-boot:run
```

Para el perfil `azuread`, define (sin commitear):

```
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://login.microsoftonline.com/{tenant-id}/v2.0
AZURE_TENANT_ID=...
# (opcional si se usa secret local)
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_SECRET_KEY=<base64>
```

Prueba rápida:

```bash
# Health público (no requiere token)
curl http://localhost:8080/api/public/health

# Ruta protegida (requiere token Bearer). Sin token -> 401
curl -i http://localhost:8080/api/me
```

### 3.2 Frontend (Angular + MSAL)

```bash
cd frontend
npm install
npm start    # -> http://localhost:4200
```

Configura MSAL en `frontend/src/environments/environment.development.ts`:

```ts
export const environment = {
  production: false,
  msal: {
    clientId: 'TU_CLIENT_ID',
    authority: 'https://login.microsoftonline.com/TU_TENANT_ID',
    redirectUri: 'http://localhost:4200/auth',
  },
  backendApiUrl: 'http://localhost:8080',
};
```

Rutas:
- `/` → pública
- `/perfil` → **protegida** por `MsalGuard`
- `/auth` → callback de redirección MSAL (`MsalRedirectComponent`)

### 3.3 Terraform (infraestructura AWS)

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars   # y rellena URL del backend + issuer
cp variables.tf                                # (añade secret/issuer si usas HS256 local)

terraform init
terraform fmt -recursive
terraform validate
terraform plan -out=tfplan
terraform apply tfplan
```

> Estado: por defecto local (`terraform.tfstate`). Para equipo se recomienda un
> backend remoto (S3 + DynamoDB).

---

## 4. Flujo de autenticación (MSAL → Azure AD → API Gateway → Backend)

```
Angular (MSAL) --loginRedirect--> Azure AD/Entra ID
   <- redirect con code ->  MSAL exchangea por access_token
Angular --Authorization: Bearer <JWT>--> API Gateway (authorizer JWT valida firma+iss+aud)
   --proxy--> Backend Spring Boot (OAuth2 Resource Server valida JWT de nuevo)
```

- **API Gateway**: valida el JWT con el authorizer JWT (JWKS del issuer) y comprueba audiencia.
- **Backend**: Spring Security `oauth2ResourceServer().jwt(...)` valida de forma independiente el token (defensa en profundidad).

---

## 5. Calidad / Validación

- Frontend: `npm run build` (dev) o `ng build` (prod).
- Backend: `./mvnw.cmd test` (incluye test end-to-end con JWT HS256 local que arranca el contexto completo).
- Terraform: `terraform fmt -check -recursive` y `terraform validate`.

---

## 6. Hitos / Fases

- [x] **Fase 1 — Scaffold y autorización**: Angular+MSAL, Spring Boot Resource Server, Terraform API Gateway. *(commit raíz del repo)*
- [ ] **Fase 2 — Despliegue real**: `terraform apply` contra cuenta AWS con credenciales reales y endpoints de `developer`.
- [ ] **Fase 3 — CI/CD**: GitHub Actions que corran test + build por rama (GitFlow: feature → develop → main).

---

## 7. Licencia / Autor

Proyecto académico — asignatura DESARROLLO_CLOUD_NATIVE_I_005D.

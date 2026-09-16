# NexoTech — DESARROLLO_CLOUD_NATIVE_I_005D

Tienda cloud-native de demostración para la asignatura **Desarrollo Cloud Native I (005D)**.

Flujo real en producción (demo):

**Angular + MSAL** → **Microsoft Entra ID** → **AWS API Gateway (JWT Authorizer)** → **BFF Spring Boot** → **8 microservicios**

Documentación ampliada de negocio y diseño: [CONTEXTO_PROYECTO.md](CONTEXTO_PROYECTO.md) · Guión de presentación: [GUIÓN_PRESENTACIÓN_EP2.md](GUIÓN_PRESENTACIÓN_EP2.md) · Entra: [MICROSOFT_ENTRA_CONFIG.md](MICROSOFT_ENTRA_CONFIG.md)

---

## Estado actual (septiembre 2026)

La demo **está desplegada en AWS Academy** (`us-east-1`) para una ventana corta (~48 h). Login con cuentas del **tenant institucional Duoc**.

| Qué | URL |
|-----|-----|
| **Frontend (HTTPS, demo)** | https://neutral-lover-realized-collaboration.trycloudflare.com |
| **API Gateway (stage `dev`)** | https://ourd5f7qr1.execute-api.us-east-1.amazonaws.com/dev |
| **BFF directo (debug)** | http://18.211.7.130:8080 |
| EC2 / nginx (origen) | `i-098478353e2ca4634` · EIP `18.211.7.130` · también `https://18-211-7-130.sslip.io` |

> **URL pública a usar:** el tunnel de Cloudflare. En redes con filtro (p. ej. Duoc) `*.sslip.io` responde *Web Filter Violation* (403). El tunnel apunta a nginx en la EC2.
>
> Si se reinicia el lab o muere `cloudflared`, la URL `*.trycloudflare.com` puede cambiar: hay que regenerar el tunnel, actualizar Entra redirects y `environment.ts`. Tras la presentación: `cd terraform && terraform destroy`.

### Smoke checks rápidos

```bash
# Catálogo público → 200
curl -s "https://ourd5f7qr1.execute-api.us-east-1.amazonaws.com/dev/api/public/products" | head -c 120

# Privado sin token → 401
curl -si "https://ourd5f7qr1.execute-api.us-east-1.amazonaws.com/dev/api/me" | head -n 1

# Frontend (tunnel) → 200
curl -sI "https://neutral-lover-realized-collaboration.trycloudflare.com/" | head -n 1
```

---

## 1. Qué es el proyecto (visión general)

NexoTech es una tienda de hardware (celulares, notebooks, consolas, etc.) pensada para **demostrar identidad + API Management + microservicios**, no para producción comercial.

- Catálogo **público** (sin sesión).
- Carrito, compras, avisos, perfil y admin **con login Microsoft**.
- Pago **simulado** (sin pasarela real).
- Persistencia **H2 en memoria** en la demo cloud (sin RDS, para ahorrar créditos).
- Tres capas de seguridad JWT: **Entra firma** → **API Gateway filtra** → **BFF/MS validan de nuevo**.

### Quién puede iniciar sesión

- Cualquier usuario del **tenant Duoc** (`AzureADMyOrg`).
- Rol **Administración** en el BFF solo para correos listados en `APP_ADMIN_USERS` (hoy: `fe.ardiles@duocuc.cl`).
- Cuentas personales / otros tenants **no** entran (app single-tenant).

---

## 2. Arquitectura

```
Usuario
  │
  ▼
┌─────────────────────────────────────┐
│  SPA Angular (nginx + HTTPS)        │
│  MSAL: loginRedirect + PKCE         │
│  MsalGuard / MsalInterceptor        │
└──────────────┬──────────────────────┘
               │ login
               ▼
┌─────────────────────────────────────┐
│  Microsoft Entra ID (tenant Duoc)   │
│  App: NexoTech Demo SPA             │
│  Scope: api://nexotech-demo-api/    │
│         access_as_user              │
└──────────────┬──────────────────────┘
               │ access_token (JWT)
               ▼
┌─────────────────────────────────────┐
│  AWS API Gateway HTTP v2            │
│  • GET /api/public/{proxy+}  (sin JWT)
│  • OPTIONS /{proxy+}         (CORS)
│  • ANY /{proxy+}             (JWT Authorizer)
└──────────────┬──────────────────────┘
               │ proxy HTTP
               ▼
┌─────────────────────────────────────┐
│  EC2 — BFF Spring Boot :8080        │
│  OAuth2 Resource Server (azuread)   │
└──────────────┬──────────────────────┘
               │
     ┌─────────┼─────────┬──────────┐
     ▼         ▼         ▼          ▼
  :8081     :8082     :8083 …    :8088
 catálogo   carrito   órdenes    reseñas
            …inventario, pago, despacho, avisos
```

### Por qué el frontend está en la EC2 (+ tunnel)

AWS Academy **restringe** CloudFront y ciertas operaciones S3 desde Terraform. La SPA se sirve con **nginx + Let’s Encrypt** en la misma EC2 (`sslip.io` sobre la Elastic IP).

Para redes que bloquean `sslip.io`, la demo pública sale por **Cloudflare Tunnel** (`cloudflared` en la EC2 → `*.trycloudflare.com`), sin cambiar el origen nginx.

---

## 3. Stack y estructura del repo

| Capa | Tecnología | Carpeta |
|------|------------|---------|
| Frontend | Angular 22 (standalone) + MSAL | `frontend/` |
| BFF | Spring Boot 4 + OAuth2 Resource Server | `backend/` |
| Microservicios | Spring Boot 4 (8 servicios) | `services/*-service/` |
| Infra | Terraform (API GW + EC2 + EIP + SG) | `terraform/` |
| Deploy | Scripts bash (local + EC2) | `scripts/` |

| Servicio | Puerto | Responsabilidad |
|----------|--------|-----------------|
| `backend` (BFF) | 8080 | Agregación, authz, `/api/me`, admin |
| `catalog-service` | 8081 | Productos y stock base |
| `cart-service` | 8082 | Carrito por usuario |
| `order-service` | 8083 | Checkout y compras |
| `notification-service` | 8084 | Avisos |
| `inventory-service` | 8085 | Reservas de inventario |
| `payment-service` | 8086 | Pago simulado |
| `shipping-service` | 8087 | Cotización / despacho |
| `review-service` | 8088 | Reseñas |

Rutas Angular relevantes:

| Ruta | Acceso |
|------|--------|
| `/`, `/catalogo`, `/catalogo/:id` | Públicas |
| `/auth` | Callback MSAL |
| `/carrito`, `/compras`, `/notificaciones`, `/perfil` | `MsalGuard` |
| `/admin` | `MsalGuard` + `ROLE_ADMIN` en BFF |
| `/acceso-denegado` | Login cancelado / fallido |

---

## 4. Microsoft Entra ID (cómo está configurado ahora)

Valores de la **demo cloud** (`frontend/src/environments/environment.ts`):

| Parámetro | Valor |
|-----------|--------|
| Tenant | `72fd0b5a-8a6a-4cff-89f6-bde961f7e250` |
| Client ID (SPA+API) | `f7d7e5dd-430c-4adb-9348-9ecd974b220c` |
| Authority | `https://login.microsoftonline.com/72fd0b5a-…` |
| API URI / scope | `api://nexotech-demo-api/access_as_user` |
| Redirect URI | `https://neutral-lover-realized-collaboration.trycloudflare.com/auth` |
| Logout URI | `https://neutral-lover-realized-collaboration.trycloudflare.com` |

Notas importantes:

- Flujo **Authorization Code + PKCE** (sin client secret en Angular).
- La app antigua `NexoTech Student` (`097bfd84-…`) sigue documentada para local; la **producción demo** usa **NexoTech Demo SPA** porque el portal Duoc no permite editar redirects de la app anterior.
- En Entra también pueden quedar redirects de `sslip.io` y `localhost` como respaldo.
- API Gateway y BFF aceptan audiencia `f7d7e5dd-…` y `api://nexotech-demo-api`.
- CORS del Gateway/BFF incluye el origen del tunnel Cloudflare.

Para desarrollo local, usa `environment.development.ts` (localhost + scope `nexotech-student-api` si esa app tiene redirect `http://localhost:4200/auth`).

---

## 5. AWS (cómo está montado)

Definido en Terraform (`terraform/`):

1. **API Gateway HTTP v2** con CORS hacia el tunnel Cloudflare, sslip.io y localhost.
2. **JWT Authorizer** contra issuer Entra v2 y audiencias de la Demo SPA.
3. **Rutas**
   - `GET /api/public/{proxy+}` → sin JWT (catálogo anónimo).
   - `OPTIONS /{proxy+}` → sin JWT (preflight CORS).
   - `ANY /{proxy+}` → JWT obligatorio.
4. **EC2** `t3.medium` + Elastic IP + security group (22/80/443/8080).
5. User-data / scripts instalan Java; jars y SPA se publican vía scripts / SSM.

Outputs útiles:

```bash
cd terraform
terraform output
# frontend_url, url_deploy, ec2_public_ip, ec2_instance_id, …
```

---

## 6. Cómo levantar en local

### Requisitos

- Node.js ≥ 22, npm  
- Java 17, Maven Wrapper (`./mvnw` / `./mvnw.cmd`)  
- (Opcional) Terraform ≥ 1.6, AWS CLI, cuenta AWS Academy  

### 6.1 Backend + microservicios

```bash
# Perfil local (JWT HS256 de desarrollo, sin Entra)
scripts/start-all.sh

# Perfil azuread (valida JWTs reales de Entra)
scripts/start-all.sh azuread

scripts/stop-all.sh   # logs en .run/logs/
```

Variables típicas para `azuread` (no commitear secretos):

```bash
export SPRING_PROFILES_ACTIVE=azuread
export AZURE_TENANT_ID=72fd0b5a-8a6a-4cff-89f6-bde961f7e250
export AZURE_API_AUDIENCE=f7d7e5dd-430c-4adb-9348-9ecd974b220c,api://nexotech-demo-api
export APP_ADMIN_USERS=fe.ardiles@duocuc.cl
export APP_CORS_ORIGINS=http://localhost:4200
```

Pruebas:

```bash
curl http://localhost:8080/api/public/products
curl -i http://localhost:8080/api/me          # → 401 sin token
```

### 6.2 Frontend

```bash
cd frontend
npm install
npm start    # http://localhost:4200
```

Ajusta `frontend/src/environments/environment.development.ts` si cambias clientId / scope / redirects.

### 6.3 Build de producción (SPA)

```bash
cd frontend
npm run build -- --configuration=production
# salida: frontend/dist/frontend/browser
```

---

## 7. Cómo desplegar / actualizar en AWS

Orden típico:

```bash
# 1) Infra
cd terraform
terraform init
terraform plan -out=tfplan
terraform apply tfplan

# 2) Compilar jars (backend + services)
# 3) Subir y arrancar en EC2
../scripts/deploy-ec2.sh

# 4) Build SPA + publicar en nginx (script o zip vía S3/SSM)
../scripts/deploy-frontend.sh
```

En Academy, si SSH con `vockey.pem` falla, el despliegue se hace con **AWS Systems Manager (SSM)** + bucket S3 auxiliar para jars/SPA.

Tras cambiar IP o dominio:

1. Actualiza redirects SPA en Entra.  
2. Actualiza `environment.ts` (`redirectUri`, `backendApiUrl`).  
3. Rebuild + redeploy frontend.  
4. Revisa `APP_CORS_ORIGINS` en la EC2.

Para apagar costos:

```bash
cd terraform && terraform destroy
```

---

## 8. Flujo de autenticación (detalle)

```
1. Usuario → SPA → MSAL loginRedirect (prompt select_account)
2. Entra autentica (PKCE) y redirige a /auth
3. MSAL guarda sesión y obtiene access_token para api://nexotech-demo-api/access_as_user
4. MsalInterceptor agrega Authorization: Bearer <JWT> a las llamadas al API
5. API Gateway valida firma (JWKS), issuer y audience
6. BFF (y cada MS con perfil azuread) vuelve a validar el JWT
7. BFF orquesta carrito / checkout / notificaciones, etc.
```

| Caso | HTTP esperado |
|------|----------------|
| `GET /api/public/products` sin token | **200** |
| `GET /api/me` sin token | **401** |
| Token basura | **401** |
| Token MSAL válido | **200** |
| `/api/admin/**` sin admin | **403** |

---

## 9. Cumplimiento rúbrica EP2 (resumen)

Peso del curso (guión): **MSAL 60% · BFF / API Gateway 40%**.

| Indicador | Evidencia en este repo / demo |
|-----------|-------------------------------|
| MSAL configurado | `frontend/src/app/auth/` + environments |
| Login / logout real | Botón en la SPA desplegada |
| `MsalGuard` / `MsalInterceptor` / `/auth` | Rutas privadas + Bearer en Network |
| Scope propio | `access_as_user` |
| BFF Resource Server | Perfil `azuread`, iss/aud/firma/exp |
| 401 / 403 / 200 | Demostrable vía Gateway o BFF |
| API Manager (Gateway) | Terraform aplicado: rutas + JWT + CORS |
| FE + BE en cloud | tunnel Cloudflare → EC2/nginx + execute-api |

---

## 10. Calidad

```bash
# Frontend
cd frontend && npm run build -- --configuration=production

# Backend (desde cada módulo o con el wrapper del servicio)
cd backend && ./mvnw test

# Terraform
cd terraform && terraform fmt -check -recursive && terraform validate
```

---

## 11. Hitos

- [x] Scaffold Angular + MSAL + BFF + 8 MS  
- [x] Entra ID (login real, PKCE, claims, admin por lista)  
- [x] Terraform API Gateway JWT + CORS  
- [x] Despliegue AWS Academy (EC2 + nginx HTTPS + Gateway)  
- [x] Demo EP1/EP2 en vivo (catálogo público + login + 401/200)  
- [ ] CI/CD (GitHub Actions)  
- [ ] Persistencia gestionada (RDS) si la demo deja de ser efímera  

---

## 12. Equipo / licencia

Proyecto académico — **DESARROLLO_CLOUD_NATIVE_I_005D**.  
Repositorio: `GmontPCGamer/DESARROLLO_CLOUD_NATIVE_I_005D` · rama `main`.

# 🎤 GUIÓN DE PRESENTACIÓN — EP2 DESARROLLO CLOUD NATIVE I (005D)

> **Formato:** 8–10 min · Demostración en vivo + sustentación técnica
> **Peso:** MSAL 60% · BFF / API Gateway 40% (igual que API Manager)

---

## ⏱️ PLAN DE TIEMPO (10 min)

| Min | Bloque | Pantalla |
|---|---|---|
| 0:00–0:30 | Contexto + arquitectura | README / diagrama |
| 0:30–3:30 | **FRONTEND + MSAL (60%)** | `localhost:4200` |
| 3:30–6:30 | **BFF valida JWT (40%)** | `localhost:8080` + Postman |
| 6:30–8:30 | API Manager / Gateway (Terraform) | `terraform/` + AWS |
| 8:30–10:00 | Cierre + rúbrica cruzada | Check list |

---

## 📖 BLOQUE 0 — CONTEXTO (30 seg)

> *"Sistema cloud-native de autenticación en tres capas: un frontend Angular que integra MSAL contra Microsoft Entra ID, un backend Spring Boot que actúa como BFF y valida el token exactamente igual que lo haría un API Gateway con JWT Authorizer, y una capa Terraform que replica esa validación en AWS API Gateway para producción."*

**Acción:** abrir `CONTEXTO.md` → mostrar el `stack:`.

---

## 🚀 BLOQUE 1 — FRONTEND + MSAL (60%) → 3 min

### 1a. Levantar y verificar
```powershell
cd "C:\Users\carva\Desktop\Cloud_native\Proyecto_estructura_V1\frontend"
npm start
# http://localhost:4200 → 200 (compilado correctamente)
```

### 1b. Login MSAL (la parte más pesada de la rúbrica)
> *"MSAL está configurado con `@azure/msal-browser` y `@azure/msal-angular`. Al pulsar 'Iniciar sesión', MSAL redirige al tenant de Entra ID, obtiene el token y vuelve por el callback configurado."*

**Acción (en pantalla):**
1. Click **Iniciar sesión** → redirige al login de Microsoft
2. Introduce credenciales → vuelve a la app **logueado**
3. Mostrar en navbar: **nombre + email** (claims visibles del token)

### 1c. Demostrar seguridad MSAL (arguments de la rúbrica)
| Argumento | Demostración |
|---|---|
| `MsalGuard` | Ruta `/perfil` protegida → sin login te redirige al login |
| `MsalInterceptor` | En DevTools → Network → la petición al backend lleva `Authorization: Bearer <token>` automáticamente |
| `MsalRedirectComponent` | El callback `/auth` procesa la respuesta de Entra |
| Claims / roles | En `app.component` o consola: mostrar `account.idTokenClaims` → email, nombre, `roles` |

```typescript
// environment.development.ts — MSAL real
msalConfig: {
  clientId: '<CLIENT_ID_REAL>',
  authority: 'https://login.microsoftonline.com/<TENANT_ID>',
  redirectUri: 'http://localhost:4200/auth',
  scopes: ['api://<BACKEND_CLIENT_ID>/read', 'api://<BACKEND_CLIENT_ID>/write']
}
```

> Copiar: "MSAL gestiona el token en el `MsalService`; el interceptor lo inyecta y el guard protege las rutas. Así toda llamada al BFF viaja autenticada contra Entra ID."

---

## 🛡️ BLOQUE 2 — BFF VALIDA JWT (40%) → 3 min

> *"El backend (BFF) valida el JWT de la misma forma que lo haría el API Gateway con un JWT Authorizer: verifica emisor, audiencia, firma y vigencia al generar el secret usando el mismo perfil."*

### Demo en vivo (Postman o terminal):
```powershell
# 1) Health público → 200 (sin token)
curl http://localhost:8080/api/public/health
# → 200 {"status":"UP",...}

# 2) Ruta protegida SIN token → 401 (rechazado como API Manager)
curl -i http://localhost:8080/api/me
# → 401

# 3) Ruta protegida con JWT INVÁLIDO/firma rota → 403/401
curl -i http://localhost:8080/api/me -H "Authorization: Bearer token.falso.invalido"
# → 403 (firma inválida / issuer no coincide)

# 4) Ruta protegida con JWT VÁLIDO (emitido por MSAL) → 200 + claims
curl -i http://localhost:8080/api/me -H "Authorization: Bearer <jwt-real-de-msal>"
# → 200 {"sub":"...","email":"...","roles":[...]}
```

**Resultados que debe ver el profesor:**
| Caso | HTTP esperado | Evidencia de |
|---|---|---|
| Sin token | **401** | Rechazo por defecto (protegido) |
| Token inválido | **403/401** | Valida firma + issuer |
| Token válido MSAL | **200** | Acepta y expone claims |

> Copiar: "El BFF replica la validación del API Manager: sin firma o con issuer distinto, rechaza con 401/403. Solo los tokens firmados por nuestro tenant y con la audiencia correcta pasan. De esta manera la capa de gateway podrá en producción perder autoridad en la nube y mantener el mismo criterio."

---

## ☁️ BLOQUE 3 — API MANAGER / GATEWAY (Terraform) → 2 min

> *"La misma validación que vimos en el BFF está replicada en infraestructura como código con Terraform: un API Gateway que actúa como API Manager y rechaza/acepta JWT con el mismo criterio."*

**Acción:** abrir `terraform/main.tf` (o el archivo del gateway) y mostrar:
1. **Rutas/endpoints** del API (rutas reales)
2. **CORS** configurado (origins desde el frontend)
3. **Authorizer JWT** (valida token contra Entra ID igual que BFF)

```hcl
# (lo que se ve en pantalla — resumen de terraform/main.tf)
resource "aws_api_gateway_authorizer" "jwt" {
  type = "JWT"
  identity_source = "method.request.header.Authorization"
  ...
}
```

```powershell
cd "C:\Users\carva\Desktop\Cloud_native\Proyecto_estructura_V1\terraform"
terraform init && terraform fmt && terraform validate
# → Success! (validación de sintaxis/estructura — no requiere Apply)
```

> Copiar: "El API Manager (API Gateway) hace exactamente lo mismo que vimos en el backend: autorizar con JWT, definir rutas y aplicar CORS — el componente que en producción centraliza la seguridad de la API."

*Nota: el `terraform apply` requiere credenciales AWS vigentes; está listo para cuando se tenga la cuenta activa (por eso el `fmt`+`validate` ya se demostró sin depender de la nube).*

---

## 🧾 BLOQUE 4 — CIERRE Y RÚBRICA (1:30)

**Cierre (mensaje de 30 seg):**
> *"En resumen: el MSAL en el frontend autentica al usuario contra Entra ID y protege las rutas, el interceptor inyecta el token, y el BFF—igual que el API Manager—valida ese token en cada petición. Donde hoy está el BFF, en producción estará el API Gateway con el mismo JWT authorizer. El 60% del MSAL está demostrado con el login real y el 40% del backend con las respuestas 401/403/200."*

### Check-list de presentación (marca antes de salir)
- [ ] `npm start` funcionando en `:4200` ✓
- [ ] Backend en `:8080` (health 200) ✓
- [ ] Postman/terminal con los 4 curls listos ✓
- [ ] DevTools abierto (pestaña Network) para mostrar el Bearer token ✓
- [ ] `CONTEXTO.md` + `README.md` abiertos ✓
- [ ] Tarjetas de respaldo para preguntas ✓

---

## ❓ PREGUNTAS PROBABLES DEL PROFESOR (y respuestas)

**¿Dónde se valida el token?**
> *"En dos capas. La primera en el BFF (Spring Boot) con `oauth2ResourceServer`, validando issuer, audiencia, firma y vigencia. La segunda, en producción, en el API Gateway con JWT Authorizer — misma lógica, mismo criterio."*

**¿Qué pasa si no hay token?** → *"401. La ruta está protegida por `authorizeHttpRequests().anyRequest().authenticated()`."*

**¿Cómo sabe el frontend que el usuario está logueado?** → *"MSAL mantiene la sesión en `sessionStorage` y el `MsalService` expone el `account`; los guards consultan esa instancia antes de cargar rutas protegidas."*

**¿Qué protege el MSAL en la SPA?** → *"La UI y las rutas. No es la frontera de seguridad real: la confianza está en el token firmado que el BFF valida."*

**¿Por qué CORS?** → *"El API Gateway autoriza los orígenes del frontend (`http://localhost:4200`) para permitir la SPA hacer llamadas entre dominios."*

---

> Repositorio: `GmontPCGamer/DESARROLLO_CLOUD_NATIVE_I_005D` · Rama `main` (demo) / `develop` (desarrollo) · GitFlow

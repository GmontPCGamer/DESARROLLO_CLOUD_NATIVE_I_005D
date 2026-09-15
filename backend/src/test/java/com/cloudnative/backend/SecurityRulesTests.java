package com.cloudnative.backend;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Reglas de autorización del BFF: público sin token, privado con JWT válido,
 * 401 sin credencial, 403 sin rol. No requiere microservicios levantados.
 */
@ActiveProfiles("local")
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesTests {

  @Autowired
  MockMvc mockMvc;

  @Test
  void healthEsPublico() throws Exception {
    mockMvc.perform(get("/api/public/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void rutaPrivadaSinTokenResponde401ConProblemDetail() throws Exception {
    mockMvc.perform(get("/api/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().exists("WWW-Authenticate"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").exists());
  }

  @Test
  void rutaPrivadaConTokenInvalidoResponde401() throws Exception {
    mockMvc.perform(get("/api/me").header("Authorization", "Bearer token.falso.invalido"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void meDevuelveClaimsConTokenValido() throws Exception {
    mockMvc.perform(get("/api/me").with(jwt()
            .jwt(jwt -> jwt
                .subject("usuario-123")
                .claim("name", "Usuario Demo")
                .claim("preferred_username", "demo@duocuc.cl")
                .claim("scp", "access_as_user"))
            .authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sub").value("usuario-123"))
        .andExpect(jsonPath("$.scopes[0]").value("access_as_user"))
        .andExpect(jsonPath("$.isAdmin").value(false));
  }

  @Test
  void adminSinRolResponde403() throws Exception {
    mockMvc.perform(get("/api/admin/inventory").with(jwt()
            .authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }
}

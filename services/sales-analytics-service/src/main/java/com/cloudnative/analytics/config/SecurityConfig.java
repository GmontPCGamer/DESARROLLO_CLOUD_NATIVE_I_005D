package com.cloudnative.analytics.config;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
  @Value("${app.security.require-scope:false}") boolean requireScope;
  @Value("${APP_ADMIN_USERS:}") String adminUsers;
  @Bean SecurityFilterChain filter(HttpSecurity http)throws Exception {
    Set<String> admins=new HashSet<>();
    Arrays.stream(adminUsers.split(",")).map(String::trim).map(String::toLowerCase).forEach(admins::add);
    var converter=new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt->{
      List<GrantedAuthority> roles=new ArrayList<>(new JwtGrantedAuthoritiesConverter().convert(jwt));
      List<String> configured=jwt.getClaimAsStringList("roles");
      if(configured!=null)configured.forEach(r->roles.add(new SimpleGrantedAuthority("ROLE_"+r.toUpperCase(Locale.ROOT))));
      for(String claim:List.of("preferred_username","email","sub")) {
        String value=jwt.getClaimAsString(claim);
        if(value!=null&&admins.contains(value.toLowerCase(Locale.ROOT)))roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
      }
      return roles;
    });
    return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a->{
          a.requestMatchers(HttpMethod.OPTIONS,"/**").permitAll();
          a.requestMatchers("/api/public/**").permitAll();
          a.anyRequest().access((auth,context)-> {
            var names=auth.get().getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
            return new AuthorizationDecision(names.contains("ROLE_ADMIN")&&(!requireScope||names.contains("SCOPE_access_as_user")));
          });
        }).oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter))).build();
  }
}

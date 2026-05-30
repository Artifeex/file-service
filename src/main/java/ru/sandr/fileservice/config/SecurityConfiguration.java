package ru.sandr.fileservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.sandr.fileservice.handler.FilterChainExceptionHandler;

import java.util.List;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            FilterChainExceptionHandler filterChainExceptionHandler,
            CorsProperties corsProperties
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource(corsProperties)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/fs/swagger-ui.html",
                                "/fs/swagger-ui/**",
                                "/fs/v3/api-docs",
                                "/fs/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/prometheus").permitAll()
                        .requestMatchers("/actuator/info").permitAll()
                        .requestMatchers("/fs/api/v1/**").authenticated()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth -> oauth
                        // Передаем наш кастомный конвертер сюда
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(filterChainExceptionHandler)
                        .accessDeniedHandler(filterChainExceptionHandler)
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Location"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // 1. Указываем имя клейма из вашего токена (например, "roles")
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");

        // 2. Настраиваем префикс. Он будет добавлен к тому, что лежит внутри roles claim. Причем
        // Вариант мы обязаны его задать(хотя бы пустым), т.к. иначе будет добавлен префикс по дефолту SCOPE_
        // Если в токене роли лежат как ["ADMIN", "USER"], ставим префикс "ROLE_".
        // Если в токене они УЖЕ с префиксом (["ROLE_ADMIN"]), передаем пустую строку "".
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }
}

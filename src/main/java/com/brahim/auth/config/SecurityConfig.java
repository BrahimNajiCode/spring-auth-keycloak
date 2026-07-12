package com.brahim.auth.config;

import com.brahim.auth.security.JwtAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity // Activates @PreAuthorize / @PostAuthorize / @Secured
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final JwtAuthenticationEntyPoint AuthenticationEntyPoint; //401
    private final JwtAccessDeniedHandler accessDeniedHandler ; //403
    private final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http
                // CSRF disabled (JWT is immune; CSRF only matters for cookies)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth->auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // preflight request
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // CORS: configure your origins explicitly in production
                .cors(cors->cors.configure(http)); // delegates to CorsConfigurationSource bean
        //  Stateless: no HttpSession
        http
                .sessionManagement(sess->sess.
                        sessionCreationPolicy(STATELESS));

        http
                .oauth2ResourceServer(oauth->{
                    oauth.jwt(jwt->jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                            .authenticationEntryPoint(AuthenticationEntyPoint) //401 handler
                            .accessDeniedHandler(accessDeniedHandler); // 403 handler
                });
        return http.build();
    }

}

package com.devanshbhosale.channelhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.devanshbhosale.channelhub.repository.UserRepository;
import com.devanshbhosale.channelhub.security.DemoAdminReadOnlyFilter;
import com.devanshbhosale.channelhub.security.JwtAuthFilter;
import com.devanshbhosale.channelhub.security.JwtService;

/**
 * Stateless JWT security. Filter order in the chain:
 * JwtAuthFilter (before UsernamePasswordAuthenticationFilter) populates the
 * SecurityContext, then DemoAdminReadOnlyFilter runs right after it and blocks
 * demo-admin writes. Both are plain classes instantiated here so they are not
 * double-registered as servlet filter beans.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public SecurityConfig(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtService, userRepository);
        DemoAdminReadOnlyFilter demoAdminReadOnlyFilter = new DemoAdminReadOnlyFilter();

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/actuator/health", "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/", "/assets/**", "/index.html", "/fonts/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                // Missing/invalid token must be 401 (not Spring's default 403) — the SPA's
                // global interceptor relies on 401 to redirect to /login. 403 stays reserved
                // for "authenticated but forbidden" (roles, demo-admin writes).
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(demoAdminReadOnlyFilter, JwtAuthFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}

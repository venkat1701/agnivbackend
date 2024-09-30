package io.github.venkat1701.agnivbackend.security.config;

import io.github.venkat1701.agnivbackend.security.jwt.JwtValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * This class provides configuration for the security of the application.
 *
 * <p>It generates a {@link SecurityFilterChain} that is used to secure the application.
 * The filter chain sets up the session management to use stateless sessions, and
 * configures the authorization of HTTP requests. Requests to "/api/**" and "/chat/query/**"
 * are authenticated.
 *
 * <p>It also provides a {@link PasswordEncoder} that is used to encode passwords.
 * The password encoder is a bcrypt password encoder.
 *
 * <p>Finally, it provides a {@link CorsConfigurationSource} that is used to configure
 * the CORS settings for the application. The CORS configuration is set to allow all
 * origins, and to allow the following headers: "Accept", "Accept-Language", "Content-Language",
 * "Content-Type", "Authorization", "Pragma", "Cache-Control", "Origin", "X-Requested-With",
 * "If-Modified-Since", "If-None-Match".
 */
@Configuration
public class AppConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .sessionManagement(management->management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests-> {
                    requests.requestMatchers("/api/**").authenticated();
                    requests.requestMatchers("/chat/query/**").authenticated();
                    requests.anyRequest().permitAll();
                })
                .addFilterBefore(new JwtValidator(), BasicAuthenticationFilter.class)
                .csrf(csrf->csrf.disable())
                .cors(cors->cors.configurationSource(new CorsConfigurationSource() {
                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                        CorsConfiguration configuration = new CorsConfiguration();
                        configuration.setAllowCredentials(true);
                        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000/**"));
                        configuration.setAllowedHeaders(List.of("*"));
                        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                        configuration.setExposedHeaders(List.of("Authorization"));
                        configuration.setMaxAge(3600L);
                        return configuration;
                    }
                }))
                .httpBasic(Customizer.withDefaults())
                .formLogin(Customizer.withDefaults())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
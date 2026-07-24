package com.dreamoutbox.ams.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/h2-console/**", "/error").permitAll()
                .requestMatchers("/users/**").hasAnyRole("LANDLORD", "MANAGER")
                .requestMatchers("/tenants/**").hasAnyRole("LANDLORD", "MANAGER")
                .requestMatchers("/rooms/**").hasAnyRole("LANDLORD", "MANAGER")
                .requestMatchers("/contracts/**").hasAnyRole("LANDLORD", "MANAGER")
                .requestMatchers("/Announcements/**").hasAnyRole("LANDLORD", "MANAGER", "ACCOUNTANT", "TECHNICIAN", "TENANT")
                .requestMatchers("/tickets/**").hasAnyRole("LANDLORD", "MANAGER", "ACCOUNTANT", "TECHNICIAN", "TENANT")
                .requestMatchers("/landlord/**").hasAnyRole("LANDLORD", "MANAGER")
                .requestMatchers("/manager/**").hasRole("MANAGER")
                .requestMatchers("/accountant/**").hasAnyRole("ACCOUNTANT", "MANAGER", "LANDLORD")
                .requestMatchers("/technician/**").hasAnyRole("TECHNICIAN", "MANAGER", "LANDLORD")
                .requestMatchers("/tenant/**").hasRole("TENANT")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(customAuthenticationSuccessHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            var authorities = authentication.getAuthorities();
            String redirectUrl = "/users";
            for (var authority : authorities) {
                String role = authority.getAuthority();
                if (role.equals("ROLE_MANAGER") || role.equals("ROLE_LANDLORD")) {
                    redirectUrl = "/users";
                    break;
                } else if (role.equals("ROLE_ACCOUNTANT")) {
                    redirectUrl = "/accountant/dashboard";
                    break;
                } else if (role.equals("ROLE_TECHNICIAN")) {
                    redirectUrl = "/technician/dashboard";
                    break;
                } else if (role.equals("ROLE_TENANT")) {
                    redirectUrl = "/tenant/dashboard";
                    break;
                }
            }
            response.sendRedirect(redirectUrl);
        };
    }
}

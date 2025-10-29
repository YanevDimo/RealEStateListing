package app.config;

import app.entity.User;
import app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Optional;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
                Optional<User> user = userRepository.findByEmail(email);
                if (user.isEmpty()) {
                    throw new UsernameNotFoundException("User not found with email: " + email);
                }
                
                User userEntity = user.get();
                return org.springframework.security.core.userdetails.User.builder()
                        .username(userEntity.getEmail())
                        .password(userEntity.getPasswordHash())
                        .roles(userEntity.getRole().name())
                        .disabled(!userEntity.getIsActive())
                        .build();
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(matcher -> matcher
                // Public endpoints  
                .requestMatchers("/", "/properties", "/properties/**", "/about", "/contact").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**", "/styles.css").permitAll()
                // Authentication endpoints
                .requestMatchers("/auth/login", "/auth/register").permitAll()
                // Public agents list page - be very explicit
                .requestMatchers("/agents").permitAll()
                .requestMatchers("/agents/list").permitAll()
                .requestMatchers("/agents/detail").permitAll()
                .requestMatchers("/test-agents").permitAll()
                // Agent registration endpoint (must come before /agent/**)
                .requestMatchers("/agent/register").permitAll()
                // Admin endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Agent management endpoints - require AGENT or ADMIN role
                .requestMatchers("/agent/dashboard", "/agent/properties/**", "/agent/profile/**").hasAnyRole("AGENT", "ADMIN")
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .successHandler(customAuthenticationSuccessHandler)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );
            // CSRF protection is enabled by default in Spring Security
            // Thymeleaf automatically includes CSRF tokens in forms using th:action

        return http.build();
    }
}

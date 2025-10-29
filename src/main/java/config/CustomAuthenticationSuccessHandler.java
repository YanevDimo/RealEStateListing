package config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;


@Component
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      Authentication authentication) throws IOException, ServletException {
        
        log.debug("Authentication successful for user: {}", authentication.getName());
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            log.debug("User has role: {}", role);
            
            if ("ROLE_ADMIN".equals(role)) {
                log.debug("Redirecting ADMIN to agent dashboard");
                response.sendRedirect("/agent/dashboard");
                return;
            } else if ("ROLE_AGENT".equals(role)) {
                log.debug("Redirecting AGENT to dashboard");
                response.sendRedirect("/agent/dashboard");
                return;
            } else if ("ROLE_USER".equals(role)) {
                log.debug("Redirecting USER to home page");
                response.sendRedirect("/");
                return;
            }
        }

        log.debug("No specific role found, redirecting to home page");
        response.sendRedirect("/");
    }
}

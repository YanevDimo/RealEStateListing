package app.controller;

import app.dto.UserRegistrationDto;
import app.entity.User;
import app.entity.UserRole;
import app.exception.InvalidRoleException;
import app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(java.util.UUID.randomUUID())
                .email("test@example.com")
                .name("Test User")
                .role(UserRole.USER)
                .isActive(true)
                .build();
    }

    @Test
    void testRegisterUser_Success() throws Exception {
        when(userService.userExistsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userService.saveUser(any(User.class))).thenReturn(testUser);

        mockMvc.perform(post("/auth/register")
                        .param("name", "New User")
                        .param("email", "new@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "USER")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(userService, times(1)).saveUser(any(User.class));
    }

    @Test
    void testRegisterUser_PasswordMismatch() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("name", "New User")
                        .param("email", "new@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "different")
                        .param("role", "USER")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    void testRegisterUser_EmailExists() throws Exception {
        when(userService.userExistsByEmail("existing@example.com")).thenReturn(true);

        mockMvc.perform(post("/auth/register")
                        .param("name", "New User")
                        .param("email", "existing@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "USER")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    void testRegisterUser_InvalidRole() throws Exception {
        when(userService.userExistsByEmail("new@example.com")).thenReturn(false);

        mockMvc.perform(post("/auth/register")
                        .param("name", "New User")
                        .param("email", "new@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "INVALID")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    void testRegisterUser_Exception() throws Exception {
        when(userService.userExistsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userService.saveUser(any(User.class))).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/auth/register")
                        .param("name", "New User")
                        .param("email", "new@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "USER")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/register"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}


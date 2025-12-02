package app.controller;


import app.entity.User;
import app.entity.UserRole;
import app.service.InquiryService;
import app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private InquiryService inquiryService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User testUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .name("Test User")
                .role(UserRole.USER)
                .isActive(true)
                .build();
    }

    @Test
    void testChangeUserRole_Success() throws Exception {
        doNothing().when(userService).changeUserRole(eq(userId), eq(UserRole.ADMIN));

        mockMvc.perform(post("/admin/users/{id}/role", userId)
                        .param("role", "ADMIN")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(userService, times(1)).changeUserRole(userId, UserRole.ADMIN);
    }

    @Test
    void testChangeUserRole_InvalidRole() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/role", userId)
                        .param("role", "INVALID")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testChangeUserRole_Exception() throws Exception {
        doThrow(new RuntimeException("Error")).when(userService).changeUserRole(any(), any());

        mockMvc.perform(post("/admin/users/{id}/role", userId)
                        .param("role", "ADMIN")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testActivateUser_Success() throws Exception {
        doNothing().when(userService).activateUser(userId);

        mockMvc.perform(post("/admin/users/{id}/activate", userId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(userService, times(1)).activateUser(userId);
    }

    @Test
    void testActivateUser_Exception() throws Exception {
        doThrow(new RuntimeException("Error")).when(userService).activateUser(userId);

        mockMvc.perform(post("/admin/users/{id}/activate", userId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testDeactivateUser_Success() throws Exception {
        doNothing().when(userService).deactivateUser(userId);

        mockMvc.perform(post("/admin/users/{id}/deactivate", userId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(userService, times(1)).deactivateUser(userId);
    }

    @Test
    void testDeactivateUser_Exception() throws Exception {
        doThrow(new RuntimeException("Error")).when(userService).deactivateUser(userId);

        mockMvc.perform(post("/admin/users/{id}/deactivate", userId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));
    }

}


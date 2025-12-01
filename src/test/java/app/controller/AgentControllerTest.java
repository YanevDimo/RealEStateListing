package app.controller;

import app.client.PropertyServiceClient;
import app.dto.AgentRegistrationDto;
import app.dto.InquiryUpdateDto;
import app.dto.PropertyDto;
import app.entity.Agent;
import app.entity.City;
import app.entity.Inquiry;
import app.entity.InquiryStatus;
import app.entity.PropertyType;
import app.entity.User;
import app.entity.UserRole;
import app.exception.AgentNotFoundException;
import app.exception.UserNotFoundException;
import app.service.*;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AgentController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentRegistrationService agentRegistrationService;

    @MockitoBean
    private AgentService agentService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PropertyServiceClient propertyServiceClient;

    @MockitoBean
    private PropertyUtilityService propertyUtilityService;

    @MockitoBean
    private CityService cityService;

    @MockitoBean
    private PropertyTypeService propertyTypeService;

    @MockitoBean
    private FileUploadService fileUploadService;

    @MockitoBean
    private InquiryService inquiryService;

    private Agent testAgent;
    private User testUser;
    private PropertyDto testProperty;
    private UUID agentId;
    private UUID userId;
    private UUID propertyId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        agentId = UUID.randomUUID();
        propertyId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("agent@example.com")
                .name("Test Agent")
                .role(UserRole.AGENT)
                .isActive(true)
                .build();

        testAgent = Agent.builder()
                .id(agentId)
                .user(testUser)
                .licenseNumber("LIC123")
                .build();

        testProperty = PropertyDto.builder()
                .id(propertyId)
                .title("Test Property")
                .price(new BigDecimal("100000"))
                .agentId(agentId)
                .build();
    }

    @Test
    void testShowAgentRegistrationForm() throws Exception {
        mockMvc.perform(get("/agent/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/agent-register"))
                .andExpect(model().attributeExists("agentRegistrationDto"));
    }

    @Test
    void testRegisterAgent_Success() throws Exception {
        AgentRegistrationDto dto = AgentRegistrationDto.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .confirmPassword("password123")
                .licenseNumber("LIC123")
                .build();

        when(fileUploadService.uploadFiles(anyList())).thenReturn(List.of());
        when(agentRegistrationService.createAgentWithProfile(any(AgentRegistrationDto.class), any()))
                .thenReturn(testAgent);

        mockMvc.perform(post("/agent/register")
                        .param("name", dto.getName())
                        .param("email", dto.getEmail())
                        .param("password", dto.getPassword())
                        .param("confirmPassword", dto.getConfirmPassword())
                        .param("licenseNumber", dto.getLicenseNumber())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void testRegisterAgent_PasswordMismatch() throws Exception {
        mockMvc.perform(post("/agent/register")
                        .param("name", "John Doe")
                        .param("email", "john@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "different")
                        .param("licenseNumber", "LIC123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/agent-register"));
    }

}


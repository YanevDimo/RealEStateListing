package app.controller;

import app.client.PropertyServiceClient;
import app.dto.InquiryDto;
import app.dto.PropertyDto;
import app.entity.Agent;
import app.entity.User;
import app.entity.UserRole;
import app.service.AgentService;
import app.service.InquiryService;
import app.service.PropertyEnrichmentService;
import app.service.PropertyUtilityService;
import app.service.SearchService;
import app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HomeController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PropertyServiceClient propertyServiceClient;

    @MockitoBean
    private AgentService agentService;

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PropertyUtilityService propertyUtilityService;

    @MockitoBean
    private PropertyEnrichmentService propertyEnrichmentService;

    @MockitoBean
    private InquiryService inquiryService;

    private PropertyDto testProperty;
    private Agent testAgent;
    private UUID propertyId;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        propertyId = UUID.randomUUID();
        agentId = UUID.randomUUID();

        testProperty = PropertyDto.builder()
                .id(propertyId)
                .title("Test Property")
                .price(new BigDecimal("100000"))
                .build();

        testAgent = Agent.builder()
                .id(agentId)
                .build();
    }

    @Test
    void testHome_Success() throws Exception {
        when(searchService.getFeaturedProperties()).thenReturn(List.of(testProperty));
        when(searchService.getAvailablePropertyTypes()).thenReturn(List.of("Apartment", "House"));
        when(searchService.getAvailableCities()).thenReturn(List.of("Sofia", "Plovdiv"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("featuredProperties"))
                .andExpect(model().attributeExists("propertyTypes"))
                .andExpect(model().attributeExists("cities"));

        verify(searchService, times(1)).getFeaturedProperties();
        verify(searchService, times(1)).getAvailablePropertyTypes();
        verify(searchService, times(1)).getAvailableCities();
    }

    @Test
    void testHome_ExceptionHandling() throws Exception {
        when(searchService.getFeaturedProperties()).thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("featuredProperties", List.of()))
                .andExpect(model().attribute("propertyTypes", List.of()))
                .andExpect(model().attribute("cities", List.of()));
    }

    @Test
    void testProperties_NoFilters() throws Exception {
        when(propertyUtilityService.getAllProperties()).thenReturn(List.of(testProperty));
        when(searchService.getAvailablePropertyTypes()).thenReturn(List.of("Apartment"));
        when(searchService.getAvailableCities()).thenReturn(List.of("Sofia"));
        when(propertyEnrichmentService.enrichPropertiesWithNames(anyList())).thenReturn(List.of(testProperty));

        mockMvc.perform(get("/properties"))
                .andExpect(status().isOk())
                .andExpect(view().name("properties/list"))
                .andExpect(model().attributeExists("properties"));

        verify(propertyUtilityService, times(1)).getAllProperties();
    }

    @Test
    void testProperties_WithSearch() throws Exception {
        when(searchService.searchPropertiesByText("test")).thenReturn(List.of(testProperty));
        when(searchService.getAvailablePropertyTypes()).thenReturn(List.of("Apartment"));
        when(searchService.getAvailableCities()).thenReturn(List.of("Sofia"));
        when(propertyEnrichmentService.enrichPropertiesWithNames(anyList())).thenReturn(List.of(testProperty));

        mockMvc.perform(get("/properties").param("search", "test"))
                .andExpect(status().isOk())
                .andExpect(view().name("properties/list"))
                .andExpect(model().attribute("search", "test"));

        verify(searchService, times(1)).searchPropertiesByText("test");
    }

    @Test
    void testProperties_WithFilters() throws Exception {
        SearchService.SearchCriteria criteria = new SearchService.SearchCriteria();
        when(searchService.buildSearchCriteria(any(), eq("Sofia"), eq("Apartment"), eq("200000"))).thenReturn(criteria);
        when(searchService.searchProperties(criteria)).thenReturn(List.of(testProperty));
        when(searchService.getAvailablePropertyTypes()).thenReturn(List.of("Apartment"));
        when(searchService.getAvailableCities()).thenReturn(List.of("Sofia"));
        when(propertyEnrichmentService.enrichPropertiesWithNames(anyList())).thenReturn(List.of(testProperty));

        mockMvc.perform(get("/properties")
                        .param("city", "Sofia")
                        .param("type", "Apartment")
                        .param("maxPrice", "200000"))
                .andExpect(status().isOk())
                .andExpect(view().name("properties/list"));

        verify(searchService, times(1)).searchProperties(any());
    }

    @Test
    void testProperties_EmptyResults() throws Exception {
        when(propertyUtilityService.getAllProperties()).thenReturn(List.of());
        when(searchService.getAvailablePropertyTypes()).thenReturn(List.of());
        when(searchService.getAvailableCities()).thenReturn(List.of());

        mockMvc.perform(get("/properties"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("warningMessage"));
    }

    @Test
    void testProperties_ExceptionHandling() throws Exception {
        when(propertyUtilityService.getAllProperties()).thenThrow(new RuntimeException("Service error"));
        when(searchService.getAvailablePropertyTypes()).thenReturn(List.of());
        when(searchService.getAvailableCities()).thenReturn(List.of());

        mockMvc.perform(get("/properties"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void testSubmitInquiry_Success() throws Exception {
        when(propertyServiceClient.getPropertyById(propertyId)).thenReturn(testProperty);
        when(propertyEnrichmentService.enrichPropertyWithNames(testProperty)).thenReturn(testProperty);
        doNothing().when(inquiryService).createInquiry(any(InquiryDto.class), eq(propertyId), any());

        mockMvc.perform(post("/properties/{id}/inquiry", propertyId.toString())
                        .param("contactName", "John Doe")
                        .param("contactEmail", "john@example.com")
                        .param("message", "I'm interested"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/properties/detail?id=" + propertyId));

        verify(inquiryService, times(1)).createInquiry(any(), eq(propertyId), any());
    }

    @Test
    void testSubmitInquiry_PropertyNotFound() throws Exception {
        when(propertyServiceClient.getPropertyById(propertyId)).thenReturn(null);

        mockMvc.perform(post("/properties/{id}/inquiry", propertyId.toString())
                        .param("contactName", "John Doe")
                        .param("contactEmail", "john@example.com")
                        .param("message", "I'm interested"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/properties"));
    }

    @Test
    void testSubmitInquiry_InvalidUUID() throws Exception {
        mockMvc.perform(post("/properties/{id}/inquiry", "invalid")
                        .param("contactName", "John Doe")
                        .param("contactEmail", "john@example.com")
                        .param("message", "I'm interested"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/properties"));
    }

    @Test
    void testAgents_Success() throws Exception {
        when(agentService.findAllAgents()).thenReturn(List.of(testAgent));
        AgentService.AgentListStatistics stats = new AgentService.AgentListStatistics(1, 1, 5.0);
        when(agentService.calculateAgentListStatistics(anyList())).thenReturn(stats);

        mockMvc.perform(get("/agents"))
                .andExpect(status().isOk())
                .andExpect(view().name("agents/list"))
                .andExpect(model().attributeExists("agents"));

        verify(agentService, times(1)).findAllAgents();
    }

    @Test
    void testAgents_EmptyList() throws Exception {
        when(agentService.findAllAgents()).thenReturn(List.of());

        mockMvc.perform(get("/agents"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalAgents", 0));
    }

    @Test
    void testAgentDetail_Success() throws Exception {
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));
        when(propertyUtilityService.getPropertiesByAgent(agentId)).thenReturn(List.of(testProperty));

        mockMvc.perform(get("/agents/detail").param("id", agentId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("agents/detail"))
                .andExpect(model().attributeExists("agent"))
                .andExpect(model().attributeExists("agentProperties"));
    }

    @Test
    void testAgentDetail_NotFound() throws Exception {
        when(agentService.findAgentById(agentId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/agents/detail").param("id", agentId.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Agent not found"));
    }

    @Test
    void testAgentDetail_InvalidUUID() throws Exception {
        mockMvc.perform(get("/agents/detail").param("id", "invalid"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Invalid agent ID"));
    }

    @Test
    void testAbout_Success() throws Exception {
        when(agentService.countAllAgents()).thenReturn(10L);
        when(propertyUtilityService.getAllProperties()).thenReturn(List.of(testProperty, testProperty));

        mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(view().name("about"))
                .andExpect(model().attribute("totalAgents", 10L))
                .andExpect(model().attribute("totalProperties", 2L));
    }

    @Test
    void testAbout_ExceptionHandling() throws Exception {
        when(agentService.countAllAgents()).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalAgents", 50))
                .andExpect(model().attribute("totalProperties", 500));
    }

    @Test
    void testContact() throws Exception {
        mockMvc.perform(get("/contact"))
                .andExpect(status().isOk())
                .andExpect(view().name("contact"));
    }
}


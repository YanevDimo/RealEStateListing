package app.controller.api;

import app.client.PropertyServiceClient;
import app.dto.PropertyDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PropertyRestController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class PropertyRestControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PropertyServiceClient propertyServiceClient;

    private PropertyDto buildSampleProperty(UUID id) {
        return PropertyDto.builder()
                .id(id)
                .title("Cozy Apartment")
                .description("Perfect downtown spot")
                .price(new BigDecimal("120000.00"))
                .agentId(UUID.randomUUID())
                .cityId(UUID.randomUUID())
                .propertyTypeId(UUID.randomUUID())
                .status("AVAILABLE")
                .bedrooms(2)
                .bathrooms(2)
                .squareFeet(850)
                .address("123 Main St")
                .isFeatured(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/properties returns list of properties")
    void testGetAllProperties() throws Exception {
        List<PropertyDto> properties = List.of(buildSampleProperty(UUID.randomUUID()));

        when(propertyServiceClient.getAllProperties(null, null, null, null)).thenReturn(properties);

        mockMvc.perform(get("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Cozy Apartment")));
    }

    @Test
    @DisplayName("GET /api/v1/properties/{id} returns property when found")
    void testGetPropertyById_Success() throws Exception {
        UUID propertyId = UUID.randomUUID();
        PropertyDto property = buildSampleProperty(propertyId);

        when(propertyServiceClient.getPropertyById(propertyId)).thenReturn(property);

        mockMvc.perform(get("/api/v1/properties/{id}", propertyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(propertyId.toString())))
                .andExpect(jsonPath("$.description", is("Perfect downtown spot")));
    }

    @Test
    @DisplayName("GET /api/v1/properties/{id} returns 404 when missing")
    void testGetPropertyById_NotFound() throws Exception {
        UUID propertyId = UUID.randomUUID();
        when(propertyServiceClient.getPropertyById(propertyId)).thenReturn(null);

        mockMvc.perform(get("/api/v1/properties/{id}", propertyId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/properties/featured returns featured properties")
    void testGetFeaturedProperties() throws Exception {
        List<PropertyDto> featured = List.of(buildSampleProperty(UUID.randomUUID()));
        when(propertyServiceClient.getFeaturedProperties()).thenReturn(featured);

        mockMvc.perform(get("/api/v1/properties/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/properties/agent/{agentId}")
    void testGetPropertiesByAgent() throws Exception {
        UUID agentId = UUID.randomUUID();
        when(propertyServiceClient.getPropertiesByAgent(agentId)).thenReturn(List.of(buildSampleProperty(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/properties/agent/{agentId}", agentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/properties/city/{cityId}")
    void testGetPropertiesByCity() throws Exception {
        UUID cityId = UUID.randomUUID();
        when(propertyServiceClient.getPropertiesByCity(cityId)).thenReturn(List.of(buildSampleProperty(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/properties/city/{cityId}", cityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/properties/search returns filtered list")
    void testSearchProperties() throws Exception {
        List<PropertyDto> results = List.of(buildSampleProperty(UUID.randomUUID()));
        when(propertyServiceClient.searchProperties(eq("downtown"), any(), any(), any())).thenReturn(results);

        mockMvc.perform(get("/api/v1/properties/search")
                        .param("search", "downtown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title", is("Cozy Apartment")));
    }

    @Test
    @DisplayName("DELETE /api/v1/properties/{id} returns 204")
    void testDeleteProperty() throws Exception {
        UUID propertyId = UUID.randomUUID();
        doNothing().when(propertyServiceClient).deleteProperty(propertyId);

        mockMvc.perform(delete("/api/v1/properties/{id}", propertyId))
                .andExpect(status().isNoContent());

        verify(propertyServiceClient).deleteProperty(propertyId);
    }
}







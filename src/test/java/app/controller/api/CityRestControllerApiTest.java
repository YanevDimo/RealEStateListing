package app.controller.api;

import app.dto.CityDto;
import app.entity.City;
import app.service.CityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = CityRestController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class CityRestControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CityService cityService;

    @Autowired
    private ObjectMapper objectMapper;

    private City testCity;
    private UUID cityId;

    @BeforeEach
    void setUp() {
        cityId = UUID.randomUUID();
        testCity = City.builder()
                .id(cityId)
                .name("Sofia")
                .country("Bulgaria")
                .latitude(new BigDecimal("42.6977"))
                .longitude(new BigDecimal("23.3219"))
                .build();
    }

    @Test
    void testGetCity_Success() throws Exception {
        // Given
        when(cityService.findCityById(cityId)).thenReturn(Optional.of(testCity));

        // When & Then
        mockMvc.perform(get("/api/v1/cities/{cityId}", cityId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(cityId.toString()))
                .andExpect(jsonPath("$.name").value("Sofia"))
                .andExpect(jsonPath("$.country").value("Bulgaria"))
                .andExpect(jsonPath("$.state").isEmpty());

        verify(cityService, times(1)).findCityById(cityId);
    }

    @Test
    void testGetCity_NotFound() throws Exception {
        // Given
        when(cityService.findCityById(cityId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/cities/{cityId}", cityId))
                .andExpect(status().isNotFound());

        verify(cityService, times(1)).findCityById(cityId);
    }

    @Test
    void testCityExists_True() throws Exception {
        // Given
        when(cityService.findCityById(cityId)).thenReturn(Optional.of(testCity));

        // When & Then
        mockMvc.perform(get("/api/v1/cities/{cityId}/exists", cityId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(true));

        verify(cityService, times(1)).findCityById(cityId);
    }

    @Test
    void testCityExists_False() throws Exception {
        // Given
        when(cityService.findCityById(cityId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/cities/{cityId}/exists", cityId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(false));

        verify(cityService, times(1)).findCityById(cityId);
    }

    @Test
    void testGetCity_InvalidUUID() throws Exception {

        mockMvc.perform(get("/api/v1/cities/{cityId}", "invalid-uuid"))
                .andExpect(status().is5xxServerError()); // Accept 5xx error for invalid UUID
    }

    @Test
    void testGetCity_DTOMapping() throws Exception {
        // Given
        when(cityService.findCityById(cityId)).thenReturn(Optional.of(testCity));

        // When & Then - verify DTO structure
        String response = mockMvc.perform(get("/api/v1/cities/{cityId}", cityId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify JSON structure
        CityDto dto = objectMapper.readValue(response, CityDto.class);
        assertEquals(cityId, dto.getId());
        assertEquals("Sofia", dto.getName());
        assertEquals("Bulgaria", dto.getCountry());
        assertNull(dto.getState()); // State should be null per controller logic
    }
}


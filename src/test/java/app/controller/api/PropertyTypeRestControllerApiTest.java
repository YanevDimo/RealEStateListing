package app.controller.api;

import app.dto.PropertyTypeDto;
import app.entity.PropertyType;
import app.service.PropertyTypeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PropertyTypeRestController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class PropertyTypeRestControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PropertyTypeService propertyTypeService;

    @Autowired
    private ObjectMapper objectMapper;

    private PropertyType testPropertyType;
    private UUID typeId;

    @BeforeEach
    void setUp() {
        typeId = UUID.randomUUID();
        testPropertyType = PropertyType.builder()
                .id(typeId)
                .name("Apartment")
                .description("A residential apartment")
                .build();
    }

    @Test
    void testGetPropertyType_Success() throws Exception {
        when(propertyTypeService.findPropertyTypeById(typeId)).thenReturn(Optional.of(testPropertyType));

        String response = mockMvc.perform(get("/api/v1/property-types/{typeId}", typeId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PropertyTypeDto dto = objectMapper.readValue(response, PropertyTypeDto.class);
        assertEquals(typeId, dto.getId());
        assertEquals("Apartment", dto.getName());
        assertEquals("A residential apartment", dto.getDescription());

        verify(propertyTypeService, times(1)).findPropertyTypeById(typeId);
    }

    @Test
    void testGetPropertyType_NoDescription() throws Exception {
        PropertyType typeNoDesc = PropertyType.builder()
                .id(typeId)
                .name("House")
                .description(null)
                .build();

        when(propertyTypeService.findPropertyTypeById(typeId)).thenReturn(Optional.of(typeNoDesc));

        String response = mockMvc.perform(get("/api/v1/property-types/{typeId}", typeId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PropertyTypeDto dto = objectMapper.readValue(response, PropertyTypeDto.class);
        assertEquals("House", dto.getName());
        assertNull(dto.getDescription());
    }

    @Test
    void testGetPropertyType_NotFound() throws Exception {
        when(propertyTypeService.findPropertyTypeById(typeId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/property-types/{typeId}", typeId))
                .andExpect(status().isNotFound());

        verify(propertyTypeService, times(1)).findPropertyTypeById(typeId);
    }

    @Test
    void testPropertyTypeExists_True() throws Exception {
        when(propertyTypeService.findPropertyTypeById(typeId)).thenReturn(Optional.of(testPropertyType));

        String response = mockMvc.perform(get("/api/v1/property-types/{typeId}/exists", typeId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals("true", response);

        verify(propertyTypeService, times(1)).findPropertyTypeById(typeId);
    }

    @Test
    void testPropertyTypeExists_False() throws Exception {
        when(propertyTypeService.findPropertyTypeById(typeId)).thenReturn(Optional.empty());

        String response = mockMvc.perform(get("/api/v1/property-types/{typeId}/exists", typeId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals("false", response);

        verify(propertyTypeService, times(1)).findPropertyTypeById(typeId);
    }

    @Test
    void testGetPropertyType_InvalidUUID() throws Exception {
        mockMvc.perform(get("/api/v1/property-types/{typeId}", "invalid-uuid"))
                .andExpect(status().is5xxServerError());
    }
}


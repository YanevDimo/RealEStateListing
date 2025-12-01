package app.exception;

import app.controller.api.CityRestController;
import app.entity.City;
import app.service.CityService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(controllers = CityRestController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CityService cityService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testHandleUserNotFoundException() throws Exception {
        UUID cityId = UUID.randomUUID();
        when(cityService.findCityById(cityId)).thenThrow(new UserNotFoundException(cityId));

        mockMvc.perform(get("/api/v1/cities/{id}", cityId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testHandleAgentNotFoundException() throws Exception {
        UUID cityId = UUID.randomUUID();
        when(cityService.findCityById(cityId)).thenThrow(new AgentNotFoundException(cityId));

        mockMvc.perform(get("/api/v1/cities/{id}", cityId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testHandleCityNotFoundException() throws Exception {
        UUID cityId = UUID.randomUUID();
        when(cityService.findCityById(cityId)).thenThrow(new CityNotFoundException(cityId));

        mockMvc.perform(get("/api/v1/cities/{id}", cityId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testHandlePropertyTypeNotFoundException() throws Exception {
        UUID cityId = UUID.randomUUID();
        when(cityService.findCityById(cityId)).thenThrow(new PropertyTypeNotFoundException(cityId));

        mockMvc.perform(get("/api/v1/cities/{id}", cityId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testHandleIllegalArgumentException() throws Exception {
        UUID cityId = UUID.randomUUID();
        when(cityService.findCityById(cityId)).thenThrow(new IllegalArgumentException("Invalid argument"));

        mockMvc.perform(get("/api/v1/cities/{id}", cityId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid input")));
    }

    @Test
    void testHandleNullPointerException() throws Exception {
        UUID cityId = UUID.randomUUID();
        when(cityService.findCityById(cityId)).thenThrow(new NullPointerException("Null value"));

        mockMvc.perform(get("/api/v1/cities/{id}", cityId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testHandleGenericException() throws Exception {
        UUID cityId = UUID.randomUUID();
        when(cityService.findCityById(cityId)).thenThrow(new RuntimeException("Generic error"));

        mockMvc.perform(get("/api/v1/cities/{id}", cityId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void testErrorResponseStructure() throws Exception {
        UUID cityId = UUID.randomUUID();
        when(cityService.findCityById(cityId)).thenThrow(new CityNotFoundException(cityId));

        String response = mockMvc.perform(get("/api/v1/cities/{id}", cityId))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(response.contains("timestamp"));
        assertTrue(response.contains("status"));
        assertTrue(response.contains("error"));
        assertTrue(response.contains("message"));
        assertTrue(response.contains("path"));
    }
}


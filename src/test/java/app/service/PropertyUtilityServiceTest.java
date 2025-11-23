package app.service;

import app.client.PropertyServiceClient;
import app.dto.PropertyDto;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyUtilityServiceTest {

    @Mock
    private PropertyServiceClient propertyServiceClient;

    @InjectMocks
    private PropertyUtilityService propertyUtilityService;

    private PropertyDto activeProperty;
    private PropertyDto inactiveProperty;
    private UUID cityId;
    private UUID agentId;
    private UUID propertyTypeId;

    @BeforeEach
    void setUp() {
        cityId = UUID.randomUUID();
        agentId = UUID.randomUUID();
        propertyTypeId = UUID.randomUUID();

        activeProperty = PropertyDto.builder()
                .id(UUID.randomUUID())
                .title("Active Property")
                .status("ACTIVE")
                .cityId(cityId)
                .agentId(agentId)
                .propertyTypeId(propertyTypeId)
                .price(new BigDecimal("100000"))
                .build();

        inactiveProperty = PropertyDto.builder()
                .id(UUID.randomUUID())
                .title("Inactive Property")
                .status("INACTIVE")
                .cityId(cityId)
                .agentId(agentId)
                .propertyTypeId(propertyTypeId)
                .build();
    }

    @Test
    void testGetAllProperties_Success() {
        // Given
        List<PropertyDto> properties = Arrays.asList(activeProperty, inactiveProperty);
        when(propertyServiceClient.getAllProperties(any(), any(), any(), any()))
                .thenReturn(properties);

        // When
        List<PropertyDto> result = propertyUtilityService.getAllProperties();

        // Then
        assertEquals(2, result.size());
        verify(propertyServiceClient).getAllProperties(null, null, null, null);
    }

    @Test
    void testGetAllProperties_FeignException_ReturnsEmpty() {
        // Given
        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(500);
        when(propertyServiceClient.getAllProperties(any(), any(), any(), any()))
                .thenThrow(feignException);

        // When
        List<PropertyDto> result = propertyUtilityService.getAllProperties();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllProperties_GenericException_ReturnsEmpty() {
        // Given
        when(propertyServiceClient.getAllProperties(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Connection error"));

        // When
        List<PropertyDto> result = propertyUtilityService.getAllProperties();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testIsActiveProperty_Active() {
        // When
        boolean result = propertyUtilityService.isActiveProperty(activeProperty);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsActiveProperty_NullStatus() {
        // Given
        PropertyDto property = PropertyDto.builder()
                .id(UUID.randomUUID())
                .status(null)
                .build();

        // When
        boolean result = propertyUtilityService.isActiveProperty(property);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsActiveProperty_Inactive() {
        // When
        boolean result = propertyUtilityService.isActiveProperty(inactiveProperty);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsActiveProperty_Null() {
        // When
        boolean result = propertyUtilityService.isActiveProperty(null);

        // Then
        assertFalse(result);
    }

    @Test
    void testFilterActiveProperties() {
        // Given
        List<PropertyDto> properties = Arrays.asList(activeProperty, inactiveProperty);

        // When
        List<PropertyDto> result = propertyUtilityService.filterActiveProperties(properties);

        // Then
        assertEquals(1, result.size());
        assertEquals("Active Property", result.get(0).getTitle());
    }

    @Test
    void testFilterActiveProperties_NullList() {
        // When
        List<PropertyDto> result = propertyUtilityService.filterActiveProperties(null);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetActivePropertiesByCityId() {
        // Given
        when(propertyServiceClient.getAllProperties(any(), any(), any(), any()))
                .thenReturn(Arrays.asList(activeProperty, inactiveProperty));

        // When
        List<PropertyDto> result = propertyUtilityService.getActivePropertiesByCityId(cityId);

        // Then
        assertEquals(1, result.size());
        assertEquals(cityId, result.get(0).getCityId());
    }

    @Test
    void testGetActivePropertiesByCityId_NullId() {
        // When
        List<PropertyDto> result = propertyUtilityService.getActivePropertiesByCityId(null);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testHasActiveProperties_True() {
        // Given
        when(propertyServiceClient.getAllProperties(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(activeProperty));

        // When
        boolean result = propertyUtilityService.hasActiveProperties(cityId);

        // Then
        assertTrue(result);
    }

    @Test
    void testHasActiveProperties_False() {
        // Given
        when(propertyServiceClient.getAllProperties(any(), any(), any(), any()))
                .thenReturn(List.of());

        // When
        boolean result = propertyUtilityService.hasActiveProperties(cityId);

        // Then
        assertFalse(result);
    }

    @Test
    void testGetPropertiesByAgent_Success() {
        // Given
        when(propertyServiceClient.getPropertiesByAgent(agentId))
                .thenReturn(Collections.singletonList(activeProperty));

        // When
        List<PropertyDto> result = propertyUtilityService.getPropertiesByAgent(agentId);

        // Then
        assertEquals(1, result.size());
        assertEquals(agentId, result.get(0).getAgentId());
    }

    @Test
    void testGetPropertiesByAgent_FeignException500_Fallback() {
        // Given
        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(500);
        when(propertyServiceClient.getPropertiesByAgent(agentId))
                .thenThrow(feignException);
        when(propertyServiceClient.getAllProperties(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(activeProperty));

        // When
        List<PropertyDto> result = propertyUtilityService.getPropertiesByAgent(agentId);

        // Then
        assertEquals(1, result.size());
        assertEquals(agentId, result.get(0).getAgentId());
    }

    @Test
    void testGetPropertiesByAgent_NullId() {
        // When
        List<PropertyDto> result = propertyUtilityService.getPropertiesByAgent(null);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testCountActivePropertiesByCityId() {
        // Given
        when(propertyServiceClient.getAllProperties(any(), any(), any(), any()))
                .thenReturn(Arrays.asList(activeProperty, inactiveProperty));

        // When
        long result = propertyUtilityService.countActivePropertiesByCityId(cityId);

        // Then
        assertEquals(1, result);
    }

    @Test
    void testAgentHasActiveProperties_True() {
        // Given
        when(propertyServiceClient.getAllProperties(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(activeProperty));

        // When
        boolean result = propertyUtilityService.agentHasActiveProperties(agentId);

        // Then
        assertTrue(result);
    }

    @Test
    void testPropertyTypeHasActiveProperties_True() {
        // Given
        when(propertyServiceClient.getAllProperties(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(activeProperty));

        // When
        boolean result = propertyUtilityService.propertyTypeHasActiveProperties(propertyTypeId);

        // Then
        assertTrue(result);
    }
}

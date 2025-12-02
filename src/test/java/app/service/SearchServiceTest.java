package app.service;

import app.client.PropertyServiceClient;
import app.dto.PropertyDto;
import app.entity.City;
import app.entity.PropertyType;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private PropertyServiceClient propertyServiceClient;

    @Mock
    private CityService cityService;

    @Mock
    private PropertyTypeService propertyTypeService;

    @Mock
    private PropertyUtilityService propertyUtilityService;

    @InjectMocks
    private SearchService searchService;

    private PropertyDto testProperty;
    private City testCity;
    private PropertyType testPropertyType;
    private UUID cityId;
    private UUID propertyTypeId;

    @BeforeEach
    void setUp() {
        cityId = UUID.randomUUID();
        propertyTypeId = UUID.randomUUID();

        testCity = City.builder()
                .id(cityId)
                .name("Sofia")
                .build();

        testPropertyType = PropertyType.builder()
                .id(propertyTypeId)
                .name("Apartment")
                .build();

        testProperty = PropertyDto.builder()
                .id(UUID.randomUUID())
                .title("Test Property")
                .description("Test Description")
                .price(new BigDecimal("100000"))
                .cityId(cityId)
                .propertyTypeId(propertyTypeId)
                .bedrooms(3)
                .bathrooms(2)
                .squareFeet(100)
                .isFeatured(false)
                .status("ACTIVE")
                .build();
    }

    @Test
    void testSearchProperties_WithAllCriteria() {
        testProperty.setIsFeatured(true);
        testProperty.setTitle("Test Property Title");
        
        SearchService.SearchCriteria criteria = new SearchService.SearchCriteria(
                "test", "Sofia", "Apartment",
                new BigDecimal("50000"), new BigDecimal("200000"),
                2, 1, new BigDecimal("50"), new BigDecimal("150"), true
        );

        when(cityService.findCityByNameIgnoreCase("Sofia")).thenReturn(Optional.of(testCity));
        when(propertyTypeService.findPropertyTypeByNameIgnoreCase("Apartment")).thenReturn(Optional.of(testPropertyType));
        when(propertyServiceClient.searchProperties("test", cityId, propertyTypeId, 200000.0))
                .thenReturn(Collections.singletonList(testProperty));

        List<PropertyDto> result = searchService.searchProperties(criteria);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(propertyServiceClient).searchProperties("test", cityId, propertyTypeId, 200000.0);
    }

    @Test
    void testSearchProperties_WithMinPriceFilter() {
        SearchService.SearchCriteria criteria = new SearchService.SearchCriteria();
        criteria.setMinPrice(new BigDecimal("150000"));
        testProperty.setPrice(new BigDecimal("100000"));

        when(propertyServiceClient.searchProperties(null, null, null, null))
                .thenReturn(Collections.singletonList(testProperty));

        List<PropertyDto> result = searchService.searchProperties(criteria);

        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchProperties_WithMaxPriceFilter() {
        SearchService.SearchCriteria criteria = new SearchService.SearchCriteria();
        criteria.setMaxPrice(new BigDecimal("50000"));
        testProperty.setPrice(new BigDecimal("100000"));

        when(propertyServiceClient.searchProperties(null, null, null, 50000.0))
                .thenReturn(Collections.emptyList());

        List<PropertyDto> result = searchService.searchProperties(criteria);

        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchProperties_WithMinBedsFilter() {
        SearchService.SearchCriteria criteria = new SearchService.SearchCriteria();
        criteria.setMinBeds(5);
        testProperty.setBedrooms(3);

        when(propertyServiceClient.searchProperties(null, null, null, null))
                .thenReturn(Collections.singletonList(testProperty));

        List<PropertyDto> result = searchService.searchProperties(criteria);

        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchProperties_WithMinBathsFilter() {
        SearchService.SearchCriteria criteria = new SearchService.SearchCriteria();
        criteria.setMinBaths(3);
        testProperty.setBathrooms(2);

        when(propertyServiceClient.searchProperties(null, null, null, null))
                .thenReturn(Collections.singletonList(testProperty));

        List<PropertyDto> result = searchService.searchProperties(criteria);

        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchProperties_WithMinAreaFilter() {
        SearchService.SearchCriteria criteria = new SearchService.SearchCriteria();
        criteria.setMinArea(new BigDecimal("150"));
        testProperty.setSquareFeet(100);

        when(propertyServiceClient.searchProperties(null, null, null, null))
                .thenReturn(Collections.singletonList(testProperty));

        List<PropertyDto> result = searchService.searchProperties(criteria);

        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchProperties_WithMaxAreaFilter() {
        SearchService.SearchCriteria criteria = new SearchService.SearchCriteria();
        criteria.setMaxArea(new BigDecimal("50"));
        testProperty.setSquareFeet(100);

        when(propertyServiceClient.searchProperties(null, null, null, null))
                .thenReturn(Collections.singletonList(testProperty));

        List<PropertyDto> result = searchService.searchProperties(criteria);

        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchProperties_WithFeaturedFilter() {
        SearchService.SearchCriteria criteria = new SearchService.SearchCriteria();
        criteria.setFeatured(true);
        testProperty.setIsFeatured(false);

        when(propertyServiceClient.searchProperties(null, null, null, null))
                .thenReturn(Collections.singletonList(testProperty));

        List<PropertyDto> result = searchService.searchProperties(criteria);

        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchProperties_FeignException_500_Fallback() {
        SearchService.SearchCriteria criteria = new SearchService.SearchCriteria();
        criteria.setSearchTerm("test");

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(500);

        when(propertyServiceClient.searchProperties("test", null, null, null))
                .thenThrow(feignException);
        when(propertyServiceClient.getAllProperties(null, null, null, null))
                .thenReturn(Collections.singletonList(testProperty));

        List<PropertyDto> result = searchService.searchProperties(criteria);

        assertNotNull(result);
        verify(propertyServiceClient).getAllProperties(null, null, null, null);
    }

    @Test
    void testSearchProperties_FeignException_OtherStatus() {
        SearchService.SearchCriteria criteria = new SearchService.SearchCriteria();

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(404);

        when(propertyServiceClient.searchProperties(null, null, null, null))
                .thenThrow(feignException);

        List<PropertyDto> result = searchService.searchProperties(criteria);

        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchProperties_GenericException() {
        SearchService.SearchCriteria criteria = new SearchService.SearchCriteria();

        when(propertyServiceClient.searchProperties(null, null, null, null))
                .thenThrow(new RuntimeException("Service error"));

        List<PropertyDto> result = searchService.searchProperties(criteria);

        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchProperties_WithPagination() {
        SearchService.SearchCriteria criteria = new SearchService.SearchCriteria();
        Pageable pageable = PageRequest.of(0, 10);

        when(propertyServiceClient.searchProperties(null, null, null, null))
                .thenReturn(Collections.singletonList(testProperty));

        Page<PropertyDto> result = searchService.searchProperties(criteria, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void testSearchPropertiesByText() {
        when(propertyServiceClient.searchProperties("test", null, null, null))
                .thenReturn(Collections.singletonList(testProperty));

        List<PropertyDto> result = searchService.searchPropertiesByText("test");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(propertyServiceClient).searchProperties("test", null, null, null);
    }

    @Test
    void testSearchPropertiesByText_FeignException_500() {
        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(500);

        when(propertyServiceClient.searchProperties("test", null, null, null))
                .thenThrow(feignException);

        List<PropertyDto> result = searchService.searchPropertiesByText("test");

        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchPropertiesByText_GenericException() {
        when(propertyServiceClient.searchProperties("test", null, null, null))
                .thenThrow(new RuntimeException("Error"));

        List<PropertyDto> result = searchService.searchPropertiesByText("test");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetFeaturedProperties() {
        when(propertyServiceClient.getFeaturedProperties())
                .thenReturn(Collections.singletonList(testProperty));

        List<PropertyDto> result = searchService.getFeaturedProperties();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(propertyServiceClient).getFeaturedProperties();
    }

    @Test
    void testGetFeaturedProperties_Exception() {
        when(propertyServiceClient.getFeaturedProperties())
                .thenThrow(new RuntimeException("Error"));

        List<PropertyDto> result = searchService.getFeaturedProperties();

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPropertiesByCityName() {
        when(cityService.findCityByNameIgnoreCase("Sofia")).thenReturn(Optional.of(testCity));
        when(propertyServiceClient.getPropertiesByCity(cityId))
                .thenReturn(Collections.singletonList(testProperty));

        List<PropertyDto> result = searchService.getPropertiesByCityName("Sofia");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetPropertiesByCityName_CityNotFound() {
        when(cityService.findCityByNameIgnoreCase("Unknown")).thenReturn(Optional.empty());

        List<PropertyDto> result = searchService.getPropertiesByCityName("Unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPropertiesByCityName_Exception() {
        when(cityService.findCityByNameIgnoreCase("Sofia")).thenReturn(Optional.of(testCity));
        when(propertyServiceClient.getPropertiesByCity(cityId))
                .thenThrow(new RuntimeException("Error"));

        List<PropertyDto> result = searchService.getPropertiesByCityName("Sofia");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPropertiesByPropertyTypeName() {
        when(propertyTypeService.findPropertyTypeByNameIgnoreCase("Apartment")).thenReturn(Optional.of(testPropertyType));
        when(propertyServiceClient.getAllProperties(null, null, propertyTypeId, null))
                .thenReturn(Collections.singletonList(testProperty));

        List<PropertyDto> result = searchService.getPropertiesByPropertyTypeName("Apartment");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetPropertiesByPropertyTypeName_TypeNotFound() {
        when(propertyTypeService.findPropertyTypeByNameIgnoreCase("Unknown")).thenReturn(Optional.empty());

        List<PropertyDto> result = searchService.getPropertiesByPropertyTypeName("Unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPropertiesByPropertyTypeName_Exception() {
        when(propertyTypeService.findPropertyTypeByNameIgnoreCase("Apartment")).thenReturn(Optional.of(testPropertyType));
        when(propertyServiceClient.getAllProperties(null, null, propertyTypeId, null))
                .thenThrow(new RuntimeException("Error"));

        List<PropertyDto> result = searchService.getPropertiesByPropertyTypeName("Apartment");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPropertiesByPriceRange() {
        when(propertyServiceClient.getAllProperties(null, null, null, 200000.0))
                .thenReturn(Collections.singletonList(testProperty));

        List<PropertyDto> result = searchService.getPropertiesByPriceRange(
                new BigDecimal("50000"), new BigDecimal("200000"));

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetPropertiesByPriceRange_WithMinPrice() {
        PropertyDto expensiveProperty = PropertyDto.builder()
                .price(new BigDecimal("300000"))
                .build();
        PropertyDto cheapProperty = PropertyDto.builder()
                .price(new BigDecimal("50000"))
                .build();
        PropertyDto validProperty = PropertyDto.builder()
                .price(new BigDecimal("150000"))
                .build();

        when(propertyServiceClient.getAllProperties(null, null, null, 200000.0))
                .thenReturn(Arrays.asList(cheapProperty, validProperty));

        List<PropertyDto> result = searchService.getPropertiesByPriceRange(
                new BigDecimal("100000"), new BigDecimal("200000"));

        assertEquals(1, result.size());
        assertEquals(validProperty, result.get(0));
    }

    @Test
    void testGetPropertiesByPriceRange_Exception() {
        when(propertyServiceClient.getAllProperties(null, null, null, 200000.0))
                .thenThrow(new RuntimeException("Error"));

        List<PropertyDto> result = searchService.getPropertiesByPriceRange(
                new BigDecimal("50000"), new BigDecimal("200000"));

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPropertiesByBeds() {
        when(propertyUtilityService.getAllProperties())
                .thenReturn(Collections.singletonList(testProperty));

        List<PropertyDto> result = searchService.getPropertiesByBeds(2);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetPropertiesByBeds_NoMatch() {
        testProperty.setBedrooms(1);
        when(propertyUtilityService.getAllProperties())
                .thenReturn(Collections.singletonList(testProperty));

        List<PropertyDto> result = searchService.getPropertiesByBeds(3);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAvailableCities() {
        when(cityService.findAllCities())
                .thenReturn(Arrays.asList(testCity));

        List<String> result = searchService.getAvailableCities();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Sofia", result.get(0));
    }

    @Test
    void testGetAvailablePropertyTypes() {
        when(propertyTypeService.findAllPropertyTypes())
                .thenReturn(Arrays.asList(testPropertyType));

        List<String> result = searchService.getAvailablePropertyTypes();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Apartment", result.get(0));
    }

    @Test
    void testBuildSearchCriteria() {
        SearchService.SearchCriteria criteria = searchService.buildSearchCriteria(
                "test", "Sofia", "Apartment", "200000");

        assertNotNull(criteria);
        assertEquals("Sofia", criteria.getCityName());
        assertEquals("Apartment", criteria.getPropertyTypeName());
        assertEquals(new BigDecimal("200000"), criteria.getMaxPrice());
    }

    @Test
    void testBuildSearchCriteria_InvalidPrice() {
        SearchService.SearchCriteria criteria = searchService.buildSearchCriteria(
                "test", "Sofia", "Apartment", "invalid");

        assertNotNull(criteria);
        assertNull(criteria.getMaxPrice());
    }

    @Test
    void testSearchCriteria_Constructor() {
        SearchService.SearchCriteria criteria = new SearchService.SearchCriteria(
                "term", "city", "type",
                new BigDecimal("100"), new BigDecimal("200"),
                2, 1, new BigDecimal("50"), new BigDecimal("150"), true);

        assertEquals("term", criteria.getSearchTerm());
        assertEquals("city", criteria.getCityName());
        assertEquals("type", criteria.getPropertyTypeName());
        assertEquals(new BigDecimal("100"), criteria.getMinPrice());
        assertEquals(new BigDecimal("200"), criteria.getMaxPrice());
        assertEquals(2, criteria.getMinBeds());
        assertEquals(1, criteria.getMinBaths());
        assertEquals(new BigDecimal("50"), criteria.getMinArea());
        assertEquals(new BigDecimal("150"), criteria.getMaxArea());
        assertTrue(criteria.getFeatured());
    }

    @Test
    void testSearchCriteria_ToString() {
        SearchService.SearchCriteria criteria = new SearchService.SearchCriteria();
        criteria.setSearchTerm("test");

        String result = criteria.toString();

        assertNotNull(result);
        assertTrue(result.contains("test"));
    }
}


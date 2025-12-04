package app.service;

import app.entity.City;
import app.repository.CityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CityServiceTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private PropertyUtilityService propertyUtilityService;

    @InjectMocks
    private CityService cityService;

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
    void testFindAllCities() {
        // Given
        List<City> cities = Collections.singletonList(testCity);
        when(cityRepository.findAll()).thenReturn(cities);

        // When
        List<City> result = cityService.findAllCities();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Sofia", result.get(0).getName());
        verify(cityRepository, times(1)).findAll();
    }

    @Test
    void testFindCityById_Found() {
        // Given
        when(cityRepository.findById(cityId)).thenReturn(Optional.of(testCity));

        // When
        Optional<City> result = cityService.findCityById(cityId);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Sofia", result.get().getName());
        assertEquals(cityId, result.get().getId());
        verify(cityRepository, times(1)).findById(cityId);
    }

    @Test
    void testFindCityById_NotFound() {
        // Given
        when(cityRepository.findById(cityId)).thenReturn(Optional.empty());

        // When
        Optional<City> result = cityService.findCityById(cityId);

        // Then
        assertFalse(result.isPresent());
        verify(cityRepository, times(1)).findById(cityId);
    }

    @Test
    void testSaveCity() {
        // Given
        when(cityRepository.save(any(City.class))).thenReturn(testCity);

        // When
        City result = cityService.saveCity(testCity);

        // Then
        assertNotNull(result);
        assertEquals("Sofia", result.getName());
        verify(cityRepository, times(1)).save(testCity);
    }

    @Test
    void testFindCityByName() {
        // Given
        when(cityRepository.findByName("Sofia")).thenReturn(Optional.of(testCity));

        // When
        Optional<City> result = cityService.findCityByName("Sofia");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Sofia", result.get().getName());
        verify(cityRepository, times(1)).findByName("Sofia");
    }

    @Test
    void testCityExistsByName() {
        // Given
        when(cityRepository.existsByName("Sofia")).thenReturn(true);

        // When
        boolean result = cityService.cityExistsByName("Sofia");

        // Then
        assertTrue(result);
        verify(cityRepository, times(1)).existsByName("Sofia");
    }

    @Test
    void testCreateCity_Success() {
        // Given
        when(cityRepository.existsByName("Sofia")).thenReturn(false);
        when(cityRepository.save(any(City.class))).thenReturn(testCity);

        // When
        City result = cityService.createCity("Sofia", "Bulgaria", 
                new BigDecimal("42.6977"), new BigDecimal("23.3219"));

        // Then
        assertNotNull(result);
        verify(cityRepository).existsByName("Sofia");
        verify(cityRepository).save(any(City.class));
    }

    @Test
    void testCreateCity_DuplicateName_ThrowsException() {
        // Given
        when(cityRepository.existsByName("Sofia")).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            cityService.createCity("Sofia", "Bulgaria", null, null));
        
        assertTrue(exception.getMessage().contains("already exists"));
        verify(cityRepository, never()).save(any(City.class));
    }

    @Test
    void testGetOrCreateCityByName_Existing() {
        // Given
        when(cityRepository.findByName("sofia")).thenReturn(Optional.of(testCity));

        // When
        City result = cityService.getOrCreateCityByName("Sofia", "Bulgaria");

        // Then
        assertEquals("Sofia", result.getName());
        verify(cityRepository, never()).save(any());
    }

    @Test
    void testGetOrCreateCityByName_New() {
        // Given
        when(cityRepository.findByName("sofia")).thenReturn(Optional.empty());
        when(cityRepository.existsByName("Sofia")).thenReturn(false);
        when(cityRepository.save(any(City.class))).thenReturn(testCity);

        // When
        City result = cityService.getOrCreateCityByName("Sofia", "Bulgaria");

        // Then
        assertNotNull(result);
        verify(cityRepository).save(any(City.class));
    }

    @Test
    void testDeleteCity() {
        // Given
        doNothing().when(cityRepository).deleteById(cityId);

        // When
        cityService.deleteCity(cityId);

        // Then
        verify(cityRepository, times(1)).deleteById(cityId);
    }

    @Test
    void testFindCitiesByCountry() {
        // Given
        City city1 = City.builder().name("Sofia").country("Bulgaria").build();
        City city2 = City.builder().name("Plovdiv").country("Bulgaria").build();
        List<City> cities = Arrays.asList(city1, city2);
        when(cityRepository.findByCountry("Bulgaria")).thenReturn(cities);

        // When
        List<City> result = cityService.findCitiesByCountry("Bulgaria");

        // Then
        assertEquals(2, result.size());
        verify(cityRepository, times(1)).findByCountry("Bulgaria");
    }

    @Test
    void testSearchCitiesByName() {
        // Given
        City city1 = City.builder().name("Sofia").country("Bulgaria").build();
        City city2 = City.builder().name("Plovdiv").country("Bulgaria").build();
        City city3 = City.builder().name("Paris").country("France").build();
        List<City> allCities = Arrays.asList(city1, city2, city3);
        when(cityRepository.findAll()).thenReturn(allCities);

        // When
        List<City> result = cityService.searchCitiesByName("ov");

        // Then
        assertEquals(1, result.size());
        assertEquals("Plovdiv", result.get(0).getName());
    }
}









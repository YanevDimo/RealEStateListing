package app.service;

import app.entity.City;
import app.repository.CityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;



@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("h2")
class CityServiceIntegrationTest {

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private PropertyUtilityService propertyUtilityService;

    private CityService cityService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public PropertyUtilityService propertyUtilityService() {
            PropertyUtilityService mock = org.mockito.Mockito.mock(PropertyUtilityService.class);
            org.mockito.Mockito.when(mock.hasActiveProperties(org.mockito.ArgumentMatchers.any(UUID.class)))
                    .thenReturn(false);
            org.mockito.Mockito.when(mock.countActivePropertiesByCityId(org.mockito.ArgumentMatchers.any(UUID.class)))
                    .thenReturn(0L);
            return mock;
        }
    }

    private City testCity;

    @BeforeEach
    void setUp() {
        // Initialize service manually since @DataJpaTest doesn't load services
        cityService = new CityService(cityRepository, propertyUtilityService);
        
        // Clear database before each test
        cityRepository.deleteAll();
        
        // Create test city
        testCity = City.builder()
                .name("Sofia")
                .country("Bulgaria")
                .latitude(new BigDecimal("42.6977"))
                .longitude(new BigDecimal("23.3219"))
                .build();
    }

    @Test
    void testSaveAndFindCity() {
        // When
        City savedCity = cityService.saveCity(testCity);

        // Then
        assertNotNull(savedCity.getId());
        assertEquals("Sofia", savedCity.getName());
        assertEquals("Bulgaria", savedCity.getCountry());

        // When
        Optional<City> found = cityService.findCityById(savedCity.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals("Sofia", found.get().getName());
    }

    @Test
    void testFindCityByName() {
        // Given
        City savedCity = cityService.saveCity(testCity);

        // When
        Optional<City> found = cityService.findCityByName("Sofia");

        // Then
        assertTrue(found.isPresent());
        assertEquals(savedCity.getId(), found.get().getId());
    }

    @Test
    void testFindCityByNameIgnoreCase() {

        City cityWithLowercase = City.builder()
                .name("sofia")
                .country("Bulgaria")
                .build();
        City savedCity = cityService.saveCity(cityWithLowercase);

        // When
        Optional<City> found = cityService.findCityByNameIgnoreCase("SOFIA");

        // Then
        assertTrue(found.isPresent());
        assertEquals(savedCity.getId(), found.get().getId());
        assertEquals("sofia", found.get().getName());
    }

    @Test
    void testFindCitiesByCountry() {
        // Given
        City city1 = City.builder().name("Sofia").country("Bulgaria").build();
        City city2 = City.builder().name("Plovdiv").country("Bulgaria").build();
        City city3 = City.builder().name("Paris").country("France").build();
        
        cityService.saveCity(city1);
        cityService.saveCity(city2);
        cityService.saveCity(city3);

        // When
        List<City> bulgarianCities = cityService.findCitiesByCountry("Bulgaria");

        // Then
        assertEquals(2, bulgarianCities.size());
        assertTrue(bulgarianCities.stream().allMatch(c -> "Bulgaria".equals(c.getCountry())));
    }

    @Test
    void testCityExistsByName() {
        // Given
        cityService.saveCity(testCity);

        // When
        boolean exists = cityService.cityExistsByName("Sofia");

        // Then
        assertTrue(exists);
    }

    @Test
    void testCityExistsByName_NotExists() {
        // When
        boolean exists = cityService.cityExistsByName("NonExistent");

        // Then
        assertFalse(exists);
    }

    @Test
    void testCreateCity_Success() {
        // When
        City created = cityService.createCity("Varna", "Bulgaria", 
                new BigDecimal("43.2141"), new BigDecimal("27.9147"));

        // Then
        assertNotNull(created.getId());
        assertEquals("Varna", created.getName());
        assertEquals("Bulgaria", created.getCountry());
        

        Optional<City> found = cityRepository.findById(created.getId());
        assertTrue(found.isPresent());
    }

    @Test
    void testCreateCity_DuplicateName_ThrowsException() {
        // Given
        cityService.saveCity(testCity);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            cityService.createCity("Sofia", "Bulgaria", null, null));
        
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void testUpdateCityCoordinates() {
        // Given
        City savedCity = cityService.saveCity(testCity);
        UUID cityId = savedCity.getId();

        // When
        BigDecimal newLat = new BigDecimal("42.7000");
        BigDecimal newLon = new BigDecimal("23.3000");
        City updated = cityService.updateCityCoordinates(cityId, newLat, newLon);

        // Then
        assertEquals(newLat, updated.getLatitude());
        assertEquals(newLon, updated.getLongitude());
        

        Optional<City> found = cityRepository.findById(cityId);
        assertTrue(found.isPresent());
        assertEquals(newLat, found.get().getLatitude());
    }

    @Test
    void testUpdateCityCoordinates_NotFound_ThrowsException() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            cityService.updateCityCoordinates(nonExistentId, 
                new BigDecimal("42.0"), new BigDecimal("23.0")));
    }

    @Test
    void testGetOrCreateCityByName_Existing() {

        City cityWithLowercase = City.builder()
                .name("sofia")
                .country("Bulgaria")
                .build();
        City savedCity = cityService.saveCity(cityWithLowercase);
        UUID savedCityId = savedCity.getId();

        // When
        City result = cityService.getOrCreateCityByName("SOFIA", "Bulgaria");

        // Then
        assertEquals(savedCityId, result.getId());
        assertEquals("sofia", result.getName());
        assertEquals(1, cityRepository.count());
    }

    @Test
    void testGetOrCreateCityByName_New() {
        // When
        City result = cityService.getOrCreateCityByName("Burgas", "Bulgaria");

        // Then
        assertNotNull(result.getId());
        assertEquals("Burgas", result.getName());
        assertEquals(1, cityRepository.count());
    }

    @Test
    void testDeleteCity() {
        // Given
        City savedCity = cityService.saveCity(testCity);
        UUID cityId = savedCity.getId();

        // When
        cityService.deleteCity(cityId);

        // Then
        Optional<City> found = cityRepository.findById(cityId);
        assertFalse(found.isPresent());
    }

    @Test
    void testSearchCitiesByName() {
        // Given
        cityService.saveCity(City.builder().name("Sofia").country("Bulgaria").build());
        cityService.saveCity(City.builder().name("Plovdiv").country("Bulgaria").build());
        cityService.saveCity(City.builder().name("Paris").country("France").build());

        // When
        List<City> results = cityService.searchCitiesByName("ov");

        // Then
        assertEquals(1, results.size());
        assertEquals("Plovdiv", results.get(0).getName());
    }

    @Test
    void testFindCitiesWithCoordinates() {
        // Given
        City withCoords = City.builder()
                .name("Sofia")
                .country("Bulgaria")
                .latitude(new BigDecimal("42.6977"))
                .longitude(new BigDecimal("23.3219"))
                .build();
        City withoutCoords = City.builder()
                .name("Plovdiv")
                .country("Bulgaria")
                .build();
        
        cityService.saveCity(withCoords);
        cityService.saveCity(withoutCoords);

        // When
        List<City> results = cityService.findCitiesWithCoordinates();

        // Then
        assertEquals(1, results.size());
        assertEquals("Sofia", results.get(0).getName());
    }

    @Test
    void testFindAllCities() {
        // Given
        cityService.saveCity(City.builder().name("Sofia").country("Bulgaria").build());
        cityService.saveCity(City.builder().name("Plovdiv").country("Bulgaria").build());

        // When
        List<City> allCities = cityService.findAllCities();

        // Then
        assertEquals(2, allCities.size());
    }
}

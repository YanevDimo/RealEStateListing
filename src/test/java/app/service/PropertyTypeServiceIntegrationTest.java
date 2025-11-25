package app.service;

import app.entity.PropertyType;
import app.repository.PropertyTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("h2")
class PropertyTypeServiceIntegrationTest {

    @Autowired
    private PropertyTypeRepository propertyTypeRepository;

    @Autowired
    private PropertyUtilityService propertyUtilityService;

    private PropertyTypeService propertyTypeService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public PropertyUtilityService propertyUtilityService() {
            PropertyUtilityService mock = org.mockito.Mockito.mock(PropertyUtilityService.class);
            org.mockito.Mockito.when(mock.propertyTypeHasActiveProperties(org.mockito.ArgumentMatchers.any(UUID.class)))
                    .thenReturn(false);
            org.mockito.Mockito.when(mock.countActivePropertiesByPropertyTypeId(org.mockito.ArgumentMatchers.any(UUID.class)))
                    .thenReturn(0L);
            return mock;
        }
    }

    private PropertyType testPropertyType;

    @BeforeEach
    void setUp() {
        propertyTypeService = new PropertyTypeService(propertyTypeRepository, propertyUtilityService);
        
        propertyTypeRepository.deleteAll();
        
        testPropertyType = PropertyType.builder()
                .name("Apartment")
                .description("A residential unit in a building")
                .build();
    }

    @Test
    void testSaveAndFindPropertyType() {
        PropertyType saved = propertyTypeService.savePropertyType(testPropertyType);

        assertNotNull(saved.getId());
        assertEquals("Apartment", saved.getName());
        assertEquals("A residential unit in a building", saved.getDescription());

        Optional<PropertyType> found = propertyTypeService.findPropertyTypeById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Apartment", found.get().getName());
    }

    @Test
    void testFindPropertyTypeByName() {
        PropertyType saved = propertyTypeService.savePropertyType(testPropertyType);

        Optional<PropertyType> found = propertyTypeService.findPropertyTypeByName("Apartment");

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void testFindPropertyTypeByNameIgnoreCase() {
        PropertyType propertyTypeLowercase = PropertyType.builder()
                .name("apartment")
                .build();
        PropertyType saved = propertyTypeService.savePropertyType(propertyTypeLowercase);

        Optional<PropertyType> found = propertyTypeService.findPropertyTypeByNameIgnoreCase("APARTMENT");

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void testPropertyTypeExistsByName() {
        propertyTypeService.savePropertyType(testPropertyType);

        boolean exists = propertyTypeService.propertyTypeExistsByName("Apartment");

        assertTrue(exists);
    }

    @Test
    void testPropertyTypeExistsByName_NotExists() {
        boolean exists = propertyTypeService.propertyTypeExistsByName("NonExistent");

        assertFalse(exists);
    }

    @Test
    void testCreatePropertyType_Success() {
        PropertyType created = propertyTypeService.createPropertyType("House", "A standalone residential building");

        assertNotNull(created.getId());
        assertEquals("House", created.getName());
        assertEquals("A standalone residential building", created.getDescription());

        Optional<PropertyType> found = propertyTypeRepository.findById(created.getId());
        assertTrue(found.isPresent());
    }

    @Test
    void testCreatePropertyType_DuplicateName_ThrowsException() {
        propertyTypeService.savePropertyType(testPropertyType);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            propertyTypeService.createPropertyType("Apartment", "Duplicate"));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void testUpdatePropertyType() {
        PropertyType saved = propertyTypeService.savePropertyType(testPropertyType);
        UUID typeId = saved.getId();

        saved.setDescription("Updated description");
        PropertyType updated = propertyTypeService.updatePropertyType(saved);

        assertEquals("Updated description", updated.getDescription());

        Optional<PropertyType> found = propertyTypeRepository.findById(typeId);
        assertTrue(found.isPresent());
        assertEquals("Updated description", found.get().getDescription());
    }

    @Test
    void testUpdatePropertyTypeDescription() {
        PropertyType saved = propertyTypeService.savePropertyType(testPropertyType);
        UUID typeId = saved.getId();

        PropertyType updated = propertyTypeService.updatePropertyTypeDescription(typeId, "New description");

        assertEquals("New description", updated.getDescription());

        Optional<PropertyType> found = propertyTypeRepository.findById(typeId);
        assertTrue(found.isPresent());
        assertEquals("New description", found.get().getDescription());
    }

    @Test
    void testUpdatePropertyTypeDescription_NotFound_ThrowsException() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> 
            propertyTypeService.updatePropertyTypeDescription(nonExistentId, "Description"));
    }

    @Test
    void testDeletePropertyType() {
        PropertyType saved = propertyTypeService.savePropertyType(testPropertyType);
        UUID typeId = saved.getId();

        propertyTypeService.deletePropertyType(typeId);

        Optional<PropertyType> found = propertyTypeRepository.findById(typeId);
        assertFalse(found.isPresent());
    }

    @Test
    void testSearchPropertyTypesByName() {
        propertyTypeService.savePropertyType(PropertyType.builder().name("Apartment").build());
        propertyTypeService.savePropertyType(PropertyType.builder().name("House").build());
        propertyTypeService.savePropertyType(PropertyType.builder().name("Villa").build());

        List<PropertyType> results = propertyTypeService.searchPropertyTypesByName("ap");

        assertEquals(1, results.size());
        assertEquals("Apartment", results.get(0).getName());
    }

    @Test
    void testFindPropertyTypesWithDescription() {
        propertyTypeService.savePropertyType(PropertyType.builder().name("Apartment").description("Has description").build());
        propertyTypeService.savePropertyType(PropertyType.builder().name("House").build());

        List<PropertyType> results = propertyTypeService.findPropertyTypesWithDescription();

        assertEquals(1, results.size());
        assertEquals("Apartment", results.get(0).getName());
    }

    @Test
    void testFindPropertyTypesWithoutDescription() {
        propertyTypeService.savePropertyType(PropertyType.builder().name("Apartment").description("Has description").build());
        propertyTypeService.savePropertyType(PropertyType.builder().name("House").build());

        List<PropertyType> results = propertyTypeService.findPropertyTypesWithoutDescription();

        assertEquals(1, results.size());
        assertEquals("House", results.get(0).getName());
    }

    @Test
    void testFindPropertyTypesCreatedAfter() {
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);
        
        propertyTypeService.savePropertyType(PropertyType.builder().name("Apartment").build());
        propertyTypeService.savePropertyType(PropertyType.builder().name("House").build());

        List<PropertyType> results = propertyTypeService.findPropertyTypesCreatedAfter(pastTime);

        assertEquals(2, results.size());
    }

    @Test
    void testFindAllPropertyTypeNames() {
        propertyTypeService.savePropertyType(PropertyType.builder().name("Villa").build());
        propertyTypeService.savePropertyType(PropertyType.builder().name("Apartment").build());
        propertyTypeService.savePropertyType(PropertyType.builder().name("House").build());

        List<String> names = propertyTypeService.findAllPropertyTypeNames();

        assertEquals(3, names.size());
        assertEquals("Apartment", names.get(0));
        assertEquals("House", names.get(1));
        assertEquals("Villa", names.get(2));
    }

    @Test
    void testGetOrCreatePropertyTypeByName_Existing() {
        PropertyType propertyTypeLowercase = PropertyType.builder()
                .name("apartment")
                .build();
        PropertyType saved = propertyTypeService.savePropertyType(propertyTypeLowercase);
        UUID savedTypeId = saved.getId();

        PropertyType result = propertyTypeService.getOrCreatePropertyTypeByName("APARTMENT", "Description");

        assertEquals(savedTypeId, result.getId());
        assertEquals("apartment", result.getName());
    }

    @Test
    void testGetOrCreatePropertyTypeByName_New() {
        PropertyType result = propertyTypeService.getOrCreatePropertyTypeByName("Condo", "A condominium unit");

        assertNotNull(result.getId());
        assertEquals("Condo", result.getName());
        assertEquals("A condominium unit", result.getDescription());
    }

    @Test
    void testFindAllPropertyTypes() {
        propertyTypeService.savePropertyType(PropertyType.builder().name("Apartment").build());
        propertyTypeService.savePropertyType(PropertyType.builder().name("House").build());

        List<PropertyType> allTypes = propertyTypeService.findAllPropertyTypes();

        assertEquals(2, allTypes.size());
    }

    @Test
    void testGetPropertyTypeStatistics() {
        propertyTypeService.savePropertyType(PropertyType.builder().name("Apartment").description("Has description").build());
        propertyTypeService.savePropertyType(PropertyType.builder().name("House").build());

        PropertyTypeService.PropertyTypeStatistics stats = propertyTypeService.getPropertyTypeStatistics();

        assertEquals(2, stats.getTotalPropertyTypes());
        assertEquals(1, stats.getPropertyTypesWithDescription());
    }
}


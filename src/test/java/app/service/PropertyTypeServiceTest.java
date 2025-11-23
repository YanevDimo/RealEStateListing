package app.service;

import app.entity.PropertyType;
import app.repository.PropertyTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class PropertyTypeServiceTest {

    @Mock
    private PropertyTypeRepository propertyTypeRepository;
    @Mock
    private PropertyUtilityService propertyUtilityService;

    @InjectMocks
    private PropertyTypeService propertyTypeService;

    private PropertyType propertyType;
    private UUID propertyTypeId;

    @BeforeEach
    void setUp() {
        propertyTypeId = UUID.randomUUID();
        propertyType = PropertyType.builder()
                .id(propertyTypeId)
                .name("Apartment")
                .description("Residential apartment")
                .build();
    }

    @Test
    void testFindAllPropertyTypes() {
        //Given
        List<PropertyType> types = Collections.singletonList(propertyType);
        when(propertyTypeRepository.findAll()).thenReturn(types);
        //When
        List<PropertyType> result = propertyTypeService.findAllPropertyTypes();

        //Then
        assertEquals(1, result.size());
        assertEquals("Apartment", result.get(0).getName());
        verify(propertyTypeRepository, times(1)).findAll();
    }

    @Test
    void testFindPropertyTypeById_Found() {
        //Given
        when(propertyTypeRepository.findById(propertyTypeId)).thenReturn(Optional.of(propertyType));

        //When
        Optional<PropertyType> result = propertyTypeService.findPropertyTypeById(propertyTypeId);

        //Then
        assertTrue(result.isPresent());
        assertEquals("Apartment", result.get().getName());
    }

    @Test
    void testFindPropertyTypeById_NotFound() {
        //Given
        when(propertyTypeRepository.findById(propertyTypeId)).thenReturn(Optional.empty());

        //When
        Optional<PropertyType> result = propertyTypeService.findPropertyTypeById(propertyTypeId);

        //Then
        assertFalse(result.isPresent());
    }

    void testSavePropertyType() {

        //Given
        when(propertyTypeRepository.save(any(PropertyType.class))).thenReturn(propertyType);

        //When
        PropertyType result = propertyTypeService.savePropertyType(propertyType);
        //Then
        assertNotNull(result);
        assertEquals("Apartment", result.getName());
        verify(propertyTypeRepository, times(1)).save(any(PropertyType.class));
    }

    @Test
    void testCreatePropertyType_Success() {
        //Given
        when(propertyTypeRepository.existsByName("Apartment")).thenReturn(false);
        when(propertyTypeRepository.save(any(PropertyType.class))).thenReturn(propertyType);

        //When
        PropertyType result = propertyTypeService.createPropertyType("Apartment","Residential apartment");

        //Then
        assertNotNull(result);
        verify(propertyTypeRepository).existsByName("Apartment");
        verify(propertyTypeRepository).save(any(PropertyType.class));

    }
    @Test
    void testCreatePropertyType_DuplicateName_ThrowsException() {
        // Given
        when(propertyTypeRepository.existsByName("Apartment")).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                propertyTypeService.createPropertyType("Apartment", "Description"));

        assertTrue(exception.getMessage().contains("already exists"));
        verify(propertyTypeRepository, never()).save(any(PropertyType.class));
    }
    @Test
    void testGetOrCreatePropertyTypeByName_Existing() {
        // Given
        when(propertyTypeRepository.findByName("apartment")).thenReturn(Optional.of(propertyType));

        // When
        PropertyType result = propertyTypeService.getOrCreatePropertyTypeByName("Apartment", "Description");

        // Then
        assertEquals("Apartment", result.getName());
        verify(propertyTypeRepository, never()).save(any());
    }
    @Test
    void testGetOrCreatePropertyTypeByName_New() {
        // Given
        when(propertyTypeRepository.findByName("apartment")).thenReturn(Optional.empty());
        when(propertyTypeRepository.existsByName("Apartment")).thenReturn(false);
        when(propertyTypeRepository.save(any(PropertyType.class))).thenReturn(propertyType);

        // When
        PropertyType result = propertyTypeService.getOrCreatePropertyTypeByName("Apartment", "Description");

        // Then
        assertNotNull(result);
        verify(propertyTypeRepository).save(any(PropertyType.class));
    }

    @Test
    void testDeletePropertyType() {
        // Given
        doNothing().when(propertyTypeRepository).deleteById(propertyTypeId);

        // When
        propertyTypeService.deletePropertyType(propertyTypeId);

        // Then
        verify(propertyTypeRepository, times(1)).deleteById(propertyTypeId);
    }
    @Test
    void testPropertyTypeExistsByName() {
        // Given
        when(propertyTypeRepository.existsByName("Apartment")).thenReturn(true);

        // When
        boolean result = propertyTypeService.propertyTypeExistsByName("Apartment");

        // Then
        assertTrue(result);
        verify(propertyTypeRepository, times(1)).existsByName("Apartment");
    }
    @Test
    void testSearchPropertyTypesByName() {
        // Given
        PropertyType type1 = PropertyType.builder().name("Apartment").build();
        PropertyType type2 = PropertyType.builder().name("House").build();
        PropertyType type3 = PropertyType.builder().name("Studio Apartment").build();
        List<PropertyType> allTypes = Arrays.asList(type1, type2, type3);
        when(propertyTypeRepository.findAll()).thenReturn(allTypes);

        // When
        List<PropertyType> result = propertyTypeService.searchPropertyTypesByName("apartment");

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(t -> t.getName().equals("Apartment")));
        assertTrue(result.stream().anyMatch(t -> t.getName().equals("Studio Apartment")));
    }
    @Test
    void testFindPropertyTypesWithDescription() {
        // Given
        PropertyType type1 = PropertyType.builder().name("Apartment").description("Has description").build();
        PropertyType type2 = PropertyType.builder().name("House").description("").build();
        PropertyType type3 = PropertyType.builder().name("Villa").description(null).build();
        List<PropertyType> allTypes = Arrays.asList(type1, type2, type3);
        when(propertyTypeRepository.findAll()).thenReturn(allTypes);

        // When
        List<PropertyType> result = propertyTypeService.findPropertyTypesWithDescription();

        // Then
        assertEquals(1, result.size());
        assertEquals("Apartment", result.get(0).getName());
    }
    @Test
    void testGetPropertyTypeStatistics() {
        // Given
        when(propertyTypeRepository.count()).thenReturn(5L);
        when(propertyTypeRepository.findAll()).thenReturn(Collections.singletonList(propertyType));
        when(propertyUtilityService.propertyTypeHasActiveProperties(propertyTypeId)).thenReturn(true);

        // When
        PropertyTypeService.PropertyTypeStatistics stats = propertyTypeService.getPropertyTypeStatistics();

        // Then
        assertNotNull(stats);
        assertEquals(5L, stats.getTotalPropertyTypes());
    }

}

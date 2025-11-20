package app.service;

import app.entity.PropertyType;
import app.repository.PropertyTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PropertyTypeService {

    private final PropertyTypeRepository propertyTypeRepository;
    private final PropertyUtilityService propertyUtilityService;


    @Cacheable("propertyTypes")
    public List<PropertyType> findAllPropertyTypes() {
        log.debug("Finding all property types");
        return propertyTypeRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = "propertyTypes", allEntries = true)
    public PropertyType savePropertyType(PropertyType propertyType) {
        log.debug("Saving property type: {}", propertyType.getName());
        return propertyTypeRepository.save(propertyType);
    }

    public Optional<PropertyType> findPropertyTypeById(UUID id) {
        log.debug("Finding property type by ID: {}", id);
        return propertyTypeRepository.findById(id);
    }


    public Optional<PropertyType> findPropertyTypeByName(String name) {
        log.debug("Finding property type by name: {}", name);
        return propertyTypeRepository.findByName(name);
    }


    public Optional<PropertyType> findPropertyTypeByNameIgnoreCase(String name) {
        log.debug("Finding property type by name (case insensitive): {}", name);
        return propertyTypeRepository.findByName(name.toLowerCase());
    }


    public boolean propertyTypeExistsByName(String name) {
        log.debug("Checking if property type exists by name: {}", name);
        return propertyTypeRepository.existsByName(name);
    }


    public List<PropertyType> searchPropertyTypesByName(String name) {
        log.debug("Searching property types by name: {}", name);
        return propertyTypeRepository.findAll().stream()
                .filter(pt -> pt.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }


    public List<PropertyType> findPropertyTypesWithActiveProperties() {
        log.debug("Finding property types with active properties");
        return propertyTypeRepository.findAll().stream()
                .filter(pt -> propertyUtilityService.propertyTypeHasActiveProperties(pt.getId()))
                .toList();
    }


    public List<PropertyType> findPropertyTypesOrderedByPropertyCount() {
        log.debug("Finding property types ordered by property count");
        
        // Count properties per property type using utility service
        return propertyTypeRepository.findAll().stream()
                .sorted((a, b) -> {
                    long countA = propertyUtilityService.countActivePropertiesByPropertyTypeId(a.getId());
                    long countB = propertyUtilityService.countActivePropertiesByPropertyTypeId(b.getId());
                    return Long.compare(countB, countA); // Descending order
                })
                .toList();
    }




    public List<PropertyType> findPropertyTypesWithDescription() {
        log.debug("Finding property types with description");
        return propertyTypeRepository.findAll().stream()
                .filter(pt -> pt.getDescription() != null && !pt.getDescription().trim().isEmpty())
                .toList();
    }


    public List<PropertyType> findPropertyTypesWithoutDescription() {
        log.debug("Finding property types without description");
        return propertyTypeRepository.findAll().stream()
                .filter(pt -> pt.getDescription() == null || pt.getDescription().trim().isEmpty())
                .toList();
    }


    public List<PropertyType> findPropertyTypesCreatedAfter(LocalDateTime date) {
        log.debug("Finding property types created after: {}", date);
        return propertyTypeRepository.findAll().stream()
                .filter(pt -> pt.getCreatedAt().isAfter(date))
                .toList();
    }


    public List<String> findAllPropertyTypeNames() {
        log.debug("Finding all property type names");
        return propertyTypeRepository.findAll().stream()
                .map(PropertyType::getName)
                .sorted()
                .toList();
    }




    @Transactional
    public PropertyType updatePropertyType(PropertyType propertyType) {
        log.debug("Updating property type: {}", propertyType.getName());
        return propertyTypeRepository.save(propertyType);
    }


    @Transactional
    public void deletePropertyType(UUID id) {
        log.debug("Deleting property type with ID: {}", id);
        propertyTypeRepository.deleteById(id);
    }


    @Transactional
    public PropertyType createPropertyType(String name, String description) {
        log.debug("Creating new property type: {}", name);
        
        if (propertyTypeRepository.existsByName(name)) {
            throw new RuntimeException("Property type with name " + name + " already exists");
        }

        PropertyType propertyType = PropertyType.builder()
                .name(name)
                .description(description)
                .build();

        return propertyTypeRepository.save(propertyType);
    }


    @Transactional
    public PropertyType updatePropertyTypeDescription(UUID id, String description) {
        log.debug("Updating property type description: {}", id);
        PropertyType propertyType = propertyTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property type not found with ID: " + id));
        propertyType.setDescription(description);
        return propertyTypeRepository.save(propertyType);
    }


    @Transactional
    public PropertyType getOrCreatePropertyTypeByName(String name, String description) {
        log.debug("Getting or creating property type by name: {}", name);
        
        Optional<PropertyType> existingPropertyType = findPropertyTypeByNameIgnoreCase(name);
        if (existingPropertyType.isPresent()) {
            return existingPropertyType.get();
        }

        return createPropertyType(name, description);
    }


    public PropertyTypeStatistics getPropertyTypeStatistics() {
        log.debug("Getting property type statistics");
        long totalPropertyTypes = propertyTypeRepository.count();
        long propertyTypesWithProperties = findPropertyTypesWithActiveProperties().size();
        long propertyTypesWithDescription = findPropertyTypesWithDescription().size();
        
        return new PropertyTypeStatistics(totalPropertyTypes, propertyTypesWithProperties, propertyTypesWithDescription);
    }


    public static class PropertyTypeStatistics {
        private final long totalPropertyTypes;
        private final long propertyTypesWithProperties;
        private final long propertyTypesWithDescription;

        public PropertyTypeStatistics(long totalPropertyTypes, long propertyTypesWithProperties, long propertyTypesWithDescription) {
            this.totalPropertyTypes = totalPropertyTypes;
            this.propertyTypesWithProperties = propertyTypesWithProperties;
            this.propertyTypesWithDescription = propertyTypesWithDescription;
        }

        public long getTotalPropertyTypes() { return totalPropertyTypes; }
        public long getPropertyTypesWithProperties() { return propertyTypesWithProperties; }
        public long getPropertyTypesWithDescription() { return propertyTypesWithDescription; }
    }
}
package app.service;


import app.entity.Property;
import app.entity.PropertyImage;
import app.entity.PropertyStatus;
import app.exception.PropertyNotFoundException;
import app.repository.PropertyImageRepository;
import app.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PropertyService {

    private final
    PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;


    public List<Property> findAllProperties() {
        log.debug("Finding all properties");
        return propertyRepository.findAll();
    }

    public long countAllProperties() {
        log.debug("Counting all properties");
        return propertyRepository.count();
    }

    public long countActiveProperties() {
        log.debug("Counting active properties");
        return propertyRepository.countByStatus(PropertyStatus.ACTIVE);
    }

    public double getAveragePropertyPrice() {
        log.debug("Getting average property price");
        return propertyRepository.findAveragePrice().orElse(0.0);
    }

    public List<Property> findAllActiveProperties() {
        log.debug("Finding all active properties");
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE);
    }


    public Page<Property> findAllActiveProperties(Pageable pageable) {
        log.debug("Finding all active properties with pagination: {}", pageable);
        // Use findAll() and filter in memory for now, or implement pagination in service layer
        List<Property> allActive = propertyRepository.findByStatus(PropertyStatus.ACTIVE);
        // Simple pagination implementation
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allActive.size());
        List<Property> pageContent = allActive.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, allActive.size());
    }


    public Optional<Property> findPropertyById(UUID id) {
        log.debug("Finding property by ID: {}", id);
        return propertyRepository.findById(id);
    }


    public List<Property> findFeaturedProperties() {
        log.debug("Finding featured properties");
        return propertyRepository.findByFeaturedTrueAndStatus(PropertyStatus.ACTIVE);
    }


    public List<Property> findPropertiesByCity(UUID cityId) {
        log.debug("Finding properties by city ID: {}", cityId);
        return propertyRepository.findByCityIdAndStatus(cityId, PropertyStatus.ACTIVE);
    }


    public List<Property> findPropertiesByType(UUID propertyTypeId) {
        log.debug("Finding properties by type ID: {}", propertyTypeId);
        return propertyRepository.findByPropertyTypeIdAndStatus(propertyTypeId, PropertyStatus.ACTIVE);
    }


    public List<Property> findPropertiesByAgent(UUID agentId) {
        log.debug("Finding properties by agent ID: {}", agentId);
        return propertyRepository.findByAgentIdAndStatus(agentId, PropertyStatus.ACTIVE);
    }


    public List<Property> findPropertiesByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Finding properties by price range: {} - {}", minPrice, maxPrice);
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE).stream()
                .filter(property -> {
                    if (minPrice != null && property.getPrice().compareTo(minPrice) < 0) return false;
                    if (maxPrice != null && property.getPrice().compareTo(maxPrice) > 0) return false;
                    return true;
                })
                .toList();
    }


    public List<Property> findPropertiesByBeds(Integer beds) {
        log.debug("Finding properties by beds: {}", beds);
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE).stream()
                .filter(property -> property.getBeds() >= beds)
                .toList();
    }


    public List<Property> findPropertiesByBaths(Integer baths) {
        log.debug("Finding properties by baths: {}", baths);
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE).stream()
                .filter(property -> property.getBaths() >= baths)
                .toList();
    }

    /**
     * Find properties by area range
     */
    public List<Property> findPropertiesByAreaRange(BigDecimal minArea, BigDecimal maxArea) {
        log.debug("Finding properties by area range: {} - {}", minArea, maxArea);
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE).stream()
                .filter(property -> {
                    BigDecimal areaSqm = property.getAreaSqm();
                    if (areaSqm == null) return false; // Skip properties without area
                    if (minArea != null && areaSqm.compareTo(minArea) < 0) return false;
                    if (maxArea != null && areaSqm.compareTo(maxArea) > 0) return false;
                    return true;
                })
                .toList();
    }


    public List<Property> searchProperties(String searchTerm) {
        log.debug("Searching properties with term: {}", searchTerm);
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }
        String lowerSearchTerm = searchTerm.toLowerCase();
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE).stream()
                .filter(property -> {
                    String title = property.getTitle();
                    String description = property.getDescription();
                    return (title != null && title.toLowerCase().contains(lowerSearchTerm)) ||
                           (description != null && description.toLowerCase().contains(lowerSearchTerm));
                })
                .toList();
    }


    public List<Property> findPropertiesByCriteria(UUID cityId, UUID propertyTypeId, 
                                                  BigDecimal minPrice, BigDecimal maxPrice,
                                                  Integer minBeds, Integer minBaths,
                                                  BigDecimal minArea, BigDecimal maxArea) {
        log.debug("Finding properties by criteria - city: {}, type: {}, price: {}-{}, beds: {}, baths: {}, area: {}-{}",
                cityId, propertyTypeId, minPrice, maxPrice, minBeds, minBaths, minArea, maxArea);
        
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE).stream()
                .filter(property -> {
                    // Filter by city
                    if (cityId != null && !property.getCity().getId().equals(cityId)) {
                        return false;
                    }
                    
                    // Filter by property type
                    if (propertyTypeId != null && !property.getPropertyType().getId().equals(propertyTypeId)) {
                        return false;
                    }
                    
                    // Filter by price range
                    if (minPrice != null && property.getPrice().compareTo(minPrice) < 0) {
                        return false;
                    }
                    if (maxPrice != null && property.getPrice().compareTo(maxPrice) > 0) {
                        return false;
                    }
                    
                    // Filter by beds
                    if (minBeds != null && property.getBeds() < minBeds) {
                        return false;
                    }
                    
                    // Filter by baths
                    if (minBaths != null && property.getBaths() < minBaths) {
                        return false;
                    }
                    
                    // Filter by area range
                    BigDecimal areaSqm = property.getAreaSqm();
                    if (areaSqm != null) {
                        if (minArea != null && areaSqm.compareTo(minArea) < 0) {
                            return false;
                        }
                        if (maxArea != null && areaSqm.compareTo(maxArea) > 0) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .toList();
    }


    public List<Property> findPropertiesWithCoordinates() {
        log.debug("Finding properties with coordinates");
        // Filter properties with coordinates from all active properties
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE).stream()
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .toList();
    }


    public List<Property> findPropertiesNearLocation(BigDecimal latitude, BigDecimal longitude, Double radiusKm) {
        log.debug("Finding properties near location: {}, {} within {} km", latitude, longitude, radiusKm);
        // For now, return all properties with coordinates - implement distance calculation in service layer
        return findPropertiesWithCoordinates();
    }




    public long countPropertiesByCity(UUID cityId) {
        log.debug("Counting properties by city ID: {}", cityId);
        return propertyRepository.findByCityIdAndStatus(cityId, PropertyStatus.ACTIVE).size();
    }


    public long countPropertiesByAgent(UUID agentId) {
        log.debug("Counting properties by agent ID: {}", agentId);
        return propertyRepository.findByAgentIdAndStatus(agentId, PropertyStatus.ACTIVE).size();
    }


    @Transactional
    public Property saveProperty(Property property) {
        log.debug("Saving property: {}", property.getTitle());
        return propertyRepository.save(property);
    }


    @Transactional
    public Property updateProperty(Property property) {
        log.debug("Updating property: {}", property.getTitle());
        return propertyRepository.save(property);
    }


    @Transactional
    public void deleteProperty(UUID id) {
        log.debug("Deleting property with ID: {}", id);
        propertyRepository.deleteById(id);
    }

    @Transactional
    public PropertyImage savePropertyImage(PropertyImage propertyImage) {
        log.debug("Saving property image for property ID: {}", propertyImage.getProperty().getId());
        return propertyImageRepository.save(propertyImage);
    }

    public List<PropertyImage> findPropertyImagesByPropertyId(UUID propertyId) {
        log.debug("Finding property images for property ID: {}", propertyId);
        return propertyImageRepository.findByPropertyIdOrderBySortOrder(propertyId);
    }


    @Transactional
    public Property markAsFeatured(UUID id) {
        log.debug("Marking property as featured: {}", id);
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new PropertyNotFoundException(id));
        property.setFeatured(true);
        return propertyRepository.save(property);
    }


    @Transactional
    public Property removeFromFeatured(UUID id) {
        log.debug("Removing property from featured: {}", id);
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new PropertyNotFoundException(id));
        property.setFeatured(false);
        return propertyRepository.save(property);
    }


    @Transactional
    public Property changePropertyStatus(UUID id, PropertyStatus status) {
        log.debug("Changing property status: {} to {}", id, status);
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new PropertyNotFoundException(id));
        property.setStatus(status);
        return propertyRepository.save(property);
    }


    public List<Property> findPropertiesCreatedAfter(LocalDateTime date) {
        log.debug("Finding properties created after: {}", date);
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE).stream()
                .filter(p -> p.getCreatedAt().isAfter(date))
                .toList();
    }


    public List<Property> findPropertiesByYearBuilt(Integer yearBuilt) {
        log.debug("Finding properties by year built: {}", yearBuilt);
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE).stream()
                .filter(p -> p.getYearBuilt() != null && p.getYearBuilt().equals(yearBuilt))
                .toList();
    }


    public List<Property> findPropertiesByYearBuiltRange(Integer startYear, Integer endYear) {
        log.debug("Finding properties by year built range: {} - {}", startYear, endYear);
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE).stream()
                .filter(p -> p.getYearBuilt() != null && p.getYearBuilt() >= startYear && p.getYearBuilt() <= endYear)
                .toList();
    }
}
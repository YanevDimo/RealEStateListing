package app.service;

import app.client.PropertyServiceClient;
import app.dto.PropertyDto;
import app.entity.City;
import app.entity.PropertyType;
import app.util.PaginationUtil;
import feign.FeignException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final PropertyServiceClient propertyServiceClient;
    private final CityService cityService;
    private final PropertyTypeService propertyTypeService;
    private final PropertyUtilityService propertyUtilityService;

    // ==========================================
    // HELPER METHODS - Reduce Code Duplication
    // ==========================================
    
    /**
     * Helper method to filter properties based on search criteria.
     * 
     * WHY THIS HELPER METHOD?
     * Before: We had the same filtering logic written twice (about 100 lines of duplicate code):
     *   - Once in the main search path (lines 64-98)
     *   - Once in the fallback path (lines 117-179)
     * 
     * After: We call this helper method instead, which:
     *   1. Makes the code much shorter and easier to read
     *   2. If we need to change how filtering works, we only change it in one place
     *   3. Reduces the chance of bugs from having two different versions of the same logic
     *   4. Makes it easier to test the filtering logic separately
     * 
     * @param property The property to check
     * @param criteria The search criteria to filter by
     * @param cityId Optional city ID filter (null if not filtering by city)
     * @param propertyTypeId Optional property type ID filter (null if not filtering by type)
     * @param maxPrice Optional max price filter (null if not filtering by max price)
     * @param includeSearchTerm Whether to also filter by search term in title/description
     * @return true if the property matches all the criteria, false otherwise
     */
    private boolean matchesFilterCriteria(PropertyDto property, SearchCriteria criteria, 
                                         UUID cityId, UUID propertyTypeId, Double maxPrice, 
                                         boolean includeSearchTerm) {
        // Filter by city (if specified)
        if (cityId != null && !cityId.equals(property.getCityId())) {
            return false;
        }
        
        // Filter by property type (if specified)
        if (propertyTypeId != null && !propertyTypeId.equals(property.getPropertyTypeId())) {
            return false;
        }
        
        // Filter by max price (if specified)
        if (maxPrice != null && 
            (property.getPrice() == null || property.getPrice().doubleValue() > maxPrice)) {
            return false;
        }
        
        // Filter by min price (if specified)
        if (criteria.getMinPrice() != null && 
            (property.getPrice() == null || property.getPrice().compareTo(criteria.getMinPrice()) < 0)) {
            return false;
        }
        
        // Filter by minimum number of beds (if specified)
        if (criteria.getMinBeds() != null && 
            (property.getBeds() == null || property.getBeds() < criteria.getMinBeds())) {
            return false;
        }
        
        // Filter by minimum number of baths (if specified)
        if (criteria.getMinBaths() != null && 
            (property.getBaths() == null || property.getBaths() < criteria.getMinBaths())) {
            return false;
        }
        
        // Filter by minimum area (if specified)
        if (criteria.getMinArea() != null && 
            (property.getAreaSqm() == null || property.getAreaSqm().compareTo(criteria.getMinArea()) < 0)) {
            return false;
        }
        
        // Filter by maximum area (if specified)
        if (criteria.getMaxArea() != null && 
            (property.getAreaSqm() == null || property.getAreaSqm().compareTo(criteria.getMaxArea()) > 0)) {
            return false;
        }
        
        // Filter by featured status (if specified)
        if (criteria.getFeatured() != null) {
            if (property.getIsFeatured() == null || !property.getIsFeatured().equals(criteria.getFeatured())) {
                return false;
            }
        }
        
        // Filter by search term in title/description (if requested and specified)
        if (includeSearchTerm && criteria.getSearchTerm() != null && !criteria.getSearchTerm().trim().isEmpty()) {
            String searchTerm = criteria.getSearchTerm().toLowerCase();
            String title = property.getTitle() != null ? property.getTitle().toLowerCase() : "";
            String description = property.getDescription() != null ? property.getDescription().toLowerCase() : "";
            if (!title.contains(searchTerm) && !description.contains(searchTerm)) {
                return false;
            }
        }
        
        // If we got here, the property matches all the criteria
        return true;
    }

    // ==========================================
    // PUBLIC SERVICE METHODS
    // ==========================================

    public List<PropertyDto> searchProperties(SearchCriteria criteria) {
        log.debug("Searching properties with criteria: {}", criteria);
        
        // Convert city name to UUID (declare outside try for use in catch block)
        UUID cityId = null;
        if (criteria.getCityName() != null && !criteria.getCityName().trim().isEmpty()) {
            cityId = cityService.findCityByNameIgnoreCase(criteria.getCityName().trim())
                    .map(City::getId)
                    .orElse(null);
        }
        
        // Convert property type name to UUID (declare outside try for use in catch block)
        UUID propertyTypeId = null;
        if (criteria.getPropertyTypeName() != null && !criteria.getPropertyTypeName().trim().isEmpty()) {
            propertyTypeId = propertyTypeService.findPropertyTypeByNameIgnoreCase(criteria.getPropertyTypeName().trim())
                    .map(PropertyType::getId)
                    .orElse(null);
        }
        
        // Convert max price (declare outside try for use in catch block)
        Double maxPrice = criteria.getMaxPrice() != null ? criteria.getMaxPrice().doubleValue() : null;
        
        try {
            List<PropertyDto> properties = propertyServiceClient.searchProperties(
                    criteria.getSearchTerm(),
                    cityId,
                    propertyTypeId,
                    maxPrice
            );
            log.info("Property-service returned {} properties for search criteria", properties != null ? properties.size() : 0);
            
            // Apply additional filters in memory (beds, baths, area, featured, minPrice)
            // Note: city, propertyType, and maxPrice are already filtered by the API call,
            // so we pass null for those. We also don't need to filter by search term here
            // because the API already did that.
            final UUID finalCityId = null; // Already filtered by API
            final UUID finalPropertyTypeId = null; // Already filtered by API
            final Double finalMaxPrice = null; // Already filtered by API
            final boolean includeSearchTerm = false; // Already filtered by API
            
            return properties.stream()
                    .filter(property -> matchesFilterCriteria(property, criteria, 
                                                              finalCityId, finalPropertyTypeId, 
                                                              finalMaxPrice, includeSearchTerm))
                    .collect(Collectors.toList());
        } catch (FeignException e) {
            log.error("Error calling property-service for search: Status {} - {}", e.status(), e.getMessage());
            
            // If it's a 500 error (Hibernate bug), try fallback to getAllProperties() and filter in memory
            if (e.status() == 500) {
                log.warn("Property-service search endpoint returned 500 error (Hibernate MultipleBagFetchException). " +
                        "Attempting fallback to getAllProperties() with in-memory filtering");
                try {
                    List<PropertyDto> allProperties = propertyServiceClient.getAllProperties(null, null, null, null);
                    if (allProperties != null && !allProperties.isEmpty()) {
                        log.info("Fallback: Retrieved {} properties via getAllProperties(), applying filters in memory", allProperties.size());
                        
                        // Create final copies for use in lambda
                        final UUID finalCityId = cityId;
                        final UUID finalPropertyTypeId = propertyTypeId;
                        final Double finalMaxPrice = maxPrice;
                        final boolean includeSearchTerm = true; // Need to filter by search term in fallback
                        
                        // Apply all filters in memory using our helper method
                        // In the fallback path, we need to filter by everything since the API didn't do it
                        return allProperties.stream()
                                .filter(property -> matchesFilterCriteria(property, criteria, 
                                                                          finalCityId, finalPropertyTypeId, 
                                                                          finalMaxPrice, includeSearchTerm))
                                .collect(Collectors.toList());
                    } else {
                        log.warn("Fallback getAllProperties() returned null or empty list");
                    }
                } catch (Exception fallbackEx) {
                    log.error("Fallback to getAllProperties() also failed", fallbackEx);
                }
            }
            
            log.warn("Returning empty list due to property-service error");
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error calling property-service for search", e);
            return List.of();
        }
    }

    /**
     * Search properties with pagination
     */
    public Page<PropertyDto> searchProperties(SearchCriteria criteria, Pageable pageable) {
        log.debug("Searching properties with criteria: {} and pagination: {}", criteria, pageable);
        
        List<PropertyDto> properties = searchProperties(criteria);
        // Use our pagination utility instead of repeating the same code
        return PaginationUtil.paginateList(properties, pageable);
    }

    /**
     * Search properties by text
     */
    public List<PropertyDto> searchPropertiesByText(String searchTerm) {
        log.debug("Searching properties by text: {}", searchTerm);
        
        try {
            List<PropertyDto> properties = propertyServiceClient.searchProperties(searchTerm, null, null, null);
            log.info("Property-service returned {} properties for search: '{}'", properties != null ? properties.size() : 0, searchTerm);
            return properties != null ? properties : List.of();
        } catch (FeignException e) {
            log.error("Error calling property-service for text search: Status {} - {}", e.status(), e.getMessage());
            if (e.status() == 500) {
                log.warn("Property-service returned 500 error (Hibernate MultipleBagFetchException). " +
                        "This is a known issue in the property-service. Returning empty list.");
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error calling property-service for text search. Is Property Service running on port 8083?", e);
            return List.of();
        }
    }

    /**
     * Get featured properties - cached for performance
     */
    @Cacheable("featuredProperties")
    public List<PropertyDto> getFeaturedProperties() {
        log.debug("Getting featured properties from property-service");
        try {
            List<PropertyDto> properties = propertyServiceClient.getFeaturedProperties();
            log.info("Property-service returned {} featured properties", properties != null ? properties.size() : 0);
            if (properties == null || properties.isEmpty()) {
                log.warn("Property-service returned empty list. Check if Property Service on port 8083 has data.");
            }
            return properties != null ? properties : List.of();
        } catch (Exception e) {
            log.error("Error calling property-service for featured properties. Is Property Service running on port 8083?", e);
            return List.of();
        }
    }


    public List<PropertyDto> getPropertiesByCityName(String cityName) {
        log.debug("Getting properties by city name: {}", cityName);
        
        try {
            return cityService.findCityByNameIgnoreCase(cityName)
                    .map(city -> propertyServiceClient.getPropertiesByCity(city.getId()))
                    .orElse(List.of());
        } catch (Exception e) {
            log.error("Error calling property-service for city properties", e);
            return List.of();
        }
    }


    public List<PropertyDto> getPropertiesByPropertyTypeName(String propertyTypeName) {
        log.debug("Getting properties by property type name: {}", propertyTypeName);
        
        try {
            return propertyTypeService.findPropertyTypeByNameIgnoreCase(propertyTypeName)
                    .map(type -> propertyServiceClient.getAllProperties(null, null, type.getId(), null))
                    .orElse(List.of());
        } catch (Exception e) {
            log.error("Error calling property-service for property type properties", e);
            return List.of();
        }
    }


    public List<PropertyDto> getPropertiesByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Getting properties by price range: {} - {}", minPrice, maxPrice);
        
        try {
            Double maxPriceDouble = maxPrice != null ? maxPrice.doubleValue() : null;
            List<PropertyDto> properties = propertyServiceClient.getAllProperties(null, null, null, maxPriceDouble);
            
            if (minPrice != null) {
                return properties.stream()
                        .filter(p -> p.getPrice() != null && p.getPrice().compareTo(minPrice) >= 0)
                        .collect(Collectors.toList());
            }
            return properties;
        } catch (Exception e) {
            log.error("Error calling property-service for price range", e);
            return List.of();
        }
    }


    public List<PropertyDto> getPropertiesByBeds(Integer beds) {
        log.debug("Getting properties by beds: {}", beds);
        return propertyUtilityService.getAllProperties().stream()
                .filter(p -> p.getBeds() != null && p.getBeds() >= beds)
                .collect(Collectors.toList());
    }


    public List<PropertyDto> getPropertiesByBaths(Integer baths) {
        log.debug("Getting properties by baths: {}", baths);
        return propertyUtilityService.getAllProperties().stream()
                .filter(p -> p.getBaths() != null && p.getBaths() >= baths)
                .collect(Collectors.toList());
    }


    public List<PropertyDto> getPropertiesByAreaRange(BigDecimal minArea, BigDecimal maxArea) {
        log.debug("Getting properties by area range: {} - {}", minArea, maxArea);
        return propertyUtilityService.getAllProperties().stream()
                .filter(property -> {
                    if (minArea != null && (property.getAreaSqm() == null || property.getAreaSqm().compareTo(minArea) < 0)) return false;
                    return maxArea == null || (property.getAreaSqm() != null && property.getAreaSqm().compareTo(maxArea) <= 0);
                })
                .collect(Collectors.toList());
    }


    public List<PropertyDto> getPropertiesNearLocation(BigDecimal latitude, BigDecimal longitude, Double radiusKm) {
        log.debug("Getting properties near location: {}, {} within {} km", latitude, longitude, radiusKm);
        // Note: Property-service doesn't have location-based search yet, returning empty for now
        return List.of();
    }


    @Cacheable(value = "cities", key = "'names'")
    public List<String> getAvailableCities() {
        log.debug("Getting available cities for search");
        return cityService.findAllCities().stream()
                .map(City::getName)
                .sorted()
                .toList();
    }


    @Cacheable(value = "propertyTypes", key = "'names'")
    public List<String> getAvailablePropertyTypes() {
        log.debug("Getting available property types for search");
        return propertyTypeService.findAllPropertyTypes().stream()
                .map(PropertyType::getName)
                .sorted()
                .toList();
    }

    /**
     * Build SearchCriteria from request parameters.
     * Handles parsing and validation of search parameters.
     */
    public SearchCriteria buildSearchCriteria(String search, String city, String type, String maxPrice) {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCityName(city);
        criteria.setPropertyTypeName(type);
        
        if (maxPrice != null && !maxPrice.trim().isEmpty()) {
            try {
                criteria.setMaxPrice(new BigDecimal(maxPrice));
            } catch (NumberFormatException e) {
                log.warn("Invalid maxPrice format: {}", maxPrice);
            }
        }
        
        return criteria;
    }


    @Setter
    @Getter
    public static class SearchCriteria {
        private String searchTerm;
        private String cityName;
        private String propertyTypeName;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private Integer minBeds;
        private Integer minBaths;
        private BigDecimal minArea;
        private BigDecimal maxArea;
        private Boolean featured;


        public SearchCriteria() {}

        public SearchCriteria(String searchTerm, String cityName, String propertyTypeName,
                            BigDecimal minPrice, BigDecimal maxPrice, Integer minBeds, Integer minBaths,
                            BigDecimal minArea, BigDecimal maxArea, Boolean featured) {
            this.searchTerm = searchTerm;
            this.cityName = cityName;
            this.propertyTypeName = propertyTypeName;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            this.minBeds = minBeds;
            this.minBaths = minBaths;
            this.minArea = minArea;
            this.maxArea = maxArea;
            this.featured = featured;
        }

        @Override
        public String toString() {
            return "SearchCriteria{" +
                    "searchTerm='" + searchTerm + '\'' +
                    ", cityName='" + cityName + '\'' +
                    ", propertyTypeName='" + propertyTypeName + '\'' +
                    ", minPrice=" + minPrice +
                    ", maxPrice=" + maxPrice +
                    ", minBeds=" + minBeds +
                    ", minBaths=" + minBaths +
                    ", minArea=" + minArea +
                    ", maxArea=" + maxArea +
                    ", featured=" + featured +
                    '}';
        }
    }
}





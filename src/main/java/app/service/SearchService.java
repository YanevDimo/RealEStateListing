package app.service;

import app.client.PropertyServiceClient;
import app.dto.PropertyDto;
import app.entity.City;
import app.entity.PropertyType;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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


    public List<PropertyDto> searchProperties(SearchCriteria criteria) {
        log.debug("Searching properties with criteria: {}", criteria);
        
        // Convert city name to UUID (declare outside try for use in catch block)
        java.util.UUID cityId = null;
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
            return properties.stream()
                    .filter(property -> {
                        // Filter by min price
                        if (criteria.getMinPrice() != null && 
                            (property.getPrice() == null || property.getPrice().compareTo(criteria.getMinPrice()) < 0)) {
                            return false;
                        }
                        
                        // Filter by beds
                        if (criteria.getMinBeds() != null && 
                            (property.getBeds() == null || property.getBeds() < criteria.getMinBeds())) {
                            return false;
                        }
                        
                        // Filter by baths
                        if (criteria.getMinBaths() != null && 
                            (property.getBaths() == null || property.getBaths() < criteria.getMinBaths())) {
                            return false;
                        }
                        
                        // Filter by area range
                        if (criteria.getMinArea() != null && 
                            (property.getAreaSqm() == null || property.getAreaSqm().compareTo(criteria.getMinArea()) < 0)) {
                            return false;
                        }
                        if (criteria.getMaxArea() != null && 
                            (property.getAreaSqm() == null || property.getAreaSqm().compareTo(criteria.getMaxArea()) > 0)) {
                            return false;
                        }
                        
                        // Filter by featured
                        return criteria.getFeatured() == null || 
                               property.getIsFeatured() != null && property.getIsFeatured().equals(criteria.getFeatured());
                    })
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
                        
                        // Create final copies
                        final UUID finalCityId = cityId;
                        final UUID finalPropertyTypeId = propertyTypeId;
                        final Double finalMaxPrice = maxPrice;
                        
                        // Apply all filters in memory
                        return allProperties.stream()
                                .filter(property -> {
                                    // Filter by city
                                    if (finalCityId != null && !finalCityId.equals(property.getCityId())) {
                                        return false;
                                    }
                                    
                                    // Filter by property type
                                    if (finalPropertyTypeId != null && !finalPropertyTypeId.equals(property.getPropertyTypeId())) {
                                        return false;
                                    }
                                    
                                    // Filter by max price
                                    if (finalMaxPrice != null && 
                                        (property.getPrice() == null || property.getPrice().doubleValue() > finalMaxPrice)) {
                                        return false;
                                    }
                                    
                                    // Filter by min price
                                    if (criteria.getMinPrice() != null && 
                                        (property.getPrice() == null || property.getPrice().compareTo(criteria.getMinPrice()) < 0)) {
                                        return false;
                                    }
                                    
                                    // Filter by beds
                                    if (criteria.getMinBeds() != null && 
                                        (property.getBeds() == null || property.getBeds() < criteria.getMinBeds())) {
                                        return false;
                                    }
                                    
                                    // Filter by baths
                                    if (criteria.getMinBaths() != null && 
                                        (property.getBaths() == null || property.getBaths() < criteria.getMinBaths())) {
                                        return false;
                                    }
                                    
                                    // Filter by area range
                                    if (criteria.getMinArea() != null && 
                                        (property.getAreaSqm() == null || property.getAreaSqm().compareTo(criteria.getMinArea()) < 0)) {
                                        return false;
                                    }
                                    if (criteria.getMaxArea() != null && 
                                        (property.getAreaSqm() == null || property.getAreaSqm().compareTo(criteria.getMaxArea()) > 0)) {
                                        return false;
                                    }
                                    
                                    // Filter by featured
                                    if (criteria.getFeatured() != null && 
                                        (property.getIsFeatured() == null || !property.getIsFeatured().equals(criteria.getFeatured()))) {
                                        return false;
                                    }
                                    
                                    // Filter by search term (text search in title/description)
                                    if (criteria.getSearchTerm() != null && !criteria.getSearchTerm().trim().isEmpty()) {
                                        String searchTerm = criteria.getSearchTerm().toLowerCase();
                                        String title = property.getTitle() != null ? property.getTitle().toLowerCase() : "";
                                        String description = property.getDescription() != null ? property.getDescription().toLowerCase() : "";
                                        return title.contains(searchTerm) || description.contains(searchTerm);
                                    }
                                    
                                    return true;
                                })
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
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), properties.size());
        List<PropertyDto> pageContent = start < properties.size() ? 
                properties.subList(start, Math.min(end, properties.size())) : List.of();
        return new PageImpl<>(pageContent, pageable, properties.size());
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

        public String getSearchTerm() { return searchTerm; }
        public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }

        public String getCityName() { return cityName; }
        public void setCityName(String cityName) { this.cityName = cityName; }

        public String getPropertyTypeName() { return propertyTypeName; }
        public void setPropertyTypeName(String propertyTypeName) { this.propertyTypeName = propertyTypeName; }

        public BigDecimal getMinPrice() { return minPrice; }
        public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

        public BigDecimal getMaxPrice() { return maxPrice; }
        public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

        public Integer getMinBeds() { return minBeds; }
        public void setMinBeds(Integer minBeds) { this.minBeds = minBeds; }

        public Integer getMinBaths() { return minBaths; }
        public void setMinBaths(Integer minBaths) { this.minBaths = minBaths; }

        public BigDecimal getMinArea() { return minArea; }
        public void setMinArea(BigDecimal minArea) { this.minArea = minArea; }

        public BigDecimal getMaxArea() { return maxArea; }
        public void setMaxArea(BigDecimal maxArea) { this.maxArea = maxArea; }

        public Boolean getFeatured() { return featured; }
        public void setFeatured(Boolean featured) { this.featured = featured; }

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





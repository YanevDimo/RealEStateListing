package app.service;


import app.entity.Property;
import app.entity.PropertyStatus;
import app.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SearchService {

    private final PropertyRepository propertyRepository;
    private final CityService cityService;
    private final PropertyTypeService propertyTypeService;


    public List<Property> searchProperties(SearchCriteria criteria) {
        log.debug("Searching properties with criteria: {}", criteria);
        
        // Get all active properties and filter in memory
        List<Property> allProperties = propertyRepository.findByStatus(PropertyStatus.ACTIVE);
        
        return allProperties.stream()
                .filter(property -> {
                    // Filter by city
                    if (criteria.getCityName() != null && !criteria.getCityName().trim().isEmpty()) {
                        if (!property.getCity().getName().equalsIgnoreCase(criteria.getCityName().trim())) {
                            return false;
                        }
                    }
                    
                    // Filter by property type
                    if (criteria.getPropertyTypeName() != null && !criteria.getPropertyTypeName().trim().isEmpty()) {
                        if (!property.getPropertyType().getName().equalsIgnoreCase(criteria.getPropertyTypeName().trim())) {
                            return false;
                        }
                    }
                    
                    // Filter by price range
                    if (criteria.getMinPrice() != null && property.getPrice().compareTo(criteria.getMinPrice()) < 0) {
                        return false;
                    }
                    if (criteria.getMaxPrice() != null && property.getPrice().compareTo(criteria.getMaxPrice()) > 0) {
                        return false;
                    }
                    
                    // Filter by beds
                    if (criteria.getMinBeds() != null && property.getBeds() < criteria.getMinBeds()) {
                        return false;
                    }
                    
                    // Filter by baths
                    if (criteria.getMinBaths() != null && property.getBaths() < criteria.getMinBaths()) {
                        return false;
                    }
                    
                    // Filter by area range
                    if (criteria.getMinArea() != null && property.getAreaSqm().compareTo(criteria.getMinArea()) < 0) {
                        return false;
                    }
                    if (criteria.getMaxArea() != null && property.getAreaSqm().compareTo(criteria.getMaxArea()) > 0) {
                        return false;
                    }
                    
                    // Filter by featured
                    return criteria.getFeatured() == null || property.getFeatured().equals(criteria.getFeatured());
                })
                .toList();
    }

    /**
     * Search properties with pagination
     */
    public Page<Property> searchProperties(SearchCriteria criteria, Pageable pageable) {
        log.debug("Searching properties with criteria: {} and pagination: {}", criteria, pageable);
        
        // For now, we'll return all results without pagination
        // In a real implementation, you'd want to implement pagination at the repository level
        return Page.empty(pageable);
    }

    /**
     * Search properties by text
     */
    public List<Property> searchPropertiesByText(String searchTerm) {
        log.debug("Searching properties by text: {}", searchTerm);
        
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE).stream()
                .filter(property -> {
                    String lowerSearchTerm = searchTerm.toLowerCase();
                    return property.getTitle().toLowerCase().contains(lowerSearchTerm) ||
                           property.getDescription().toLowerCase().contains(lowerSearchTerm);
                })
                .toList();
    }

    /**
     * Get featured properties
     */
    public List<Property> getFeaturedProperties() {
        log.debug("Getting featured properties");
        return propertyRepository.findByFeaturedTrueAndStatus(PropertyStatus.ACTIVE);
    }


    public List<Property> getPropertiesByCityName(String cityName) {
        log.debug("Getting properties by city name: {}", cityName);
        
        return cityService.findCityByNameIgnoreCase(cityName)
                .map(city -> propertyRepository.findByCityIdAndStatus(city.getId(), PropertyStatus.ACTIVE))
                .orElse(List.of());
    }


    public List<Property> getPropertiesByPropertyTypeName(String propertyTypeName) {
        log.debug("Getting properties by property type name: {}", propertyTypeName);
        
        return propertyTypeService.findPropertyTypeByNameIgnoreCase(propertyTypeName)
                .map(type -> propertyRepository.findByPropertyTypeIdAndStatus(type.getId(), PropertyStatus.ACTIVE))
                .orElse(List.of());
    }


    public List<Property> getPropertiesByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Getting properties by price range: {} - {}", minPrice, maxPrice);
        
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE).stream()
                .filter(property -> {
                    if (minPrice != null && property.getPrice().compareTo(minPrice) < 0) return false;
                    if (maxPrice != null && property.getPrice().compareTo(maxPrice) > 0) return false;
                    return true;
                })
                .toList();
    }


    public List<Property> getPropertiesByBeds(Integer beds) {
        log.debug("Getting properties by beds: {}", beds);
        
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE).stream()
                .filter(property -> property.getBeds() >= beds)
                .toList();
    }


    public List<Property> getPropertiesByBaths(Integer baths) {
        log.debug("Getting properties by baths: {}", baths);
        
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE).stream()
                .filter(property -> property.getBaths() >= baths)
                .toList();
    }


    public List<Property> getPropertiesByAreaRange(BigDecimal minArea, BigDecimal maxArea) {
        log.debug("Getting properties by area range: {} - {}", minArea, maxArea);
        
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE).stream()
                .filter(property -> {
                    if (minArea != null && property.getAreaSqm().compareTo(minArea) < 0) return false;
                    if (maxArea != null && property.getAreaSqm().compareTo(maxArea) > 0) return false;
                    return true;
                })
                .toList();
    }


    public List<Property> getPropertiesNearLocation(BigDecimal latitude, BigDecimal longitude, Double radiusKm) {
        log.debug("Getting properties near location: {}, {} within {} km", latitude, longitude, radiusKm);
        // For now, return all properties with coordinates - implement distance calculation in service layer
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE).stream()
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .toList();
    }


    public List<String> getAvailableCities() {
        log.debug("Getting available cities for search");
        return cityService.findAllCities().stream()
                .map(city -> city.getName())
                .sorted()
                .toList();
    }


    public List<String> getAvailablePropertyTypes() {
        log.debug("Getting available property types for search");
        return propertyTypeService.findAllPropertyTypes().stream()
                .map(type -> type.getName())
                .sorted()
                .toList();
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

        // Getters and Setters
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





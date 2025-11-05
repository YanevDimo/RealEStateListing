package app.service;

import app.client.PropertyServiceClient;
import app.dto.PropertyDto;
import app.entity.City;
import app.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
public class CityService {

    private final CityRepository cityRepository;
    private final PropertyServiceClient propertyServiceClient;
    private final PropertyUtilityService propertyUtilityService;


    @Cacheable("cities")
    public List<City> findAllCities() {
        log.debug("Finding all cities");
        return cityRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = "cities", allEntries = true)
    public City saveCity(City city) {
        log.debug("Saving city: {}", city.getName());
        return cityRepository.save(city);
    }


    public Optional<City> findCityById(UUID id) {
        log.debug("Finding city by ID: {}", id);
        return cityRepository.findById(id);
    }


    public Optional<City> findCityByName(String name) {
        log.debug("Finding city by name: {}", name);
        return cityRepository.findByName(name);
    }


    public Optional<City> findCityByNameIgnoreCase(String name) {
        log.debug("Finding city by name (case insensitive): {}", name);
        return cityRepository.findByName(name.toLowerCase());
    }


    public boolean cityExistsByName(String name) {
        log.debug("Checking if city exists by name: {}", name);
        return cityRepository.existsByName(name);
    }


    public List<City> findCitiesByCountry(String country) {
        log.debug("Finding cities by country: {}", country);
        return cityRepository.findByCountry(country);
    }


    public List<City> findCitiesByCountryIgnoreCase(String country) {
        log.debug("Finding cities by country (case insensitive): {}", country);
        return cityRepository.findByCountry(country.toLowerCase());
    }


    public List<City> searchCitiesByName(String name) {
        log.debug("Searching cities by name: {}", name);
        return cityRepository.findAll().stream()
                .filter(c -> c.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }


    public List<City> searchCitiesByCountry(String country) {
        log.debug("Searching cities by country: {}", country);
        return cityRepository.findAll().stream()
                .filter(c -> c.getCountry().toLowerCase().contains(country.toLowerCase()))
                .toList();
    }


    public List<City> findCitiesWithCoordinates() {
        log.debug("Finding cities with coordinates");
        return cityRepository.findAll().stream()
                .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                .toList();
    }


    public List<City> findCitiesWithoutCoordinates() {
        log.debug("Finding cities without coordinates");
        return cityRepository.findAll().stream()
                .filter(c -> c.getLatitude() == null || c.getLongitude() == null)
                .toList();
    }


    public List<City> findCitiesWithActiveProperties() {
        log.debug("Finding cities with active properties");
        return cityRepository.findAll().stream()
                .filter(c -> propertyUtilityService.hasActiveProperties(c.getId()))
                .toList();
    }


    public List<City> findCitiesOrderedByPropertyCount() {
        log.debug("Finding cities ordered by property count");
        
        // Count properties per city using utility service
        return cityRepository.findAll().stream()
                .sorted((a, b) -> {
                    long countA = propertyUtilityService.countActivePropertiesByCityId(a.getId());
                    long countB = propertyUtilityService.countActivePropertiesByCityId(b.getId());
                    return Long.compare(countB, countA); // Descending order
                })
                .toList();
    }


    public List<City> findCitiesWithMostProperties() {
        log.debug("Finding cities with most properties");
        return findCitiesOrderedByPropertyCount();
    }


    public List<City> findCitiesNearLocation(BigDecimal latitude, BigDecimal longitude, Double radiusKm) {
        log.debug("Finding cities near location: {}, {} within {} km", latitude, longitude, radiusKm);
        // For now, return all cities with coordinates - implement distance calculation in service layer
        return findCitiesWithCoordinates();
    }


    public List<City> findCitiesCreatedAfter(LocalDateTime date) {
        log.debug("Finding cities created after: {}", date);
        return cityRepository.findAll().stream()
                .filter(c -> c.getCreatedAt().isAfter(date))
                .toList();
    }


    public List<String> findAllCountries() {
        log.debug("Finding all unique countries");
        return cityRepository.findAll().stream()
                .map(City::getCountry)
                .distinct()
                .sorted()
                .toList();
    }


    public long countActivePropertiesByCity(UUID cityId) {
        log.debug("Counting active properties by city: {}", cityId);
        try {
            List<PropertyDto> properties = propertyServiceClient.getPropertiesByCity(cityId);
            return properties.stream()
                    .filter(p -> p.getStatus() == null || "ACTIVE".equals(p.getStatus()))
                    .count();
        } catch (Exception e) {
            log.error("Error counting properties from property-service for city: {}", cityId, e);
            return 0;
        }
    }


    @Transactional
    public City updateCity(City city) {
        log.debug("Updating city: {}", city.getName());
        return cityRepository.save(city);
    }


    @Transactional
    public void deleteCity(UUID id) {
        log.debug("Deleting city with ID: {}", id);
        cityRepository.deleteById(id);
    }

    @Transactional
    public City createCity(String name, String country, BigDecimal latitude, BigDecimal longitude) {
        log.debug("Creating new city: {}", name);
        
        if (cityRepository.existsByName(name)) {
            throw new RuntimeException("City with name " + name + " already exists");
        }

        City city = City.builder()
                .name(name)
                .country(country != null ? country : "Bulgaria")
                .latitude(latitude)
                .longitude(longitude)
                .build();

        return cityRepository.save(city);
    }


    @Transactional
    public City updateCityCoordinates(UUID id, BigDecimal latitude, BigDecimal longitude) {
        log.debug("Updating city coordinates: {}", id);
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("City not found with ID: " + id));
        city.setLatitude(latitude);
        city.setLongitude(longitude);
        return cityRepository.save(city);
    }


    @Transactional
    public City getOrCreateCityByName(String name, String country) {
        log.debug("Getting or creating city by name: {}", name);
        
        Optional<City> existingCity = findCityByNameIgnoreCase(name);
        if (existingCity.isPresent()) {
            return existingCity.get();
        }

        return createCity(name, country, null, null);
    }


    public CityStatistics getCityStatistics() {
        log.debug("Getting city statistics");
        long totalCities = cityRepository.count();
        long citiesWithCoordinates = findCitiesWithCoordinates().size();
        long citiesWithProperties = findCitiesWithActiveProperties().size();
        List<String> countries = findAllCountries();
        
        return new CityStatistics(totalCities, citiesWithCoordinates, citiesWithProperties, countries.size());
    }

    public static class CityStatistics {
        private final long totalCities;
        private final long citiesWithCoordinates;
        private final long citiesWithProperties;
        private final int totalCountries;

        public CityStatistics(long totalCities, long citiesWithCoordinates, long citiesWithProperties, int totalCountries) {
            this.totalCities = totalCities;
            this.citiesWithCoordinates = citiesWithCoordinates;
            this.citiesWithProperties = citiesWithProperties;
            this.totalCountries = totalCountries;
        }

        public long getTotalCities() { return totalCities; }
        public long getCitiesWithCoordinates() { return citiesWithCoordinates; }
        public long getCitiesWithProperties() { return citiesWithProperties; }
        public int getTotalCountries() { return totalCountries; }
    }
}
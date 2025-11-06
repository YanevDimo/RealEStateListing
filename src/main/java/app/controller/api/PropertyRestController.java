package app.controller.api;

import app.client.PropertyServiceClient;
import app.dto.PropertyDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Property REST Controller - Proxies requests to property-service microservice.
 * This controller acts as a proxy/forwarder to the property-service.
 */
@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
@Slf4j
public class PropertyRestController {

    private final PropertyServiceClient propertyServiceClient;

    @GetMapping
    public ResponseEntity<List<PropertyDto>> getAllProperties(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) UUID propertyTypeId,
            @RequestParam(required = false) Double maxPrice) {
        
        try {
            List<PropertyDto> properties = propertyServiceClient.getAllProperties(
                    search, cityId, propertyTypeId, maxPrice);
            return ResponseEntity.ok(properties);
        } catch (Exception e) {
            log.error("Error calling property-service for getAllProperties", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropertyDto> getPropertyById(@PathVariable UUID id) {
        try {
            PropertyDto property = propertyServiceClient.getPropertyById(id);
            if (property == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(property);
        } catch (Exception e) {
            log.error("Error calling property-service for getPropertyById: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/featured")
    public ResponseEntity<List<PropertyDto>> getFeaturedProperties() {
        try {
            List<PropertyDto> properties = propertyServiceClient.getFeaturedProperties();
            return ResponseEntity.ok(properties);
        } catch (Exception e) {
            log.error("Error calling property-service for getFeaturedProperties", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/agent/{agentId}")
    public ResponseEntity<List<PropertyDto>> getPropertiesByAgent(@PathVariable UUID agentId) {
        try {
            List<PropertyDto> properties = propertyServiceClient.getPropertiesByAgent(agentId);
            return ResponseEntity.ok(properties);
        } catch (Exception e) {
            log.error("Error calling property-service for getPropertiesByAgent: {}", agentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/city/{cityId}")
    public ResponseEntity<List<PropertyDto>> getPropertiesByCity(@PathVariable UUID cityId) {
        try {
            List<PropertyDto> properties = propertyServiceClient.getPropertiesByCity(cityId);
            return ResponseEntity.ok(properties);
        } catch (Exception e) {
            log.error("Error calling property-service for getPropertiesByCity: {}", cityId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<PropertyDto>> searchProperties(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) UUID propertyTypeId,
            @RequestParam(required = false) Double maxPrice) {
        try {
            List<PropertyDto> properties = propertyServiceClient.searchProperties(
                    search, cityId, propertyTypeId, maxPrice);
            return ResponseEntity.ok(properties);
        } catch (Exception e) {
            log.error("Error calling property-service for searchProperties", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProperty(@PathVariable UUID id) {
        try {
            propertyServiceClient.deleteProperty(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error calling property-service for deleteProperty: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Note: Create, Update, and Toggle Featured operations 
    // should be handled directly by property-service
    // These endpoints are not proxied to avoid duplication
}


package app.client;

import app.dto.PropertyCreateDto;
import app.dto.PropertyDto;
import app.dto.PropertyUpdateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

 // Feign Client for calling Property Service from main application.
 // This allows the main app to fetch property data from the property-service microservice.

@FeignClient(name = "property-service", url = "${property.service.url:http://localhost:8083}")
public interface PropertyServiceClient {

    @GetMapping("/api/v1/properties")
    List<PropertyDto> getAllProperties(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) UUID propertyTypeId,
            @RequestParam(required = false) Double maxPrice
    );

    @GetMapping("/api/v1/properties/{id}")
    PropertyDto getPropertyById(@PathVariable UUID id);

    @GetMapping("/api/v1/properties/featured")
    List<PropertyDto> getFeaturedProperties();

    @GetMapping("/api/v1/properties/search")
    List<PropertyDto> searchProperties(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) UUID propertyTypeId,
            @RequestParam(required = false) Double maxPrice
    );

    @GetMapping("/api/v1/properties/agent/{agentId}")
    List<PropertyDto> getPropertiesByAgent(@PathVariable UUID agentId);

    @GetMapping("/api/v1/properties/city/{cityId}")
    List<PropertyDto> getPropertiesByCity(@PathVariable UUID cityId);

    @PostMapping("/api/v1/properties")
    PropertyDto createProperty(@RequestBody PropertyCreateDto dto);

    @PutMapping("/api/v1/properties/{id}")
    PropertyDto updateProperty(@PathVariable UUID id, @RequestBody PropertyUpdateDto dto);

    @DeleteMapping("/api/v1/properties/{id}")
    void deleteProperty(@PathVariable UUID id);
}


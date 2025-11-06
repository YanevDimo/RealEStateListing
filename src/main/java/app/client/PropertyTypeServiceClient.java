package app.client;

import app.dto.PropertyTypeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign Client for calling Property Type Service from Property Service.
 * This interface will be used when Property Service is extracted.
 */
@FeignClient(name = "property-type-service", url = "${property-type.service.url:http://localhost:8080}")
public interface PropertyTypeServiceClient {

    @GetMapping("/api/v1/property-types/{typeId}")
    PropertyTypeDto getPropertyType(@PathVariable UUID typeId);

    @GetMapping("/api/v1/property-types/{typeId}/exists")
    boolean propertyTypeExists(@PathVariable UUID typeId);
}






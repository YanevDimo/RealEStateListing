package app.client;

import app.dto.CityDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign Client for calling City Service from Property Service.
 * This interface will be used when Property Service is extracted.
 */
@FeignClient(name = "city-service", url = "${city.service.url:http://localhost:8080}")
public interface CityServiceClient {

    @GetMapping("/api/v1/cities/{cityId}")
    CityDto getCity(@PathVariable UUID cityId);

    @GetMapping("/api/v1/cities/{cityId}/exists")
    boolean cityExists(@PathVariable UUID cityId);
}






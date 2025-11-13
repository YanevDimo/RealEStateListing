package app.controller.api;

import app.dto.CityDto;
import app.entity.City;
import app.exception.CityNotFoundException;
import app.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cities")
@RequiredArgsConstructor
public class CityRestController {

    private final CityService cityService;

    @GetMapping("/{cityId}")
    public ResponseEntity<CityDto> getCity(@PathVariable UUID cityId) {
        City city = cityService.findCityById(cityId)
                .orElseThrow(() -> new CityNotFoundException("City not found with ID: " + cityId));
        
        // Map to DTO format expected by property-service
        CityDto dto = CityDto.builder()
                .id(city.getId())
                .name(city.getName())
                .state(null) // State not available in main app entity
                .country(city.getCountry())
                .build();
        
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{cityId}/exists")
    public ResponseEntity<Boolean> cityExists(@PathVariable UUID cityId) {
        boolean exists = cityService.findCityById(cityId).isPresent();
        return ResponseEntity.ok(exists);
    }
}








package app.controller.api;

import app.dto.PropertyTypeDto;
import app.entity.PropertyType;
import app.exception.PropertyTypeNotFoundException;
import app.service.PropertyTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/property-types")
@RequiredArgsConstructor
public class PropertyTypeRestController {

    private final PropertyTypeService propertyTypeService;

    @GetMapping("/{typeId}")
    public ResponseEntity<PropertyTypeDto> getPropertyType(@PathVariable UUID typeId) {
        PropertyType propertyType = propertyTypeService.findPropertyTypeById(typeId)
                .orElseThrow(() -> new PropertyTypeNotFoundException("Property type not found with ID: " + typeId));
        
        PropertyTypeDto dto = PropertyTypeDto.builder()
                .id(propertyType.getId())
                .name(propertyType.getName())
                .description(propertyType.getDescription())
                .build();
        
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{typeId}/exists")
    public ResponseEntity<Boolean> propertyTypeExists(@PathVariable UUID typeId) {
        boolean exists = propertyTypeService.findPropertyTypeById(typeId).isPresent();
        return ResponseEntity.ok(exists);
    }
}








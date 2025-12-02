package app.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PropertyCreateDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidPropertyCreateDto() {
        PropertyCreateDto dto = PropertyCreateDto.builder()
                .title("Test Property")
                .description("Test Description")
                .price(new BigDecimal("100000"))
                .agentId(UUID.randomUUID())
                .cityId(UUID.randomUUID())
                .propertyTypeId(UUID.randomUUID())
                .bedrooms(3)
                .bathrooms(2)
                .build();

        Set<ConstraintViolation<PropertyCreateDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testTitle_NotBlank() {
        PropertyCreateDto dto = PropertyCreateDto.builder()
                .title("")
                .price(new BigDecimal("100000"))
                .agentId(UUID.randomUUID())
                .cityId(UUID.randomUUID())
                .propertyTypeId(UUID.randomUUID())
                .build();

        Set<ConstraintViolation<PropertyCreateDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    void testPrice_NotNull() {
        PropertyCreateDto dto = PropertyCreateDto.builder()
                .title("Test Property")
                .price(null)
                .agentId(UUID.randomUUID())
                .cityId(UUID.randomUUID())
                .propertyTypeId(UUID.randomUUID())
                .build();

        Set<ConstraintViolation<PropertyCreateDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("price")));
    }

    @Test
    void testPrice_DecimalMin() {
        PropertyCreateDto dto = PropertyCreateDto.builder()
                .title("Test Property")
                .price(BigDecimal.ZERO)
                .agentId(UUID.randomUUID())
                .cityId(UUID.randomUUID())
                .propertyTypeId(UUID.randomUUID())
                .build();

        Set<ConstraintViolation<PropertyCreateDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("price")));
    }

    @Test
    void testBedrooms_Min() {
        PropertyCreateDto dto = PropertyCreateDto.builder()
                .title("Test Property")
                .price(new BigDecimal("100000"))
                .agentId(UUID.randomUUID())
                .cityId(UUID.randomUUID())
                .propertyTypeId(UUID.randomUUID())
                .bedrooms(-1)
                .build();

        Set<ConstraintViolation<PropertyCreateDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("bedrooms")));
    }
}





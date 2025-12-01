package app.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PropertyUpdateDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidPropertyUpdateDto() {
        PropertyUpdateDto dto = PropertyUpdateDto.builder()
                .title("Updated Property")
                .description("Updated Description")
                .price(new BigDecimal("150000"))
                .bedrooms(4)
                .bathrooms(3)
                .build();

        Set<ConstraintViolation<PropertyUpdateDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testPrice_DecimalMin() {
        PropertyUpdateDto dto = PropertyUpdateDto.builder()
                .price(BigDecimal.ZERO)
                .build();

        Set<ConstraintViolation<PropertyUpdateDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testBedrooms_Min() {
        PropertyUpdateDto dto = PropertyUpdateDto.builder()
                .bedrooms(-1)
                .build();

        Set<ConstraintViolation<PropertyUpdateDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testBathrooms_Min() {
        PropertyUpdateDto dto = PropertyUpdateDto.builder()
                .bathrooms(-1)
                .build();

        Set<ConstraintViolation<PropertyUpdateDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }
}



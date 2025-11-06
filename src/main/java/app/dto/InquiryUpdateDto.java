package app.dto;

import app.entity.InquiryStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryUpdateDto {

    private InquiryStatus status;

    @Size(max = 2000, message = "Response must not exceed 2000 characters")
    private String response;
}


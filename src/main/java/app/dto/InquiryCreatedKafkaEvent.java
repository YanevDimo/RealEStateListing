package app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryCreatedKafkaEvent {
    private UUID inquiryId;
    private UUID propertyId;
    private String propertyTitle;
    private UUID agentId;
    private String agentEmail;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String message;
    private LocalDateTime createdAt;
}


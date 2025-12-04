package app.listener;

import app.dto.InquiryCreatedKafkaEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class InquiryKafkaListener {


    @KafkaListener(topics = "inquiry-created", groupId = "inquiry-consumer-group")
    public void handleInquiryCreated(InquiryCreatedKafkaEvent event, Acknowledgment acknowledgment) {
        log.info(" KAFKA EVENT RECEIVED: New inquiry created via Kafka");
        log.info("   Inquiry ID: {}", event.getInquiryId());
        log.info("   Property: '{}' (ID: {})", event.getPropertyTitle(), event.getPropertyId());
        log.info("   Agent: {} (ID: {})", event.getAgentEmail(), event.getAgentId());
        log.info("   Contact: {} ({})", event.getContactName(), event.getContactEmail());
        log.info("   Message: {}", 
                event.getMessage() != null && event.getMessage().length() > 50 
                    ? event.getMessage().substring(0, 50) + "..." 
                    : event.getMessage());
        

        if (acknowledgment != null) {
            acknowledgment.acknowledge();
        }
        
        log.info(" Kafka event processed successfully");
    }
}


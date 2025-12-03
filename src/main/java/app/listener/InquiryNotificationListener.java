package app.listener;

import app.event.InquiryCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InquiryNotificationListener {

    @EventListener
    @Async
    public void sendAgentNotification(InquiryCreatedEvent event) {
        log.info("EVENT: Sending notification to agent: {} about new inquiry for property: '{}'",
                event.getAgentEmail(), event.getPropertyTitle());
        log.info("Inquiry details - From: {} ({}) - Message: {}",
                event.getInquiry().getContactName(),
                event.getInquiry().getContactEmail(),
                event.getInquiry().getMessage().length() > 50 
                    ? event.getInquiry().getMessage().substring(0, 50) + "..." 
                    : event.getInquiry().getMessage());
        
    }

    @EventListener
    @Async
    public void sendWelcomeToInquirer(InquiryCreatedEvent event) {
        log.info("EVENT: Sending welcome email to: {} for inquiry about property: '{}'",
                event.getInquiry().getContactEmail(), event.getPropertyTitle());
        log.info("Thank you for your interest! We will contact you soon regarding: {}",
                event.getPropertyTitle());
        
    }
}



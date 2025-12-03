package app.event;

import app.entity.Inquiry;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class InquiryCreatedEvent extends ApplicationEvent {
    private final Inquiry inquiry;
    private final UUID propertyId;
    private final String propertyTitle;
    private final UUID agentId;
    private final String agentEmail;

    public InquiryCreatedEvent(Object source, Inquiry inquiry, UUID propertyId, 
                              String propertyTitle, UUID agentId, String agentEmail) {
        super(source);
        this.inquiry = inquiry;
        this.propertyId = propertyId;
        this.propertyTitle = propertyTitle;
        this.agentId = agentId;
        this.agentEmail = agentEmail;
    }
}



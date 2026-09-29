package com.aicyber.jgmoli.email.provider;

import com.aicyber.jgmoli.email.model.QueuedEmail;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "jgmoli.email.provider", havingValue = "disabled", matchIfMissing = true)
public class DisabledEmailGateway implements EmailGateway {
    @Override
    public String send(QueuedEmail email) {
        throw new IllegalStateException("Transactional email delivery is not configured");
    }
}


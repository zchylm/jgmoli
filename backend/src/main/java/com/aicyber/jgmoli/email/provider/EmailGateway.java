package com.aicyber.jgmoli.email.provider;

import com.aicyber.jgmoli.email.model.QueuedEmail;

public interface EmailGateway {
    String send(QueuedEmail email);
}


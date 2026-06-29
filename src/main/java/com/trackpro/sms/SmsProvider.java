package com.trackpro.sms;

import com.trackpro.sms.dto.OutboundSmsResult;
import java.util.List;

public interface SmsProvider {

    String getProviderName();

    OutboundSmsResult send(String to, String message);

    default List<OutboundSmsResult> sendBulk(List<String> recipients, String message) {
        return recipients.stream().map(r -> send(r, message)).toList();
    }

    OutboundSmsResult checkStatus(String messageId);
}

package com.trackpro.sms;

import org.springframework.stereotype.Component;

@Component
public class SmsPhoneNumberNormalizer {

    public String normalise(String number) {
        if (number == null) return "";
        String cleaned = number.replaceAll("[^0-9+]", "");
        if (cleaned.startsWith("0")) {
            return "+234" + cleaned.substring(1);
        }
        if (cleaned.startsWith("234") && !cleaned.startsWith("+")) {
            return "+" + cleaned;
        }
        return cleaned;
    }
}

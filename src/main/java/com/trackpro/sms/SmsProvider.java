package com.trackpro.sms;

public interface SmsProvider {
    void send(String to, String message);
}

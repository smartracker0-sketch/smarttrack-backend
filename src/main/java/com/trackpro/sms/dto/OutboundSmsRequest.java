package com.trackpro.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OutboundSmsRequest {

    @JsonProperty("api_key")
    private String apiKey;

    private String to;
    private String from;
    private String sms;
    private String type = "plain";
    private String channel = "generic";

    public OutboundSmsRequest() {}

    public OutboundSmsRequest(String apiKey, String from, String to, String sms) {
        this.apiKey = apiKey;
        this.from = from;
        this.to = to;
        this.sms = sms;
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getSms() { return sms; }
    public void setSms(String sms) { this.sms = sms; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
}

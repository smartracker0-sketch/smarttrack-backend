package com.trackpro.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InboundSmsPayload {

    @JsonProperty("from")
    private String from;

    @JsonProperty("to")
    private String to;

    @JsonProperty("text")
    private String text;

    @JsonProperty("id")
    private String id;

    @JsonProperty("date")
    private String date;

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}

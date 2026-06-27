package com.trackpro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trackpro.sms")
public class SmsProperties {

    private String provider = "termii";
    private Termii termii = new Termii();
    private Activation activation = new Activation();
    private Server server = new Server();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public Termii getTermii() { return termii; }
    public void setTermii(Termii termii) { this.termii = termii; }

    public Activation getActivation() { return activation; }
    public void setActivation(Activation activation) { this.activation = activation; }

    public Server getServer() { return server; }
    public void setServer(Server server) { this.server = server; }

    public static class Termii {
        private String apiKey = "";
        private String senderId = "N-Alert";
        private String baseUrl = "https://api.ng.termii.com";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class Activation {
        private String command = "STATUS";
        private int timeoutMinutes = 5;
        private int retryAttempts = 2;
        private int retryIntervalMinutes = 3;

        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }

        public int getTimeoutMinutes() { return timeoutMinutes; }
        public void setTimeoutMinutes(int timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }

        public int getRetryAttempts() { return retryAttempts; }
        public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }

        public int getRetryIntervalMinutes() { return retryIntervalMinutes; }
        public void setRetryIntervalMinutes(int retryIntervalMinutes) { this.retryIntervalMinutes = retryIntervalMinutes; }
    }

    public static class Server {
        private String host = "";
        private int tcpPort = 5023;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getTcpPort() { return tcpPort; }
        public void setTcpPort(int tcpPort) { this.tcpPort = tcpPort; }
    }
}

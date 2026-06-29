package com.trackpro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trackpro.sms")
public class SmsProperties {

    private String provider = "termii";
    private Termii termii = new Termii();
    private AfricasTalking africastalking = new AfricasTalking();
    private Activation activation = new Activation();
    private Server server = new Server();
    private Retry retry = new Retry();
    private RateLimit rateLimit = new RateLimit();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public Termii getTermii() { return termii; }
    public void setTermii(Termii termii) { this.termii = termii; }

    public Activation getActivation() { return activation; }
    public void setActivation(Activation activation) { this.activation = activation; }

    public Server getServer() { return server; }
    public void setServer(Server server) { this.server = server; }

    public AfricasTalking getAfricastalking() { return africastalking; }
    public void setAfricastalking(AfricasTalking africastalking) { this.africastalking = africastalking; }

    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }

    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }

    public static class AfricasTalking {
        private String apiKey = "";
        private String username = "";
        private String senderId = "STTracker";
        private String baseUrl = "https://api.africastalking.com";
        private boolean sandboxMode = false;
        private int timeoutSeconds = 10;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public boolean isSandboxMode() { return sandboxMode; }
        public void setSandboxMode(boolean sandboxMode) { this.sandboxMode = sandboxMode; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public static class Retry {
        private int maxAttempts = 3;
        private int backoffSeconds = 5;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public int getBackoffSeconds() { return backoffSeconds; }
        public void setBackoffSeconds(int backoffSeconds) { this.backoffSeconds = backoffSeconds; }
    }

    public static class RateLimit {
        private int maxPerMinute = 60;

        public int getMaxPerMinute() { return maxPerMinute; }
        public void setMaxPerMinute(int maxPerMinute) { this.maxPerMinute = maxPerMinute; }
    }

    public static class Termii {
        private String apiKey = "";
        private String senderId = "N-Alert";
        private String baseUrl = "https://api.ng.termii.com";
        private String channel = "generic";
        private int timeoutSeconds = 10;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
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

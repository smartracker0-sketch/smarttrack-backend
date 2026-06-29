package com.trackpro.controller;

import com.trackpro.sms.SmsService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/platform")
public class AdminPlatformController {

    private final SmsService smsService;

    public AdminPlatformController(SmsService smsService) {
        this.smsService = smsService;
    }

    @GetMapping("/settings/sms-provider")
    public Map<String, String> getSmsProvider() {
        return Map.of("provider", smsService.getActiveProviderName());
    }

    @PatchMapping("/settings/sms-provider")
    public ResponseEntity<Map<String, String>> switchSmsProvider(@RequestBody Map<String, String> body) {
        String provider = body.get("provider");
        if (provider == null || provider.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "provider field is required"));
        }
        try {
            smsService.switchProvider(provider);
            return ResponseEntity.ok(Map.of("provider", provider, "message", "SMS provider switched successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

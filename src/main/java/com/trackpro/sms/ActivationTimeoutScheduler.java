package com.trackpro.sms;

import com.trackpro.config.SmsProperties;
import com.trackpro.model.DeviceEntity;
import com.trackpro.repository.DeviceRepository;
import com.trackpro.sms.dto.DeviceActivationEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ActivationTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(ActivationTimeoutScheduler.class);

    private final DeviceRepository deviceRepository;
    private final DeviceActivationService activationService;
    private final SimpMessagingTemplate ws;
    private final SmsProperties props;

    public ActivationTimeoutScheduler(DeviceRepository deviceRepository,
                                      DeviceActivationService activationService,
                                      SimpMessagingTemplate ws,
                                      SmsProperties props) {
        this.deviceRepository = deviceRepository;
        this.activationService = activationService;
        this.ws = ws;
        this.props = props;
    }

    @Scheduled(fixedRate = 120_000)
    @Transactional
    public void checkForTimedOutActivations() {
        Instant cutoff = Instant.now().minus(
                Duration.ofMinutes(props.getActivation().getTimeoutMinutes()));

        List<DeviceEntity> timedOut = deviceRepository
                .findByActivationStatusAndActivationAttemptedAtBefore(
                        DeviceActivationStatus.PENDING.name(), cutoff);

        if (timedOut.isEmpty()) return;
        log.info("Activation timeout check — {} device(s) pending past cutoff", timedOut.size());

        for (DeviceEntity device : timedOut) {
            int attempts = device.getActivationAttempts();
            if (attempts < props.getActivation().getRetryAttempts()) {
                log.info("Retrying activation for IMEI {} (attempt {})", device.getImei(), attempts + 1);
                device.setActivationAttempts(attempts + 1);
                deviceRepository.save(device);
                activationService.initiateCheck(device);
            } else {
                log.warn("IMEI {} exhausted {} retries — marking UNREACHABLE",
                        device.getImei(), props.getActivation().getRetryAttempts());
                device.setActivationStatus(DeviceActivationStatus.UNREACHABLE.name());
                deviceRepository.save(device);

                try {
                    ws.convertAndSend("/topic/admin/device-activation",
                            new DeviceActivationEvent(device.getImei(), DeviceActivationStatus.UNREACHABLE.name(), null));
                } catch (Exception e) {
                    log.warn("WS broadcast failed for UNREACHABLE device {}", device.getImei());
                }
            }
        }
    }
}

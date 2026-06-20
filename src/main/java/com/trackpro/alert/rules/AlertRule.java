package com.trackpro.alert.rules;

import com.trackpro.alert.dto.AlertEvent;
import com.trackpro.model.DeviceEntity;
import com.trackpro.telemetry.DeviceFrame;
import java.util.List;

public interface AlertRule {
    com.trackpro.alert.AlertType getType();
    List<AlertEvent> evaluate(DeviceFrame current, DeviceFrame previous, DeviceEntity device, AlertRuleContext context);
}

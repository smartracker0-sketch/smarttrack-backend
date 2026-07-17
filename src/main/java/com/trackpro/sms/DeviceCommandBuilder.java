package com.trackpro.sms;

import com.trackpro.config.SmsProperties;
import com.trackpro.model.DeviceEntity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeviceCommandBuilder {

    private final SmsProperties props;

    public DeviceCommandBuilder(SmsProperties props) {
        this.props = props;
    }

    public List<String> buildActivationCommands(DeviceEntity device) {
        SmsProperties.CommandProfile profile = props.commandProfileFor(device.getManufacturer());
        List<String> commands = new ArrayList<>();

        String serverHost = props.getServer().getHost();
        if (serverHost != null && !serverHost.isBlank()) {
            commands.add(render(profile.getServerCommandTemplate(), device));
        }

        if (device.getSimApn() != null && !device.getSimApn().isBlank()) {
            commands.add(render(profile.getApnCommandTemplate(), device));
        }

        commands.add(render(profile.getStatusCommand(), device));
        return commands.stream().filter(s -> s != null && !s.isBlank()).toList();
    }

    private String render(String template, DeviceEntity device) {
        if (template == null || template.isBlank()) return "";
        return template
                .replace("{host}", nullToEmpty(props.getServer().getHost()))
                .replace("{port}", String.valueOf(props.getServer().getTcpPort()))
                .replace("{apn}", nullToEmpty(device.getSimApn()))
                .replace("{imei}", nullToEmpty(device.getImei()))
                .replace("{manufacturer}", nullToEmpty(device.getManufacturer()));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

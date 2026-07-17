package com.trackpro.sms;

import com.trackpro.config.SmsProperties;
import com.trackpro.model.DeviceEntity;
import com.trackpro.security.DeviceCommandPasswordCipher;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeviceCommandBuilder {

    private final SmsProperties props;
    private final DeviceCommandPasswordCipher commandPasswordCipher;

    public DeviceCommandBuilder(SmsProperties props, DeviceCommandPasswordCipher commandPasswordCipher) {
        this.props = props;
        this.commandPasswordCipher = commandPasswordCipher;
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
        String password = template.contains("{password}") && device.getSmsCommandPasswordEncrypted() != null
                ? commandPasswordCipher.decrypt(device.getSmsCommandPasswordEncrypted())
                : "";
        return template
                .replace("{password}", password)
                .replace("{host}", nullToEmpty(props.getServer().getHost()))
                .replace("{port}", String.valueOf(props.getServer().getTcpPort()))
                .replace("{apn}", nullToEmpty(device.getSimApn()))
                .replace("{imei}", nullToEmpty(device.getImei()))
                .replace("{manufacturer}", nullToEmpty(device.getManufacturer()))
                .replace("{model}", nullToEmpty(device.getModel()));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

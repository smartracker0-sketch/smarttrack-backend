package com.trackpro.sms;

import com.trackpro.config.SmsProperties;
import com.trackpro.model.DeviceEntity;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceCommandBuilderTest {

    @Test
    void buildsDefaultActivationCommandsWithPlaceholders() {
        SmsProperties props = new SmsProperties();
        props.getServer().setHost("api.smarttracker.cloud");
        props.getServer().setTcpPort(5023);
        DeviceCommandBuilder builder = new DeviceCommandBuilder(props, new com.trackpro.security.DeviceCommandPasswordCipher(""));

        DeviceEntity device = new DeviceEntity();
        device.setImei("123456789012345");
        device.setSimApn("internet");

        assertThat(builder.buildActivationCommands(device))
                .containsExactly(
                        "SERVER,1,api.smarttracker.cloud,5023,0",
                        "APN,internet",
                        "STATUS"
                );
    }

    @Test
    void usesManufacturerSpecificProfileWhenConfigured() {
        SmsProperties props = new SmsProperties();
        props.getServer().setHost("api.smarttracker.cloud");
        props.getServer().setTcpPort(5023);

        SmsProperties.CommandProfile concox = new SmsProperties.CommandProfile();
        concox.setServerCommandTemplate("CONCOX,{host},{port}");
        concox.setApnCommandTemplate("CONCOXAPN,{apn}");
        concox.setStatusCommand("CONCOXSTATUS");
        props.setCommandProfiles(Map.of("generic", new SmsProperties.CommandProfile(), "concox", concox));

        DeviceCommandBuilder builder = new DeviceCommandBuilder(props, new com.trackpro.security.DeviceCommandPasswordCipher(""));
        DeviceEntity device = new DeviceEntity();
        device.setImei("123456789012345");
        device.setManufacturer("Concox");
        device.setSimApn("web.gprs.mtnnigeria.net");

        assertThat(builder.buildActivationCommands(device))
                .containsExactly(
                        "CONCOX,api.smarttracker.cloud,5023",
                        "CONCOXAPN,web.gprs.mtnnigeria.net",
                        "CONCOXSTATUS"
                );
    }
}

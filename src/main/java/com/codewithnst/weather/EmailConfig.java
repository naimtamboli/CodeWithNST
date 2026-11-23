package com.codewithnst.weather;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

record EmailConfig(String sender, String recipient, String appPassword) {
    static EmailConfig load() throws IOException {
        Path configPath = Path.of(System.getenv().getOrDefault(
                "CITY_WEATHER_CONFIG",
                Path.of(System.getenv().getOrDefault("APPDATA", System.getProperty("user.home")),
                        "CityWeather", "weather.properties").toString()));
        if (!Files.isRegularFile(configPath)) {
            throw new IOException("Email configuration not found at " + configPath
                    + ". Run scripts\\setup-weather-email.ps1 first.");
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
        }

        String sender = required(properties, "gmail.address");
        String recipient = required(properties, "gmail.recipient");
        String appPassword = required(properties, "gmail.appPassword");
        return new EmailConfig(sender, recipient, appPassword);
    }

    private static String required(Properties properties, String name) throws IOException {
        String value = properties.getProperty(name, "").trim();
        if (value.isEmpty()) {
            throw new IOException("Missing " + name + " in the weather email configuration.");
        }
        return value;
    }
}

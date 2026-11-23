package com.codewithnst.weather;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class WeatherEmailer {
    private final WeatherService weatherService = new WeatherService();

    public void sendPuneUpdate() throws IOException, InterruptedException, MessagingException {
        EmailConfig config = EmailConfig.load();
        WeatherReport report = weatherService.getCurrentWeather("Pune");

        Properties mailProperties = new Properties();
        mailProperties.setProperty("mail.smtp.host", "smtp.gmail.com");
        mailProperties.setProperty("mail.smtp.port", "587");
        mailProperties.setProperty("mail.smtp.auth", "true");
        mailProperties.setProperty("mail.smtp.starttls.enable", "true");
        mailProperties.setProperty("mail.smtp.starttls.required", "true");
        mailProperties.setProperty("mail.smtp.connectiontimeout", "10000");
        mailProperties.setProperty("mail.smtp.timeout", "10000");
        mailProperties.setProperty("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(mailProperties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.sender(), config.appPassword());
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.sender()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.recipient()));
        message.setSubject("Pune weather update - " + report.updatedAt(), StandardCharsets.UTF_8.name());
        message.setText(report.toText() + "\n\nWeather data provided by Open-Meteo.",
                StandardCharsets.UTF_8.name());
        Transport.send(message);
    }
}

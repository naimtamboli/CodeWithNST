package com.codewithnst.weather;

import jakarta.mail.MessagingException;

import java.io.IOException;

public final class WeatherApp {
    public static void main(String[] args) {
        if (args.length > 0 && "--mcp".equals(args[0])) {
            try {
                new WeatherMcpServer().run();
            } catch (IOException e) {
                System.err.println("Could not start weather MCP server:");
                e.printStackTrace(System.err);
                System.exit(1);
            }
            return;
        }

        if (args.length == 0 || (args.length == 1 && "--send-email".equals(args[0]))) {
            try {
                new WeatherEmailer().sendPuneUpdate();
                System.out.println("Pune weather email sent.");
            } catch (IOException | MessagingException e) {
                System.err.println("Could not send Pune weather email:");
                e.printStackTrace(System.err);
                System.exit(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Weather request was interrupted:");
                e.printStackTrace(System.err);
                System.exit(1);
            }
            return;
        }

        String city = String.join(" ", args).trim();
        if (city.isBlank()) {
            System.err.println("Usage: WeatherApp [--send-email | --mcp | city name]");
            return;
        }

        try {
            WeatherReport report = new WeatherService().getCurrentWeather(city);
            System.out.println(report.toText());
            System.out.println("\nWeather data provided by Open-Meteo.");
        } catch (IOException e) {
            System.err.println("Could not retrieve weather:");
            e.printStackTrace(System.err);
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Weather request was interrupted:");
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}

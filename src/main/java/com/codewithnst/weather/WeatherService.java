package com.codewithnst.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class WeatherService {
    private static final String GEOCODING_URL =
            "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL =
            "https://api.open-meteo.com/v1/forecast";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeatherReport getCurrentWeather(String city) throws IOException, InterruptedException {
        JsonNode location = findLocation(city);
        if (location == null) {
            throw new IOException("No matching city found for \"" + city + "\".");
        }

        JsonNode current = getJson(URI.create(FORECAST_URL
                + "?latitude=" + location.path("latitude").asDouble()
                + "&longitude=" + location.path("longitude").asDouble()
                + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,"
                + "is_day,precipitation,weather_code,wind_speed_10m"
                + "&temperature_unit=celsius&wind_speed_unit=kmh&timezone=auto"))
                .path("current");
        if (current.isMissingNode()) {
            throw new IOException("The weather service returned no current conditions.");
        }

        return new WeatherReport(
                location.path("name").asText(city),
                location.path("admin1").asText(""),
                location.path("country").asText(""),
                describeWeather(current.path("weather_code").asInt(-1)),
                current.path("is_day").asInt() == 1 ? "day" : "night",
                current.path("temperature_2m").asDouble(),
                current.path("apparent_temperature").asDouble(),
                current.path("relative_humidity_2m").asDouble(),
                current.path("wind_speed_10m").asDouble(),
                current.path("precipitation").asDouble(),
                current.path("time").asText("unknown"));
    }

    private JsonNode findLocation(String city) throws IOException, InterruptedException {
        String query = URLEncoder.encode(city, StandardCharsets.UTF_8);
        JsonNode results = getJson(URI.create(GEOCODING_URL
                + "?name=" + query + "&count=1&language=en&format=json"))
                .path("results");
        return results.isArray() && !results.isEmpty() ? results.get(0) : null;
    }

    private JsonNode getJson(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Weather service returned HTTP " + response.statusCode() + ".");
        }
        return objectMapper.readTree(response.body());
    }

    private static String describeWeather(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75, 77 -> "Snow";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95, 96, 99 -> "Thunderstorm";
            default -> "Unknown conditions";
        };
    }
}

package com.codewithnst.weather;

public record WeatherReport(
        String city,
        String region,
        String country,
        String conditions,
        String dayNight,
        double temperatureC,
        double apparentTemperatureC,
        double humidityPercent,
        double windKmh,
        double precipitationMm,
        String updatedAt) {

    public String locationName() {
        return city
                + (region.isBlank() ? "" : ", " + region)
                + (country.isBlank() ? "" : ", " + country);
    }

    public String toText() {
        return "Weather for " + locationName() + "\n"
                + "----------------------------------------\n"
                + "Conditions: " + conditions + " (" + dayNight + ")\n"
                + String.format("Temperature: %.1f °C (feels like %.1f °C)%n",
                        temperatureC, apparentTemperatureC)
                + String.format("Humidity: %.0f%%%n", humidityPercent)
                + String.format("Wind: %.1f km/h%n", windKmh)
                + String.format("Precipitation: %.1f mm%n", precipitationMm)
                + "Updated: " + updatedAt;
    }
}

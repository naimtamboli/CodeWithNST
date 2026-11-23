package com.codewithnst.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WeatherMcpServer {
    private static final Set<String> SUPPORTED_PROTOCOL_VERSIONS =
            Set.of("2025-11-25", "2025-06-18", "2025-03-26", "2024-11-05");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WeatherService weatherService = new WeatherService();
    private final WeatherEmailer emailer = new WeatherEmailer();

    public void run() throws IOException {
        try (BufferedReader input = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = input.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                JsonNode request;
                try {
                    request = OBJECT_MAPPER.readTree(line);
                } catch (IOException e) {
                    writeError(null, -32700, "Invalid JSON.");
                    continue;
                }
                if (request == null || !request.isObject()) {
                    writeError(null, -32600, "Expected a JSON-RPC request object.");
                    continue;
                }

                JsonNode id = request.path("id");
                String method = request.path("method").asText("");
                if (id.isMissingNode() || id.isNull()) {
                    continue;
                }

                try {
                    Object result = switch (method) {
                        case "initialize" -> initialize(request.path("params"));
                        case "ping" -> Map.of();
                        case "tools/list" -> listTools();
                        case "tools/call" -> callTool(request.path("params"));
                        default -> null;
                    };
                    if (result == null) {
                        writeError(id, -32601, "Method not found: " + method);
                    } else {
                        writeResult(id, result);
                    }
                } catch (IllegalArgumentException e) {
                    writeError(id, -32602, e.getMessage());
                } catch (IOException | InterruptedException | MessagingException e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    writeResult(id, toolError(e.getMessage()));
                }
            }
        }
    }

    private static Object initialize(JsonNode params) {
        String requestedVersion = params.path("protocolVersion").asText("");
        String protocolVersion = SUPPORTED_PROTOCOL_VERSIONS.contains(requestedVersion)
                ? requestedVersion : "2025-11-25";
        return Map.of(
                "protocolVersion", protocolVersion,
                "capabilities", Map.of("tools", Map.of()),
                "serverInfo", Map.of("name", "city-weather", "version", "1.0.0"));
    }

    private static Object listTools() {
        Map<String, Object> inputSchema =
                Map.of("type", "object", "properties", Map.of(), "additionalProperties", false);
        return Map.of("tools", List.of(
                Map.of(
                        "name", "get_pune_weather",
                        "description", "Get the current weather in Pune, India.",
                        "inputSchema", inputSchema),
                Map.of(
                        "name", "send_pune_weather_email",
                        "description", "Send the current Pune weather to the configured Gmail recipient.",
                        "inputSchema", inputSchema)));
    }

    private Object callTool(JsonNode params) throws IOException, InterruptedException, MessagingException {
        String name = params.path("name").asText("");
        return switch (name) {
            case "get_pune_weather" -> toolSuccess(weatherService.getCurrentWeather("Pune").toText());
            case "send_pune_weather_email" -> {
                emailer.sendPuneUpdate();
                yield toolSuccess("Pune weather email sent.");
            }
            default -> throw new IllegalArgumentException("Unknown tool: " + name);
        };
    }

    private static Map<String, Object> toolSuccess(String text) {
        return Map.of("content", List.of(Map.of("type", "text", "text", text)));
    }

    private static Map<String, Object> toolError(String text) {
        return Map.of(
                "content", List.of(Map.of("type", "text", "text",
                        text == null || text.isBlank() ? "The operation failed." : text)),
                "isError", true);
    }

    private static void writeResult(JsonNode id, Object result) throws IOException {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", OBJECT_MAPPER.convertValue(id, Object.class));
        response.put("result", result);
        writeMessage(response);
    }

    private static void writeError(JsonNode id, int code, String message) throws IOException {
        Map<String, Object> error = Map.of("code", code, "message", message);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id == null ? null : OBJECT_MAPPER.convertValue(id, Object.class));
        response.put("error", error);
        writeMessage(response);
    }

    private static void writeMessage(Object message) throws IOException {
        System.out.println(OBJECT_MAPPER.writeValueAsString(message));
        System.out.flush();
    }
}

# City Weather

A Java console app and stdio MCP server that can email Pune weather when run.
Weather data comes from the free Open-Meteo geocoding and forecast APIs; no
weather API key is needed.

## Requirements

- Java 17 or newer
- Maven
- Internet access

## Build and run

Build the executable jar:

```powershell
mvn -DskipTests package
```

Run the `WeatherApp` class or jar with no arguments to send one Pune weather
email. There is no background or recurring schedule:

```powershell
java -jar target\city-weather-1.0.0.jar
```

Run it with a city name to display current weather instead:

```powershell
java -jar target\city-weather-1.0.0.jar Pune
```

To send once and keep the command window open so success output and errors
remain visible, use:

```powershell
.\scripts\run-weather-email.bat
```

## Email setup

1. Enable 2-Step Verification on the sending Google account and create a Google
   App Password. Do not use your regular Gmail password.
2. From the project directory, run:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\setup-weather-email.ps1
   ```

3. Enter the sending Gmail address, the receiving address (press Enter to use
   the sending address), and the App Password when prompted. These values are
   saved in `%APPDATA%\CityWeather\weather.properties`; subsequent setup runs
   reuse the saved values unless changed.

Setup removes the old `Pune Weather Email` Windows Task Scheduler task, if
present. To send another email, run the Java class again.

## MCP server

The project includes a VS Code MCP configuration at `.vscode/mcp.json`. Build
the jar first, then open the project in VS Code and enable the `city-weather`
server. It exposes:

- `get_pune_weather` — get current conditions for Pune.
- `send_pune_weather_email` — send the current Pune report to the configured
  Gmail recipient.

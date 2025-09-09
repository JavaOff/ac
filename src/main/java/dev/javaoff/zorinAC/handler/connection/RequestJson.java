package dev.javaoff.zorinAC.handler.connection;

import dev.javaoff.zorinAC.ZorinAC;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class RequestJson {

    public void sendRequestJson(String json, String adress) {
        try {
            URL url = new URL(adress);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            connection.getInputStream().close();
        } catch (Exception e) {
            ZorinAC.logger().warning(e.getMessage());
        }
    }
}
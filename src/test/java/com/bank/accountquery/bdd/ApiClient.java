package com.bank.accountquery.bdd;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 以 JDK 內建 HttpClient 對隨機埠上的真實伺服器發送請求；
 * 對 4xx/5xx 不丟例外，方便情境直接斷言錯誤狀態碼。
 */
@Component
public class ApiClient {

    private final Environment env;
    private final HttpClient http = HttpClient.newHttpClient();

    public ApiClient(Environment env) {
        this.env = env;
    }

    public record Response(int status, String body) {}

    public Response get(String path, String customerId) {
        int port = Integer.parseInt(env.getProperty("local.server.port"));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (customerId != null) {
            builder.header("X-Customer-Id", customerId);
        }
        try {
            HttpResponse<String> resp = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(resp.statusCode(), resp.body());
        } catch (Exception e) {
            throw new RuntimeException("HTTP 請求失敗：" + path, e);
        }
    }
}

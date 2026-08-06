package dev.gungoguma.verify.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.gungoguma.verify.config.VerifyConfig;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OAuthHttpServer {
    private final VerifyConfig config;
    private final OAuthCallbackProcessor callbackProcessor;
    private HttpServer server;
    private ExecutorService executorService;

    public OAuthHttpServer(VerifyConfig config, OAuthCallbackProcessor callbackProcessor) {
        this.config = config;
        this.callbackProcessor = callbackProcessor;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(config.oauthServerHost(), config.oauthServerPort()), 0);
        server.createContext(config.oauthCallbackPath(), this::handleCallback);
        server.createContext("/health", this::handleHealth);
        executorService = Executors.newFixedThreadPool(2);
        server.setExecutor(executorService);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private void handleCallback(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, page(OAuthCallbackResult.failure("Unsupported request method.")));
            return;
        }

        OAuthCallbackResult result = callbackProcessor.process(parseQuery(exchange.getRequestURI().getRawQuery()));
        send(exchange, result.success() ? 200 : 400, page(result));
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        send(exchange, 200, "ok");
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> query = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return query;
        }

        for (String pair : rawQuery.split("&")) {
            int separator = pair.indexOf('=');
            if (separator < 0) {
                query.put(decode(pair), "");
                continue;
            }

            query.put(decode(pair.substring(0, separator)), decode(pair.substring(separator + 1)));
        }
        return query;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String page(OAuthCallbackResult result) {
        String color = result.success() ? "#15803d" : "#b91c1c";
        return "<!doctype html><html lang=\"ko\"><head><meta charset=\"utf-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
            + "<title>GSM-Verify</title></head>"
            + "<body style=\"font-family:system-ui,sans-serif;line-height:1.5;padding:32px;\">"
            + "<h1 style=\"color:" + color + ";\">" + escape(result.title()) + "</h1>"
            + "<p>" + escape(result.message()) + "</p>"
            + "</body></html>";
    }

    private String escape(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private void send(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}

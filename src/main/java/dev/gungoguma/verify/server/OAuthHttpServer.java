package dev.gungoguma.verify.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.gungoguma.verify.config.VerifyConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OAuthHttpServer {
    private static final String SUCCESS_ICON_SVG =
        "<svg viewBox=\"0 0 24 24\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">"
            + "<path d=\"M5 13l4 4L19 7\" stroke=\"#ffffff\" stroke-width=\"2.5\" "
            + "stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>";
    private static final String FAILURE_ICON_SVG =
        "<svg viewBox=\"0 0 24 24\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">"
            + "<path d=\"M6 6l12 12M18 6L6 18\" stroke=\"#ffffff\" stroke-width=\"2.5\" "
            + "stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>";

    private final VerifyConfig config;
    private final OAuthCallbackProcessor callbackProcessor;
    private final String resultPageTemplate;
    private HttpServer server;
    private ExecutorService executorService;

    public OAuthHttpServer(VerifyConfig config, OAuthCallbackProcessor callbackProcessor) {
        this.config = config;
        this.callbackProcessor = callbackProcessor;
        this.resultPageTemplate = loadResultPageTemplate();
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
        String headline = result.success() ? "Verification Done!" : "Verification Failed.";
        String iconSvg = result.success() ? SUCCESS_ICON_SVG : FAILURE_ICON_SVG;
        String accentStart = result.success() ? "#4ade80" : "#f87171";
        String accentEnd = result.success() ? "#15803d" : "#b91c1c";
        String accentShadow = result.success() ? "rgba(34, 197, 94, 0.35)" : "rgba(239, 68, 68, 0.35)";

        return resultPageTemplate
            .replace("%%ICON_SVG%%", iconSvg)
            .replace("%%HEADLINE%%", escape(headline))
            .replace("%%DETAIL%%", escape(result.message()))
            .replace("%%ACCENT_START%%", accentStart)
            .replace("%%ACCENT_END%%", accentEnd)
            .replace("%%ACCENT_SHADOW%%", accentShadow);
    }

    private static String loadResultPageTemplate() {
        try (InputStream inputStream = OAuthHttpServer.class.getResourceAsStream("/web/verify-result.html")) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing bundled resource: /web/verify-result.html");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load verify-result.html template.", exception);
        }
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

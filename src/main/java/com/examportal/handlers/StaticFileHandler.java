package com.examportal.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLConnection;
import java.nio.file.*;

public class StaticFileHandler implements HttpHandler {
    private final String staticDir;

    public StaticFileHandler(String staticDir) {
        this.staticDir = staticDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String uriPath = exchange.getRequestURI().getPath();

        // Serve index.html for root
        if ("/".equals(uriPath) || uriPath.isEmpty()) {
            uriPath = "/index.html";
        }

        // Sanitize path (prevent directory traversal)
        String sanitized = uriPath.replace("..", "").replace("//", "/");
        Path filePath = Paths.get(staticDir, sanitized);

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            // Try serving index.html for SPA routes
            filePath = Paths.get(staticDir, "index.html");
            if (!Files.exists(filePath)) {
                byte[] body = "404 Not Found".getBytes();
                exchange.sendResponseHeaders(404, body.length);
                exchange.getResponseBody().write(body);
                exchange.getResponseBody().close();
                return;
            }
        }

        String contentType = getContentType(filePath.toString());
        byte[] fileBytes = Files.readAllBytes(filePath);

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, fileBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(fileBytes);
        }
    }

    private String getContentType(String filename) {
        if (filename.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filename.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filename.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filename.endsWith(".json")) return "application/json";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        if (filename.endsWith(".ico")) return "image/x-icon";
        if (filename.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
    }
}

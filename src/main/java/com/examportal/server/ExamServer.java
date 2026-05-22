package com.examportal.server;

import com.examportal.handlers.*;
import com.examportal.utils.DatabaseManager;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

public class ExamServer {
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        // Initialize database
        System.out.println("[Server] Initializing database...");
        DatabaseManager.getInstance();

        // Create HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Determine static files directory — try dev path first, then production path
        java.nio.file.Path devPath  = Paths.get("src", "main", "resources", "static");
        java.nio.file.Path prodPath = Paths.get("out", "classes", "static");
        java.nio.file.Path staticPath = java.nio.file.Files.exists(devPath) ? devPath : prodPath;
        String staticDir = staticPath.toAbsolutePath().toString();
        System.out.println("[Server] Serving static files from: " + staticDir);

        // Register API handlers
        AuthHandler authHandler = new AuthHandler();
        ExamHandler examHandler = new ExamHandler();
        ResultHandler resultHandler = new ResultHandler();
        UserHandler userHandler = new UserHandler();

        server.createContext("/api/auth", authHandler);
        server.createContext("/api/exams", examHandler);
        server.createContext("/api/results", resultHandler);
        server.createContext("/api/dashboard", resultHandler);
        server.createContext("/api/users", userHandler);

        // Serve static files
        server.createContext("/", new StaticFileHandler(staticDir));

        // Thread pool
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("==================================================");
        System.out.println("  Online Examination System Started!");
        System.out.println("==================================================");
        System.out.println("  URL:   http://localhost:" + PORT);
        System.out.println("  Admin: admin / admin123");
        System.out.println("==================================================");
    }
}

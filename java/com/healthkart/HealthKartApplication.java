package com.healthkart;

import com.healthkart.gui.HealthKartGUI;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * HealthKart Application Main Launcher.
 * Starts Spring Boot Web Server (port 3000) powering the HTML & CSS Frontend,
 * backed by pure JDBC database connectivity, DAOs, multithreading, and OOP architecture.
 */
@SpringBootApplication
public class HealthKartApplication {
    public static void main(String[] args) {
        // Start Spring Boot Web Server powering the HTML & CSS Frontend on http://localhost:3000
        SpringApplication.run(HealthKartApplication.class, args);
        System.out.println("==================================================================");
        System.out.println("HealthKart Web Application is running!");
        System.out.println("Frontend (HTML/CSS): http://localhost:3000");
        System.out.println("Database Diagnostics: http://localhost:3000/pages/database.html");
        System.out.println("Database Engine: Pure SQLite JDBC (data/healthkart.db)");
        System.out.println("==================================================================");

        // Optional Swing Desktop GUI if --gui flag is passed
        boolean launchSwing = Arrays.asList(args).contains("--gui");
        if (launchSwing && !GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(() -> {
                try {
                    HealthKartGUI gui = new HealthKartGUI();
                    gui.setVisible(true);
                } catch (Exception e) {
                    System.err.println("Notice: " + e.getMessage());
                }
            });
        }
    }
}

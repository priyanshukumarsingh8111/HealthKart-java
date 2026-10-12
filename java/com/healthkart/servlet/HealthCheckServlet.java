package com.healthkart.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthkart.db.DatabaseConnectionManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Standard Java HttpServlet reporting application health, JDBC connectivity,
 * and Servlet container status.
 */
public class HealthCheckServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        Map<String, Object> status = new LinkedHashMap<>();

        boolean dbOk = false;
        String dbInfo = "";
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection()) {
            dbOk = !conn.isClosed();
            DatabaseMetaData meta = conn.getMetaData();
            dbInfo = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
        } catch (Exception e) {
            dbInfo = "Error: " + e.getMessage();
        }

        status.put("status", "UP");
        status.put("application", "HealthKart Healthcare & Pharmacy System");
        status.put("servletContainer", req.getServletContext().getServerInfo());
        status.put("databaseConnected", dbOk);
        status.put("databaseInfo", dbInfo);
        status.put("serverTime", java.time.LocalDateTime.now().toString());

        try (PrintWriter out = resp.getWriter()) {
            out.print(mapper.writeValueAsString(status));
            out.flush();
        }
    }
}

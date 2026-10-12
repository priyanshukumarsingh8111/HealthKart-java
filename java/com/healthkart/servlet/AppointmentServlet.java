package com.healthkart.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthkart.dao.AppointmentDao;
import com.healthkart.dao.AppointmentDaoImpl;
import com.healthkart.model.Appointment;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard Java HttpServlet handling appointment querying and scheduling.
 * Demonstrates:
 * - Servlets & Web Integration (7 Marks)
 * - Multithreading & Synchronization: invoking ReentrantLock synchronized booking via Servlet
 */
public class AppointmentServlet extends HttpServlet {

    private final AppointmentDao appointmentDao;
    private final ObjectMapper mapper;

    public AppointmentServlet() {
        this.appointmentDao = new AppointmentDaoImpl();
        this.mapper = new ObjectMapper();
    }

    public AppointmentServlet(AppointmentDao appointmentDao) {
        this.appointmentDao = appointmentDao;
        this.mapper = new ObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String email = req.getParameter("email");
        String doctorId = req.getParameter("doctorId");
        Map<String, Object> result = new LinkedHashMap<>();

        List<Appointment> list;
        if (email != null && !email.isEmpty()) {
            list = appointmentDao.findByPatientEmail(email);
        } else if (doctorId != null && !doctorId.isEmpty()) {
            list = appointmentDao.findByDoctorId(doctorId);
        } else {
            list = appointmentDao.findAll();
        }

        result.put("success", true);
        result.put("count", list.size());
        result.put("appointments", list);
        result.put("source", "AppointmentServlet (Jakarta HttpServlet)");

        try (PrintWriter out = resp.getWriter()) {
            out.print(mapper.writeValueAsString(result));
            out.flush();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String doctorId = req.getParameter("doctorId");
        String date = req.getParameter("date");
        String time = req.getParameter("time");
        String patientName = req.getParameter("patientName");
        String patientEmail = req.getParameter("patientEmail");
        Map<String, Object> result = new LinkedHashMap<>();

        if (doctorId == null || date == null || time == null || patientName == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("success", false);
            result.put("error", "doctorId, date, time, and patientName parameters are required.");
        } else {
            String aptId = "HK-SRV-" + (System.currentTimeMillis() % 100000);
            Appointment apt = new Appointment();
            apt.setAppointmentId(aptId);
            apt.setDoctorId(doctorId);
            apt.setDoctorName(req.getParameter("doctorName") != null ? req.getParameter("doctorName") : "Consulting Doctor");
            apt.setPatientName(patientName);
            apt.setPatientEmail(patientEmail != null ? patientEmail : "guest@healthkart.com");
            apt.setPatientPhone(req.getParameter("patientPhone") != null ? req.getParameter("patientPhone") : "9876543210");
            apt.setDate(date);
            apt.setTime(time);
            apt.setFee(500.0);

            try {
                // Synchronized JDBC booking with ReentrantLock
                appointmentDao.bookAppointmentWithLock(apt);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                result.put("success", true);
                result.put("message", "Appointment successfully scheduled via AppointmentServlet!");
                result.put("appointmentId", aptId);
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                result.put("success", false);
                result.put("error", e.getMessage());
            }
        }

        try (PrintWriter out = resp.getWriter()) {
            out.print(mapper.writeValueAsString(result));
            out.flush();
        }
    }
}

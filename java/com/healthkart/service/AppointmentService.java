package com.healthkart.service;

import com.healthkart.dao.AppointmentDao;
import com.healthkart.dao.DoctorDao;
import com.healthkart.dao.NotificationDao;
import com.healthkart.exception.EntityNotFoundException;
import com.healthkart.exception.SlotConflictException;
import com.healthkart.exception.ValidationException;
import com.healthkart.model.Appointment;
import com.healthkart.model.Doctor;
import com.healthkart.model.Notification;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service class orchestrating appointment business logic, slot validation,
 * multithreaded booking synchronization, and notification dispatch.
 *
 * Demonstrates:
 * - OOP: Interface segregation, Business Exception handling
 * - Collections: Lists, Maps, PriorityQueues
 * - Multithreading: Concurrency management via synchronized booking in DAO
 */
@Service
public class AppointmentService {

    private final AppointmentDao appointmentDao;
    private final DoctorDao doctorDao;
    private final NotificationDao notificationDao;

    public AppointmentService(AppointmentDao appointmentDao, DoctorDao doctorDao, NotificationDao notificationDao) {
        this.appointmentDao = appointmentDao;
        this.doctorDao = doctorDao;
        this.notificationDao = notificationDao;
    }

    public static boolean isSunday(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return false;
        try {
            LocalDate date = LocalDate.parse(dateStr.trim());
            return date.getDayOfWeek() == DayOfWeek.SUNDAY;
        } catch (Exception e) {
            return false;
        }
    }

    public List<Map<String, Object>> getSlots(String doctorId, String date) {
        if (doctorId == null || doctorId.trim().isEmpty() || date == null || date.trim().isEmpty()) {
            throw new ValidationException("doctorId and date parameters are required");
        }
        if (isSunday(date)) {
            throw new ValidationException("Appointments are not available on Sundays. Please select another day.");
        }

        Doctor doc = doctorDao.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor", doctorId));

        return appointmentDao.getDoctorSlots(doctorId, date, doc.getAvailableTimeSlots());
    }

    public Appointment bookAppointment(Appointment apt) {
        if (apt.getDoctorId() == null || apt.getDoctorId().trim().isEmpty()) {
            throw new ValidationException("Doctor ID is required");
        }
        if (apt.getDate() == null || apt.getDate().trim().isEmpty()) {
            throw new ValidationException("Appointment date is required");
        }
        if (apt.getTime() == null || apt.getTime().trim().isEmpty()) {
            throw new ValidationException("Appointment time slot is required");
        }
        if (apt.getPatientName() == null || apt.getPatientName().trim().isEmpty()) {
            throw new ValidationException("Patient name is required");
        }
        if (apt.getPatientEmail() == null || apt.getPatientEmail().trim().isEmpty()) {
            throw new ValidationException("Patient email is required");
        }
        if (isSunday(apt.getDate())) {
            throw new ValidationException("Cannot book on Sundays");
        }

        Doctor doctor = doctorDao.findById(apt.getDoctorId())
                .orElseThrow(() -> new EntityNotFoundException("Doctor", apt.getDoctorId()));

        apt.setDoctorName(doctor.getName());
        apt.setFee(doctor.getConsultationFee());
        if (apt.getAppointmentId() == null || apt.getAppointmentId().isEmpty()) {
            apt.setAppointmentId("apt-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 900 + 100));
        }
        apt.setJoinUrl("/pages/call.html?room=" + apt.getAppointmentId());

        // Delegate to synchronized JDBC transaction
        Appointment saved = appointmentDao.bookAppointmentWithLock(apt);

        // Auto-create confirmation notification
        try {
            Notification n = new Notification(
                    "notif-" + System.currentTimeMillis(),
                    apt.getPatientEmail(),
                    "Appointment Confirmed",
                    "Your consultation with " + doctor.getName() + " is confirmed for " + apt.getDate() + " at " + apt.getTime(),
                    "APPOINTMENT",
                    false
            );
            notificationDao.save(n);
        } catch (Exception e) {
            System.err.println("Non-critical notification save error: " + e.getMessage());
        }

        return saved;
    }

    public boolean cancelAppointment(String appointmentId) {
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            throw new ValidationException("Appointment ID is required for cancellation");
        }
        return appointmentDao.cancelAppointment(appointmentId);
    }

    public List<Appointment> getAllAppointments() {
        return appointmentDao.findAll();
    }

    public List<Appointment> getAppointmentsForPatient(String email) {
        return appointmentDao.findByPatientEmail(email);
    }

    public List<Appointment> getAppointmentsForDoctor(String doctorId) {
        return appointmentDao.findByDoctorId(doctorId);
    }

    public Optional<Appointment> getAppointmentById(String id) {
        return appointmentDao.findById(id);
    }

    /**
     * Demonstrates Collections PriorityQueue & Generics triage.
     */
    public PriorityQueue<Appointment> getUpcomingTriageQueue() {
        return appointmentDao.getUpcomingPriorityQueue();
    }
}

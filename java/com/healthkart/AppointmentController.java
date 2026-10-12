package com.healthkart;

import com.healthkart.dao.AppointmentDao;
import com.healthkart.dao.DoctorDao;
import com.healthkart.dao.NotificationDao;
import com.healthkart.exception.SlotConflictException;
import com.healthkart.model.Appointment;
import com.healthkart.model.Doctor;
import com.healthkart.model.Notification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.healthkart.Util.*;

/**
 * Appointments and Realtime Notifications Controller for HTML/CSS Frontend.
 * Backed by pure JDBC DAOs (AppointmentDao, DoctorDao, NotificationDao) with thread synchronization.
 */
@RestController
@RequestMapping("/api")
public class AppointmentController {
    private final AppointmentDao appointmentDao;
    private final DoctorDao doctorDao;
    private final NotificationDao notificationDao;
    private final Realtime rt;

    public AppointmentController(AppointmentDao appointmentDao, DoctorDao doctorDao,
                                 NotificationDao notificationDao, Realtime rt) {
        this.appointmentDao = appointmentDao;
        this.doctorDao = doctorDao;
        this.notificationDao = notificationDao;
        this.rt = rt;
    }

    // GET /api/appointments/slots?doctorId=rahul&date=2026-10-12
    @GetMapping("/appointments/slots")
    public Map<String, Object> slots(@RequestParam(name = "doctorId", required = false) String doctorId,
                                     @RequestParam(name = "date", required = false) String date) {
        if (s(doctorId) == null)
            throw ApiException.of(400, "doctorId parameter is required");

        if (s(date) == null) {
            date = java.time.LocalDate.now().toString();
        }

        if (isSunday(date)) {
            throw new ApiException(400, m("success", false, "isSunday", true,
                    "error", "Appointments are not available on Sundays. Please select another day.",
                    "slots", List.of()));
        }

        Doctor doc = doctorDao.findById(doctorId)
                .orElseThrow(() -> ApiException.of(404, "Doctor not found"));

        List<Map<String, Object>> slotList = appointmentDao.getDoctorSlots(doctorId, date, doc.getAvailableTimeSlots());
        return m("success", true, "doctorId", doctorId, "date", date, "isSunday", false, "slots", slotList);
    }

    // POST /api/appointments/book
    @PostMapping("/appointments/book")
    public ResponseEntity<Map<String, Object>> book(@RequestBody(required = false) Map<String, Object> raw) {
        Map<String, Object> b = body(raw);
        String doctorId = s(b.get("doctorId"));
        String date = or(s(b.get("appointmentDate")), s(b.get("date")));
        String time = or(s(b.get("appointmentTime")), or(s(b.get("timeSlot")), s(b.get("time"))));
        String patientName = s(b.get("patientName"));
        String patientMobile = or(s(b.get("patientMobile")), s(b.get("patientPhone")));
        String patientEmail = or(s(b.get("patientEmail")), "");

        if (doctorId == null || date == null || time == null || patientName == null || patientMobile == null)
            throw ApiException.of(400, "Missing required booking information.");

        if (isSunday(date)) {
            throw new ApiException(400, m("success", false, "isSunday", true,
                    "error", "Appointments are not available on Sundays. Please select another day."));
        }

        Doctor doctor = doctorDao.findById(doctorId)
                .orElseThrow(() -> ApiException.of(404, "Doctor not found."));

        String appointmentId = "HK-" + rand6();
        String now = now();

        Appointment apt = new Appointment();
        apt.setAppointmentId(appointmentId);
        apt.setDoctorId(doctorId);
        apt.setDoctorName(doctor.getName());
        apt.setDoctorImage(doctor.getImage());
        apt.setSpecialization(doctor.getSpecialization());
        apt.setClinicHospital(doctor.getClinicHospital());
        apt.setPatientName(patientName);
        apt.setPatientEmail(patientEmail);
        apt.setPatientPhone(patientMobile);
        apt.setDate(date);
        apt.setTime(time);
        apt.setSymptoms(or(b.get("healthConcern"), "General Consultation"));
        apt.setConsultationType(or(b.get("consultationType"), "Video Consultation"));
        apt.setFee(doctor.getConsultationFee());
        apt.setJoinUrl("/pages/call.html?appointment=" + appointmentId + "&role=patient");
        apt.setStatus("CONFIRMED");
        apt.setCreatedAt(now);

        try {
            // Synchronized, multithread-safe booking via JDBC transaction
            appointmentDao.bookAppointmentWithLock(apt);
        } catch (SlotConflictException e) {
            throw new ApiException(409, m("success", false, "isSlotTaken", true,
                    "error", "Sorry, this appointment slot has just been booked by another patient."));
        }

        // Create in-app alert notification
        Notification notif = new Notification(
                notifId(),
                patientEmail.isEmpty() ? "guest" : patientEmail,
                "Appointment Booked Successfully!",
                "Your appointment with " + doctor.getName() + " is booked for " + time + " on " + date + ".",
                "appointment_confirmed",
                false
        );
        notif.setCreatedAt(now);
        try {
            notificationDao.save(notif);
        } catch (Exception ex) {
            System.err.println("Notification notice: " + ex.getMessage());
        }

        // Socket.IO events for live HTML/CSS updates
        rt.emit("slot_booked", m("doctorId", doctorId, "date", date, "time", time, "appointmentId", appointmentId));
        rt.emit("realtime_notification", m("recipientEmail", patientEmail, "notification", notif));

        return ResponseEntity.status(201).body(m("success", true,
                "message", "Appointment booked successfully!", "appointment", apt));
    }

    @GetMapping("/appointments/my")
    public Map<String, Object> my(@RequestParam(name = "email", required = false) String email,
                                  @RequestParam(name = "mobile", required = false) String mobile) {
        List<Appointment> all;
        if (s(email) != null) {
            all = appointmentDao.findByPatientEmail(email);
        } else {
            all = appointmentDao.findAll();
        }

        if (s(mobile) != null) {
            all = all.stream().filter(a -> mobile.equals(a.getPatientPhone())).collect(Collectors.toList());
        }

        return m("success", true, "count", all.size(), "appointments", all);
    }

    @GetMapping("/appointments/doctor/{doctorId}")
    public Map<String, Object> forDoctor(@PathVariable("doctorId") String doctorId) {
        List<Appointment> mine = appointmentDao.findByDoctorId(doctorId);
        String today = today();
        List<Appointment> todayList = mine.stream()
                .filter(a -> today.equals(a.getDate())).collect(Collectors.toList());
        List<Appointment> upcoming = mine.stream()
                .filter(a -> "CONFIRMED".equals(a.getStatus()) && a.getDate().compareTo(today) >= 0)
                .collect(Collectors.toList());

        return m("success", true, "count", mine.size(), "today", todayList, "upcoming", upcoming, "all", mine);
    }

    @PostMapping("/appointments/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable("id") String id,
                                      @RequestBody(required = false) Map<String, Object> raw) {
        Appointment a = appointmentDao.findById(id)
                .orElseThrow(() -> ApiException.of(404, "Appointment not found"));

        if ("CANCELLED".equals(a.getStatus())) {
            throw ApiException.of(400, "Appointment is already cancelled");
        }

        appointmentDao.cancelAppointment(id);
        a.setStatus("CANCELLED");

        Notification notif = new Notification(
                notifId(),
                or(a.getPatientEmail(), "guest"),
                "Appointment Cancelled",
                "Your appointment with " + a.getDoctorName() + " for " + a.getDate() + " at " + a.getTime() + " has been cancelled.",
                "appointment_cancelled",
                false
        );
        notif.setCreatedAt(now());
        try {
            notificationDao.save(notif);
        } catch (Exception ignored) {}

        rt.emit("slot_freed", m("doctorId", a.getDoctorId(), "date", a.getDate(), "time", a.getTime()));
        rt.emit("realtime_notification", m("recipientEmail", or(a.getPatientEmail(), ""), "notification", notif));

        return m("success", true,
                "message", "Appointment cancelled successfully. The slot is now available again.",
                "appointment", a);
    }

    @PostMapping("/appointments/{id}/complete")
    public Map<String, Object> complete(@PathVariable("id") String id) {
        Appointment a = appointmentDao.findById(id)
                .orElseThrow(() -> ApiException.of(404, "Appointment not found"));
        a.setStatus("COMPLETED");
        appointmentDao.update(a);
        return m("success", true, "message", "Appointment marked as completed.", "appointment", a);
    }

    @PostMapping("/webrtc/verify-access")
    public ResponseEntity<Map<String, Object>> verifyAccess(@RequestBody(required = false) Map<String, Object> raw) {
        Map<String, Object> b = body(raw);
        String appointmentId = s(b.get("appointmentId"));
        if (appointmentId == null)
            return ResponseEntity.badRequest().body(m("authorized", false, "error", "Appointment ID is required"));

        Appointment a = appointmentDao.findById(appointmentId).orElse(null);
        if (a == null)
            return ResponseEntity.status(404).body(m("authorized", false, "error", "Appointment does not exist."));
        if ("CANCELLED".equals(a.getStatus()))
            return ResponseEntity.status(403).body(m("authorized", false, "error", "This appointment has been cancelled."));

        String doctorId = s(b.get("doctorId"));
        boolean isDoctor = "doctor".equals(b.get("userRole")) || (doctorId != null && doctorId.equals(a.getDoctorId()));

        return ResponseEntity.ok(m("authorized", true, "role", isDoctor ? "doctor" : "patient",
                "callRoomId", "call_" + a.getAppointmentId(), "appointment", a));
    }

    // ---------- Notifications ----------
    @GetMapping("/notifications")
    public Map<String, Object> notifications(@RequestParam(name = "email", required = false) String email) {
        List<Notification> list = (s(email) != null) ?
                notificationDao.findByPatientEmail(email) :
                notificationDao.findAll();

        return m("success", true, "count", list.size(), "notifications", list);
    }

    @PostMapping("/notifications/mark-read")
    public Map<String, Object> markRead() {
        List<Notification> list = notificationDao.findAll();
        for (Notification n : list) {
            notificationDao.markAsRead(n.getId());
        }
        return m("success", true, "message", "All notifications marked as read");
    }
}

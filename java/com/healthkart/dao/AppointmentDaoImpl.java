package com.healthkart.dao;

import com.healthkart.db.DatabaseConnectionManager;
import com.healthkart.exception.DatabaseException;
import com.healthkart.exception.SlotConflictException;
import com.healthkart.model.Appointment;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Concrete JDBC implementation of AppointmentDao.
 * Demonstrates:
 * - Multithreading & Synchronization: Explicit ReentrantLock protecting critical booking sections against race conditions
 * - Database Operations (JDBC): ACID Transactions (commit/rollback), prepared statements, batch operations
 * - Collections & Generics: PriorityQueue for triage scheduling, Maps for fast slot lookup
 * - OOP: Inheritance (extends AbstractJdbcDao), Polymorphism, Custom Exception Handling
 */
@Repository
public class AppointmentDaoImpl extends AbstractJdbcDao<Appointment, String> implements AppointmentDao {

    // Thread synchronization lock ensuring atomic slot check and booking
    private static final ReentrantLock BOOKING_LOCK = new ReentrantLock();

    @Override
    protected String getTableName() {
        return "appointments";
    }

    @Override
    protected String getIdColumnName() {
        return "appointment_id";
    }

    @Override
    protected Appointment mapResultSet(ResultSet rs) throws SQLException {
        Appointment apt = new Appointment();
        apt.setAppointmentId(rs.getString("appointment_id"));
        apt.setDoctorId(rs.getString("doctor_id"));
        apt.setDoctorName(rs.getString("doctor_name"));
        apt.setPatientName(rs.getString("patient_name"));
        apt.setPatientEmail(rs.getString("patient_email"));
        apt.setPatientPhone(rs.getString("patient_phone"));
        apt.setDate(rs.getString("date"));
        apt.setTime(rs.getString("time"));
        apt.setSymptoms(rs.getString("symptoms"));
        apt.setStatus(rs.getString("status"));
        apt.setFee(rs.getDouble("fee"));
        apt.setJoinUrl(rs.getString("join_url"));
        try { apt.setClinicHospital(rs.getString("clinic_hospital")); } catch (Exception ignored) {}
        try { apt.setDoctorImage(rs.getString("doctor_image")); } catch (Exception ignored) {}
        try { apt.setSpecialization(rs.getString("specialization")); } catch (Exception ignored) {}
        apt.setCreatedAt(rs.getString("created_at"));
        return apt;
    }

    @Override
    public Appointment save(Appointment apt) {
        return bookAppointmentWithLock(apt);
    }

    @Override
    public Appointment update(Appointment apt) {
        String sql = """
            UPDATE appointments SET doctor_name = ?, patient_name = ?, patient_email = ?,
                                    patient_phone = ?, date = ?, time = ?, symptoms = ?,
                                    status = ?, fee = ?, join_url = ?, clinic_hospital = ?,
                                    doctor_image = ?, specialization = ?
            WHERE appointment_id = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, apt.getDoctorName());
            ps.setString(2, apt.getPatientName());
            ps.setString(3, apt.getPatientEmail());
            ps.setString(4, apt.getPatientPhone());
            ps.setString(5, apt.getDate());
            ps.setString(6, apt.getTime());
            ps.setString(7, apt.getSymptoms());
            ps.setString(8, apt.getStatus());
            ps.setDouble(9, apt.getFee());
            ps.setString(10, apt.getJoinUrl());
            ps.setString(11, apt.getClinicHospital());
            ps.setString(12, apt.getDoctorImage());
            ps.setString(13, apt.getSpecialization());
            ps.setString(14, apt.getAppointmentId());
            ps.executeUpdate();
            return apt;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update appointment: " + apt.getAppointmentId(), sql, e);
        }
    }

    /**
     * Multithreaded synchronized slot booking with JDBC Transaction.
     * Prevents race conditions and double-booking.
     */
    @Override
    public Appointment bookAppointmentWithLock(Appointment apt) {
        BOOKING_LOCK.lock(); // Synchronize across multiple threads
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Begin ACID Transaction

            String slotKey = apt.getSlotKey();

            // 1. Check if slot is already booked
            boolean isTaken = false;
            String checkSql = "SELECT status FROM booked_slots WHERE slot_key = ?";
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setString(1, slotKey);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("status");
                        if (!"CANCELLED".equalsIgnoreCase(status)) {
                            isTaken = true;
                        }
                    }
                }
            }

            if (isTaken) {
                conn.rollback();
                throw new SlotConflictException(apt.getDoctorId(), apt.getDate(), apt.getTime());
            }

            // 2. Insert into appointments table
            String aptSql = """
                INSERT INTO appointments (appointment_id, doctor_id, doctor_name, patient_name,
                                         patient_email, patient_phone, date, time, symptoms,
                                         status, fee, join_url, clinic_hospital, doctor_image, specialization, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            try (PreparedStatement aptPs = conn.prepareStatement(aptSql)) {
                aptPs.setString(1, apt.getAppointmentId());
                aptPs.setString(2, apt.getDoctorId());
                aptPs.setString(3, apt.getDoctorName());
                aptPs.setString(4, apt.getPatientName());
                aptPs.setString(5, apt.getPatientEmail());
                aptPs.setString(6, apt.getPatientPhone());
                aptPs.setString(7, apt.getDate());
                aptPs.setString(8, apt.getTime());
                aptPs.setString(9, apt.getSymptoms());
                aptPs.setString(10, apt.getStatus());
                aptPs.setDouble(11, apt.getFee());
                aptPs.setString(12, apt.getJoinUrl());
                aptPs.setString(13, apt.getClinicHospital());
                aptPs.setString(14, apt.getDoctorImage());
                aptPs.setString(15, apt.getSpecialization());
                aptPs.setString(16, apt.getCreatedAt() != null ? apt.getCreatedAt() : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                aptPs.executeUpdate();
            }

            // 3. Upsert slot into booked_slots table
            String slotSql;
            if (DatabaseConnectionManager.getInstance().isMySQL()) {
                slotSql = """
                    INSERT INTO booked_slots (slot_key, doctor_id, date, time, appointment_id, status, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        appointment_id = VALUES(appointment_id),
                        status = VALUES(status),
                        updated_at = VALUES(updated_at)
                """;
            } else {
                slotSql = """
                    INSERT INTO booked_slots (slot_key, doctor_id, date, time, appointment_id, status, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(slot_key) DO UPDATE SET
                        appointment_id = excluded.appointment_id,
                        status = excluded.status,
                        updated_at = excluded.updated_at
                """;
            }
            try (PreparedStatement slotPs = conn.prepareStatement(slotSql)) {
                slotPs.setString(1, slotKey);
                slotPs.setString(2, apt.getDoctorId());
                slotPs.setString(3, apt.getDate());
                slotPs.setString(4, apt.getTime());
                slotPs.setString(5, apt.getAppointmentId());
                slotPs.setString(6, "CONFIRMED");
                slotPs.setString(7, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                slotPs.executeUpdate();
            }

            conn.commit(); // Commit Transaction atomically
            return apt;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* suppress */ }
            }
            throw new DatabaseException("Failed to book appointment transactionally", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ignored) {}
            }
            BOOKING_LOCK.unlock(); // Release lock
        }
    }

    @Override
    public boolean isSlotBooked(String doctorId, String date, String time) {
        String slotKey = doctorId + "_" + date + "_" + time;
        String sql = "SELECT status FROM booked_slots WHERE slot_key = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slotKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return !"CANCELLED".equalsIgnoreCase(rs.getString("status"));
                }
            }
            return false;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to verify slot booking status", sql, e);
        }
    }

    @Override
    public List<Map<String, Object>> getDoctorSlots(String doctorId, String date, List<String> allTimeSlots) {
        String sql = "SELECT time, appointment_id, status FROM booked_slots WHERE doctor_id = ? AND date = ?";
        Map<String, Map<String, Object>> bookedMap = new HashMap<>(); // Demonstrates Collections Map
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doctorId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String time = rs.getString("time");
                    String status = rs.getString("status");
                    String aptId = rs.getString("appointment_id");
                    Map<String, Object> data = new HashMap<>();
                    data.put("status", status);
                    data.put("appointmentId", aptId);
                    bookedMap.put(time, data);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch doctor booked slots", sql, e);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String slot : allTimeSlots) {
            Map<String, Object> booked = bookedMap.get(slot);
            boolean isBooked = booked != null && !"CANCELLED".equalsIgnoreCase(String.valueOf(booked.get("status")));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("time", slot);
            item.put("isBooked", isBooked);
            item.put("appointmentId", isBooked ? booked.get("appointmentId") : null);
            result.add(item);
        }
        return result;
    }

    @Override
    public boolean cancelAppointment(String appointmentId) {
        Optional<Appointment> opt = findById(appointmentId);
        if (opt.isEmpty()) return false;
        Appointment apt = opt.get();

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            String sqlApt = "UPDATE appointments SET status = 'CANCELLED' WHERE appointment_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlApt)) {
                ps.setString(1, appointmentId);
                ps.executeUpdate();
            }

            String sqlSlot = "UPDATE booked_slots SET status = 'CANCELLED' WHERE slot_key = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlSlot)) {
                ps.setString(1, apt.getSlotKey());
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* suppress */ }
            }
            throw new DatabaseException("Failed to cancel appointment: " + appointmentId, e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* suppress */ }
            }
        }
    }

    @Override
    public List<Appointment> findByPatientEmail(String email) {
        String sql = "SELECT * FROM appointments WHERE LOWER(patient_email) = LOWER(?) ORDER BY date DESC, time DESC";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find appointments for patient: " + email, sql, e);
        }
    }

    @Override
    public List<Appointment> findByDoctorId(String doctorId) {
        String sql = "SELECT * FROM appointments WHERE doctor_id = ? ORDER BY date DESC, time DESC";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find appointments for doctor: " + doctorId, sql, e);
        }
    }

    @Override
    public List<Appointment> findByDate(String date) {
        String sql = "SELECT * FROM appointments WHERE date = ? ORDER BY time ASC";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find appointments for date: " + date, sql, e);
        }
    }

    /**
     * Demonstrates Collections PriorityQueue & Generics.
     * Orders upcoming non-cancelled appointments by chronological priority.
     */
    @Override
    public PriorityQueue<Appointment> getUpcomingPriorityQueue() {
        PriorityQueue<Appointment> queue = new PriorityQueue<>();
        String sql = "SELECT * FROM appointments WHERE status = 'CONFIRMED' ORDER BY date ASC, time ASC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                queue.add(mapResultSet(rs));
            }
            return queue;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to populate upcoming appointments PriorityQueue", sql, e);
        }
    }
}

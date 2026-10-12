package com.healthkart.dao;

import com.healthkart.exception.DatabaseException;
import com.healthkart.model.Prescription;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete JDBC implementation of PrescriptionDao.
 */
@Repository
public class PrescriptionDaoImpl extends AbstractJdbcDao<Prescription, String> implements PrescriptionDao {

    @Override
    protected String getTableName() {
        return "prescriptions";
    }

    @Override
    protected Prescription mapResultSet(ResultSet rs) throws SQLException {
        Prescription p = new Prescription();
        p.setId(rs.getString("id"));
        p.setPatientName(rs.getString("patient_name"));
        p.setPatientEmail(rs.getString("patient_email"));
        p.setDoctorName(rs.getString("doctor_name"));
        p.setDoctorId(rs.getString("doctor_id"));
        p.setAppointmentId(rs.getString("appointment_id"));
        p.setDiagnosis(rs.getString("diagnosis"));
        p.setNotes(rs.getString("notes"));
        p.setMedicinesJson(rs.getString("medicines_json"));
        p.setCreatedAt(rs.getString("created_at"));
        return p;
    }

    @Override
    public Prescription save(Prescription p) {
        String sql = """
            INSERT INTO prescriptions (id, patient_name, patient_email, doctor_name,
                                      doctor_id, appointment_id, diagnosis, notes, medicines_json, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getPatientName());
            ps.setString(3, p.getPatientEmail());
            ps.setString(4, p.getDoctorName());
            ps.setString(5, p.getDoctorId());
            ps.setString(6, p.getAppointmentId());
            ps.setString(7, p.getDiagnosis());
            ps.setString(8, p.getNotes());
            ps.setString(9, p.getMedicinesJson());
            ps.setString(10, p.getCreatedAt());
            ps.executeUpdate();
            return p;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save prescription: " + p.getId(), sql, e);
        }
    }

    @Override
    public Prescription update(Prescription p) {
        String sql = """
            UPDATE prescriptions SET patient_name = ?, patient_email = ?, doctor_name = ?,
                                     doctor_id = ?, appointment_id = ?, diagnosis = ?,
                                     notes = ?, medicines_json = ?
            WHERE id = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getPatientName());
            ps.setString(2, p.getPatientEmail());
            ps.setString(3, p.getDoctorName());
            ps.setString(4, p.getDoctorId());
            ps.setString(5, p.getAppointmentId());
            ps.setString(6, p.getDiagnosis());
            ps.setString(7, p.getNotes());
            ps.setString(8, p.getMedicinesJson());
            ps.setString(9, p.getId());
            ps.executeUpdate();
            return p;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update prescription: " + p.getId(), sql, e);
        }
    }

    @Override
    public List<Prescription> findByPatientEmail(String email) {
        String sql = "SELECT * FROM prescriptions WHERE LOWER(patient_email) = LOWER(?) ORDER BY created_at DESC";
        List<Prescription> list = new ArrayList<>();
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
            throw new DatabaseException("Failed to find prescriptions for email: " + email, sql, e);
        }
    }

    @Override
    public List<Prescription> findByDoctorId(String doctorId) {
        String sql = "SELECT * FROM prescriptions WHERE doctor_id = ? ORDER BY created_at DESC";
        List<Prescription> list = new ArrayList<>();
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
            throw new DatabaseException("Failed to find prescriptions for doctor: " + doctorId, sql, e);
        }
    }

    @Override
    public List<Prescription> findByAppointmentId(String appointmentId) {
        String sql = "SELECT * FROM prescriptions WHERE appointment_id = ? ORDER BY created_at DESC";
        List<Prescription> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find prescriptions for appointment: " + appointmentId, sql, e);
        }
    }
}

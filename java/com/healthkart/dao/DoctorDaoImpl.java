package com.healthkart.dao;

import com.healthkart.exception.DatabaseException;
import com.healthkart.model.Doctor;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Concrete JDBC implementation of DoctorDao.
 * Demonstrates:
 * - OOP: Inheritance (extends AbstractJdbcDao), Polymorphism (overriding abstract methods), Interface implementation
 * - Database Operations: Prepared statements, CRUD, Result sets, batch operations
 * - Collections: Lists, Sets (TreeSet for sorted distinct specialties)
 */
@Repository
public class DoctorDaoImpl extends AbstractJdbcDao<Doctor, String> implements DoctorDao {

    @Override
    protected String getTableName() {
        return "doctors";
    }

    @Override
    protected Doctor mapResultSet(ResultSet rs) throws SQLException {
        Doctor doc = new Doctor();
        doc.setId(rs.getString("id"));
        doc.setName(rs.getString("name"));
        doc.setEmail(rs.getString("email"));
        doc.setPhone(rs.getString("phone"));
        doc.setSpecialization(rs.getString("specialization"));
        doc.setClinicHospital(rs.getString("clinic_hospital"));
        doc.setConsultationFee(rs.getDouble("consultation_fee"));
        doc.setExperience(rs.getInt("experience"));
        doc.setRating(rs.getDouble("rating"));
        doc.setImage(rs.getString("image"));

        String slotsStr = rs.getString("available_time_slots");
        if (slotsStr != null && !slotsStr.trim().isEmpty()) {
            doc.setAvailableTimeSlots(new ArrayList<>(Arrays.asList(slotsStr.split(","))));
        } else {
            doc.setAvailableTimeSlots(new ArrayList<>());
        }
        doc.setCreatedAt(rs.getString("created_at"));
        return doc;
    }

    @Override
    public Doctor save(Doctor doc) {
        String sql = """
            INSERT INTO doctors (id, name, email, phone, specialization, clinic_hospital,
                                 consultation_fee, experience, rating, image, available_time_slots, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doc.getId());
            ps.setString(2, doc.getName());
            ps.setString(3, doc.getEmail());
            ps.setString(4, doc.getPhone());
            ps.setString(5, doc.getSpecialization());
            ps.setString(6, doc.getClinicHospital());
            ps.setDouble(7, doc.getConsultationFee());
            ps.setInt(8, doc.getExperience());
            ps.setDouble(9, doc.getRating());
            ps.setString(10, doc.getImage());
            ps.setString(11, String.join(",", doc.getAvailableTimeSlots()));
            ps.setString(12, doc.getCreatedAt());
            ps.executeUpdate();
            return doc;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save doctor: " + doc.getName(), sql, e);
        }
    }

    @Override
    public Doctor update(Doctor doc) {
        String sql = """
            UPDATE doctors SET name = ?, email = ?, phone = ?, specialization = ?,
                               clinic_hospital = ?, consultation_fee = ?, experience = ?,
                               rating = ?, image = ?, available_time_slots = ?
            WHERE id = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doc.getName());
            ps.setString(2, doc.getEmail());
            ps.setString(3, doc.getPhone());
            ps.setString(4, doc.getSpecialization());
            ps.setString(5, doc.getClinicHospital());
            ps.setDouble(6, doc.getConsultationFee());
            ps.setInt(7, doc.getExperience());
            ps.setDouble(8, doc.getRating());
            ps.setString(9, doc.getImage());
            ps.setString(10, String.join(",", doc.getAvailableTimeSlots()));
            ps.setString(11, doc.getId());
            ps.executeUpdate();
            return doc;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update doctor: " + doc.getId(), sql, e);
        }
    }

    @Override
    public List<Doctor> findBySpecialization(String specialization) {
        String sql = "SELECT * FROM doctors WHERE LOWER(specialization) = LOWER(?)";
        List<Doctor> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, specialization);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find doctors by specialization", sql, e);
        }
    }

    @Override
    public List<Doctor> searchDoctors(String query) {
        String sql = """
            SELECT * FROM doctors
            WHERE LOWER(name) LIKE ? OR LOWER(specialization) LIKE ? OR LOWER(clinic_hospital) LIKE ?
        """;
        List<Doctor> list = new ArrayList<>();
        String pattern = "%" + (query == null ? "" : query.toLowerCase().trim()) + "%";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search doctors with query: " + query, sql, e);
        }
    }

    @Override
    public Set<String> getAllSpecializations() {
        String sql = "SELECT DISTINCT specialization FROM doctors ORDER BY specialization ASC";
        Set<String> specs = new TreeSet<>(); // Demonstrates Collections TreeSet (sorted unique)
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                specs.add(rs.getString(1));
            }
            return specs;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve specializations", sql, e);
        }
    }
}

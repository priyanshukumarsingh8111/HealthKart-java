package com.healthkart.dao;

import com.healthkart.exception.DatabaseException;
import com.healthkart.model.Reminder;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete JDBC implementation of ReminderDao.
 */
@Repository
public class ReminderDaoImpl extends AbstractJdbcDao<Reminder, String> implements ReminderDao {

    @Override
    protected String getTableName() {
        return "reminders";
    }

    @Override
    protected Reminder mapResultSet(ResultSet rs) throws SQLException {
        Reminder r = new Reminder();
        r.setId(rs.getString("id"));
        r.setMedicine(rs.getString("medicine"));
        r.setTiming(rs.getString("timing"));
        r.setTime(rs.getString("time"));
        r.setInstruction(rs.getString("instruction"));
        r.setPatientEmail(rs.getString("patient_email"));
        r.setActive(rs.getInt("active") == 1);
        r.setCreatedAt(rs.getString("created_at"));
        return r;
    }

    @Override
    public Reminder save(Reminder r) {
        String sql = """
            INSERT INTO reminders (id, medicine, timing, time, instruction, patient_email, active, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getId());
            ps.setString(2, r.getMedicine());
            ps.setString(3, r.getTiming());
            ps.setString(4, r.getTime());
            ps.setString(5, r.getInstruction());
            ps.setString(6, r.getPatientEmail());
            ps.setInt(7, r.isActive() ? 1 : 0);
            ps.setString(8, r.getCreatedAt());
            ps.executeUpdate();
            return r;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save reminder", sql, e);
        }
    }

    @Override
    public Reminder update(Reminder r) {
        String sql = """
            UPDATE reminders SET medicine = ?, timing = ?, time = ?, instruction = ?,
                                 patient_email = ?, active = ?
            WHERE id = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getMedicine());
            ps.setString(2, r.getTiming());
            ps.setString(3, r.getTime());
            ps.setString(4, r.getInstruction());
            ps.setString(5, r.getPatientEmail());
            ps.setInt(6, r.isActive() ? 1 : 0);
            ps.setString(7, r.getId());
            ps.executeUpdate();
            return r;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update reminder", sql, e);
        }
    }

    @Override
    public List<Reminder> findByPatientEmail(String email) {
        String sql = "SELECT * FROM reminders WHERE LOWER(patient_email) = LOWER(?) ORDER BY time ASC";
        List<Reminder> list = new ArrayList<>();
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
            throw new DatabaseException("Failed to find reminders for email: " + email, sql, e);
        }
    }

    @Override
    public List<Reminder> findActiveReminders() {
        String sql = "SELECT * FROM reminders WHERE active = 1 ORDER BY time ASC";
        List<Reminder> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find active reminders", sql, e);
        }
    }

    @Override
    public boolean toggleActive(String id, boolean active) {
        String sql = "UPDATE reminders SET active = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setString(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to toggle reminder state", sql, e);
        }
    }
}

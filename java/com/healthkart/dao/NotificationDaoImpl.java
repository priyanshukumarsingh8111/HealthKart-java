package com.healthkart.dao;

import com.healthkart.exception.DatabaseException;
import com.healthkart.model.Notification;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete JDBC implementation of NotificationDao.
 */
@Repository
public class NotificationDaoImpl extends AbstractJdbcDao<Notification, String> implements NotificationDao {

    @Override
    protected String getTableName() {
        return "notifications";
    }

    @Override
    protected Notification mapResultSet(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getString("id"));
        n.setPatientEmail(rs.getString("patient_email"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(rs.getString("type"));
        n.setRead(rs.getInt("is_read") == 1);
        n.setCreatedAt(rs.getString("created_at"));
        return n;
    }

    @Override
    public Notification save(Notification n) {
        String sql = """
            INSERT INTO notifications (id, patient_email, title, message, type, is_read, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, n.getId());
            ps.setString(2, n.getPatientEmail());
            ps.setString(3, n.getTitle());
            ps.setString(4, n.getMessage());
            ps.setString(5, n.getType());
            ps.setInt(6, n.isRead() ? 1 : 0);
            ps.setString(7, n.getCreatedAt());
            ps.executeUpdate();
            return n;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save notification", sql, e);
        }
    }

    @Override
    public Notification update(Notification n) {
        String sql = """
            UPDATE notifications SET patient_email = ?, title = ?, message = ?,
                                     type = ?, is_read = ?
            WHERE id = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, n.getPatientEmail());
            ps.setString(2, n.getTitle());
            ps.setString(3, n.getMessage());
            ps.setString(4, n.getType());
            ps.setInt(5, n.isRead() ? 1 : 0);
            ps.setString(6, n.getId());
            ps.executeUpdate();
            return n;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update notification", sql, e);
        }
    }

    @Override
    public List<Notification> findByPatientEmail(String email) {
        String sql = "SELECT * FROM notifications WHERE LOWER(patient_email) = LOWER(?) ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();
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
            throw new DatabaseException("Failed to find notifications for email: " + email, sql, e);
        }
    }

    @Override
    public boolean markAsRead(String id) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to mark notification as read", sql, e);
        }
    }

    @Override
    public long countUnread(String email) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE LOWER(patient_email) = LOWER(?) AND is_read = 0";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count unread notifications", sql, e);
        }
    }
}

package com.healthkart.dao;

import com.healthkart.exception.DatabaseException;
import com.healthkart.model.User;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Concrete JDBC implementation of UserDao extending AbstractJdbcDao.
 * Demonstrates:
 * - OOP: Inheritance (extends AbstractJdbcDao), Polymorphism (mapResultSet override)
 * - Classes for Database Operations (DAO Pattern)
 * - Database Connectivity (JDBC): Connection, PreparedStatement, ResultSet, transactions
 * - Exception Handling: Catching SQLExceptions, wrapping into DatabaseException
 */
@Repository
public class UserDaoImpl extends AbstractJdbcDao<User, String> implements UserDao {

    @Override
    protected String getTableName() {
        return "users";
    }

    @Override
    protected String getIdColumnName() {
        return "id";
    }

    @Override
    protected User mapResultSet(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getString("id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setRole(rs.getString("role"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setDoctorId(rs.getString("doctor_id"));
        u.setAddress(rs.getString("address"));
        u.setActive(rs.getInt("active") == 1);
        u.setCreatedAt(rs.getString("created_at"));
        u.setUpdatedAt(rs.getString("updated_at"));
        return u;
    }

    public static String hashPassword(String plain) {
        if (plain == null || plain.isEmpty()) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return plain; // fallback
        }
    }

    @Override
    public User save(User entity) {
        String sql = """
            INSERT INTO users (id, name, email, phone, role, password_hash, doctor_id, address, active, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String pass = entity.getPasswordHash();
            // If not already a 64-char hex hash, hash it
            if (pass != null && pass.length() != 64) {
                pass = hashPassword(pass);
            }

            ps.setString(1, entity.getId());
            ps.setString(2, entity.getName());
            ps.setString(3, entity.getEmail().toLowerCase().trim());
            ps.setString(4, entity.getPhone());
            ps.setString(5, entity.getRole() != null ? entity.getRole().toUpperCase() : "PATIENT");
            ps.setString(6, pass);
            ps.setString(7, entity.getDoctorId());
            ps.setString(8, entity.getAddress());
            ps.setInt(9, entity.isActive() ? 1 : 0);
            ps.setString(10, entity.getCreatedAt());
            ps.setString(11, entity.getUpdatedAt());

            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save user: " + entity.getEmail(), e);
        }
    }

    @Override
    public User update(User entity) {
        String sql = """
            UPDATE users SET name = ?, email = ?, phone = ?, role = ?, doctor_id = ?, address = ?, active = ?, updated_at = ?
            WHERE id = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, entity.getName());
            ps.setString(2, entity.getEmail().toLowerCase().trim());
            ps.setString(3, entity.getPhone());
            ps.setString(4, entity.getRole());
            ps.setString(5, entity.getDoctorId());
            ps.setString(6, entity.getAddress());
            ps.setInt(7, entity.isActive() ? 1 : 0);
            ps.setString(8, entity.getUpdatedAt());
            ps.setString(9, entity.getId());

            ps.executeUpdate();
            return entity;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update user: " + entity.getId(), e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query user by email: " + email, e);
        }
        return Optional.empty();
    }

    @Override
    public boolean authenticate(String email, String plainPassword) {
        return login(email, plainPassword).isPresent();
    }

    @Override
    public Optional<User> login(String email, String plainPassword) {
        if (email == null || plainPassword == null) return Optional.empty();
        Optional<User> opt = findByEmail(email);
        if (opt.isEmpty()) return Optional.empty();

        User user = opt.get();
        if (!user.isActive()) return Optional.empty();

        String expectedHash = user.getPasswordHash();
        String candidateHash = hashPassword(plainPassword);

        // Verify either against hash or plain password (for backward compatibility)
        if (candidateHash.equalsIgnoreCase(expectedHash) || plainPassword.equals(expectedHash)) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    @Override
    public boolean emailExists(String email) {
        if (email == null) return false;
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check email existence: " + email, e);
        }
    }

    @Override
    public boolean updatePassword(String email, String newPlainPassword) {
        String sql = "UPDATE users SET password_hash = ?, updated_at = ? WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashPassword(newPlainPassword));
            ps.setString(2, java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            ps.setString(3, email.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update password for: " + email, e);
        }
    }

    @Override
    public List<User> findByRole(String role) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE UPPER(role) = UPPER(?) ORDER BY name ASC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query users by role: " + role, e);
        }
        return list;
    }
}

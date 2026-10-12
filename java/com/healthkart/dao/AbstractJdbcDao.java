package com.healthkart.dao;

import com.healthkart.db.DatabaseConnectionManager;
import com.healthkart.exception.DatabaseException;
import com.healthkart.model.BaseEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Abstract Generic JDBC DAO providing template execution, connection handling,
 * and standard CRUD implementations.
 *
 * Demonstrates:
 * - OOP: Inheritance, Polymorphism (template method pattern), Exception Handling
 * - Generics: Parameterized on Entity type T and Identifier type ID
 * - JDBC: java.sql.Connection, PreparedStatement, ResultSet, SQLException
 *
 * @param <T>  Entity type extending BaseEntity
 * @param <ID> Identity key type
 */
public abstract class AbstractJdbcDao<T extends BaseEntity, ID> implements CrudDao<T, ID> {

    protected final DatabaseConnectionManager connectionManager;

    public AbstractJdbcDao() {
        this.connectionManager = DatabaseConnectionManager.getInstance();
    }

    protected Connection getConnection() throws SQLException {
        return connectionManager.getConnection();
    }

    /**
     * Abstract template method: children override polymorphically
     * to map a single SQL ResultSet row to entity T.
     */
    protected abstract T mapResultSet(ResultSet rs) throws SQLException;

    /**
     * Database table name for this DAO.
     */
    protected abstract String getTableName();

    /**
     * Primary key column name (defaults to "id").
     */
    protected String getIdColumnName() {
        return "id";
    }

    @Override
    public Optional<T> findById(ID id) {
        String sql = "SELECT * FROM " + getTableName() + " WHERE " + getIdColumnName() + " = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Error finding " + getTableName() + " by ID: " + id, sql, e);
        }
    }

    @Override
    public List<T> findAll() {
        String sql = "SELECT * FROM " + getTableName();
        List<T> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding all records from " + getTableName(), sql, e);
        }
    }

    @Override
    public boolean deleteById(ID id) {
        String sql = "DELETE FROM " + getTableName() + " WHERE " + getIdColumnName() + " = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting from " + getTableName() + " ID: " + id, sql, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + getTableName();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting records in " + getTableName(), sql, e);
        }
    }
}

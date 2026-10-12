package com.healthkart.dao;

import com.healthkart.exception.DatabaseException;
import com.healthkart.model.Medicine;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Concrete JDBC implementation of MedicineDao.
 * Demonstrates:
 * - OOP: Inheritance (extends AbstractJdbcDao), Polymorphism, Interfaces
 * - Database Operations: Prepared statements, CRUD, stock update transactions
 * - Collections & Generics: TreeSet for categories, Comparators for in-memory sorting fallback
 */
@Repository
public class MedicineDaoImpl extends AbstractJdbcDao<Medicine, String> implements MedicineDao {

    @Override
    protected String getTableName() {
        return "medicines";
    }

    @Override
    protected Medicine mapResultSet(ResultSet rs) throws SQLException {
        Medicine med = new Medicine();
        med.setId(rs.getString("id"));
        med.setName(rs.getString("name"));
        med.setCategory(rs.getString("category"));
        med.setDescription(rs.getString("description"));
        med.setPrice(rs.getDouble("price"));
        med.setRating(rs.getDouble("rating"));
        med.setIcon(rs.getString("icon"));
        String img = null;
        try {
            img = rs.getString("image");
        } catch (SQLException ignored) {}
        if (img == null || img.isBlank()) {
            img = "/images/medicines/" + med.getId() + ".jpg";
        }
        med.setImage(img);
        med.setStockQuantity(rs.getInt("stock_quantity"));
        med.setManufacturer(rs.getString("manufacturer"));
        med.setCreatedAt(rs.getString("created_at"));

        // Generate provider comparison prices for Compare feature
        double p = med.getPrice();
        List<Map<String, Object>> prov = new ArrayList<>();
        prov.add(Map.of("name", "1mg", "price", Math.round(p * 1.05 * 100.0) / 100.0));
        prov.add(Map.of("name", "PharmEasy", "price", Math.round(p * 0.96 * 100.0) / 100.0));
        prov.add(Map.of("name", "Apollo", "price", Math.round(p * 1.00 * 100.0) / 100.0));
        med.setProviders(prov);

        return med;
    }

    @Override
    public Medicine save(Medicine med) {
        String sql = """
            INSERT INTO medicines (id, name, category, description, price, rating, icon, image, stock_quantity, manufacturer, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, med.getId());
            ps.setString(2, med.getName());
            ps.setString(3, med.getCategory());
            ps.setString(4, med.getDescription());
            ps.setDouble(5, med.getPrice());
            ps.setDouble(6, med.getRating());
            ps.setString(7, med.getIcon());
            ps.setString(8, med.getImage());
            ps.setInt(9, med.getStockQuantity());
            ps.setString(10, med.getManufacturer());
            ps.setString(11, med.getCreatedAt());
            ps.executeUpdate();
            return med;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save medicine: " + med.getName(), sql, e);
        }
    }

    @Override
    public Medicine update(Medicine med) {
        String sql = """
            UPDATE medicines SET name = ?, category = ?, description = ?, price = ?,
                                 rating = ?, icon = ?, image = ?, stock_quantity = ?, manufacturer = ?
            WHERE id = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, med.getName());
            ps.setString(2, med.getCategory());
            ps.setString(3, med.getDescription());
            ps.setDouble(4, med.getPrice());
            ps.setDouble(5, med.getRating());
            ps.setString(6, med.getIcon());
            ps.setString(7, med.getImage());
            ps.setInt(8, med.getStockQuantity());
            ps.setString(9, med.getManufacturer());
            ps.setString(10, med.getId());
            ps.executeUpdate();
            return med;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update medicine: " + med.getId(), sql, e);
        }
    }

    @Override
    public List<Medicine> searchAndFilter(String search, String category, String sortBy) {
        StringBuilder sql = new StringBuilder("SELECT * FROM medicines WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(name) LIKE ? OR LOWER(description) LIKE ? OR LOWER(category) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (category != null && !category.trim().isEmpty() && !"all".equalsIgnoreCase(category)) {
            sql.append("AND LOWER(category) = LOWER(?) ");
            params.add(category.trim());
        }

        if ("price-low".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY price ASC");
        } else if ("price-high".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY price DESC");
        } else if ("rating".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY rating DESC");
        } else {
            sql.append("ORDER BY name ASC");
        }

        List<Medicine> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query medicines", sql.toString(), e);
        }
    }

    @Override
    public Set<String> getAllCategories() {
        String sql = "SELECT DISTINCT category FROM medicines ORDER BY category ASC";
        Set<String> categories = new TreeSet<>(); // Demonstrates Collections TreeSet
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(rs.getString(1));
            }
            return categories;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve medicine categories", sql, e);
        }
    }

    @Override
    public synchronized boolean updateStock(String medicineId, int quantityDelta) {
        String sql = "UPDATE medicines SET stock_quantity = stock_quantity + ? WHERE id = ? AND stock_quantity + ? >= 0";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantityDelta);
            ps.setString(2, medicineId);
            ps.setInt(3, quantityDelta);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update medicine stock", sql, e);
        }
    }

    @Override
    public List<Medicine> findLowStockMedicines(int threshold) {
        String sql = "SELECT * FROM medicines WHERE stock_quantity <= ? ORDER BY stock_quantity ASC";
        List<Medicine> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query low stock medicines", sql, e);
        }
    }
}

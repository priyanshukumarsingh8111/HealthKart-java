package com.healthkart.dao;

import com.healthkart.model.Medicine;

import java.util.List;
import java.util.Set;

/**
 * Medicine DAO interface extending generic CrudDao.
 * Demonstrates Collections, Generics, and OOP Interfaces.
 */
public interface MedicineDao extends CrudDao<Medicine, String> {

    List<Medicine> searchAndFilter(String search, String category, String sortBy);

    Set<String> getAllCategories();

    boolean updateStock(String medicineId, int quantityDelta);

    List<Medicine> findLowStockMedicines(int threshold);
}

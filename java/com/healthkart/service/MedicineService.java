package com.healthkart.service;

import com.healthkart.dao.MedicineDao;
import com.healthkart.exception.EntityNotFoundException;
import com.healthkart.exception.ValidationException;
import com.healthkart.model.Medicine;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service class managing medicines, inventory, search, and sorting.
 *
 * Demonstrates:
 * - OOP: Business validation and service abstraction
 * - Collections & Generics: Custom Comparators, Sets, Lists, Streams
 */
@Service
public class MedicineService {

    private final MedicineDao medicineDao;

    public MedicineService(MedicineDao medicineDao) {
        this.medicineDao = medicineDao;
    }

    public List<Medicine> getMedicines(String search, String category, String sort) {
        return medicineDao.searchAndFilter(search, category, sort);
    }

    public Medicine getMedicineById(String id) {
        return medicineDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Medicine", id));
    }

    public Set<String> getCategories() {
        return medicineDao.getAllCategories();
    }

    public Medicine saveMedicine(Medicine med) {
        if (med.getName() == null || med.getName().trim().isEmpty()) {
            throw new ValidationException("Medicine name cannot be empty");
        }
        if (med.getPrice() < 0) {
            throw new ValidationException("Medicine price cannot be negative");
        }
        return medicineDao.save(med);
    }

    public boolean purchaseMedicine(String id, int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("Purchase quantity must be greater than zero");
        }
        return medicineDao.updateStock(id, -quantity);
    }

    public List<Medicine> getLowStockAlerts(int threshold) {
        return medicineDao.findLowStockMedicines(threshold);
    }
}

package com.healthkart;

import com.healthkart.dao.DoctorDao;
import com.healthkart.dao.MedicineDao;
import com.healthkart.model.Doctor;
import com.healthkart.model.Medicine;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.healthkart.Util.*;

/**
 * Catalog REST Controller for HTML & CSS Frontend.
 * Directly powered by pure JDBC DAOs (DoctorDao, MedicineDao) and domain models.
 */
@RestController
@RequestMapping("/api")
public class CatalogController {
    private final DoctorDao doctorDao;
    private final MedicineDao medicineDao;

    public CatalogController(DoctorDao doctorDao, MedicineDao medicineDao) {
        this.doctorDao = doctorDao;
        this.medicineDao = medicineDao;
    }

    @GetMapping("/doctors")
    public Map<String, Object> doctors() {
        List<Doctor> list = doctorDao.findAll();
        return m("success", true, "count", list.size(), "doctors", list);
    }

    @GetMapping("/doctors/{id}")
    public Map<String, Object> doctor(@PathVariable("id") String id) {
        return doctorDao.findById(id)
                .map(d -> m("success", true, "doctor", d))
                .orElseThrow(() -> ApiException.of(404, "Doctor not found"));
    }

    @GetMapping("/medicines")
    public Map<String, Object> medicines(@RequestParam(name = "search", required = false) String search,
                                         @RequestParam(name = "category", required = false) String category,
                                         @RequestParam(name = "sort", required = false) String sort) {
        List<Medicine> list = medicineDao.searchAndFilter(search, category, sort);
        return m("success", true, "count", list.size(), "medicines", list);
    }

    @GetMapping("/medicines/{id}")
    public Map<String, Object> medicine(@PathVariable("id") String id) {
        return medicineDao.findById(id)
                .map(x -> m("success", true, "medicine", x))
                .orElseThrow(() -> ApiException.of(404, "Product not found"));
    }
}

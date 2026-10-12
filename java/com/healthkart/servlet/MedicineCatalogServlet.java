package com.healthkart.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthkart.dao.MedicineDao;
import com.healthkart.dao.MedicineDaoImpl;
import com.healthkart.model.Medicine;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Standard Java HttpServlet serving medicine catalog and categories.
 * Demonstrates:
 * - Servlets & Web Integration (7 Marks)
 * - Collections & Generics: TreeSet categories and List<Medicine>
 * - JDBC Database Integration: Pure JDBC queries via MedicineDao
 */
public class MedicineCatalogServlet extends HttpServlet {

    private final MedicineDao medicineDao;
    private final ObjectMapper mapper;

    public MedicineCatalogServlet() {
        this.medicineDao = new MedicineDaoImpl();
        this.mapper = new ObjectMapper();
    }

    public MedicineCatalogServlet(MedicineDao medicineDao) {
        this.medicineDao = medicineDao;
        this.mapper = new ObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String action = req.getParameter("action");
        String search = req.getParameter("search");
        String category = req.getParameter("category");
        String sort = req.getParameter("sort");

        Map<String, Object> result = new LinkedHashMap<>();

        if ("categories".equalsIgnoreCase(action)) {
            Set<String> categories = medicineDao.getAllCategories();
            result.put("success", true);
            result.put("count", categories.size());
            result.put("categories", categories);
        } else {
            List<Medicine> medicines = medicineDao.searchAndFilter(search, category, sort);
            result.put("success", true);
            result.put("count", medicines.size());
            result.put("medicines", medicines);
        }

        result.put("source", "MedicineCatalogServlet (Jakarta HttpServlet)");

        try (PrintWriter out = resp.getWriter()) {
            out.print(mapper.writeValueAsString(result));
            out.flush();
        }
    }
}

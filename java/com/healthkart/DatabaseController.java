package com.healthkart;

import com.healthkart.dao.AppointmentDao;
import com.healthkart.dao.DoctorDao;
import com.healthkart.dao.MedicineDao;
import com.healthkart.db.DatabaseConnectionManager;
import com.healthkart.exception.SlotConflictException;
import com.healthkart.model.Appointment;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.healthkart.Util.m;

/**
 * REST Controller exposing live JDBC status, database schema inspection,
 * and multithreaded concurrency diagnostics for the HTML & CSS Frontend.
 */
@RestController
@RequestMapping("/api/jdbc")
public class DatabaseController {

    private final DatabaseConnectionManager connectionManager;
    private final AppointmentDao appointmentDao;
    private final DoctorDao doctorDao;
    private final MedicineDao medicineDao;

    public DatabaseController(AppointmentDao appointmentDao, DoctorDao doctorDao, MedicineDao medicineDao) {
        this.connectionManager = DatabaseConnectionManager.getInstance();
        this.appointmentDao = appointmentDao;
        this.doctorDao = doctorDao;
        this.medicineDao = medicineDao;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        List<Map<String, Object>> tableStats = new ArrayList<>();
        String dbProduct = "SQLite 3";
        String driver = "org.sqlite.JDBC";
        boolean connected = false;
        long latencyMs = 0;

        try {
            long start = System.currentTimeMillis();
            try (Connection conn = connectionManager.getConnection()) {
                connected = !conn.isClosed();
                DatabaseMetaData meta = conn.getMetaData();
                dbProduct = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
                driver = meta.getDriverName() + " " + meta.getDriverVersion();

                String[] tables = {"users", "doctors", "medicines", "appointments", "booked_slots", "prescriptions", "reminders", "notifications", "orders"};
                String[] daoNames = {"UserDaoImpl", "DoctorDaoImpl", "MedicineDaoImpl", "AppointmentDaoImpl", "AppointmentDaoImpl", "PrescriptionDaoImpl", "ReminderDaoImpl", "NotificationDaoImpl", "OrderDaoImpl"};

                try (Statement stmt = conn.createStatement()) {
                    for (int i = 0; i < tables.length; i++) {
                        String t = tables[i];
                        long count = 0;
                        try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM " + t)) {
                            if (rs.next()) count = rs.getLong(1);
                        } catch (Exception ignored) {}

                        tableStats.add(m("table", t, "count", count, "daoClass", daoNames[i]));
                    }
                }
            }
            latencyMs = System.currentTimeMillis() - start;
        } catch (Exception e) {
            return m("success", false, "error", e.getMessage());
        }

        return m(
                "success", true,
                "connected", connected,
                "latencyMs", latencyMs,
                "dbProduct", dbProduct,
                "driver", driver,
                "jdbcUrl", connectionManager.getJdbcUrl(),
                "tables", tableStats
        );
    }

    @PostMapping("/concurrency-test")
    public Map<String, Object> runConcurrencyTest() {
        final String runId = UUID.randomUUID().toString().substring(0, 8);
        final String testDate = LocalDate.now().plusDays(4).toString();
        final String testTime = "03:00 PM";
        final String docId = "rahul";
        final String testSlotKey = docId + "_" + testDate + "_" + testTime;

        // Clean any previous test run
        try (Connection conn = connectionManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM booked_slots WHERE slot_key = '" + testSlotKey + "'");
            stmt.executeUpdate("DELETE FROM appointments WHERE appointment_id LIKE 'WEB-STRESS-%'");
        } catch (Exception ignored) {}

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(threadCount);

        List<String> threadLogs = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 1; i <= threadCount; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    startSignal.await(); // Synchronize exact simultaneous execution
                    threadLogs.add("[Thread-" + id + "] Attempting to book slot via synchronized JDBC transaction...");

                    Appointment apt = new Appointment();
                    apt.setAppointmentId("WEB-STRESS-" + runId + "-" + id);
                    apt.setDoctorId(docId);
                    apt.setDoctorName("Dr. Rahul Sharma");
                    apt.setPatientName("Concurrent Patient " + id);
                    apt.setPatientEmail("concurrent" + id + "@test.com");
                    apt.setPatientPhone("9876543210");
                    apt.setDate(testDate);
                    apt.setTime(testTime);
                    apt.setSymptoms("Web concurrency test");
                    apt.setFee(500);

                    appointmentDao.bookAppointmentWithLock(apt);
                    successCount.incrementAndGet();
                    threadLogs.add(">>> [Thread-" + id + "] SUCCESS: Acquired ReentrantLock and committed JDBC transaction!");

                } catch (SlotConflictException e) {
                    conflictCount.incrementAndGet();
                    threadLogs.add("    [Thread-" + id + "] REJECTED: SlotConflictException caught. Double-booking prevented!");
                } catch (Exception e) {
                    threadLogs.add("    [Thread-" + id + "] Error: " + e.getMessage());
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        // Trigger all 5 threads simultaneously
        startSignal.countDown();

        try {
            doneSignal.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        executor.shutdown();

        boolean passed = (successCount.get() == 1 && conflictCount.get() == (threadCount - 1));

        return m(
                "success", true,
                "passed", passed,
                "threadsTested", threadCount,
                "successfulBookings", successCount.get(),
                "preventedConflicts", conflictCount.get(),
                "conclusion", passed ? "PASS: Exactly 1 thread booked the slot. 4 threads were rejected. Zero double-bookings!" : "FAIL: Race condition occurred.",
                "logs", threadLogs
        );
    }
}

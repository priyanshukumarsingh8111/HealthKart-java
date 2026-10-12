package com.healthkart;

import com.healthkart.dao.*;
import com.healthkart.db.DatabaseConnectionManager;
import com.healthkart.exception.SlotConflictException;
import com.healthkart.exception.ValidationException;
import com.healthkart.model.*;
import com.healthkart.service.AppointmentService;
import com.healthkart.service.MedicineService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated System Architecture and Engineering Verification Test Suite.
 */
public class ArchitectureVerificationTest {

    private static DoctorDao doctorDao;
    private static MedicineDao medicineDao;
    private static AppointmentDao appointmentDao;
    private static PrescriptionDao prescriptionDao;
    private static ReminderDao reminderDao;
    private static NotificationDao notificationDao;

    private static AppointmentService appointmentService;
    private static MedicineService medicineService;

    @BeforeAll
    public static void setup() {
        doctorDao = new DoctorDaoImpl();
        medicineDao = new MedicineDaoImpl();
        appointmentDao = new AppointmentDaoImpl();
        prescriptionDao = new PrescriptionDaoImpl();
        reminderDao = new ReminderDaoImpl();
        notificationDao = new NotificationDaoImpl();

        appointmentService = new AppointmentService(appointmentDao, doctorDao, notificationDao);
        medicineService = new MedicineService(medicineDao);
    }

    @Test
    @DisplayName("Architecture Test: OOP Implementation (Polymorphism, Inheritance, Exception Handling, Interfaces)")
    public void testOOPImplementation() {
        // 1. Inheritance: Person extends BaseEntity, Doctor extends Person
        Doctor doc = new Doctor("doc-test", "Dr. Test Specialist", "test@hk.com", "9999999999",
                "Cardiologist", "Apollo Clinic", 1200.0, 14, 4.9);
        assertTrue(doc instanceof Person, "Doctor must inherit from Person");
        assertTrue(doc instanceof BaseEntity, "Doctor must inherit from BaseEntity");
        assertNotNull(doc.getCreatedAt(), "BaseEntity must initialize audit fields");

        // 2. Polymorphism: overridden getRoleDescription()
        String roleDesc = doc.getRoleDescription();
        assertTrue(roleDesc.contains("Cardiologist"), "Polymorphic method must return Doctor specific description");

        // 3. Interfaces: CrudDao, DoctorDao
        assertTrue(doctorDao instanceof CrudDao, "DoctorDao must implement generic CrudDao interface");

        // 4. Exception Handling
        assertThrows(ValidationException.class, () -> {
            appointmentService.getSlots(null, "2026-10-15");
        }, "ValidationException must be thrown on missing parameters");

        // Sunday rule exception
        assertThrows(ValidationException.class, () -> {
            appointmentService.getSlots("rahul", "2026-10-18"); // 2026-10-18 is Sunday
        }, "ValidationException must reject Sunday bookings");
    }

    @Test
    @DisplayName("Architecture Test: Collections & Generics Framework")
    public void testCollectionsAndGenerics() {
        // 1. Generics: DataResult<T>
        DataResult<Doctor> result = DataResult.success("Success", new Doctor());
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());

        // 2. Collections: List & Comparable Sorting
        List<Medicine> medicines = medicineDao.findAll();
        assertFalse(medicines.isEmpty(), "Medicines list should not be empty");

        // Sort using Collections Comparable<Medicine>
        Collections.sort(medicines);
        assertTrue(medicines.get(0).getName().compareToIgnoreCase(medicines.get(medicines.size() - 1).getName()) <= 0);

        // 3. Collections: TreeSet for unique sorted categories
        Set<String> categories = medicineDao.getAllCategories();
        assertFalse(categories.isEmpty());
        assertTrue(categories instanceof TreeSet, "Categories must use sorted TreeSet");

        // 4. Collections: PriorityQueue for appointment triage
        PriorityQueue<Appointment> queue = appointmentDao.getUpcomingPriorityQueue();
        assertNotNull(queue, "PriorityQueue must be initialized");
    }

    @Test
    @DisplayName("Architecture Test: Multithreading & Synchronization with Lock Protection")
    public void testMultithreadingAndSynchronization() throws InterruptedException {
        // High concurrency stress test: 5 threads attempting to book the EXACT SAME SLOT simultaneously
        String testDate = LocalDate.now().plusDays(15 + (int)(Math.random() * 100)).toString();
        String testTime = "11:00 AM";
        String doctorId = "rahul";

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        final String runId = System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize thread release

                    Appointment apt = new Appointment();
                    apt.setAppointmentId("CONC-" + runId + "-" + id);
                    apt.setDoctorId(doctorId);
                    apt.setDoctorName("Dr. Rahul Sharma");
                    apt.setPatientName("Patient " + id);
                    apt.setPatientEmail("patient" + id + "@test.com");
                    apt.setPatientPhone("9876543210");
                    apt.setDate(testDate);
                    apt.setTime(testTime);
                    apt.setFee(500);

                    appointmentDao.bookAppointmentWithLock(apt);
                    successCount.incrementAndGet();
                } catch (SlotConflictException e) {
                    conflictCount.incrementAndGet(); // Thread safety correctly rejected double-booking
                } catch (Throwable e) {
                    System.err.println("THREAD ERROR: " + e.getClass().getName() + " -> " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Fire all threads at once!
        startLatch.countDown();
        finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Verification: Exactly 1 thread must succeed, and 4 threads must receive SlotConflictException
        assertEquals(1, successCount.get(), "Synchronization lock must allow exactly ONE booking for the slot");
        assertEquals(4, conflictCount.get(), "Synchronization lock must reject all competing threads to prevent double booking");

        // Clean up test appointment
        for (int i = 0; i < threadCount; i++) {
            appointmentDao.deleteById("CONC-" + runId + "-" + i);
        }
    }

    @Test
    @DisplayName("Architecture Test: Data Access Object (DAO) Operations")
    public void testDatabaseOperationsClasses() {
        // Test DoctorDao operations
        long initialDocs = doctorDao.count();
        assertTrue(initialDocs > 0, "Doctors should exist in database");

        Doctor testDoc = new Doctor("dao-doc-1", "Dr. Unit Test", "unit@doc.com", "9123456789",
                "Dentist", "Dental Care Clinic", 450.0, 7, 4.8);
        doctorDao.save(testDoc);

        Optional<Doctor> foundDoc = doctorDao.findById("dao-doc-1");
        assertTrue(foundDoc.isPresent(), "DoctorDao.save and findById must work");
        assertEquals("Dr. Unit Test", foundDoc.get().getName());

        testDoc.setClinicHospital("Updated Hospital");
        doctorDao.update(testDoc);
        assertEquals("Updated Hospital", doctorDao.findById("dao-doc-1").get().getClinicHospital());

        doctorDao.deleteById("dao-doc-1");
        assertFalse(doctorDao.findById("dao-doc-1").isPresent(), "DoctorDao.deleteById must remove entity");

        // Test MedicineDao operations
        long initialMeds = medicineDao.count();
        assertTrue(initialMeds > 0, "MedicineDao must have records");

        // Verify real photography and provider comparisons
        List<Doctor> allDocs = doctorDao.findAll();
        assertFalse(allDocs.isEmpty());
        for (Doctor d : allDocs) {
            assertNotNull(d.getImage(), "Doctor image must not be null: " + d.getName());
            assertFalse(d.getImage().trim().isEmpty(), "Doctor image must be populated: " + d.getName());
            assertTrue(d.getImage().startsWith("http"), "Doctor image must be valid URL: " + d.getName());
        }

        List<Medicine> allMeds = medicineDao.findAll();
        assertFalse(allMeds.isEmpty());
        for (Medicine m : allMeds) {
            assertNotNull(m.getImage(), "Medicine image must not be null: " + m.getName());
            assertFalse(m.getImage().trim().isEmpty(), "Medicine image must be populated: " + m.getName());
            assertTrue(m.getImage().startsWith("http"), "Medicine image must be valid URL: " + m.getName());
            assertNotNull(m.getProviders(), "Providers list must not be null");
            assertFalse(m.getProviders().isEmpty(), "Providers must be populated for price comparison");
        }
    }

    @Test
    @DisplayName("Architecture Test: Pure JDBC Database Connectivity")
    public void testDatabaseConnectivityJDBC() throws Exception {
        DatabaseConnectionManager manager = DatabaseConnectionManager.getInstance();
        assertNotNull(manager, "DatabaseConnectionManager must exist");

        try (Connection conn = manager.getConnection()) {
            assertNotNull(conn, "JDBC Connection must be non-null");
            assertFalse(conn.isClosed(), "JDBC Connection must be active");

            // Execute SQL query via JDBC Statement
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT count(*) FROM doctors")) {
                assertTrue(rs.next(), "ResultSet must contain count record");
                int count = rs.getInt(1);
                assertTrue(count >= 0, "Count must be non-negative");
            }
        }
    }
}

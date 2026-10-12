package com.healthkart.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthkart.exception.DatabaseException;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * DatabaseConnectionManager handles pure JDBC connectivity, schema creation,
 * connection lifecycle, and dialect support for MySQL 8.0 with automatic SQLite fallback.
 *
 * Demonstrates:
 * - Database Connectivity (JDBC): Connection, DriverManager, Statement, PreparedStatement, ResultSet
 * - Multithreading & Synchronization: Thread-safe Singleton with Double-Checked Locking
 * - Exception Handling: Catching SQLExceptions and wrapping into custom DatabaseException
 * - Relational Database Support: Primary MySQL 8.0 relational database with SQLite local fallback
 */
public class DatabaseConnectionManager {
    private static volatile DatabaseConnectionManager instance;
    private static final Object LOCK = new Object();

    // Database connection state
    private String databaseType = "MySQL";
    private boolean isMysql = true;
    private String jdbcUrl;
    private String username = "root";
    private String password = "";
    private String driverClassName = "com.mysql.cj.jdbc.Driver";

    private static String resolveSqliteUrl() {
        File dataDir = new File("data");
        if (!dataDir.exists() && new File("healthkart-java/data").exists()) {
            return "jdbc:sqlite:healthkart-java/data/healthkart.db";
        }
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        return "jdbc:sqlite:data/healthkart.db";
    }

    private DatabaseConnectionManager() {
        loadConfiguration();
        initSchema();
        seedUsersIfEmpty();
        migrateFromJsonIfEmpty();
        syncRealImagesAndProviders();
    }

    /**
     * Loads database configuration from application.properties or environment,
     * defaulting to MySQL 8.0 on localhost:3306 with seamless SQLite fallback.
     */
    private void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception ignored) {}

        if (props.isEmpty()) {
            for (String loc : List.of("application.properties", "src/main/resources/application.properties", "healthkart-java/src/main/resources/application.properties")) {
                File f = new File(loc);
                if (f.exists()) {
                    try (var fis = new java.io.FileInputStream(f)) {
                        props.load(fis);
                        break;
                    } catch (Exception ignored) {}
                }
            }
        }

        String prefType = System.getProperty("healthkart.db.type", props.getProperty("healthkart.db.type", "mysql")).trim().toLowerCase();
        String defaultMysqlUrl = "jdbc:mysql://localhost:3306/healthkart_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true";
        String mysqlUrl = System.getProperty("healthkart.db.mysql.url", props.getProperty("healthkart.db.mysql.url", defaultMysqlUrl));
        String mysqlUser = System.getProperty("healthkart.db.mysql.username", props.getProperty("healthkart.db.mysql.username", "root"));
        String mysqlPass = System.getProperty("healthkart.db.mysql.password", props.getProperty("healthkart.db.mysql.password", "Priyanshu@12345"));
        String mysqlDriver = System.getProperty("healthkart.db.mysql.driver", props.getProperty("healthkart.db.mysql.driver", "com.mysql.cj.jdbc.Driver"));

        String sqliteDriver = "org.sqlite.JDBC";
        String sqliteUrl = System.getProperty("healthkart.db.sqlite.url", props.getProperty("healthkart.db.sqlite.url", resolveSqliteUrl()));

        if ("mysql".equals(prefType)) {
            try {
                Class.forName(mysqlDriver);
                try (Connection testConn = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPass)) {
                    this.driverClassName = mysqlDriver;
                    this.jdbcUrl = mysqlUrl;
                    this.username = mysqlUser;
                    this.password = mysqlPass;
                    this.isMysql = true;
                    this.databaseType = "MySQL";
                    System.out.println(">>> [JDBC] Connected successfully to Primary MySQL Database: " + mysqlUrl);
                    return;
                }
            } catch (Throwable t) {
                System.err.println(">>> [JDBC] MySQL connection failed (" + t.getMessage() + "). Falling back to SQLite.");
            }
        }

        // Fallback or explicit SQLite mode
        this.driverClassName = sqliteDriver;
        this.jdbcUrl = sqliteUrl;
        this.username = "";
        this.password = "";
        this.isMysql = false;
        this.databaseType = "SQLite";
        try {
            Class.forName(sqliteDriver);
        } catch (ClassNotFoundException e) {
            System.err.println(">>> [JDBC] SQLite driver not found: " + e.getMessage());
        }
        System.out.println(">>> [JDBC] Active Database Engine: SQLite (" + sqliteUrl + ")");
    }

    /**
     * Thread-safe Singleton access using Double-Checked Locking.
     */
    public static DatabaseConnectionManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new DatabaseConnectionManager();
                }
            }
        }
        return instance;
    }

    /**
     * Obtains a standard JDBC Connection.
     */
    public Connection getConnection() throws SQLException {
        if (!isMysql) {
            File dataDir = new File("data");
            if (!dataDir.exists() && !new File("healthkart-java/data").exists()) {
                dataDir.mkdirs();
            }
        }
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /**
     * Returns true if MySQL is the active relational database engine.
     */
    public boolean isMySQL() {
        return isMysql;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getUsername() {
        return username;
    }

    /**
     * Initializes relational database schema with tables and indexes.
     * Compatible with both MySQL 8.0 and SQLite.
     */
    private void initSchema() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            if (isMysql) {
                // --- MySQL 8.0 Relational Schema ---
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS doctors (
                        id VARCHAR(64) PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        email VARCHAR(255),
                        phone VARCHAR(50),
                        specialization VARCHAR(255) NOT NULL,
                        clinic_hospital VARCHAR(255),
                        consultation_fee DOUBLE NOT NULL,
                        experience INT NOT NULL,
                        rating DOUBLE NOT NULL,
                        image VARCHAR(255),
                        available_time_slots TEXT,
                        created_at VARCHAR(50)
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS medicines (
                        id VARCHAR(64) PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        category VARCHAR(255) NOT NULL,
                        description TEXT,
                        price DOUBLE NOT NULL,
                        rating DOUBLE NOT NULL,
                        icon VARCHAR(64),
                        image VARCHAR(255),
                        stock_quantity INT DEFAULT 100,
                        manufacturer VARCHAR(255) DEFAULT 'HealthKart Labs',
                        providers_json TEXT,
                        created_at VARCHAR(50)
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS appointments (
                        appointment_id VARCHAR(64) PRIMARY KEY,
                        doctor_id VARCHAR(64) NOT NULL,
                        doctor_name VARCHAR(255) NOT NULL,
                        patient_name VARCHAR(255) NOT NULL,
                        patient_email VARCHAR(255) NOT NULL,
                        patient_phone VARCHAR(50),
                        date VARCHAR(20) NOT NULL,
                        time VARCHAR(20) NOT NULL,
                        symptoms TEXT,
                        status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
                        fee DOUBLE NOT NULL,
                        join_url VARCHAR(255),
                        clinic_hospital VARCHAR(255),
                        doctor_image VARCHAR(255),
                        specialization VARCHAR(255),
                        created_at VARCHAR(50)
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS booked_slots (
                        slot_key VARCHAR(128) PRIMARY KEY,
                        doctor_id VARCHAR(64) NOT NULL,
                        date VARCHAR(20) NOT NULL,
                        time VARCHAR(20) NOT NULL,
                        appointment_id VARCHAR(64) NOT NULL,
                        status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
                        updated_at VARCHAR(50)
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS prescriptions (
                        id VARCHAR(64) PRIMARY KEY,
                        patient_name VARCHAR(255) NOT NULL,
                        patient_email VARCHAR(255) NOT NULL,
                        doctor_name VARCHAR(255) NOT NULL,
                        doctor_id VARCHAR(64) NOT NULL,
                        appointment_id VARCHAR(64),
                        diagnosis TEXT,
                        notes TEXT,
                        medicines_json LONGTEXT,
                        created_at VARCHAR(50)
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS reminders (
                        id VARCHAR(64) PRIMARY KEY,
                        medicine VARCHAR(255) NOT NULL,
                        timing VARCHAR(255),
                        time VARCHAR(20) NOT NULL,
                        instruction TEXT,
                        patient_email VARCHAR(255) NOT NULL,
                        active INT DEFAULT 1,
                        created_at VARCHAR(50)
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS notifications (
                        id VARCHAR(64) PRIMARY KEY,
                        patient_email VARCHAR(255) NOT NULL,
                        title VARCHAR(255) NOT NULL,
                        message TEXT,
                        type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
                        is_read INT DEFAULT 0,
                        created_at VARCHAR(50)
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id VARCHAR(64) PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        email VARCHAR(255) NOT NULL UNIQUE,
                        phone VARCHAR(50),
                        role VARCHAR(50) NOT NULL DEFAULT 'PATIENT',
                        password_hash VARCHAR(255) NOT NULL,
                        doctor_id VARCHAR(64),
                        address TEXT,
                        active INT DEFAULT 1,
                        created_at VARCHAR(50),
                        updated_at VARCHAR(50)
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS orders (
                        order_id VARCHAR(64) PRIMARY KEY,
                        user_email VARCHAR(255) NOT NULL,
                        customer_name VARCHAR(255) NOT NULL,
                        customer_phone VARCHAR(50),
                        delivery_address TEXT,
                        city VARCHAR(100),
                        pincode VARCHAR(20),
                        delivery_instructions TEXT,
                        items_json LONGTEXT,
                        item_count INT DEFAULT 1,
                        mrp_total DOUBLE DEFAULT 0.0,
                        discount_amount DOUBLE DEFAULT 0.0,
                        delivery_fee DOUBLE DEFAULT 0.0,
                        total_amount DOUBLE NOT NULL,
                        payment_method VARCHAR(100) NOT NULL,
                        payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING_COD',
                        order_status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
                        estimated_delivery VARCHAR(100),
                        created_at VARCHAR(50)
                    );
                """);

            } else {
                // --- SQLite Relational Schema ---
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS doctors (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        email TEXT,
                        phone TEXT,
                        specialization TEXT NOT NULL,
                        clinic_hospital TEXT,
                        consultation_fee REAL NOT NULL,
                        experience INTEGER NOT NULL,
                        rating REAL NOT NULL,
                        image TEXT,
                        available_time_slots TEXT,
                        created_at TEXT
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS medicines (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        description TEXT,
                        price REAL NOT NULL,
                        rating REAL NOT NULL,
                        icon TEXT,
                        image TEXT,
                        stock_quantity INTEGER DEFAULT 100,
                        manufacturer TEXT DEFAULT 'HealthKart Labs',
                        providers_json TEXT,
                        created_at TEXT
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS appointments (
                        appointment_id TEXT PRIMARY KEY,
                        doctor_id TEXT NOT NULL,
                        doctor_name TEXT NOT NULL,
                        patient_name TEXT NOT NULL,
                        patient_email TEXT NOT NULL,
                        patient_phone TEXT,
                        date TEXT NOT NULL,
                        time TEXT NOT NULL,
                        symptoms TEXT,
                        status TEXT NOT NULL DEFAULT 'CONFIRMED',
                        fee REAL NOT NULL,
                        join_url TEXT,
                        clinic_hospital TEXT,
                        doctor_image TEXT,
                        specialization TEXT,
                        created_at TEXT
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS booked_slots (
                        slot_key TEXT PRIMARY KEY,
                        doctor_id TEXT NOT NULL,
                        date TEXT NOT NULL,
                        time TEXT NOT NULL,
                        appointment_id TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'CONFIRMED',
                        updated_at TEXT
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS prescriptions (
                        id TEXT PRIMARY KEY,
                        patient_name TEXT NOT NULL,
                        patient_email TEXT NOT NULL,
                        doctor_name TEXT NOT NULL,
                        doctor_id TEXT NOT NULL,
                        appointment_id TEXT,
                        diagnosis TEXT,
                        notes TEXT,
                        medicines_json TEXT,
                        created_at TEXT
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS reminders (
                        id TEXT PRIMARY KEY,
                        medicine TEXT NOT NULL,
                        timing TEXT,
                        time TEXT NOT NULL,
                        instruction TEXT,
                        patient_email TEXT NOT NULL,
                        active INTEGER DEFAULT 1,
                        created_at TEXT
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS notifications (
                        id TEXT PRIMARY KEY,
                        patient_email TEXT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        type TEXT NOT NULL DEFAULT 'SYSTEM',
                        is_read INTEGER DEFAULT 0,
                        created_at TEXT
                    );
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        email TEXT NOT NULL UNIQUE,
                        phone TEXT,
                        role TEXT NOT NULL DEFAULT 'PATIENT',
                        password_hash TEXT NOT NULL,
                        doctor_id TEXT,
                        address TEXT,
                        active INTEGER DEFAULT 1,
                        created_at TEXT,
                        updated_at TEXT
                    );
                """);
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email);");

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS orders (
                        order_id TEXT PRIMARY KEY,
                        user_email TEXT NOT NULL,
                        customer_name TEXT NOT NULL,
                        customer_phone TEXT,
                        delivery_address TEXT,
                        city TEXT,
                        pincode TEXT,
                        delivery_instructions TEXT,
                        items_json TEXT,
                        item_count INTEGER DEFAULT 1,
                        mrp_total REAL DEFAULT 0.0,
                        discount_amount REAL DEFAULT 0.0,
                        delivery_fee REAL DEFAULT 0.0,
                        total_amount REAL NOT NULL,
                        payment_method TEXT NOT NULL,
                        payment_status TEXT NOT NULL DEFAULT 'PENDING_COD',
                        order_status TEXT NOT NULL DEFAULT 'CONFIRMED',
                        estimated_delivery TEXT,
                        created_at TEXT
                    );
                """);

                try { stmt.execute("ALTER TABLE medicines ADD COLUMN image TEXT;"); } catch (SQLException ignored) {}
                try { stmt.execute("ALTER TABLE medicines ADD COLUMN providers_json TEXT;"); } catch (SQLException ignored) {}
                try { stmt.execute("ALTER TABLE appointments ADD COLUMN clinic_hospital TEXT;"); } catch (SQLException ignored) {}
                try { stmt.execute("ALTER TABLE appointments ADD COLUMN doctor_image TEXT;"); } catch (SQLException ignored) {}
                try { stmt.execute("ALTER TABLE appointments ADD COLUMN specialization TEXT;"); } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            System.err.println("Database schema initialization warning: " + e.getMessage());
        }
    }

    /**
     * Synchronizes authentic, high-quality photography for all doctor profiles and medicine products.
     */
    public void syncRealImagesAndProviders() {
        Map<String, String> doctorImages = new LinkedHashMap<>();
        doctorImages.put("rahul", "/images/doctors/rahul.jpg");
        doctorImages.put("priya", "/images/doctors/priya.jpg");
        doctorImages.put("amit", "/images/doctors/amit.jpg");
        doctorImages.put("arjun", "/images/doctors/arjun.jpg");
        doctorImages.put("neha", "/images/doctors/neha.jpg");
        doctorImages.put("ananya", "/images/doctors/ananya.jpg");

        Map<String, String> medImages = new LinkedHashMap<>();
        medImages.put("paracetamol-500", "/images/medicines/paracetamol-500.jpg");
        medImages.put("vitamin-d3", "/images/medicines/vitamin-d3.jpg");
        medImages.put("vitamin-d3-1000", "/images/medicines/vitamin-d3.jpg");
        medImages.put("cetirizine-10", "/images/medicines/cetirizine-10.jpg");
        medImages.put("amoxicillin-500", "/images/medicines/amoxicillin-500.jpg");
        medImages.put("bp-monitor", "/images/medicines/bp-monitor.jpg");
        medImages.put("thermometer", "/images/medicines/thermometer.jpg");
        medImages.put("glucometer", "/images/medicines/glucometer.jpg");
        medImages.put("ors", "/images/medicines/ors.jpg");
        medImages.put("omega3", "/images/medicines/omega3.jpg");
        medImages.put("multivit", "/images/medicines/multivit.jpg");
        medImages.put("pantoprazole-40", "/images/medicines/pantoprazole-40.jpg");
        medImages.put("sanitizer", "/images/medicines/sanitizer.jpg");
        medImages.put("pulse-oximeter", "/images/medicines/pulse-oximeter.jpg");
        medImages.put("first-aid-kit", "/images/medicines/first-aid-kit.jpg");
        medImages.put("face-cleanser", "/images/medicines/face-cleanser.jpg");

        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE doctors SET image = ? WHERE id = ?")) {
                for (Map.Entry<String, String> e : doctorImages.entrySet()) {
                    ps.setString(1, e.getValue());
                    ps.setString(2, e.getKey());
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE medicines SET image = ? WHERE id = ?")) {
                for (Map.Entry<String, String> e : medImages.entrySet()) {
                    ps.setString(1, e.getValue());
                    ps.setString(2, e.getKey());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Notice: could not sync real images to database: " + e.getMessage());
        }
    }

    /**
     * Seeds initial default authentication accounts (Patients, Doctors, Admins)
     * into users table for immediate login and testing.
     */
    private void seedUsersIfEmpty() {
        try (Connection conn = getConnection()) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return; // Already populated
                }
            }

            String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String sql;
            if (isMysql) {
                sql = """
                    INSERT INTO users (id, name, email, phone, role, password_hash, doctor_id, address, active, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
                    ON DUPLICATE KEY UPDATE name = VALUES(name)
                """;
            } else {
                sql = """
                    INSERT INTO users (id, name, email, phone, role, password_hash, doctor_id, address, active, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
                """;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                // Pre-configured test accounts:
                String docHash = com.healthkart.dao.UserDaoImpl.hashPassword("doctor123");
                addSeedUser(ps, "usr-doc-rahul", "Dr. Rahul Sharma", "rahul@healthkart.com", "9876543210", "DOCTOR", docHash, "rahul", "Max Super Speciality Hospital, New Delhi", nowStr);
                addSeedUser(ps, "usr-doc-priya", "Dr. Priya Mehta", "priya@healthkart.com", "9876543211", "DOCTOR", docHash, "priya", "Fortis Escorts Heart Institute, New Delhi", nowStr);
                addSeedUser(ps, "usr-doc-amit", "Dr. Amit Verma", "amit@healthkart.com", "9876543212", "DOCTOR", docHash, "amit", "Medanta - The Medicity, Gurugram", nowStr);
                addSeedUser(ps, "usr-doc-arjun", "Dr. Arjun Singh", "arjun@healthkart.com", "9876543213", "DOCTOR", docHash, "arjun", "Indraprastha Apollo Hospital, New Delhi", nowStr);
                addSeedUser(ps, "usr-doc-neha", "Dr. Neha Kapoor", "neha@healthkart.com", "9876543214", "DOCTOR", docHash, "neha", "Rainbow Children's Hospital, Delhi", nowStr);
                addSeedUser(ps, "usr-doc-ananya", "Dr. Ananya Gupta", "ananya@healthkart.com", "9876543215", "DOCTOR", docHash, "ananya", "Apollo Spectra Hospitals, New Delhi", nowStr);

                // Patients (password: patient123)
                String patHash = com.healthkart.dao.UserDaoImpl.hashPassword("patient123");
                addSeedUser(ps, "usr-pat-priya", "Priya Sharma", "priya@example.com", "9812345678", "PATIENT", patHash, null, "124 Greater Kailash, New Delhi", nowStr);
                addSeedUser(ps, "usr-pat-rahul", "Rahul Verma", "rahul@example.com", "9823456789", "PATIENT", patHash, null, "Sector 45, Gurgaon", nowStr);
                addSeedUser(ps, "usr-pat-demo", "Demo Patient", "demo@healthkart.com", "9800000000", "PATIENT", com.healthkart.dao.UserDaoImpl.hashPassword("demo123"), null, "New Delhi", nowStr);
                addSeedUser(ps, "usr-pat-test", "Verified Patient", "patient@test.com", "9811111111", "PATIENT", com.healthkart.dao.UserDaoImpl.hashPassword("password123"), null, "Delhi NCR", nowStr);

                ps.executeBatch();
            }
        } catch (SQLException e) {
            System.err.println("Notice: could not seed default users: " + e.getMessage());
        }
    }

    private void addSeedUser(PreparedStatement ps, String id, String name, String email, String phone,
                            String role, String hash, String doctorId, String address, String nowStr) throws SQLException {
        ps.setString(1, id);
        ps.setString(2, name);
        ps.setString(3, email.toLowerCase().trim());
        ps.setString(4, phone);
        ps.setString(5, role);
        ps.setString(6, hash);
        ps.setString(7, doctorId);
        ps.setString(8, address);
        ps.setString(9, nowStr);
        ps.setString(10, nowStr);
        ps.addBatch();
    }

    /**
     * Seeds initial database from existing data/db.json if doctors table is empty.
     */
    @SuppressWarnings("unchecked")
    private void migrateFromJsonIfEmpty() {
        Path jsonPath = Path.of("data/db.json");
        if (!Files.exists(jsonPath) && Files.exists(Path.of("healthkart-java/data/db.json"))) {
            jsonPath = Path.of("healthkart-java/data/db.json");
        }
        if (!Files.exists(jsonPath)) return;

        try (Connection conn = getConnection()) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM doctors")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return; // Already populated
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            String jsonContent = Files.readString(jsonPath);
            if (jsonContent.startsWith("\uFEFF")) {
                jsonContent = jsonContent.substring(1);
            }
            Map<String, Object> db = mapper.readValue(jsonContent, new TypeReference<LinkedHashMap<String, Object>>() {});

            conn.setAutoCommit(false); // Transaction begins

            // 1. Migrate doctors
            List<Map<String, Object>> doctors = (List<Map<String, Object>>) db.get("doctors");
            if (doctors != null) {
                String sql = "INSERT INTO doctors (id, name, email, phone, specialization, clinic_hospital, consultation_fee, experience, rating, image, available_time_slots, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Map<String, Object> d : doctors) {
                        ps.setString(1, String.valueOf(d.get("id")));
                        ps.setString(2, String.valueOf(d.get("name")));
                        ps.setString(3, d.get("id") + "@healthkart.com");
                        ps.setString(4, "9876543210");
                        ps.setString(5, String.valueOf(d.get("specialization")));
                        ps.setString(6, String.valueOf(d.get("clinicHospital")));
                        ps.setDouble(7, toDouble(d.get("consultationFee"), 500.0));
                        ps.setInt(8, toInt(d.get("experience"), 5));
                        ps.setDouble(9, toDouble(d.get("rating"), 4.5));
                        ps.setString(10, String.valueOf(d.getOrDefault("image", "")));
                        List<?> slots = (List<?>) d.get("availableTimeSlots");
                        ps.setString(11, slots != null ? String.join(",", slots.stream().map(Object::toString).toList()) : "");
                        ps.setString(12, "2026-10-11 20:00:00");
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            // 2. Migrate medicines
            List<Map<String, Object>> medicines = (List<Map<String, Object>>) db.get("medicines");
            if (medicines != null) {
                String sql = "INSERT INTO medicines (id, name, category, description, price, rating, icon, image, stock_quantity, manufacturer, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Map<String, Object> m : medicines) {
                        String id = String.valueOf(m.get("id"));
                        String img = String.valueOf(m.getOrDefault("image", ""));
                        if (img.isBlank()) {
                            img = "/images/medicines/" + id + ".jpg";
                        }
                        ps.setString(1, id);
                        ps.setString(2, String.valueOf(m.get("name")));
                        ps.setString(3, String.valueOf(m.get("category")));
                        ps.setString(4, String.valueOf(m.get("description")));
                        ps.setDouble(5, toDouble(m.get("price"), 0.0));
                        ps.setDouble(6, toDouble(m.get("rating"), 4.5));
                        ps.setString(7, String.valueOf(m.getOrDefault("icon", "fa-pills")));
                        ps.setString(8, img);
                        ps.setInt(9, toInt(m.get("stockQuantity"), 100));
                        ps.setString(10, "HealthKart Labs");
                        ps.setString(11, "2026-10-11 20:00:00");
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            // 3. Migrate appointments
            List<Map<String, Object>> appointments = (List<Map<String, Object>>) db.get("appointments");
            if (appointments != null) {
                String sql = "INSERT INTO appointments (appointment_id, doctor_id, doctor_name, patient_name, patient_email, patient_phone, date, time, symptoms, status, fee, join_url, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Map<String, Object> a : appointments) {
                        String aptId = String.valueOf(a.get("appointmentId"));
                        ps.setString(1, aptId);
                        ps.setString(2, String.valueOf(a.get("doctorId")));
                        ps.setString(3, String.valueOf(a.get("doctorName")));
                        ps.setString(4, String.valueOf(a.get("patientName")));
                        ps.setString(5, String.valueOf(a.get("patientEmail")));
                        ps.setString(6, String.valueOf(a.getOrDefault("patientPhone", "")));
                        ps.setString(7, String.valueOf(a.get("date")));
                        ps.setString(8, String.valueOf(a.get("time")));
                        ps.setString(9, String.valueOf(a.getOrDefault("symptoms", "")));
                        ps.setString(10, String.valueOf(a.getOrDefault("status", "CONFIRMED")));
                        ps.setDouble(11, toDouble(a.get("fee"), 500.0));
                        ps.setString(12, String.valueOf(a.getOrDefault("joinUrl", "")));
                        ps.setString(13, String.valueOf(a.getOrDefault("createdAt", "2026-10-11 20:00:00")));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            // 4. Migrate booked slots
            Map<String, Object> bookedSlots = (Map<String, Object>) db.get("bookedSlots");
            if (bookedSlots != null) {
                String sql = "INSERT INTO booked_slots (slot_key, doctor_id, date, time, appointment_id, status, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Map.Entry<String, Object> e : bookedSlots.entrySet()) {
                        String slotKey = e.getKey();
                        Object valObj = e.getValue();
                        if (valObj instanceof Map<?, ?> val) {
                            ps.setString(1, slotKey);
                            Object docId = val.get("doctorId");
                            ps.setString(2, docId != null ? docId.toString() : "");
                            Object d = val.get("date");
                            ps.setString(3, d != null ? d.toString() : "");
                            Object t = val.get("time");
                            ps.setString(4, t != null ? t.toString() : "");
                            Object aptId = val.get("appointmentId");
                            ps.setString(5, aptId != null ? aptId.toString() : "");
                            Object st = val.get("status");
                            ps.setString(6, st != null ? st.toString() : "CONFIRMED");
                            ps.setString(7, "2026-10-11 20:00:00");
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
            }

            // 5. Migrate prescriptions
            List<Map<String, Object>> prescriptions = (List<Map<String, Object>>) db.get("prescriptions");
            if (prescriptions != null) {
                String sql = "INSERT INTO prescriptions (id, patient_name, patient_email, doctor_name, doctor_id, appointment_id, diagnosis, notes, medicines_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Map<String, Object> p : prescriptions) {
                        ps.setString(1, String.valueOf(p.get("id")));
                        ps.setString(2, String.valueOf(p.get("patientName")));
                        ps.setString(3, String.valueOf(p.get("patientEmail")));
                        ps.setString(4, String.valueOf(p.get("doctorName")));
                        ps.setString(5, String.valueOf(p.get("doctorId")));
                        ps.setString(6, String.valueOf(p.getOrDefault("appointmentId", "")));
                        ps.setString(7, String.valueOf(p.getOrDefault("diagnosis", "")));
                        ps.setString(8, String.valueOf(p.getOrDefault("notes", "")));
                        ps.setString(9, mapper.writeValueAsString(p.getOrDefault("medicines", List.of())));
                        ps.setString(10, String.valueOf(p.getOrDefault("createdAt", "2026-10-11 20:00:00")));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            // 6. Migrate reminders
            List<Map<String, Object>> reminders = (List<Map<String, Object>>) db.get("reminders");
            if (reminders != null) {
                String sql = "INSERT INTO reminders (id, medicine, timing, time, instruction, patient_email, active, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Map<String, Object> r : reminders) {
                        ps.setString(1, String.valueOf(r.get("id")));
                        ps.setString(2, String.valueOf(r.get("medicine")));
                        ps.setString(3, String.valueOf(r.getOrDefault("timing", "")));
                        ps.setString(4, String.valueOf(r.get("time")));
                        ps.setString(5, String.valueOf(r.getOrDefault("instruction", "")));
                        ps.setString(6, String.valueOf(r.get("patientEmail")));
                        ps.setInt(7, toBoolInt(r.get("active")));
                        ps.setString(8, String.valueOf(r.getOrDefault("createdAt", "2026-10-11 20:00:00")));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            // 7. Migrate notifications
            List<Map<String, Object>> notifications = (List<Map<String, Object>>) db.get("notifications");
            if (notifications != null) {
                String sql = "INSERT INTO notifications (id, patient_email, title, message, type, is_read, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Map<String, Object> n : notifications) {
                        ps.setString(1, String.valueOf(n.get("id")));
                        ps.setString(2, String.valueOf(n.get("patientEmail")));
                        ps.setString(3, String.valueOf(n.get("title")));
                        ps.setString(4, String.valueOf(n.get("message")));
                        ps.setString(5, String.valueOf(n.getOrDefault("type", "SYSTEM")));
                        ps.setInt(6, toBoolInt(n.get("read") != null ? n.get("read") : n.get("is_read")));
                        ps.setString(7, String.valueOf(n.getOrDefault("createdAt", "2026-10-11 20:00:00")));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            conn.commit(); // Transaction committed
            System.out.println("Successfully migrated existing data from JSON to relational database!");
        } catch (Exception e) {
            System.err.println("Migration notice: " + e.getMessage());
        }
    }

    private static double toDouble(Object val, double def) {
        if (val == null) return def;
        if (val instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(val.toString().trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static int toInt(Object val, int def) {
        if (val == null) return def;
        if (val instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(val.toString().trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static int toBoolInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Boolean b) return b ? 1 : 0;
        if (val instanceof Number n) return n.intValue() > 0 ? 1 : 0;
        String s = val.toString().trim().toLowerCase();
        return ("true".equals(s) || "1".equals(s)) ? 1 : 0;
    }
}

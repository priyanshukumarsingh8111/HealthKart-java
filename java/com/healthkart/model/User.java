package com.healthkart.model;

/**
 * User domain entity representing an authenticated user (Patient, Doctor, or Staff).
 * Demonstrates:
 * - OOP Inheritance: User -> Person -> BaseEntity
 * - Dynamic Polymorphism: Overridden getRoleDescription() and getEntitySummary()
 * - Encapsulation: Private credentials and profile data with controlled getters/setters
 * - Comparable: Natural ordering by user name for Collections framework
 */
public class User extends Person implements Comparable<User> {
    private String passwordHash;
    private String doctorId;
    private String address = "";
    private boolean active = true;

    public User() {
        super();
        setRole("PATIENT");
    }

    public User(String id, String name, String email, String phone, String role, String passwordHash) {
        super(id, name, email, phone, role);
        this.passwordHash = passwordHash;
        this.active = true;
    }

    public User(String id, String name, String email, String phone, String role, String passwordHash, String doctorId) {
        super(id, name, email, phone, role);
        this.passwordHash = passwordHash;
        this.doctorId = doctorId;
        this.active = true;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String getRoleDescription() {
        String r = getRole() != null ? getRole().toUpperCase() : "PATIENT";
        if ("DOCTOR".equalsIgnoreCase(r)) {
            return "Medical Specialist User (Doctor ID: " + (doctorId != null ? doctorId : "N/A") + ")";
        } else if ("ADMIN".equalsIgnoreCase(r)) {
            return "System Administrator";
        }
        return "Registered Patient Account";
    }

    @Override
    public String getEntitySummary() {
        return "User [" + getId() + "] " + getName() + " (" + getEmail() + ") - Role: " + getRole();
    }

    @Override
    public int compareTo(User o) {
        if (o == null || o.getName() == null) return 1;
        if (this.getName() == null) return -1;
        return this.getName().compareToIgnoreCase(o.getName());
    }
}

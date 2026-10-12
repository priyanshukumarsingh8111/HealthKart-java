package com.healthkart.model;

/**
 * Concrete Patient entity extending Person.
 * Demonstrates OOP Inheritance and Polymorphism.
 */
public class Patient extends Person {
    private String bloodGroup;
    private String emergencyContact;
    private int age;
    private String medicalHistory;

    public Patient() {
        super();
        setRole("PATIENT");
    }

    public Patient(String id, String name, String email, String phone,
                   String bloodGroup, String emergencyContact, int age) {
        super(id, name, email, phone, "PATIENT");
        this.bloodGroup = bloodGroup;
        this.emergencyContact = emergencyContact;
        this.age = age;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    @Override
    public String getRoleDescription() {
        return "Patient (Age: " + age + ", Blood Group: " + bloodGroup + ")";
    }
}

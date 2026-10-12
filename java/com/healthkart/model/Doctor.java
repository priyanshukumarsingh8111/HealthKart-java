package com.healthkart.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete Doctor entity extending Person.
 * Demonstrates OOP Inheritance, Method Overriding (Polymorphism),
 * and Comparable<T> interface implementation for Collections sorting.
 */
public class Doctor extends Person implements Comparable<Doctor> {
    private String specialization;
    private String clinicHospital;
    private double consultationFee;
    private int experience;
    private double rating;
    private String image;
    private List<String> availableTimeSlots = new ArrayList<>();

    public Doctor() {
        super();
        setRole("DOCTOR");
    }

    public Doctor(String id, String name, String email, String phone, String specialization,
                  String clinicHospital, double consultationFee, int experience, double rating) {
        super(id, name, email, phone, "DOCTOR");
        this.specialization = specialization;
        this.clinicHospital = clinicHospital;
        this.consultationFee = consultationFee;
        this.experience = experience;
        this.rating = rating;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getClinicHospital() {
        return clinicHospital;
    }

    public void setClinicHospital(String clinicHospital) {
        this.clinicHospital = clinicHospital;
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getAvailableTimeSlots() {
        return availableTimeSlots;
    }

    public void setAvailableTimeSlots(List<String> availableTimeSlots) {
        this.availableTimeSlots = availableTimeSlots != null ? availableTimeSlots : new ArrayList<>();
    }

    @Override
    public String getRoleDescription() {
        return "Doctor specializing in " + specialization + " with " + experience + " years experience.";
    }

    @Override
    public String getEntitySummary() {
        return getName() + " (" + specialization + ") - " + clinicHospital + " [Fee: ₹" + consultationFee + ", Rating: " + rating + "★]";
    }

    /**
     * Natural ordering by doctor rating descending, then name ascending.
     * Demonstrates Generics and Collections interface implementation.
     */
    @Override
    public int compareTo(Doctor other) {
        if (other == null) return 1;
        int ratingCompare = Double.compare(other.rating, this.rating);
        if (ratingCompare != 0) return ratingCompare;
        return (getName() != null && other.getName() != null) ? getName().compareToIgnoreCase(other.getName()) : 0;
    }
}

package com.healthkart.model;

/**
 * Prescription entity extending BaseEntity.
 */
public class Prescription extends BaseEntity {
    private String patientName;
    private String patientEmail;
    private String doctorName;
    private String doctorId;
    private String appointmentId;
    private String diagnosis;
    private String notes;
    private String medicinesJson;

    public Prescription() {
        super();
    }

    public Prescription(String id, String patientName, String patientEmail,
                        String doctorName, String doctorId, String appointmentId,
                        String diagnosis, String notes, String medicinesJson) {
        super(id);
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.doctorName = doctorName;
        this.doctorId = doctorId;
        this.appointmentId = appointmentId;
        this.diagnosis = diagnosis;
        this.notes = notes;
        this.medicinesJson = medicinesJson;
    }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getMedicinesJson() { return medicinesJson; }
    public void setMedicinesJson(String medicinesJson) { this.medicinesJson = medicinesJson; }

    @Override
    public String getEntitySummary() {
        return "Prescription [" + getId() + "] for " + patientName + " by Dr. " + doctorName + " - " + diagnosis;
    }
}

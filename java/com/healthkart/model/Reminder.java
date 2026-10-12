package com.healthkart.model;

/**
 * Medicine reminder entity extending BaseEntity.
 */
public class Reminder extends BaseEntity {
    private String medicine;
    private String timing;
    private String time;
    private String instruction;
    private String patientEmail;
    private boolean active;

    public Reminder() {
        super();
        this.active = true;
    }

    public Reminder(String id, String medicine, String timing, String time,
                    String instruction, String patientEmail, boolean active) {
        super(id);
        this.medicine = medicine;
        this.timing = timing;
        this.time = time;
        this.instruction = instruction;
        this.patientEmail = patientEmail;
        this.active = active;
    }

    public String getMedicine() { return medicine; }
    public void setMedicine(String medicine) { this.medicine = medicine; }

    public String getTiming() { return timing; }
    public void setTiming(String timing) { this.timing = timing; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String getEntitySummary() {
        return "Reminder: " + medicine + " at " + time + " (" + timing + ") - " + (active ? "ACTIVE" : "INACTIVE");
    }
}

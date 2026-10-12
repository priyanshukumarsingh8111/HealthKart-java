package com.healthkart.model;

/**
 * System notification entity extending BaseEntity.
 */
public class Notification extends BaseEntity {
    private String patientEmail;
    private String title;
    private String message;
    private String type; // APPOINTMENT, REMINDER, SYSTEM, CALL
    private boolean read;

    public Notification() {
        super();
        this.read = false;
        this.type = "SYSTEM";
    }

    public Notification(String id, String patientEmail, String title,
                        String message, String type, boolean read) {
        super(id);
        this.patientEmail = patientEmail;
        this.title = title;
        this.message = message;
        this.type = type;
        this.read = read;
    }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    @Override
    public String getEntitySummary() {
        return "[" + type + "] " + title + ": " + message + (read ? " (Read)" : " (Unread)");
    }
}

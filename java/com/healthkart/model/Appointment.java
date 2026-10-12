package com.healthkart.model;

/**
 * Appointment domain entity extending BaseEntity.
 * Implements Comparable<Appointment> for priority scheduling in PriorityQueue (Collections & Generics).
 */
public class Appointment extends BaseEntity implements Comparable<Appointment> {
    private String appointmentId;
    private String doctorId;
    private String doctorName;
    private String doctorImage = "";
    private String specialization = "General Physician";
    private String clinicHospital = "Max Super Speciality Hospital, New Delhi";
    private String patientName;
    private String patientEmail;
    private String patientPhone;
    private String date;
    private String time;
    private String symptoms;
    private String consultationType = "Video Consultation";
    private String status = "CONFIRMED"; // CONFIRMED, COMPLETED, CANCELLED
    private double fee;
    private String joinUrl;

    public Appointment() {
        super();
        this.status = "CONFIRMED";
    }

    public Appointment(String appointmentId, String doctorId, String doctorName,
                       String patientName, String patientEmail, String patientPhone,
                       String date, String time, String symptoms, double fee) {
        super(appointmentId);
        this.appointmentId = appointmentId;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.patientPhone = patientPhone;
        this.date = date;
        this.time = time;
        this.symptoms = symptoms;
        this.status = "CONFIRMED";
        this.fee = fee;
        this.joinUrl = "/pages/call.html?appointment=" + appointmentId + "&role=patient";
    }

    public String getAppointmentId() {
        return appointmentId != null ? appointmentId : getId();
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
        setId(appointmentId);
    }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorImage() { return doctorImage; }
    public void setDoctorImage(String doctorImage) { this.doctorImage = doctorImage; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getClinicHospital() { return clinicHospital; }
    public void setClinicHospital(String clinicHospital) { this.clinicHospital = clinicHospital; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getConsultationType() { return consultationType; }
    public void setConsultationType(String consultationType) { this.consultationType = consultationType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getFee() { return fee; }
    public void setFee(double fee) { this.fee = fee; }

    public String getJoinUrl() { return joinUrl; }
    public void setJoinUrl(String joinUrl) { this.joinUrl = joinUrl; }

    // Aliases for HTML/CSS Frontend JSON compatibility
    public String getAppointmentDate() { return date; }
    public void setAppointmentDate(String appointmentDate) { this.date = appointmentDate; }

    public String getAppointmentTime() { return time; }
    public void setAppointmentTime(String appointmentTime) { this.time = appointmentTime; }

    public String getPatientMobile() { return patientPhone; }
    public void setPatientMobile(String patientMobile) { this.patientPhone = patientMobile; }

    public String getHealthConcern() { return symptoms; }
    public void setHealthConcern(String healthConcern) { this.symptoms = healthConcern; }

    public String getCallRoomId() { return "call_" + getAppointmentId(); }

    /**
     * Unique slot key used for thread-safe synchronization and O(1) slot conflict checks.
     */
    public String getSlotKey() {
        return doctorId + "_" + date + "_" + time;
    }

    @Override
    public String getEntitySummary() {
        return "Appointment [" + getAppointmentId() + "] " + date + " " + time + " | Dr. " + doctorName + " with " + patientName + " (" + status + ")";
    }

    /**
     * Ordering for PriorityQueue: chronological by date and time.
     */
    @Override
    public int compareTo(Appointment other) {
        if (other == null) return 1;
        int dateCmp = (this.date != null && other.date != null) ? this.date.compareTo(other.date) : 0;
        if (dateCmp != 0) return dateCmp;
        return (this.time != null && other.time != null) ? this.time.compareTo(other.time) : 0;
    }
}

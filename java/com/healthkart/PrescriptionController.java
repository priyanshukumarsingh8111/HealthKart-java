package com.healthkart;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.healthkart.Util.*;

/** Prescriptions + Reminders APIs. */
@RestController
@RequestMapping("/api")
public class PrescriptionController {
    private final JsonStore store;
    private final Realtime rt;
    private final com.healthkart.dao.PrescriptionDao prescriptionDao;
    private final com.healthkart.dao.ReminderDao reminderDao;

    public PrescriptionController(JsonStore store, Realtime rt,
                                  com.healthkart.dao.PrescriptionDao prescriptionDao,
                                  com.healthkart.dao.ReminderDao reminderDao) {
        this.store = store;
        this.rt = rt;
        this.prescriptionDao = prescriptionDao;
        this.reminderDao = reminderDao;
    }

    // ---------------- Prescriptions ----------------
    @GetMapping("/prescriptions")
    public Map<String, Object> prescriptions(@RequestParam(name = "email", required = false) String email,
                                             @RequestParam(name = "appointmentId", required = false) String appointmentId,
                                             @RequestParam(name = "doctorId", required = false) String doctorId) {
        List<Map<String, Object>> list = store.read(db -> new ArrayList<>(list(db, "prescriptions")));
        if (s(email) != null)
            list = list.stream().filter(p -> s(p.get("patientEmail")) != null
                    && p.get("patientEmail").toString().equalsIgnoreCase(email)).collect(Collectors.toList());
        if (s(appointmentId) != null)
            list = list.stream().filter(p -> appointmentId.equals(p.get("appointmentId"))).collect(Collectors.toList());
        if (s(doctorId) != null)
            list = list.stream().filter(p -> doctorId.equals(p.get("doctorId"))).collect(Collectors.toList());
        return m("success", true, "count", list.size(), "prescriptions", list);
    }

    @GetMapping("/prescriptions/{id}")
    public Map<String, Object> prescription(@PathVariable("id") String id) {
        return store.read(db -> list(db, "prescriptions").stream()
                .filter(p -> id.equals(p.get("id"))).findFirst()
                .map(p -> m("success", true, "prescription", p))
                .orElseThrow(() -> ApiException.of(404, "Prescription not found")));
    }

    private static String defaultMealTime(String timing) {
        if (timing == null) return "13:00";
        String t = timing.toLowerCase();
        if (t.contains("breakfast")) return t.contains("before") ? "07:45" : "08:30";
        if (t.contains("lunch")) return t.contains("before") ? "12:30" : "13:30";
        if (t.contains("dinner")) return t.contains("before") ? "20:00" : "21:00";
        if (t.contains("bedtime") || t.contains("night")) return "22:00";
        if (t.contains("empty")) return "07:00";
        return "14:00";
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asList(Object o) {
        return o instanceof List<?> ? (List<Map<String, Object>>) o : new ArrayList<>();
    }

    @PostMapping("/prescriptions")
    public ResponseEntity<Map<String, Object>> createPrescription(@RequestBody(required = false) Map<String, Object> raw) {
        Map<String, Object> b = body(raw);
        String patientName = s(b.get("patientName"));
        String diagnosis = s(b.get("diagnosis"));
        if (patientName == null || diagnosis == null)
            throw ApiException.of(400, "Patient name and diagnosis are required");

        Map<String, Object>[] out = store.write(db -> {
            String rxId = "RX-" + rand6();
            String nowIso = now();
            String startDate = nowIso.substring(0, 10);

            Map<String, Object> rx = m(
                    "id", rxId,
                    "appointmentId", or(b.get("appointmentId"), ""),
                    "doctorId", or(b.get("doctorId"), "rahul"),
                    "doctorName", or(b.get("doctorName"), "Dr. Rahul Sharma"),
                    "doctorSpecialization", or(b.get("doctorSpecialization"), "General Physician"),
                    "doctorHospital", or(b.get("doctorHospital"), "Max Super Speciality Hospital, New Delhi"),
                    "patientName", patientName,
                    "patientEmail", or(b.get("patientEmail"), "priya@healthkart.com"),
                    "patientMobile", or(b.get("patientMobile"), ""),
                    "diagnosis", diagnosis,
                    "medicines", asList(b.get("medicines")),
                    "yogaRoutines", asList(b.get("yogaRoutines")),
                    "advice", or(b.get("advice"), "Follow prescribed meal timings and yoga routines consistently."),
                    "createdAt", nowIso);
            list(db, "prescriptions").add(0, rx);

            String doctorName = (String) rx.get("doctorName");
            String patientEmail = (String) rx.get("patientEmail");
            List<Map<String, Object>> newReminders = new ArrayList<>();
            long ts = System.currentTimeMillis();

            // 1. Medicine reminders
            List<Map<String, Object>> meds = asList(rx.get("medicines"));
            for (int i = 0; i < meds.size(); i++) {
                Map<String, Object> med = meds.get(i);
                String mealTiming = s(med.get("mealTiming"));
                Map<String, Object> r = m(
                        "id", "rem_" + ts + "_med_" + i,
                        "type", "medicine",
                        "medicineName", med.get("name"),
                        "dosage", or(med.get("dosage"), "1 Dose"),
                        "mealTiming", or(mealTiming, "After Lunch"),
                        "reminderTime", or(med.get("time"), defaultMealTime(mealTiming)),
                        "frequency", or(med.get("frequency"), "Daily"),
                        "startDate", startDate,
                        "duration", or(med.get("duration"), "5 Days"),
                        "prescribedBy", doctorName,
                        "prescriptionId", rxId,
                        "userEmail", patientEmail,
                        "status", "ACTIVE",
                        "instructions", or(med.get("instructions"),
                                "Take " + (mealTiming != null ? mealTiming : "after food") + " with water."));
                list(db, "reminders").add(0, r);
                newReminders.add(r);
            }

            // 2. Yoga reminders
            List<Map<String, Object>> yogas = asList(rx.get("yogaRoutines"));
            for (int i = 0; i < yogas.size(); i++) {
                Map<String, Object> y = yogas.get(i);
                String pose = or(y.get("poseName"), or(y.get("name"), or(y.get("yogaName"), "Daily Yoga")));
                Map<String, Object> r = m(
                        "id", "rem_" + ts + "_yoga_" + i,
                        "type", "yoga",
                        "yogaName", pose,
                        "targetBenefit", or(y.get("targetBenefit"), or(y.get("benefit"), "Overall health & mobility")),
                        "reminderTime", or(y.get("timing"), or(y.get("reminderTime"), or(y.get("time"), "07:00"))),
                        "duration", or(y.get("duration"), "15 Mins"),
                        "frequency", or(y.get("frequency"), "Daily"),
                        "startDate", startDate,
                        "prescribedBy", doctorName,
                        "prescriptionId", rxId,
                        "userEmail", patientEmail,
                        "status", "ACTIVE",
                        "instructions", or(y.get("instructions"), "Perform on empty stomach with synchronized deep breathing."));
                list(db, "reminders").add(0, r);
                newReminders.add(r);
            }

            // 3. Notification
            Map<String, Object> notif = m(
                    "id", notifId(),
                    "recipientEmail", patientEmail,
                    "patientName", patientName,
                    "title", "New Doctor Prescription Issued!",
                    "message", doctorName + " has issued prescription " + rxId
                            + ". Your medicine eating reminders and yoga routines are now active.",
                    "appointmentId", or(b.get("appointmentId"), ""),
                    "prescriptionId", rxId,
                    "type", "prescription_issued",
                    "read", false,
                    "createdAt", nowIso);
            list(db, "notifications").add(0, notif);

            @SuppressWarnings("unchecked")
            Map<String, Object>[] res = new Map[]{rx, notif, m("list", newReminders)};
            return res;
        });

        Map<String, Object> rx = out[0];
        Object reminders = out[2].get("list");
        String email = (String) rx.get("patientEmail");

        // Sync to SQLite JDBC database
        try {
            com.healthkart.model.Prescription pEntity = new com.healthkart.model.Prescription();
            pEntity.setId(String.valueOf(rx.get("id")));
            pEntity.setPatientName(String.valueOf(rx.get("patientName")));
            pEntity.setPatientEmail(String.valueOf(rx.get("patientEmail")));
            pEntity.setDoctorName(String.valueOf(rx.get("doctorName")));
            pEntity.setDoctorId(String.valueOf(rx.get("doctorId")));
            pEntity.setAppointmentId(String.valueOf(rx.getOrDefault("appointmentId", "")));
            pEntity.setDiagnosis(String.valueOf(rx.getOrDefault("diagnosis", "")));
            pEntity.setNotes(String.valueOf(rx.getOrDefault("advice", "")));
            pEntity.setMedicinesJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(rx.getOrDefault("medicines", List.of())));
            pEntity.setCreatedAt(String.valueOf(rx.getOrDefault("createdAt", now())));
            prescriptionDao.save(pEntity);
        } catch (Exception ex) {
            System.err.println("Notice: SQLite prescription sync: " + ex.getMessage());
        }

        rt.emit("new_prescription", m("recipientEmail", email, "prescription", rx, "reminders", reminders));
        rt.emit("realtime_notification", m("recipientEmail", email, "notification", out[1]));
        rt.emit("reminders_updated", m("recipientEmail", email));

        return ResponseEntity.status(201).body(m("success", true,
                "message", "Prescription created and reminders scheduled successfully!",
                "prescription", rx,
                "remindersCount", ((List<?>) reminders).size(),
                "generatedReminders", reminders));
    }

    // ---------------- Reminders ----------------
    @GetMapping("/reminders")
    public Map<String, Object> reminders(@RequestParam(name = "email", required = false) String email,
                                         @RequestParam(name = "type", required = false) String type) {
        List<Map<String, Object>> list = store.read(db -> new ArrayList<>(list(db, "reminders")));
        if (s(email) != null)
            list = list.stream().filter(r -> {
                String u = s(r.get("userEmail"));
                return u == null || u.equalsIgnoreCase(email) || u.equals("guest");
            }).collect(Collectors.toList());
        if (s(type) != null)
            list = list.stream().filter(r -> type.equals(r.get("type"))).collect(Collectors.toList());
        return m("success", true, "count", list.size(), "reminders", list);
    }

    @PostMapping("/reminders")
    public ResponseEntity<Map<String, Object>> createReminder(@RequestBody(required = false) Map<String, Object> raw) {
        Map<String, Object> b = body(raw);
        String type = or(b.get("type"), "medicine");
        String medicineName = s(b.get("medicineName"));
        String yogaName = s(b.get("yogaName"));

        if (type.equals("medicine") && medicineName == null) throw ApiException.of(400, "Medicine name is required");
        if (type.equals("yoga") && yogaName == null) throw ApiException.of(400, "Yoga routine name is required");

        boolean med = type.equals("medicine"), yoga = type.equals("yoga");
        String mealTiming = s(b.get("mealTiming"));

        // Fields that don't apply to this type are omitted (same as JS `undefined`)
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", "rem_" + System.currentTimeMillis() + "_" + randBase36(6));
        r.put("type", type);
        if (med) {
            r.put("medicineName", medicineName);
            r.put("dosage", or(b.get("dosage"), "1 Dose"));
            r.put("mealTiming", or(mealTiming, "After Meal"));
        }
        if (yoga) {
            r.put("yogaName", yogaName);
            r.put("targetBenefit", or(b.get("targetBenefit"), "General Wellness"));
            r.put("duration", or(b.get("duration"), "15 Mins"));
        }
        r.put("reminderTime", or(b.get("reminderTime"), yoga ? "07:00" : "13:00"));
        r.put("frequency", or(b.get("frequency"), "Daily"));
        r.put("startDate", or(b.get("startDate"), today()));
        r.put("instructions", or(b.get("instructions"),
                med ? "Take " + (mealTiming != null ? mealTiming : "after food") + " with water." : "Steady breathing."));
        r.put("userEmail", or(b.get("userEmail"), "priya@healthkart.com"));
        r.put("status", "ACTIVE");
        r.put("createdAt", now());

        store.write(db -> {
            list(db, "reminders").add(0, r);
            return null;
        });
        rt.emit("reminders_updated", m("recipientEmail", r.get("userEmail")));

        return ResponseEntity.status(201).body(m("success", true,
                "message", (yoga ? "Yoga" : "Medicine") + " reminder saved successfully!", "reminder", r));
    }

    @PutMapping("/reminders/{id}")
    public Map<String, Object> updateReminder(@PathVariable("id") String id,
                                              @RequestBody(required = false) Map<String, Object> raw) {
        Map<String, Object> updates = body(raw);
        Map<String, Object> r = store.write(db -> {
            Map<String, Object> found = list(db, "reminders").stream()
                    .filter(x -> id.equals(x.get("id"))).findFirst()
                    .orElseThrow(() -> ApiException.of(404, "Reminder not found"));
            found.putAll(updates);                 // Object.assign(reminder, updates, ...)
            found.put("updatedAt", now());
            return found;
        });
        rt.emit("reminders_updated", m("recipientEmail", r.get("userEmail")));
        return m("success", true, "reminder", r);
    }

    @DeleteMapping("/reminders/{id}")
    public Map<String, Object> deleteReminder(@PathVariable("id") String id) {
        Map<String, Object> removed = store.write(db -> {
            List<Map<String, Object>> list = list(db, "reminders");
            for (int i = 0; i < list.size(); i++)
                if (id.equals(list.get(i).get("id"))) return list.remove(i);
            throw ApiException.of(404, "Reminder not found");
        });
        rt.emit("reminders_updated", m("recipientEmail", removed.get("userEmail")));
        return m("success", true, "message", "Reminder deleted successfully");
    }
}

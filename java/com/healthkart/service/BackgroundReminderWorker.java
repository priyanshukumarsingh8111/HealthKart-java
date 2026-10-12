package com.healthkart.service;

import com.healthkart.dao.AppointmentDao;
import com.healthkart.dao.ReminderDao;
import com.healthkart.model.Appointment;
import com.healthkart.model.Reminder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background worker demonstrating Multithreading & Synchronization.
 * Runs a background daemon thread periodically inspecting reminders and appointments.
 */
@Component
public class BackgroundReminderWorker {

    private final ReminderDao reminderDao;
    private final AppointmentDao appointmentDao;
    private ScheduledExecutorService scheduler;

    public BackgroundReminderWorker(ReminderDao reminderDao, AppointmentDao appointmentDao) {
        this.reminderDao = reminderDao;
        this.appointmentDao = appointmentDao;
    }

    @PostConstruct
    public synchronized void startWorker() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "HealthKart-BackgroundReminderWorker");
                t.setDaemon(true);
                return t;
            });

            // Run check every 45 seconds
            scheduler.scheduleWithFixedDelay(this::checkRemindersAndTriage, 5, 45, TimeUnit.SECONDS);
            System.out.println("[Thread: HealthKart-BackgroundReminderWorker] Started background reminder and appointment monitoring.");
        }
    }

    private void checkRemindersAndTriage() {
        try {
            // Check active reminders
            List<Reminder> activeReminders = reminderDao.findActiveReminders();
            String nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

            for (Reminder r : activeReminders) {
                if (r.getTime() != null && r.getTime().startsWith(nowTime)) {
                    System.out.println("[ALARM THREAD] Medicine Reminder Triggered: " + r.getMedicine() + " (" + r.getInstruction() + ") for " + r.getPatientEmail());
                }
            }

            // Inspect upcoming triage PriorityQueue (Demonstrates Collections & Generics in multithreaded context)
            PriorityQueue<Appointment> queue = appointmentDao.getUpcomingPriorityQueue();
            if (!queue.isEmpty()) {
                Appointment next = queue.peek();
                // System.out.println("[TRIAGE THREAD] Next upcoming appointment: " + next.getAppointmentId() + " on " + next.getDate() + " at " + next.getTime());
            }

        } catch (Exception e) {
            System.err.println("[BackgroundReminderWorker Error] " + e.getMessage());
        }
    }

    @PreDestroy
    public synchronized void stopWorker() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("[BackgroundReminderWorker] Worker stopped gracefully.");
        }
    }
}

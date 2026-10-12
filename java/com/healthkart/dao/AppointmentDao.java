package com.healthkart.dao;

import com.healthkart.model.Appointment;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Appointment DAO interface extending generic CrudDao.
 * Demonstrates:
 * - OOP: Interfaces, Exception specifications
 * - Collections: Lists, Maps, and PriorityQueue for chronologically ordered triage
 */
public interface AppointmentDao extends CrudDao<Appointment, String> {

    List<Appointment> findByPatientEmail(String email);

    List<Appointment> findByDoctorId(String doctorId);

    List<Appointment> findByDate(String date);

    boolean isSlotBooked(String doctorId, String date, String time);

    List<Map<String, Object>> getDoctorSlots(String doctorId, String date, List<String> allTimeSlots);

    Appointment bookAppointmentWithLock(Appointment apt);

    boolean cancelAppointment(String appointmentId);

    PriorityQueue<Appointment> getUpcomingPriorityQueue();
}

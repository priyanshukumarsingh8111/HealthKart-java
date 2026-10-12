package com.healthkart.dao;

import com.healthkart.model.Prescription;

import java.util.List;

/**
 * Prescription DAO interface extending generic CrudDao.
 */
public interface PrescriptionDao extends CrudDao<Prescription, String> {

    List<Prescription> findByPatientEmail(String email);

    List<Prescription> findByDoctorId(String doctorId);

    List<Prescription> findByAppointmentId(String appointmentId);
}

package com.healthkart.dao;

import com.healthkart.model.Doctor;

import java.util.List;
import java.util.Set;

/**
 * Doctor DAO interface extending generic CrudDao.
 * Demonstrates OOP Interfaces, Collections, and Generics.
 */
public interface DoctorDao extends CrudDao<Doctor, String> {

    List<Doctor> findBySpecialization(String specialization);

    List<Doctor> searchDoctors(String query);

    Set<String> getAllSpecializations();
}

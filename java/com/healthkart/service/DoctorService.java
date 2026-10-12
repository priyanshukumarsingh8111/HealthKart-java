package com.healthkart.service;

import com.healthkart.dao.DoctorDao;
import com.healthkart.exception.EntityNotFoundException;
import com.healthkart.model.Doctor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Service managing doctor listings and availability.
 */
@Service
public class DoctorService {

    private final DoctorDao doctorDao;

    public DoctorService(DoctorDao doctorDao) {
        this.doctorDao = doctorDao;
    }

    public List<Doctor> getAllDoctors() {
        List<Doctor> doctors = doctorDao.findAll();
        Collections.sort(doctors); // Natural ordering by rating descending via Comparable<Doctor>
        return doctors;
    }

    public Doctor getDoctorById(String id) {
        return doctorDao.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Doctor", id));
    }

    public List<Doctor> searchDoctors(String query) {
        return doctorDao.searchDoctors(query);
    }

    public Set<String> getSpecializations() {
        return doctorDao.getAllSpecializations();
    }
}

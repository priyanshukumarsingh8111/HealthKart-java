package com.healthkart.dao;

import com.healthkart.model.Reminder;

import java.util.List;

/**
 * Reminder DAO interface extending generic CrudDao.
 */
public interface ReminderDao extends CrudDao<Reminder, String> {

    List<Reminder> findByPatientEmail(String email);

    List<Reminder> findActiveReminders();

    boolean toggleActive(String id, boolean active);
}

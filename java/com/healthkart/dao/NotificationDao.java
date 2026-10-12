package com.healthkart.dao;

import com.healthkart.model.Notification;

import java.util.List;

/**
 * Notification DAO interface extending generic CrudDao.
 */
public interface NotificationDao extends CrudDao<Notification, String> {

    List<Notification> findByPatientEmail(String email);

    boolean markAsRead(String id);

    long countUnread(String email);
}

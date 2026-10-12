package com.healthkart.dao;

import com.healthkart.model.User;

import java.util.List;
import java.util.Optional;

/**
 * User DAO interface extending generic CrudDao.
 * Demonstrates:
 * - OOP: Interfaces and Generics
 * - Classes for Database Operations: dedicated contract for User persistence and authentication
 */
public interface UserDao extends CrudDao<User, String> {

    /**
     * Finds a user by their registered email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifies user credentials against SQLite database.
     */
    boolean authenticate(String email, String plainPassword);

    /**
     * Authenticates user and returns populated User object.
     */
    Optional<User> login(String email, String plainPassword);

    /**
     * Checks if an email is already registered in the database.
     */
    boolean emailExists(String email);

    /**
     * Updates user password with secure hash.
     */
    boolean updatePassword(String email, String newPlainPassword);

    /**
     * Finds users by role (PATIENT, DOCTOR, ADMIN).
     */
    List<User> findByRole(String role);
}

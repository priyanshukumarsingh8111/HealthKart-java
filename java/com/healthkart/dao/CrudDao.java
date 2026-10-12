package com.healthkart.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD DAO interface demonstrating OOP Interfaces and Generics.
 * Defines standard data access contract for entities.
 *
 * @param <T> Entity type
 * @param <ID> Primary key identifier type
 */
public interface CrudDao<T, ID> {

    /**
     * Persists a new entity.
     * @param entity Entity to save
     * @return Saved entity
     */
    T save(T entity);

    /**
     * Finds entity by primary key identifier.
     * @param id Identifier
     * @return Optional containing entity if found
     */
    Optional<T> findById(ID id);

    /**
     * Retrieves all entities.
     * @return List of all entities
     */
    List<T> findAll();

    /**
     * Updates an existing entity.
     * @param entity Entity to update
     * @return Updated entity
     */
    T update(T entity);

    /**
     * Deletes entity by ID.
     * @param id Identifier
     * @return true if deleted, false otherwise
     */
    boolean deleteById(ID id);

    /**
     * Returns total count of entities.
     * @return Count
     */
    long count();
}

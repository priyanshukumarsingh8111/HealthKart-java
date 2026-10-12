package com.healthkart.dao;

import com.healthkart.model.Order;
import java.util.List;

/**
 * Data Access Object contract for pharmacy orders.
 * Adheres to Interface-based abstraction (OOP Rubric).
 */
public interface OrderDao extends CrudDao<Order, String> {

    /**
     * Retrieve all orders placed by a specific user.
     * @param email User email
     * @return List of matching orders
     */
    List<Order> findByUserEmail(String email);

    /**
     * Place order and update inventory stock within a single ACID transaction.
     * @param order Order to persist
     * @return Persisted order
     */
    Order placeOrderTransaction(Order order);
}

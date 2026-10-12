package com.healthkart;

import com.healthkart.dao.NotificationDao;
import com.healthkart.dao.OrderDao;
import com.healthkart.model.Notification;
import com.healthkart.model.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.healthkart.Util.*;

/**
 * REST Controller for pharmacy orders & checkout.
 * Provides complete server-side order lifecycle, validation, and notification hooks.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderDao orderDao;
    private final NotificationDao notificationDao;
    private final Realtime rt;

    public OrderController(OrderDao orderDao, NotificationDao notificationDao, Realtime rt) {
        this.orderDao = orderDao;
        this.notificationDao = notificationDao;
        this.rt = rt;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> placeOrder(@RequestBody(required = false) Map<String, Object> raw) {
        Map<String, Object> b = body(raw);

        String customerName = s(b.get("customerName"));
        String customerPhone = s(b.get("customerPhone"));
        String deliveryAddress = s(b.get("deliveryAddress"));
        String userEmail = or(s(b.get("userEmail")), "guest@healthkart.com");

        if (customerName == null || customerName.isBlank()) {
            throw ApiException.of(400, "Recipient name is required for delivery.");
        }
        if (customerPhone == null || customerPhone.isBlank()) {
            throw ApiException.of(400, "Contact mobile number is required.");
        }
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            throw ApiException.of(400, "Delivery street address is required.");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) b.get("items");
        if (items == null || items.isEmpty()) {
            throw ApiException.of(400, "Cannot place an order with an empty cart.");
        }

        String orderId = "HK-ORD-" + (int)(100000 + Math.random() * 900000);
        String now = now();

        double subtotal = 0.0;
        int totalQty = 0;
        for (Map<String, Object> itm : items) {
            double price = 0.0;
            int qty = 1;
            try {
                price = Double.parseDouble(String.valueOf(itm.getOrDefault("price", 0.0)));
                qty = Integer.parseInt(String.valueOf(itm.getOrDefault("qty", 1)));
            } catch (Exception ignored) {}
            subtotal += price * qty;
            totalQty += qty;
        }

        double discount = Math.round(subtotal * 0.10 * 100.0) / 100.0; // 10% HealthKart Member discount
        double deliveryFee = (subtotal - discount) >= 500.0 ? 0.0 : 40.0;
        double netTotal = Math.round((subtotal - discount + deliveryFee) * 100.0) / 100.0;

        String paymentMethod = or(s(b.get("paymentMethod")), "Cash on Delivery");
        String city = or(s(b.get("city")), "New Delhi");
        String pincode = or(s(b.get("pincode")), "110001");
        String instructions = or(s(b.get("deliveryInstructions")), "Standard delivery");

        LocalDate estDate = LocalDate.now().plusDays(2);
        String estimatedDelivery = estDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"));

        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserEmail(userEmail);
        order.setCustomerName(customerName);
        order.setCustomerPhone(customerPhone);
        order.setDeliveryAddress(deliveryAddress);
        order.setCity(city);
        order.setPincode(pincode);
        order.setDeliveryInstructions(instructions);
        order.setItems(items);
        order.setItemCount(totalQty);
        order.setMrpTotal(subtotal);
        order.setDiscountAmount(discount);
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(netTotal);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus("Cash on Delivery".equalsIgnoreCase(paymentMethod) ? "PAY_ON_DELIVERY" : "PAID_ONLINE");
        order.setOrderStatus("CONFIRMED");
        order.setEstimatedDelivery(estimatedDelivery);
        order.setCreatedAt(now);

        // Save order and update medicine inventory stocks via pure JDBC transaction
        orderDao.placeOrderTransaction(order);

        // Create in-app notification
        try {
            Notification notif = new Notification(
                    "notif-" + UUID.randomUUID().toString().substring(0, 8),
                    userEmail,
                    "Order Confirmed: " + orderId,
                    "Your order of " + totalQty + " items (₹" + netTotal + ") has been placed successfully. Estimated delivery by " + estimatedDelivery + ".",
                    "ORDER_PLACED",
                    false
            );
            notificationDao.save(notif);
        } catch (Exception ex) {
            System.err.println("Notice: Order notification: " + ex.getMessage());
        }

        // Notify real-time socket
        rt.emit("order_placed", m("recipientEmail", userEmail, "orderId", orderId, "totalAmount", netTotal));

        return ResponseEntity.status(201).body(m(
                "success", true,
                "message", "Order confirmed and placed successfully!",
                "order", order
        ));
    }

    @GetMapping
    public Map<String, Object> listOrders(@RequestParam(name = "email", required = false) String email) {
        List<Order> orders;
        if (email != null && !email.isBlank()) {
            orders = orderDao.findByUserEmail(email);
        } else {
            orders = orderDao.findAll();
        }
        return m("success", true, "count", orders.size(), "orders", orders);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOrder(@PathVariable("id") String id) {
        Order order = orderDao.findById(id)
                .orElseThrow(() -> ApiException.of(404, "Order not found."));
        return m("success", true, "order", order);
    }
}

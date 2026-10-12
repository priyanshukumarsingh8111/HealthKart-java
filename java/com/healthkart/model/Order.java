package com.healthkart.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Domain entity representing a customer's pharmacy order.
 * Extends BaseEntity to adhere to OOP principles (Inheritance & Polymorphism).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Order extends BaseEntity {

    private String orderId;
    private String userEmail;
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private String city;
    private String pincode;
    private String deliveryInstructions;
    private List<Map<String, Object>> items = new ArrayList<>();
    private int itemCount;
    private double mrpTotal;
    private double discountAmount;
    private double deliveryFee;
    private double totalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;
    private String estimatedDelivery;

    public Order() {
        super();
        this.orderStatus = "CONFIRMED";
        this.paymentStatus = "PENDING_COD";
        this.paymentMethod = "Cash on Delivery";
    }

    public Order(String orderId, String userEmail, String customerName, String customerPhone,
                 String deliveryAddress, double totalAmount, String paymentMethod) {
        super(orderId);
        this.orderId = orderId;
        this.userEmail = userEmail;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.deliveryAddress = deliveryAddress;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.orderStatus = "CONFIRMED";
        this.paymentStatus = "Cash on Delivery".equalsIgnoreCase(paymentMethod) ? "PENDING_ON_DELIVERY" : "PAID";
    }

    @Override
    public String getEntitySummary() {
        return String.format("Order [%s] by %s (%s) - Total: ₹%.2f [%s]",
                orderId, customerName, userEmail, totalAmount, orderStatus);
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) {
        this.orderId = orderId;
        setId(orderId);
    }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getDeliveryInstructions() { return deliveryInstructions; }
    public void setDeliveryInstructions(String deliveryInstructions) { this.deliveryInstructions = deliveryInstructions; }

    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }

    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    public double getMrpTotal() { return mrpTotal; }
    public void setMrpTotal(double mrpTotal) { this.mrpTotal = mrpTotal; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public double getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(double deliveryFee) { this.deliveryFee = deliveryFee; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(String estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }
}

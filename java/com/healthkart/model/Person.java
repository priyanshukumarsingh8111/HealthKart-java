package com.healthkart.model;

/**
 * Abstract Person class extending BaseEntity.
 * Demonstrates multi-level OOP Inheritance and Polymorphic methods.
 */
public abstract class Person extends BaseEntity {
    private String name;
    private String email;
    private String phone;
    private String role;

    public Person() {
        super();
    }

    public Person(String id, String name, String email, String phone, String role) {
        super(id);
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Polymorphic method to be implemented by Doctor and Patient subclasses.
     */
    public abstract String getRoleDescription();

    @Override
    public String getEntitySummary() {
        return name + " (" + role + ") - " + email;
    }
}

package model;

import model.annotation.Id;
import model.annotation.MaxLength;

import java.io.Serializable;

/** A bank customer with a generated id such as CUST101. */
public class Customer implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    private static long customerCounter = 100;

    @Id
    private final String customerId;
    @MaxLength(50)
    private String name;
    private String email;
    private String mobile;
    private Address address;

    public Customer(String name, String email, String mobile) {
        this(name, email, mobile, new Address("", "", ""));
    }

    public Customer(String name, String email, String mobile, Address address) {
        this.customerId = generateCustomerId();
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.address = address;
    }

    private static synchronized String generateCustomerId() {
        customerCounter++;
        return "CUST" + customerCounter;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getMobile() {
        return mobile;
    }

    public Address getAddress() {
        return address;
    }

    /** Returns a copy of this customer (Address is immutable, so sharing it is safe). */
    @Override
    public Customer clone() {
        try {
            return (Customer) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);    // cannot happen: we implement Cloneable
        }
    }

    @Override
    public String toString() {
        return customerId + " | " + name + " | " + mobile + " | " + email;
    }

    /** Nested class: a postal address. */
    public static class Address implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String line;
        private final String city;
        private final String pincode;

        public Address(String line, String city, String pincode) {
            this.line = line;
            this.city = city;
            this.pincode = pincode;
        }

        public String getLine() {
            return line;
        }

        public String getCity() {
            return city;
        }

        public String getPincode() {
            return pincode;
        }

        @Override
        public String toString() {
            return line + ", " + city + " - " + pincode;
        }
    }
}

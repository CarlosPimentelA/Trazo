package com.trucks_logistics.Trucks.Logistics.exceptions;

public class EmailDuplicated extends RuntimeException {
    public EmailDuplicated(String msg) {
        super(msg);
    }
}

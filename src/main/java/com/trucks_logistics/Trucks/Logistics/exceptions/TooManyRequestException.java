package com.trucks_logistics.Trucks.Logistics.exceptions;

public class TooManyRequestException extends RuntimeException {
    public TooManyRequestException(String msg) {
        super(msg);
    }
}

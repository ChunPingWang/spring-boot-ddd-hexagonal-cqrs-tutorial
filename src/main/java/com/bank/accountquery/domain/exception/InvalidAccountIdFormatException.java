package com.bank.accountquery.domain.exception;

public class InvalidAccountIdFormatException extends RuntimeException {
    public InvalidAccountIdFormatException(String message) {
        super(message);
    }
}

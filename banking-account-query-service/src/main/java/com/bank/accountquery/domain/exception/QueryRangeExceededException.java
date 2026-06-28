package com.bank.accountquery.domain.exception;

public class QueryRangeExceededException extends RuntimeException {
    public QueryRangeExceededException(String message) {
        super(message);
    }
}

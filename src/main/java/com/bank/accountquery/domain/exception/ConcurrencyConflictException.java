package com.bank.accountquery.domain.exception;

public class ConcurrencyConflictException extends RuntimeException {
    public ConcurrencyConflictException(String aggregateId, int expectedVersion, int actualVersion) {
        super("聚合 [%s] 版本衝突：預期 %d，實際 %d".formatted(aggregateId, expectedVersion, actualVersion));
    }
}

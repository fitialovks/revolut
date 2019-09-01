package ru.halcraes.revolut.db;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class Transaction {
    private final TransactionId id;
    private final AccountId fromAccount;
    private final AccountId toAccount;
    private final BigDecimal money;
    private final Instant timestamp;
}

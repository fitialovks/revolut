package ru.halcraes.revolut.web;

import lombok.Data;
import ru.halcraes.revolut.db.AccountId;
import ru.halcraes.revolut.db.TransactionId;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class CreateTransactionResponse {
    private TransactionId id;
    private BigDecimal amount;
    private AccountId from;
    private AccountId to;
    private Instant timestamp;
}

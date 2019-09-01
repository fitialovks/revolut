package ru.halcraes.revolut.web;

import lombok.Data;
import ru.halcraes.revolut.db.AccountId;
import ru.halcraes.revolut.db.TransactionId;

import java.math.BigDecimal;

@Data
public class CreateTransactionRequest {
    private TransactionId id;
    private AccountId from;
    private AccountId to;
    private BigDecimal amount;
}

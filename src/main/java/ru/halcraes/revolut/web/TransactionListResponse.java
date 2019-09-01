package ru.halcraes.revolut.web;

import lombok.Data;
import ru.halcraes.revolut.db.AccountId;
import ru.halcraes.revolut.db.TransactionId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class TransactionListResponse {
    private List<Transaction> transactions;

    @Data
    public static class Transaction {
        private TransactionId id;
        private BigDecimal amount;
        private AccountId otherAccount;
        private Instant timestamp;
    }
}

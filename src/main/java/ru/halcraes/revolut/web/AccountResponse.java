package ru.halcraes.revolut.web;

import lombok.Data;
import ru.halcraes.revolut.db.AccountId;

import java.math.BigDecimal;

@Data
public class AccountResponse {
    private AccountId id;
    private BigDecimal balance;
    private String description;
}

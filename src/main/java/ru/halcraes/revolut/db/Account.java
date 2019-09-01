package ru.halcraes.revolut.db;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class Account {
    private final AccountId id;
    private final BigDecimal balance;
    private final String description;
}

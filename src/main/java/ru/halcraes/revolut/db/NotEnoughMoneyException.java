package ru.halcraes.revolut.db;

public class NotEnoughMoneyException extends IllegalArgumentException {
    private final AccountId account;

    public NotEnoughMoneyException(AccountId account) {
        super("Not enough money on account " + account);
        this.account = account;
    }

    public AccountId getAccount() {
        return account;
    }
}

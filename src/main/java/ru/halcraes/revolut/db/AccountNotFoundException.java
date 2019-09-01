package ru.halcraes.revolut.db;

public class AccountNotFoundException extends IllegalArgumentException {
    private final AccountId accountId;

    public AccountNotFoundException(AccountId accountId) {
        super("Account not found: " + accountId.toString());
        this.accountId = accountId;
    }

    public AccountId getAccountId() {
        return accountId;
    }
}

package ru.halcraes.revolut.db;

public class DuplicateTransactionIdException extends IllegalArgumentException {
    private final TransactionId transactionId;

    public DuplicateTransactionIdException(TransactionId transactionId) {
        super("Duplicate transaction id: " + transactionId);
        this.transactionId = transactionId;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }
}

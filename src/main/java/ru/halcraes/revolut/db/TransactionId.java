package ru.halcraes.revolut.db;

import lombok.Data;
import net.jcip.annotations.Immutable;

import java.util.UUID;

@Immutable
@Data
public class TransactionId {
    private final UUID value;

    private TransactionId(UUID value) {
        this.value = value;
    }

    public static TransactionId create() {
        return new TransactionId(UUID.randomUUID());
    }

    public static TransactionId deserialize(byte[] bytes) {
        return new TransactionId(UuidUtil.deserialize(bytes));
    }

    public byte[] serialize() {
        return UuidUtil.serialize(value);
    }

    public static TransactionId parse(String value) {
        return new TransactionId(UUID.fromString(value));
    }

    /**
     * Serialization for storage, don't mix with {@code toString()} that is for logging.
     */
    public String asString() {
        return value.toString();
    }
}

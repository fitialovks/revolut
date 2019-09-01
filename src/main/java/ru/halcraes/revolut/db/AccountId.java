package ru.halcraes.revolut.db;

import com.google.common.base.Preconditions;
import lombok.Data;
import net.jcip.annotations.Immutable;

@Immutable
@Data
public class AccountId {
    private final long value;

    public static AccountId parse(String value) {
        Preconditions.checkNotNull(value);
        Preconditions.checkArgument(!value.isBlank());
        return new AccountId(Long.parseLong(value));
    }

    public String serialize() {
        return String.valueOf(value);
    }
}

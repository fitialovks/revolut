package ru.halcraes.revolut.db;

import com.google.common.base.Preconditions;
import net.jcip.annotations.ThreadSafe;
import org.h2.api.ErrorCode;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ThreadSafe
public class AccountService {
    private final DataSource dataSource;

    public AccountService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public AccountId createAccount(@Nullable String description) {
        try (var conn = dataSource.getConnection();
             var statement = conn.prepareStatement("insert into account (description, money) values ( ?, 0 )", Statement.RETURN_GENERATED_KEYS);
        ) {
            statement.setString(1, description);
            statement.execute();
            if (!statement.getGeneratedKeys().first()) {
                throw new InternalException("Database did not return a generated key");
            }
            long id = statement.getGeneratedKeys().getLong("id");
            conn.commit();
            return new AccountId(id);
        } catch (SQLException e) {
            throw new InternalException(e);
        }
    }

    public Transaction moveMoney(
            @CheckForNull AccountId fromAccount,
            @CheckForNull AccountId toAccount,
            TransactionId transactionId,
            BigDecimal money
    ) {
        Preconditions.checkArgument(toAccount != null || fromAccount != null, "At least one account must be not null");
        Preconditions.checkNotNull(transactionId);
        Preconditions.checkNotNull(money);
        Preconditions.checkArgument(money.compareTo(BigDecimal.ZERO) > 0, "Money amount must be positive, found %s", money);

        try (Connection conn = dataSource.getConnection()) {
            try {
                conn.setAutoCommit(false);

                Transaction transaction;
                try {
                    // This code relies on primary key to prevent duplicate transactions.
                    transaction = createTransaction(conn, transactionId, fromAccount, toAccount, money);
                } catch (SQLException e) {
                    switch (e.getErrorCode()) {
                        case ErrorCode.DUPLICATE_KEY_1:
                            Transaction existingTransaction = getTransaction(conn, transactionId);
                            if (existingTransaction != null) {
                                if (Objects.equals(fromAccount, existingTransaction.getFromAccount())
                                        && Objects.equals(toAccount, existingTransaction.getToAccount())
                                        && money.equals(existingTransaction.getMoney())) {
                                    // this is a duplicate call
                                    return existingTransaction;
                                } else {
                                    // same id, but different values
                                    throw new DuplicateTransactionIdException(transactionId);
                                }
                            } else {
                                // transactions are not expected to disappear from the database
                                throw new InternalException("Duplicate transaction was removed");
                            }
                        case ErrorCode.REFERENTIAL_INTEGRITY_VIOLATED_PARENT_MISSING_1:
                            // A bit fragile, but there's a test.
                            if (e.getMessage().contains("FOREIGN KEY(FROM_ACC)")) {
                                throw new AccountNotFoundException(fromAccount);
                            }
                            if (e.getMessage().contains("FOREIGN KEY(TO_ACC)")) {
                                throw new AccountNotFoundException(toAccount);
                            }
                            throw new InternalException("Failed to recognize the violated constraint.");
                        default:
                            throw e;
                    }
                }

                if (fromAccount != null) {
                    try (var statement = conn.prepareStatement("update account set money = money - ? where id = ? and money >= ?")) {
                        statement.setBigDecimal(1, money);
                        statement.setLong(2, fromAccount.getValue());
                        statement.setBigDecimal(3, money);
                        statement.execute();
                        if (statement.getUpdateCount() != 1) {
                            // Note that we have already checked for account existence with a foreign key in the transaction
                            throw new NotEnoughMoneyException(fromAccount);
                        }
                    }
                }

                if (toAccount != null) {
                    try (var statement = conn.prepareStatement("update account set money = money + ? where id = ?")) {
                        statement.setBigDecimal(1, money);
                        statement.setLong(2, toAccount.getValue());
                        statement.execute();
                        if (statement.getUpdateCount() != 1) {
                            // this should not really happen as transaction has a foreign key constraint
                            throw new AccountNotFoundException(toAccount);
                        }
                    }
                }

                conn.commit();
                return transaction;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new InternalException(e);
        }
    }

    public Account getAccount(AccountId account) {
        Preconditions.checkNotNull(account);
        try (Connection conn = dataSource.getConnection()) {
            try (var statement = conn.prepareStatement("select * from account where id = ?")) {
                statement.setLong(1, account.getValue());
                statement.execute();
                try (var rs = statement.getResultSet()) {
                    if (!rs.first()) {
                        throw new AccountNotFoundException(account);
                    }
                    var balance = rs.getBigDecimal("money");
                    var description = rs.getString("description");
                    return Account.builder()
                            .id(account)
                            .balance(balance)
                            .description(description)
                            .build();
                }
            }
        } catch (SQLException e) {
            throw new InternalException(e);
        }
    }

    public BigDecimal getBalance(AccountId account) {
        return getAccount(account).getBalance();
    }

    public List<Transaction> getTransactions(
            AccountId account,
            @CheckForNull Integer limit,
            @CheckForNull Integer offset,
            @CheckForNull Instant fromTimestamp,
            @CheckForNull Instant toTimestamp
    ) {
        Preconditions.checkNotNull(account);
        if (limit == null) limit = 100;
        if (offset == null) offset = 0;
        try (Connection conn = dataSource.getConnection()) {
            // I expect the driver or the db to optimize this query, but did not check.
            try (var statement = conn.prepareStatement("select * from transaction where" +
                    " (from_acc = ? or to_acc = ?) and (? is null or time < ?) and (? is null or time > ?)" +
                    " order by time desc limit ? offset ?")) {
                statement.setLong(1, account.getValue());
                statement.setLong(2, account.getValue());
                Timestamp from = fromTimestamp == null ? null : Timestamp.from(fromTimestamp);
                statement.setTimestamp(3, from);
                statement.setTimestamp(4, from);
                Timestamp to = toTimestamp == null ? null : Timestamp.from(toTimestamp);
                statement.setTimestamp(5, to);
                statement.setTimestamp(6, to);
                statement.setInt(7, limit);
                statement.setInt(8, offset);
                statement.execute();
                List<Transaction> transactions = new ArrayList<>();
                try (var rs = statement.getResultSet()) {
                    while (rs.next()) {
                        transactions.add(parseTransaction(rs));
                    }
                }
                return transactions;
            }
        } catch (SQLException e) {
            throw new InternalException(e);
        }
    }

    @CheckForNull
    private Transaction getTransaction(Connection conn, TransactionId transactionId) throws SQLException {
        try (var statement = conn.prepareStatement("select * from transaction where id = ?")) {
            statement.setBytes(1, transactionId.serialize());
            statement.execute();
            try (var rs = statement.getResultSet()) {
                if (rs.first()) {
                    return parseTransaction(rs);
                } else {
                    return null;
                }
            }
        }
    }

    private static Transaction parseTransaction(ResultSet rs) throws SQLException {
        long fromId = rs.getLong("from_acc");
        AccountId fromAcc = rs.wasNull() ? null : new AccountId(fromId);
        long toId = rs.getLong("to_acc");
        AccountId toAcc = rs.wasNull() ? null : new AccountId(toId);
        return Transaction.builder()
                .id(TransactionId.deserialize(rs.getBytes("id")))
                .money(rs.getBigDecimal("money"))
                .fromAccount(fromAcc)
                .toAccount(toAcc)
                .timestamp(rs.getTimestamp("time").toInstant())
                .build();
    }

    private Transaction createTransaction(
            Connection conn,
            TransactionId transactionId,
            @Nullable AccountId from,
            @Nullable AccountId to,
            BigDecimal money
    ) throws SQLException {
        Preconditions.checkNotNull(conn);
        Preconditions.checkNotNull(transactionId);
        Preconditions.checkNotNull(money);

        Instant timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        try (var statement = conn.prepareStatement(
                "insert into transaction(id, from_acc, to_acc, money, time) values(?, ?, ?, ?, ?)")) {
            statement.setBytes(1, transactionId.serialize());
            if (from != null) {
                statement.setLong(2, from.getValue());
            } else {
                statement.setNull(2, Types.BIGINT);
            }
            if (to != null) {
                statement.setLong(3, to.getValue());
            } else {
                statement.setNull(3, Types.BIGINT);
            }
            statement.setBigDecimal(4, money);
            statement.setTimestamp(5, Timestamp.from(timestamp));
            statement.execute();
            if (statement.getUpdateCount() != 1) {
                throw new InternalException("Failed to create a transaction");
            }
            return Transaction.builder()
                    .id(transactionId)
                    .fromAccount(from)
                    .toAccount(to)
                    .money(money)
                    .timestamp(timestamp)
                    .build();
        }
    }

}

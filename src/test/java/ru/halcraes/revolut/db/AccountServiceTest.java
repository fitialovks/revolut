package ru.halcraes.revolut.db;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class AccountServiceTest {
    private static Database database;

    @BeforeClass
    public static void setUp() {
        database = Database.initialize();
    }

    @AfterClass
    public static void tearDown() {
        database.close();
    }

    private AccountService getAccountService() {
        return new AccountService(database.getDataSource());
    }

    @Test
    public void addMoney() {
        AccountService accountService = getAccountService();
        var account = accountService.createAccount("foo");

        var transactionId = TransactionId.create();
        var transaction = accountService.moveMoney(null, account, transactionId, new BigDecimal("1.00"));
        assertEquals(transactionId, transaction.getId());
        assertEquals(account, transaction.getToAccount());
        assertNull(transaction.getFromAccount());
        assertEquals(new BigDecimal("1.00"), transaction.getMoney());
        assertNotNull(transaction.getTimestamp());
        var balance = accountService.getBalance(account);
        assertEquals(new BigDecimal("1.00"), balance);
    }

    @Test
    public void addMoneyDuplicate() {
        AccountService accountService = getAccountService();
        var account = accountService.createAccount("foo");

        var transactionId = TransactionId.create();
        var transaction = accountService.moveMoney(null, account, transactionId, new BigDecimal("1.00"));
        var transactionCopy = accountService.moveMoney(null, account, transactionId, new BigDecimal("1.00"));
        assertEquals(transaction, transactionCopy);
        var balance = accountService.getBalance(account);
        assertEquals(new BigDecimal("1.00"), balance);
    }

    @Test
    public void removeMoney() {
        AccountService accountService = getAccountService();
        var account = accountService.createAccount("foo");

        accountService.moveMoney(null, account, TransactionId.create(), new BigDecimal("10.00"));
        var transactionId = TransactionId.create();
        var transaction = accountService.moveMoney(account, null, transactionId, new BigDecimal("1.00"));
        assertEquals(transactionId, transaction.getId());
        assertEquals(account, transaction.getFromAccount());
        assertNull(transaction.getToAccount());
        assertEquals(new BigDecimal("1.00"), transaction.getMoney());
        assertNotNull(transaction.getTimestamp());
        var balance = accountService.getBalance(account);
        assertEquals(new BigDecimal("9.00"), balance);
    }

    @Test
    public void removeMoneyDuplicate() {
        AccountService accountService = getAccountService();
        var account = accountService.createAccount("foo");

        accountService.moveMoney(null, account, TransactionId.create(), new BigDecimal("10.00"));
        var transactionId = TransactionId.create();
        var transaction = accountService.moveMoney(account, null, transactionId, new BigDecimal("1.00"));
        var transactionCopy = accountService.moveMoney(account, null, transactionId, new BigDecimal("1.00"));
        assertEquals(transaction, transactionCopy);
        var balance = accountService.getBalance(account);
        assertEquals(new BigDecimal("9.00"), balance);
    }

    @Test
    public void moveMoney() {
        AccountService accountService = getAccountService();
        var accountFrom = accountService.createAccount("foo");
        var accountTo = accountService.createAccount("bar");

        accountService.moveMoney(null, accountFrom, TransactionId.create(), new BigDecimal("100.00"));
        accountService.moveMoney(null, accountTo, TransactionId.create(), new BigDecimal("10.00"));
        var transactionId = TransactionId.create();
        var transaction = accountService.moveMoney(accountFrom, accountTo, transactionId, new BigDecimal("20.00"));
        assertEquals(transactionId, transaction.getId());
        assertEquals(accountFrom, transaction.getFromAccount());
        assertEquals(accountTo, transaction.getToAccount());
        assertEquals(new BigDecimal("20.00"), transaction.getMoney());
        assertNotNull(transaction.getTimestamp());
        var balanceFrom = accountService.getBalance(accountFrom);
        assertEquals(new BigDecimal("80.00"), balanceFrom);
        var balanceTo = accountService.getBalance(accountTo);
        assertEquals(new BigDecimal("30.00"), balanceTo);
    }

    @Test
    public void moveMoneyDuplicate() {
        AccountService accountService = getAccountService();
        var accountFrom = accountService.createAccount("foo");
        var accountTo = accountService.createAccount("bar");

        accountService.moveMoney(null, accountFrom, TransactionId.create(), new BigDecimal("20.00"));
        accountService.moveMoney(null, accountTo, TransactionId.create(), new BigDecimal("10.00"));
        var transactionId = TransactionId.create();
        var transaction = accountService.moveMoney(accountFrom, accountTo, transactionId, new BigDecimal("20.00"));
        var transactionCopy = accountService.moveMoney(accountFrom, accountTo, transactionId, new BigDecimal("20.00"));
        assertEquals(transaction, transactionCopy);

        var balanceFrom = accountService.getBalance(accountFrom);
        assertEquals(new BigDecimal("0.00"), balanceFrom);
        var balanceTo = accountService.getBalance(accountTo);
        assertEquals(new BigDecimal("30.00"), balanceTo);
    }

    @Test
    public void moveMoneyInvalidDuplicateFrom() {
        AccountService accountService = getAccountService();
        var accountFrom = accountService.createAccount("foo");
        var accountTo = accountService.createAccount("bar");
        var accountOther = accountService.createAccount("baz");

        accountService.moveMoney(null, accountFrom, TransactionId.create(), new BigDecimal("100.00"));
        accountService.moveMoney(null, accountTo, TransactionId.create(), new BigDecimal("10.00"));
        var transactionId = TransactionId.create();
        accountService.moveMoney(accountFrom, accountTo, transactionId, new BigDecimal("20.00"));
        try {
            accountService.moveMoney(accountOther, accountTo, transactionId, new BigDecimal("20.00"));
            fail("Expected an exception");
        } catch (DuplicateTransactionIdException e) {
            assertEquals(e.getTransactionId(), transactionId);
        }
    }

    @Test
    public void moveMoneyInvalidDuplicateTo() {
        AccountService accountService = getAccountService();
        var accountFrom = accountService.createAccount("foo");
        var accountTo = accountService.createAccount("bar");
        var accountOther = accountService.createAccount("baz");

        accountService.moveMoney(null, accountFrom, TransactionId.create(), new BigDecimal("100.00"));
        accountService.moveMoney(null, accountTo, TransactionId.create(), new BigDecimal("10.00"));
        var transactionId = TransactionId.create();
        accountService.moveMoney(accountFrom, accountTo, transactionId, new BigDecimal("20.00"));
        try {
            accountService.moveMoney(accountFrom, accountOther, transactionId, new BigDecimal("20.00"));
            fail("Expected an exception");
        } catch (DuplicateTransactionIdException e) {
            assertEquals(e.getTransactionId(), transactionId);
        }
    }

    @Test
    public void moveMoneyInvalidDuplicateMoney() {
        AccountService accountService = getAccountService();
        var accountFrom = accountService.createAccount("foo");
        var accountTo = accountService.createAccount("bar");

        accountService.moveMoney(null, accountFrom, TransactionId.create(), new BigDecimal("100.00"));
        accountService.moveMoney(null, accountTo, TransactionId.create(), new BigDecimal("10.00"));
        var transactionId = TransactionId.create();
        accountService.moveMoney(accountFrom, accountTo, transactionId, new BigDecimal("20.00"));
        try {
            accountService.moveMoney(accountFrom, accountTo, transactionId, new BigDecimal("19.00"));
            fail("Expected an exception");
        } catch (DuplicateTransactionIdException e) {
            assertEquals(e.getTransactionId(), transactionId);
        }
    }

    @Test
    public void moveMoneyInvalidAmount() {
        AccountService accountService = getAccountService();
        var accountFrom = accountService.createAccount("foo");
        var accountTo = accountService.createAccount("bar");

        accountService.moveMoney(null, accountFrom, TransactionId.create(), new BigDecimal("10.00"));
        accountService.moveMoney(null, accountTo, TransactionId.create(), new BigDecimal("10.00"));
        var transactionId = TransactionId.create();
        try {
            accountService.moveMoney(accountFrom, accountTo, transactionId, new BigDecimal("100.00"));
            fail("Expected an exception");
        } catch (NotEnoughMoneyException e) {
            assertEquals(e.getAccount(), accountFrom);
        }
    }

    @Test
    public void moveMoneyInvalidFrom() {
        AccountService accountService = getAccountService();
        var accountTo = accountService.createAccount("bar");
        var accountFrom = new AccountId(-666);

        try {
            accountService.moveMoney(accountFrom, accountTo, TransactionId.create(), new BigDecimal("100.00"));
            fail("Expected an exception");
        } catch (AccountNotFoundException e) {
            assertEquals(accountFrom, e.getAccountId());
        }
    }

    @Test
    public void moveMoneyInvalidTo() {
        AccountService accountService = getAccountService();
        var accountFrom = accountService.createAccount("bar");
        var accountTo = new AccountId(-666);

        try {
            accountService.moveMoney(accountFrom, accountTo, TransactionId.create(), new BigDecimal("100.00"));
            fail("Expected an exception");
        } catch (AccountNotFoundException e) {
            assertEquals(accountTo, e.getAccountId());
        }
    }
}

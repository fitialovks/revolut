package ru.halcraes.revolut.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.halcraes.revolut.db.AccountService;
import ru.halcraes.revolut.db.Database;
import ru.halcraes.revolut.db.TransactionId;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.Assert.*;
import static spark.Spark.*;

public class RestApiTest {
    private static Database database;
    private static AccountService accountService;
    private static RestApi restApi;
    private static OkHttpClient httpClient;
    private static ObjectMapper objectMapper = SerializationUtil.getObjectMapper();

    @BeforeClass
    public static void setUp() {
        database = Database.initialize();
        accountService = new AccountService(database.getDataSource());
        restApi = new RestApi(accountService);
        restApi.configure();
        init();
        awaitInitialization();
        httpClient = new OkHttpClient();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stop();
        awaitStop();
        database.close();
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
        var cache = httpClient.cache();
        if (cache != null) cache.close();
    }

    @Test
    public void createAccount() throws IOException {
        var response = postJson("account", "{\"description\": \"test\"}");
        var result = objectMapper.readValue(response, AccountResponse.class);
        assertEquals("test", result.getDescription());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        assertNotNull(result.getId());
        // Now check the DB
        var account = accountService.getAccount(result.getId());
        assertEquals("test", account.getDescription());
        assertEquals(BigDecimal.ZERO, account.getBalance());
    }

    @Test
    public void getAccount() throws IOException {
        var account = accountService.createAccount("test");
        accountService.moveMoney(null, account, TransactionId.create(), new BigDecimal("123.45"));

        String json = getJson("account/" + account.serialize());
        var response = objectMapper.readValue(json, AccountResponse.class);
        assertEquals(account, response.getId());
        assertEquals("test", response.getDescription());
        assertEquals(new BigDecimal("123.45"), response.getBalance());
    }

    @Test
    public void generateTransactionId() throws IOException {
        String notReallyJson = postJson("transaction/id", "");
        assertNotNull(UUID.fromString(notReallyJson));
    }

    @Test
    public void sendFromExternal() throws IOException {
        var account = accountService.createAccount("test");
        TransactionId tid = TransactionId.create();
        String request = "{"
                + "\"id\": \"" + tid.asString() + "\","
                + "\"from\": null,"
                + "\"to\": " + account.serialize() + ","
                + "\"amount\": 123.45"
                + "}";
        var response = postJson("transaction", request);
        var result = objectMapper.readValue(response, CreateTransactionResponse.class);
        assertEquals(tid, result.getId());
        assertEquals(new BigDecimal("123.45"), result.getAmount());
        assertNull(result.getFrom());
        assertEquals(account, result.getTo());
        assertNotNull(result.getTimestamp());
    }

    @Test
    public void sendToExternal() throws IOException {
        var account = accountService.createAccount("test");
        accountService.moveMoney(null, account, TransactionId.create(), new BigDecimal("1000"));
        TransactionId tid = TransactionId.create();
        String request = "{"
                + "\"id\": \"" + tid.asString() + "\","
                + "\"from\": " + account.serialize() + ","
                + "\"to\": null,"
                + "\"amount\": 123.45"
                + "}";
        var response = postJson("transaction", request);
        var result = objectMapper.readValue(response, CreateTransactionResponse.class);
        assertEquals(tid, result.getId());
        assertEquals(new BigDecimal("123.45"), result.getAmount());
        assertEquals(account, result.getFrom());
        assertNull(result.getTo());
        assertNotNull(result.getTimestamp());
    }

    @Test
    public void sendInternal() throws IOException {
        var accountFrom = accountService.createAccount("test");
        accountService.moveMoney(null, accountFrom, TransactionId.create(), new BigDecimal("1000"));
        var accountTo = accountService.createAccount("test");
        TransactionId tid = TransactionId.create();
        String request = "{"
                + "\"id\": \"" + tid.asString() + "\","
                + "\"from\": " + accountFrom.serialize() + ","
                + "\"to\": " + accountTo.serialize() + ","
                + "\"amount\": 123.45"
                + "}";
        var response = postJson("transaction", request);
        var result = objectMapper.readValue(response, CreateTransactionResponse.class);
        assertEquals(tid, result.getId());
        assertEquals(new BigDecimal("123.45"), result.getAmount());
        assertEquals(accountFrom, result.getFrom());
        assertEquals(accountTo, result.getTo());
        assertNotNull(result.getTimestamp());
    }

    private static String postJson(String path, String json) throws IOException {
        var request = new Request.Builder()
                .url("http://localhost:" + port() + "/api/v1/" + path)
                .post(RequestBody.create(json, MediaType.get("application/json")))
                .build();
        var response = httpClient.newCall(request).execute();
        assertEquals(200, response.code());
        var body = response.body();
        assertNotNull(body);
        return body.string();
    }

    private static String getJson(String path) throws IOException {
        var request = new Request.Builder()
                .url("http://localhost:" + port() + "/api/v1/" + path)
                .get()
                .build();
        var response = httpClient.newCall(request).execute();
        assertEquals(200, response.code());
        var body = response.body();
        assertNotNull(body);
        return body.string();
    }
}

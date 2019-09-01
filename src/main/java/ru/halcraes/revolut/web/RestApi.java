package ru.halcraes.revolut.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.http.HttpStatus;
import ru.halcraes.revolut.db.*;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class RestApi {
    private final AccountService accountService;
    private final ObjectMapper objectMapper = SerializationUtil.getObjectMapper();

    public RestApi(AccountService accountService) {
        this.accountService = accountService;
    }

    public void configure() {
        JsonTransformer transformer = new JsonTransformer(objectMapper);
        get("/api/v1/account/:id", "application/json", this::getAccount, transformer);
        post("/api/v1/account", "application/json", this::createAccount, transformer);
        get("/api/v1/transaction", "application/json", this::getTransactions, transformer);
        post("/api/v1/transaction", "application/json", this::send, transformer);
        post("/api/v1/transaction/id", "text/plain", this::generateTransactionId, Objects::toString);

        exception(AccountNotFoundException.class, (exception, request, response) -> {
            var error = Error.builder()
                    .message(String.format("Account %s not found.", exception.getAccountId()))
                    .build();
            sendError(HttpStatus.BAD_REQUEST_400, error, response);
        });
        exception(NotEnoughMoneyException.class, (exception, request, response) -> {
            var error = Error.builder()
                    .message(String.format("Account %s does not have enough funds.", exception.getAccount()))
                    .build();
            sendError(HttpStatus.BAD_REQUEST_400, error, response);
        });

        // Input validation can be drastically improved, but requires a lot of code with custom exceptions.
        exception(IllegalArgumentException.class, (exception, request, response) -> {
            var error = Error.builder()
                    .message(exception.getMessage())
                    .build();
            sendError(HttpStatus.BAD_REQUEST_400, error, response);
        });
        exception(NullPointerException.class, (exception, request, response) -> {
            var error = Error.builder()
                    .message(exception.getMessage())
                    .build();
            sendError(HttpStatus.BAD_REQUEST_400, error, response);
        });
    }

    private void sendError(int status, Error error, Response response) {
        response.status(status);
        response.type("application/json");
        try {
            response.body(objectMapper.writeValueAsString(error));
        } catch (JsonProcessingException e) {
            throw new InternalException(e);
        }
    }

    private AccountResponse createAccount(Request request, Response response) throws IOException {
        CreateAccountRequest car = objectMapper.readValue(request.body(), CreateAccountRequest.class);
        AccountId account = accountService.createAccount(car.getDescription());
        var result = new AccountResponse();
        result.setId(account);
        result.setBalance(BigDecimal.ZERO);
        result.setDescription(car.getDescription());
        return result;
    }

    private AccountResponse getAccount(Request request, Response response) {
        AccountId id = AccountId.parse(request.params("id"));
        Account account = accountService.getAccount(id);
        AccountResponse result = new AccountResponse();
        result.setBalance(account.getBalance());
        result.setId(account.getId());
        result.setDescription(account.getDescription());
        return result;
    }

    private TransactionListResponse getTransactions(Request request, Response response) {
        AccountId id = AccountId.parse(request.queryParams("account"));
        String limitStr = request.queryParams("limit");
        Integer limit = limitStr == null ? null : Integer.valueOf(limitStr);
        String offsetStr = request.queryParams("offset");
        Integer offset = limitStr == null ? null : Integer.valueOf(offsetStr);
        String fromStr = request.queryParams("from");
        Instant from = fromStr == null ? null : Instant.parse(fromStr);
        String toStr = request.queryParams("to");
        Instant to = fromStr == null ? null : Instant.parse(toStr);
        var result = new TransactionListResponse();
        List<Transaction> transactions = accountService.getTransactions(id, limit, offset, from, to);
        result.setTransactions(transactions.stream().map(t -> {
            var m = new TransactionListResponse.Transaction();
            m.setId(t.getId());
            m.setAmount(id.equals(t.getFromAccount()) ? t.getMoney().negate() : t.getMoney());
            var otherAccount = id.equals(t.getFromAccount()) ? t.getToAccount() : t.getFromAccount();
            m.setOtherAccount(otherAccount);
            m.setTimestamp(t.getTimestamp());
            return m;
        }).collect(Collectors.toList()));
        return result;
    }

    private CreateTransactionResponse send(Request request, Response response) throws IOException {
        var tr = objectMapper.readValue(request.body(), CreateTransactionRequest.class);
        var t = accountService.moveMoney(tr.getFrom(), tr.getTo(), tr.getId(), tr.getAmount());
        var result = new CreateTransactionResponse();
        result.setId(t.getId());
        result.setAmount(t.getMoney());
        result.setFrom(t.getFromAccount());
        result.setTo(t.getToAccount());
        result.setTimestamp(t.getTimestamp());
        return result;
    }

    private String generateTransactionId(Request request, Response response) {
        response.type("text/plain");
        return TransactionId.create().asString();
    }
}

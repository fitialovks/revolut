package ru.halcraes.revolut.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.halcraes.revolut.db.AccountId;
import ru.halcraes.revolut.db.TransactionId;

import java.io.IOException;

public class SerializationUtil {

    public static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(getAccountIdModule());
        objectMapper.registerModule(getTransactionIdModule());
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    public static Module getAccountIdModule() {
        SimpleModule module = new SimpleModule();

        module.addDeserializer(AccountId.class, new JsonDeserializer<>() {
            @Override
            public AccountId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                switch (p.getCurrentTokenId()) {
                    case JsonTokenId.ID_NUMBER_INT:
                    case JsonTokenId.ID_NUMBER_FLOAT:
                        return new AccountId(p.getLongValue());
                }
                return (AccountId) ctxt.handleUnexpectedToken(AccountId.class, p);
            }
        });

        module.addSerializer(AccountId.class, new JsonSerializer<>() {
            @Override
            public void serialize(AccountId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeNumber(value.getValue());
            }
        });

        return module;
    }

    public static Module getTransactionIdModule() {
        SimpleModule module = new SimpleModule();

        module.addSerializer(TransactionId.class, new JsonSerializer<>() {
            @Override
            public void serialize(TransactionId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value.asString());
            }
        });

        module.addDeserializer(TransactionId.class, new JsonDeserializer<>() {
            @Override
            public TransactionId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                switch (p.getCurrentTokenId()) {
                    case JsonTokenId.ID_STRING:
                        return TransactionId.parse(p.getValueAsString());
                }
                return (TransactionId) ctxt.handleUnexpectedToken(TransactionId.class, p);
            }
        });

        return module;
    }
}

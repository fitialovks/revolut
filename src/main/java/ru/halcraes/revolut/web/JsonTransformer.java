package ru.halcraes.revolut.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import spark.ResponseTransformer;

public class JsonTransformer implements ResponseTransformer {
    private final ObjectMapper objectMapper;

    public JsonTransformer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String render(Object model) throws Exception {
        // return String?! - WTF
        return objectMapper.writeValueAsString(model);
    }
}

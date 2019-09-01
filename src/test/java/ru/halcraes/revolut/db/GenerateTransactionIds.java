package ru.halcraes.revolut.db;

import com.google.common.io.BaseEncoding;

public class GenerateTransactionIds {
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            byte[] bytes = UuidUtil.serialize(TransactionId.create().getValue());
            System.out.println(BaseEncoding.base16().encode(bytes));
        }
    }
}

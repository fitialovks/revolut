#!/usr/bin/env bash

curl -X POST http://localhost:8080/api/v1/transaction \
  -H "accept: application/json" \
  -H "Content-Type: application/json" \
  -d "{ \"id\": \"0838c28c-3502-47f1-b2fc-1a664e6db38e\", \"from\": 1, \"to\": 2, \"amount\": 21.00}"
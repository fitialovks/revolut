#!/usr/bin/env bash

curl -X POST http://localhost:8080/api/v1/account \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -d "{ \"description\": \"Test account\"}"
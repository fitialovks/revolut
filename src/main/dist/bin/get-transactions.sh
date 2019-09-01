#!/usr/bin/env bash

ACCOUNT_ID="1"

curl "http://localhost:8080/api/v1/transaction?account=$ACCOUNT_ID"
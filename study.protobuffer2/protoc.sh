#!/usr/bin/env bash
cd src/main
protoc --java_out=java/ resources/proto/AddressBook.proto
#!/bin/bash
ssh  cliente0  "pkill -f java" &
echo 'cliente0 killed'
ssh  cliente1  "pkill -f java" &
echo 'cliente1 killed'
ssh  cliente2  "pkill -f java" &
echo 'cliente2 killed'
ssh  cliente3  "pkill -f java" &
echo 'cliente3 killed'
ssh  replica3 "pkill -f java" &
echo 'replica3 killed'
ssh  replica2 "pkill -f java" &
echo 'replica2 killed'
ssh  replica1 "pkill -f java" &
echo 'replica1 killed'
ssh  replica0 "pkill -f java" &
echo 'replica0 killed'
echo
echo 'finished hibrid'

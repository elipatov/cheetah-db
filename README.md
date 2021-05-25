<p align="left">
  <img src="readme/logo.png"/>
</p>

[![Build Status](https://github.com/elipatov/cheetah-db/workflows/CI/badge.svg)](https://github.com/elipatov/cheetah-db/actions)

CheetahDB is a distributed CRDT based database.

## Introduction

CRDTs (Conflict-free Replicated Data Types) are data structures that can be updated concurrently, without any locking or coordination, and remains consistent. CRDTs are said to provide "strong eventual consistency".

#### CmRDT: Operation based design
In the operation based implementation, the increment operation is transmitted to all other replicas.

#### CvRDT: State based design
In the state based implementation, the counter state is transmitted to all other replicas.

#### G-Counter
A grow-only counter. As the name suggests, g-counter only support increment operation.

<p align="left">
  <img src="readme/g-counter.png"/>
</p>

## Build
```
sbt core/assembly
docker build . -t cheetah-db
docker compose up
```

## Example

After starting a cluster we can now increment counter at each node in the cluster.
```
$ curl -XPUT "localhost:8080/v1/gcounter/key0" -i -d '3' -H "Content-Type: application/json"
$ curl -XPUT "localhost:8081/v1/gcounter/key0" -i -d '5' -H "Content-Type: application/json"
$ curl -XPUT "localhost:8082/v1/gcounter/key0" -i -d '7' -H "Content-Type: application/json"
```

Nodes synchronises with each other. Each node in the cluster returns consistent value.
```
$ curl "localhost:8080/v1/gcounter/key0" -i
$ curl "localhost:8081/v1/gcounter/key0" -i 
$ curl "localhost:8082/v1/gcounter/key0" -i
```

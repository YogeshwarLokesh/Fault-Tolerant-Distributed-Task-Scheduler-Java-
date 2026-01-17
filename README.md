# Fault-Tolerant Distributed Task Scheduler (Java)

## Overview

This project is a **fault-tolerant distributed task scheduler** built in **Java** that reliably executes background jobs despite **worker crashes, network failures, duplicate execution, and system restarts**.

The system follows **industry-proven distributed systems patterns** used in platforms like Kafka consumers, Apache Airflow, and cloud-based task queues.

---

## Problem This Solves

In real-world systems, background jobs often fail **silently** when:

- A worker crashes mid-execution  
- A machine loses network connectivity  
- A process is killed or restarted  
- The scheduler itself crashes  

Naive queue-based systems leave jobs **stuck forever**.

👉 This scheduler guarantees that **jobs are never lost** and are **safely retried or recovered**.

---

## High-Level Architecture

            ┌────────────────────┐
            │    Scheduler Node   │
            │  (Leader Elected)   │
            └─────────┬──────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
        ▼             ▼             ▼
  ┌─────────┐   ┌─────────┐   ┌─────────┐
  │ Worker-1│   │ Worker-2│   │ Worker-N│
  │ Stateless│  │ Stateless│  │ Stateless│
  └────┬────┘   └────┬────┘   └────┬────┘
       │             │             │
       │ Heartbeats  │ Heartbeats  │
       ▼             ▼             ▼
  ┌────────────────────────────────────┐
  │               Redis                │
  │  - Job Queue (BRPOP)               │
  │  - Worker Heartbeats (TTL)         │
  │  - Leader Lease                    │
  └────────────────────────────────────┘
                      │
                      ▼
  ┌────────────────────────────────────┐
  │            PostgreSQL               │
  │  - Job State (source of truth)     │
  │  - Atomic job claiming             │
  │  - Retry metadata                  │
  └────────────────────────────────────┘

---

## Core Design Principles

- Assume everything fails  
- Workers are stateless  
- Duplicate execution is allowed  
- Data corruption is not  
- Recovery is automatic  

---

## Job Lifecycle

PENDING → RUNNING → SUCCESS
│
└──→ RETRYING → FAILED


- State transitions are **atomic**
- Retries use **exponential backoff**
- Jobs survive crashes and restarts

---

## Execution Guarantees

- **At-least-once execution**
- **Exactly-once state commitment**
- **Idempotent recovery logic**

This is the same execution model used in **production distributed systems**.

---

## How to Run Locally

### Prerequisites

- Java 17+
- Maven
- Redis
- PostgreSQL

---


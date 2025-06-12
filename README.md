# 📊 URL Stats Service – Infrastructure as Code with AWS CDK (Java)

## 🧩 Project Overview

This project consists of two main modules:

---

### 1️⃣ **Infrastructure Project**

This module provides an **Infrastructure as Code (IaC)** solution for deploying the **URL Stats Service**, a microservice responsible for collecting and serving usage statistics for shortened URLs.

The infrastructure is implemented using the **AWS Cloud Development Kit (CDK)** in **Java**, enabling scalable, maintainable, and repeatable deployments on AWS.

**Key AWS components deployed:**
- `ECS (Fargate)` – for running the Stats Service containers
- `DynamoDB` – for storing URL events
- `API Gateway`, `Application Load Balancer (ALB)`
- `SQS`, `SNS`, `EventBridge`
- `CloudWatch`, `X-Ray`, and alarms
- A scheduled `Lambda` function for cleaning up old events (see below)

---

### 2️⃣ **Lambda Cleanup Project  (UNDER DEVELOPMENT)**

This module defines a scheduled **AWS Lambda** function that performs cleanup tasks on the `DynamoDB` table containing URL events.

**Function responsibilities:**
- Find `CREATE` events older than **1 month**
- Check for related `RESOLVE` events for the same `shortUrlHash`
- If **all RESOLVE events are older than 3 months**, archive all related events to `Amazon S3` and then delete them from `DynamoDB`

**Benefits:**
- Reduces stale data and storage costs
- Ensures auditability by archiving deleted records

---

## 🧩 Part of a Microservices-Based URL Shortener Platform

The **URL Stats Service** is one of several components that make up the larger **URL Shortener Platform**, a modern, microservices-based solution for URL shortening, tracking, and analytics. The complete platform includes:

- 🔗 [**URL Shortener Service**](https://github.com/dobrevd/url_shortener_service) — The core backend service for creating and resolving shortened URLs.
- 📈 [**URL Audit Service**](https://github.com/dobrevd/url-audit-service) — Responsible for logging and analyzing user interactions for auditing purposes.
- 🖥️ [**Frontend Application**](https://github.com/dobrevd/url-shortener-frontend) — A user-friendly web interface for interacting with the system.
- 📊 [**URL Stats Service**](https://github.com/dobrevd/url_stats_service) — A microservice (currently under development) for providing real-time and historical statistics on URL usage.

## 🚀 Key Features

- Infrastructure defined entirely in Java using AWS CDK
- Designed for scalability and high availability on AWS
- Easily extendable as the Stats Service evolves
- Aligns with modern DevOps practices and microservices architecture

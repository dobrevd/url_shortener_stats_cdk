# 📊 URL Stats Service – Infrastructure as Code with AWS CDK (Java)

This project provides an **Infrastructure as Code (IaC)** solution for deploying the **URL Stats Service**, a microservice responsible for collecting and serving usage statistics for shortened URLs. The infrastructure is implemented using the **AWS Cloud Development Kit (CDK)** in **Java**, enabling scalable, maintainable, and repeatable deployments on AWS.

## 🧩 Part of a Microservices-Based URL Shortener Platform

The **URL Stats Service** is one of several components that make up the larger **URL Shortener Platform**, a modern, microservices-based solution for URL shortening, tracking, and analytics. The complete platform includes:

- 🔗 [**URL Shortener Service**](https://github.com/dobrevd/url_shortener_service) — The core backend service for creating and resolving shortened URLs.
- 📈 [**URL Audit Service**](https://github.com/dobrevd/url-audit-service) — Responsible for logging and analyzing user interactions for auditing purposes.
- 🖥️ [**Frontend Application**](https://github.com/dobrevd/url-shortener-frontend) — A user-friendly web interface for interacting with the system.
- 📊 **URL Stats Service** — A microservice (currently under development) for providing real-time and historical statistics on URL usage.

## 🚀 Key Features

- Infrastructure defined entirely in Java using AWS CDK
- Designed for scalability and high availability on AWS
- Easily extendable as the Stats Service evolves
- Aligns with modern DevOps practices and microservices architecture

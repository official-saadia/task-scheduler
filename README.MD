# Task Scheduler

A production-grade task scheduling system built with Java and Spring Boot. Designed to reliably execute, track, and manage scheduled tasks with built-in retry logic, dead letter queue support, and analytics.

> Currently supports email notifications as a task type, with extensibility for additional task types in future releases.

---

## Functional Requirements

1. Users can create scheduled tasks with configurable execution times
2. System currently supports email notifications as a task type, with extensibility for additional task types in future releases
3. System supports cron expressions for task scheduling and recurrence patterns
4. Users can create and manage reusable email templates with dynamic placeholders
5. System automatically retries failed tasks up to three times with configurable delays
6. Tasks exhausting all retry attempts are moved to a Dead Letter Queue for manual analysis
7. System maintains a complete audit log of all notification attempts including recipient, status, and timestamps
8. System provides analytics showing total tasks, notifications sent, success rate, and failure breakdown

---

## Non Functional Requirements

1. System supports configurable thread pool sizing for concurrent task execution
2. System ensures thread safety and prevents race conditions during concurrent executions

---

## Tech Stack

- **Language:** Java 25
- **Framework:** Spring Boot 4
- **Database:** TBD
- **Build Tool:** Maven

---

## Architecture

### Architecture Diagram

![Task Scheduler Architecture Diagram](../../../../Downloads/task_scheduler_architecture_final.svg"/>


## ERD
![Task Scheduler ERD](../../../../Downloads/Task_Scheduler_ERD.drawio.png)
---

## Setup Instructions

> Coming soon — setup and run instructions will be added once the initial implementation is complete.

---

## Roadmap

- [x] Define functional and non-functional requirements
- [ ] Design ERD and system architecture
- [ ] Implement core task scheduling engine
- [ ] Implement email notification task type
- [ ] Add retry logic and Dead Letter Queue
- [ ] Add analytics endpoint
- [ ] Build React UI for task management and analytics
- [ ] Add database backup as a task type
- [ ] Add data cleanup as a task type
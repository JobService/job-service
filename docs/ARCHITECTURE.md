# Job Service Architecture

## Overview

The Job Service is a distributed system for orchestration, management, and monitoring of data processing jobs. It provides the ability to submit jobs, track their progress, manage dependencies between jobs, and control job execution through pause, resume, and cancellation operations.

**Version:** 9.2.0-SNAPSHOT  
**Primary Use Case:** Batch document processing  
**Technology Stack:** Java, PostgreSQL, RabbitMQ, Flyway, Dropwizard

---

## System Components

The Job Service is organized as a multi-module Maven project with the following key components:

### Core Service Components

1. **job-service-contract**
   - OpenAPI/Swagger specification defining the REST API contract
   - Contract-first approach for API design
   - Defines all request/response models, endpoints, and status codes

2. **job-service-core**
   - Core business logic and interfaces
   - Job type definitions and processing framework
   - Queue services abstraction

3. **job-service-db**
   - Database schema and migration scripts using Flyway
   - PostgreSQL stored procedures and functions for job operations
   - Database versioning and change management

4. **job-service-db-client**
   - Client library for database connectivity
   - Connection pooling and management

5. **job-service-dropwizard**
   - Main application entry point using Dropwizard framework
   - REST API implementation
   - Health checks and metrics

6. **job-service-container**
   - Docker containerization for the main Job Service
   - Deployment configuration and scripts

### Supporting Components

7. **job-service-scheduled-executor**
   - Polling service that identifies jobs ready to run
   - Handles jobs with delays after dependency completion
   - Publishes messages to RabbitMQ to trigger job execution
   - Configurable failure propagation

8. **worker-jobtracking**
   - Acts as a proxy for task messages
   - Reports job progress to the database
   - Monitors job completion to trigger dependent jobs
   - Handles job cancellation checks
   - Configurable failure propagation through dependent jobs

9. **worker-jobtracking-container**
   - Docker containerization for the Job Tracking Worker

### Client & Testing Components

10. **job-service-caller**
    - Python script for creating jobs and waiting for completion
    - Testing and integration utility

11. **job-service-acceptance-tests**
    - End-to-end integration tests
    - Acceptance test suites

12. **job-service-container-tests**
    - Container-level integration tests
    - REST API validation tests

---

## Architecture Style

The Job Service follows a **layered architecture** with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│                     REST API Layer                       │
│              (Dropwizard Controllers)                    │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                  Service/Business Logic                  │
│            (Job Orchestration, State Management)         │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                    Data Access Layer                     │
│          (PostgreSQL Functions & Procedures)             │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                   PostgreSQL Database                    │
│              (Jobs, Dependencies, Tasks)                 │
└─────────────────────────────────────────────────────────┘
```

---

## Communication Patterns

### Synchronous Communication
- **REST API**: Clients interact with the Job Service via RESTful HTTP endpoints
- **Database Queries**: Direct SQL queries for job status, listing, and metadata retrieval

### Asynchronous Communication
- **RabbitMQ**: Message-based task distribution to workers
- **Job Tracking Worker**: Proxy pattern for progress reporting
- **Scheduled Executor**: Polling pattern for dependency resolution

---

## Key Design Patterns

### 1. **Contract-First API Design**
The system uses OpenAPI/Swagger specifications to define the API contract before implementation, ensuring:
- Consistent API design
- Clear documentation
- Code generation capabilities

### 2. **Proxy Pattern (Job Tracking Worker)**
The Job Tracking Worker acts as a transparent proxy, intercepting task messages to:
- Report progress to the database
- Check for cancellation
- Forward messages to the intended worker

### 3. **Dependency Inversion**
Business logic depends on abstractions (interfaces) rather than concrete implementations, improving:
- Testability
- Flexibility
- Module independence

### 4. **Database-Centric Processing**
Complex operations like dependency resolution are implemented as PostgreSQL functions, providing:
- Transactional consistency
- Optimized query execution
- Reduced network overhead

---

## Scalability Considerations

### Horizontal Scaling
- Multiple Job Service instances can run behind a load balancer
- Worker instances can be scaled independently based on workload
- RabbitMQ provides message distribution across worker instances

### Partitioning
- Jobs are partitioned using a `partition_id` for multi-tenancy
- Each partition can be isolated for security and performance
- Queries are optimized per partition

### Database Performance
- Indexes on job_id, partition_id, and status fields
- Optimistic locking using version fields
- Connection pooling for efficient resource utilization

---

## High Availability

### Fault Tolerance
- Job state is persisted in PostgreSQL (ACID guarantees)
- Workers can be restarted without losing job state
- RabbitMQ message persistence ensures task delivery

### Health Checks
- Service health endpoints for monitoring
- Database connectivity checks
- Queue availability validation

---

## Security Considerations

### Data Isolation
- Partition-based multi-tenancy
- Jobs cannot access other partitions' data

### Correlation IDs
- Request tracing across services using `CAF-Correlation-Id` header
- Audit trail for debugging and compliance

### Input Validation
- Pattern-based validation on job IDs and partition IDs
- Protection against SQL injection through parameterized queries

---

## Deployment Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                       Load Balancer                           │
└──────────────────────────────────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        ▼                  ▼                  ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│ Job Service   │  │ Job Service   │  │ Job Service   │
│  Instance 1   │  │  Instance 2   │  │  Instance N   │
└───────────────┘  └───────────────┘  └───────────────┘
        │                  │                  │
        └──────────────────┴──────────────────┘
                           │
        ┌──────────────────┴──────────────────┐
        ▼                                      ▼
┌───────────────────────┐          ┌─────────────────────┐
│   PostgreSQL DB       │          │     RabbitMQ        │
│   (Primary/Replica)   │          │   (Clustered)       │
└───────────────────────┘          └─────────────────────┘
                                              │
                ┌─────────────────────────────┴─────────────────┐
                ▼                             ▼                 ▼
        ┌──────────────┐            ┌──────────────┐   ┌──────────────┐
        │  Job Tracking│            │ Scheduled    │   │   Worker     │
        │    Worker    │            │  Executor    │   │  Instances   │
        └──────────────┘            └──────────────┘   └──────────────┘
```

---

## Technology Stack Details

### Backend Framework
- **Dropwizard**: REST framework with embedded Jetty
- **Jackson**: JSON serialization/deserialization
- **Logback**: Logging framework

### Database
- **PostgreSQL 11+**: Required minimum version
- **Flyway**: Database migration and versioning
- **PL/pgSQL**: Stored procedures for complex operations

### Messaging
- **RabbitMQ**: Message broker for asynchronous task distribution
- **CAF Worker Framework**: Foundation for worker implementation

### Build & Deployment
- **Maven**: Build automation and dependency management
- **Docker**: Containerization for deployment
- **Docker Compose**: Multi-container orchestration

---

## Module Dependencies

Key dependency relationships between modules:

```
job-service-dropwizard
    ├── job-service-contract (API specification)
    ├── job-service-core (business logic)
    ├── job-service-db-client (database access)
    └── job-service-config (configuration)

worker-jobtracking
    ├── worker-jobtracking-shared (public interfaces)
    └── job-service-db-client (database access)

job-service-scheduled-executor
    ├── job-service-core (queue services)
    └── job-service-db-client (database access)
```

---

## Future Considerations

### Potential Enhancements
- Event-driven architecture with event sourcing
- GraphQL API for flexible querying
- Job execution history and analytics
- Advanced scheduling (cron-like expressions)
- Job templates and reusable definitions
- Distributed tracing integration (OpenTelemetry)

### Performance Optimizations
- Caching layer for frequently accessed job data
- Read replicas for query scaling
- Materialized views for complex reporting

---

## Related Documentation

- [Job Lifecycle](./JOB-LIFECYCLE.md) - Detailed job state transitions
- [Dependency Management](./DEPENDENCY-MANAGEMENT.md) - How job dependencies work
- [API Reference](./API-REFERENCE.md) - REST API documentation
- [Database Schema](./DATABASE-SCHEMA.md) - Table structure and relationships
- [Known Issues](./KNOWN-ISSUES.md) - Current limitations and workarounds


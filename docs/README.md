# Job Service Documentation

> **Comprehensive technical documentation for the Job Service**  
> Version: 9.2.0-SNAPSHOT  
> Last Updated: November 3, 2025

---

## 📚 Overview

The **Job Service** is a distributed system for **orchestration, management, and monitoring** of batch data processing jobs. It provides capabilities for job submission, progress tracking, dependency management, and execution control.

**Key Features:**
- ✅ RESTful API for job management
- ✅ Complex job dependency graphs (prerequisites)
- ✅ Job scheduling with delays
- ✅ Progress tracking and reporting
- ✅ Job lifecycle control (pause, resume, cancel)
- ✅ Multi-tenant isolation via partitions
- ✅ Distributed worker execution via RabbitMQ
- ✅ PostgreSQL-backed persistence with ACID guarantees

**Primary Use Case:** Batch document processing workflows with interdependent stages.

---

## 🗂️ Documentation Index

### Getting Started
- **[Architecture Overview](./ARCHITECTURE.md)** - System design, components, and communication patterns
- **[Database Schema](./DATABASE-SCHEMA.md)** - Complete database structure, tables, and functions
- **[Job Lifecycle](./JOB-LIFECYCLE.md)** - Job states, transitions, and lifecycle management

### Deep Dives
- **[Dependency Management](./DEPENDENCY-MANAGEMENT.md)** - How job dependencies work, resolution logic, and patterns
- **[Known Issues](./KNOWN-ISSUES.md)** - Current limitations, workarounds, and proposed solutions

### Reference
- **[API Reference](https://jobservice.github.io/job-service/)** - REST API specification (OpenAPI/Swagger)
- **[Database Functions Reference](./DATABASE-SCHEMA.md#database-functions--procedures)** - PostgreSQL function signatures

---

## 🚀 Quick Start

### Prerequisites
- **Java 11+**
- **PostgreSQL 11+**
- **RabbitMQ 3.8+**
- **Maven 3.6+**
- **Docker** (optional, for containerized deployment)

### Building from Source

```bash
# Clone repository
git clone https://github.com/JobService/job-service.git
cd job-service

# Build all modules
mvn clean install

# Build specific module
cd job-service-container
mvn clean package
```

### Database Setup

```bash
# Option 1: Using Docker
docker pull jobservice/job-service-postgres
docker run --rm jobservice/job-service-postgres \
  ./install_job_service_db.sh \
  -db.host localhost \
  -db.port 5432 \
  -db.name jobservice \
  -db.user postgres \
  -db.pass root

# Option 2: Using Flyway JAR
java -cp "*:classpath" com.github.cafapi.util.flywayinstaller.Application \
  -db.host localhost \
  -db.port 5432 \
  -db.name jobservice \
  -db.user postgres \
  -db.pass root
```

### Running the Service

```bash
# Run Job Service container
docker run -p 8080:8080 \
  -e CAF_RABBITMQ_HOST=rabbitmq \
  -e CAF_DATABASE_URL=jdbc:postgresql://postgres:5432/jobservice \
  -e CAF_DATABASE_USERNAME=postgres \
  -e CAF_DATABASE_PASSWORD=root \
  jobservice/job-service

# Run Job Tracking Worker
docker run \
  -e CAF_RABBITMQ_HOST=rabbitmq \
  -e CAF_DATABASE_URL=jdbc:postgresql://postgres:5432/jobservice \
  -e CAF_JOB_TRACKING_PROPAGATE_FAILURES=true \
  jobservice/worker-jobtracking

# Run Scheduled Executor
docker run \
  -e CAF_RABBITMQ_HOST=rabbitmq \
  -e CAF_DATABASE_URL=jdbc:postgresql://postgres:5432/jobservice \
  -e CAF_JOB_SCHEDULER_PROPAGATE_FAILURES=true \
  jobservice/job-service-scheduled-executor
```

### Creating Your First Job

```bash
# Simple job (no dependencies)
curl -X PUT http://localhost:8080/job-service/v1/partitions/default/jobs/job-1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Process Documents",
    "description": "Ingests and processes documents",
    "task": {
      "taskClassifier": "DocumentWorker",
      "taskApiVersion": 1,
      "taskData": {
        "action": "process",
        "files": ["doc1.pdf", "doc2.pdf"]
      },
      "taskPipe": "document-input",
      "targetPipe": "document-output"
    },
    "labels": {
      "owner": "alice",
      "project": "batch-001"
    }
  }'

# Job with dependencies
curl -X PUT http://localhost:8080/job-service/v1/partitions/default/jobs/job-2 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Archive Processed Documents",
    "description": "Archives documents after processing completes",
    "prerequisiteJobIds": ["job-1"],
    "delay": 60,
    "task": {
      "taskClassifier": "ArchiveWorker",
      "taskApiVersion": 1,
      "taskData": {
        "action": "archive",
        "destination": "s3://archive-bucket"
      },
      "taskPipe": "archive-input",
      "targetPipe": "archive-output"
    }
  }'
```

### Checking Job Status

```bash
# Get specific job
curl http://localhost:8080/job-service/v1/partitions/default/jobs/job-1

# List all jobs
curl http://localhost:8080/job-service/v1/partitions/default/jobs

# Filter by status
curl "http://localhost:8080/job-service/v1/partitions/default/jobs?status=Active"

# Get job count
curl http://localhost:8080/job-service/v1/partitions/default/jobs:count
```

---

## 🏗️ Architecture at a Glance

```
┌─────────────┐
│   Clients   │ (REST API calls)
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────┐
│         Job Service (Dropwizard)        │
│  ┌─────────────────────────────────┐   │
│  │  REST Controllers               │   │
│  │  ├─ Job Management              │   │
│  │  ├─ Status Queries              │   │
│  │  └─ Lifecycle Operations        │   │
│  └─────────────────────────────────┘   │
│  ┌─────────────────────────────────┐   │
│  │  Business Logic                 │   │
│  │  ├─ Job Orchestration           │   │
│  │  ├─ Dependency Validation       │   │
│  │  └─ State Management            │   │
│  └─────────────────────────────────┘   │
└──────┬──────────────────────┬───────────┘
       │                      │
       ▼                      ▼
┌──────────────┐      ┌──────────────────┐
│  PostgreSQL  │      │    RabbitMQ      │
│  (Job State) │      │  (Task Queue)    │
└──────────────┘      └────────┬─────────┘
                               │
           ┌───────────────────┼───────────────────┐
           │                   │                   │
           ▼                   ▼                   ▼
    ┌─────────────┐    ┌──────────────┐   ┌──────────────┐
    │ Job Tracking│    │  Scheduled   │   │   Worker     │
    │   Worker    │    │  Executor    │   │  Instances   │
    └─────────────┘    └──────────────┘   └──────────────┘
```

**Key Components:**
1. **Job Service** - REST API and orchestration
2. **PostgreSQL** - Persistent job state and dependency graph
3. **RabbitMQ** - Task distribution to workers
4. **Job Tracking Worker** - Progress reporting proxy
5. **Scheduled Executor** - Dependency resolution poller
6. **Workers** - Actual task execution (customer-provided)

---

## 📊 Job States

Jobs progress through well-defined states:

```
Waiting → Active → Completed
   │         │
   │         └─> Failed
   │         └─> Paused → Active
   │
   └──────────> Cancelled
```

**States:**
- **Waiting**: New job waiting for dependencies or delay
- **Active**: Currently executing
- **Paused**: Temporarily suspended by user
- **Completed**: Successfully finished ✅
- **Failed**: Encountered an error ❌
- **Cancelled**: Terminated by user 🚫

See [Job Lifecycle](./JOB-LIFECYCLE.md) for detailed state transition rules.

---

## 🔗 Dependency Management

Jobs can specify **prerequisite jobs** that must complete before execution:

```
Job A (Ingestion)
  │
  ├─> Job B (Processing) [depends on A]
  │     │
  │     └─> Job D (Archive) [depends on B]
  │
  └─> Job C (Validation) [depends on A]
```

**Key Features:**
- Multiple prerequisites (AND logic)
- Recursive dependency resolution
- Optional delays after prerequisites complete
- Configurable failure propagation

**Example:**
```json
{
  "name": "Process Stage 2",
  "prerequisiteJobIds": ["stage1-job-a", "stage1-job-b"],
  "delay": 300,
  "task": { ... }
}
```

See [Dependency Management](./DEPENDENCY-MANAGEMENT.md) for complete details.

---

## ⚠️ Important Considerations

### Critical Issue: Job Cancellation and Dependencies

**When a job is cancelled while running, dependent jobs are NOT automatically updated.**

**Scenario:**
1. Job A is running
2. Job B depends on Job A
3. Job A is cancelled
4. **Result:** Job B remains stuck in `Waiting` state indefinitely

**Workarounds:**
- Enable failure propagation: `CAF_JOB_TRACKING_PROPAGATE_FAILURES=true`
- Manually cancel dependent jobs
- Never reuse job IDs after cancellation

See [Known Issues](./KNOWN-ISSUES.md#1-️-dependent-jobs-not-updated-when-parent-job-is-cancelled-and-recreated) for detailed analysis and proposed solutions.

---

## 🔧 Configuration

### Job Service Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `CAF_DATABASE_URL` | PostgreSQL JDBC URL | Required |
| `CAF_DATABASE_USERNAME` | Database username | Required |
| `CAF_DATABASE_PASSWORD` | Database password | Required |
| `CAF_RABBITMQ_HOST` | RabbitMQ hostname | `localhost` |
| `CAF_RABBITMQ_PORT` | RabbitMQ port | `5672` |
| `CAF_RABBITMQ_USERNAME` | RabbitMQ username | `guest` |
| `CAF_RABBITMQ_PASSWORD` | RabbitMQ password | `guest` |

### Worker Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `CAF_JOB_TRACKING_PROPAGATE_FAILURES` | Cascade failures to dependent jobs | `false` |
| `CAF_JOB_SCHEDULER_PROPAGATE_FAILURES` | Cascade failures (Scheduled Executor) | `false` |

**Recommendation:** Set both to `true` in production to prevent jobs from waiting indefinitely on failed prerequisites.

---

## 🧪 Testing

### Running Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify -P integration-test

# Acceptance tests
cd job-service-acceptance-tests
mvn verify
```

### Test Coverage

The project includes:
- Unit tests for business logic
- Integration tests for database operations
- Container tests for REST API
- Acceptance tests for end-to-end workflows

---

## 📖 API Examples

### Create a Job

```http
PUT /job-service/v1/partitions/default/jobs/{jobId}
Content-Type: application/json

{
  "name": "Job Name",
  "description": "Job description",
  "task": {
    "taskClassifier": "WorkerType",
    "taskApiVersion": 1,
    "taskData": { "key": "value" },
    "taskPipe": "input-queue",
    "targetPipe": "output-queue"
  },
  "prerequisiteJobIds": ["prerequisite-job-1"],
  "delay": 60,
  "labels": {
    "owner": "user",
    "environment": "production"
  }
}
```

### Get Job Status

```http
GET /job-service/v1/partitions/default/jobs/{jobId}/status
```

Response:
```json
"Waiting" | "Active" | "Completed" | "Failed" | "Cancelled" | "Paused"
```

### Cancel a Job

```http
POST /job-service/v1/partitions/default/jobs/{jobId}/cancel
```

### List Jobs with Filtering

```http
GET /job-service/v1/partitions/default/jobs?status=Active&limit=100&offset=0
```

### Bulk Operations

```http
# Cancel multiple jobs
POST /job-service/v1/partitions/default/jobs:cancel?jobIdStartsWith=batch-2024

# Delete multiple jobs
POST /job-service/v1/partitions/default/jobs:delete?status=Completed
```

---

## 🐛 Troubleshooting

### Jobs Stuck in Waiting State

**Symptoms:** Jobs remain in `Waiting` status for extended periods.

**Possible Causes:**
1. Prerequisite job failed (propagation disabled)
2. Prerequisite job cancelled
3. Circular dependency
4. Delay not yet expired

**Solutions:**
1. Check prerequisite job status:
   ```sql
   SELECT jd.dependent_job_id, j.status
   FROM job_dependency jd
   INNER JOIN job j ON j.partition_id = jd.partition_id 
                   AND j.job_id = jd.dependent_job_id
   WHERE jd.job_id = 'stuck-job-id';
   ```

2. Enable failure propagation in configuration

3. Check for circular dependencies:
   ```sql
   -- Query dependency graph
   SELECT * FROM job_dependency 
   WHERE partition_id = 'default';
   ```

4. Verify `eligible_to_run_date`:
   ```sql
   SELECT job_id, eligible_to_run_date 
   FROM job_task_data
   WHERE job_id = 'stuck-job-id';
   ```

### Jobs Not Progressing

**Symptoms:** Jobs stuck in `Active` with no progress updates.

**Possible Causes:**
1. Worker crashed or stopped
2. RabbitMQ connection issue
3. Database connectivity problem

**Solutions:**
1. Check worker logs
2. Verify RabbitMQ queue depth
3. Check Job Tracking Worker health
4. Query stale jobs:
   ```sql
   SELECT * FROM job
   WHERE status = 'Active'
     AND last_update_date < now() - interval '30 minutes';
   ```

### Database Performance Issues

**Symptoms:** Slow API responses, timeouts.

**Solutions:**
1. Check for missing indexes
2. Analyze query plans
3. Monitor connection pool
4. Partition large job tables by date

---

## 📚 Additional Resources

### Official Documentation
- [Project Website](https://jobservice.github.io/job-service/)
- [GitHub Repository](https://github.com/JobService/job-service)

### Module-Specific READMEs
- [Job Service Container](../job-service-container/README.md)
- [Job Service Database](../job-service-db/README.md)
- [Job Tracking Worker](../worker-jobtracking/README.md)
- [Job Service Caller](../job-service-caller/README.md)
- [Scheduled Executor](../job-service-scheduled-executor/README.md)

### Developer Guides
- [Getting Started Guide](https://jobservice.github.io/job-service/pages/en-us/Getting-Started)
- [Feature Testing Guide](../testcases/README.md)

---

## 🤝 Contributing

### Reporting Issues

If you encounter issues:
1. Check [Known Issues](./KNOWN-ISSUES.md)
2. Search existing GitHub issues
3. Collect diagnostic information:
   - Job IDs
   - Database logs
   - API request/response
   - Worker logs
4. Create a new issue with reproduction steps

### Development Guidelines

- Use Flyway for database migrations
- Follow contract-first approach for APIs
- Write comprehensive tests
- Document complex logic
- Keep lines under 120 characters

---

## 👥 Maintainers

- Andy Reid (Belfast, UK)
- Dermot Hardy (Belfast, UK)
- Anthony McGreevy (Belfast, UK)
- Thilagavathi Santhoshkumar (Belfast, UK)
- Michael Bryson (Belfast, UK)
- Rahul Kulkarni (Chicago, USA)
- Kusuma Ghosh Dastidar (Pleasanton, USA)

---

## 📄 License

Copyright 2016-2025 Open Text.

Licensed under the Apache License, Version 2.0.  
See [LICENSE](../LICENSE) file for details.

---

## 📝 Version History

- **9.2.0-SNAPSHOT** (Current)
  - Enhanced documentation
  - Identified dependency management issues
  - See [release-notes-9.2.0.md](../release-notes-9.2.0.md)

- Previous versions: See individual release notes

---

**Last Updated:** November 3, 2025  
**Documentation Version:** 1.0


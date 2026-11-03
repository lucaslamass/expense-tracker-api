# Expense Tracker API

[![CI](https://github.com/lucaslamass/expense-tracker-api/actions/workflows/ci.yml/badge.svg)](https://github.com/lucaslamass/expense-tracker-api/actions/workflows/ci.yml)

REST API for personal expense tracking, built with **Java 21** and **Spring Boot 4**.
Each user can create, list, update and delete their own expenses, and get a summary of their spending by category.

The project was developed following **Test-Driven Development (TDD)**: every endpoint is covered by integration tests.

## Features

- Full CRUD for expenses (`GET`, `POST`, `PUT`, `DELETE`)
- Pagination and sorting (`?page=0&size=10&sort=amount,desc`), newest expenses first by default
- Spending summary: total and breakdown by category
- Authentication with **Spring Security** (HTTP Basic) and BCrypt-hashed passwords
- Ownership checks: users can only see and modify their own expenses
- Input validation with clear error messages (`400 Bad Request`)
- Money handled with `BigDecimal` to avoid rounding errors
- Continuous integration with **GitHub Actions**

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4 (Web MVC, Data JDBC, Security, Validation) |
| Database | H2 (in-memory) |
| Testing | JUnit 5, AssertJ, Spring Boot Test, JsonPath |
| Build | Gradle |
| CI | GitHub Actions |

## Endpoints

All endpoints require authentication.

| Method | Endpoint | Description | Success |
|---|---|---|---|
| `GET` | `/expenses` | List the user's expenses (paginated) | `200 OK` |
| `GET` | `/expenses/{id}` | Get one expense | `200 OK` |
| `GET` | `/expenses/summary` | Total and totals by category | `200 OK` |
| `POST` | `/expenses` | Create an expense | `201 Created` + `Location` header |
| `PUT` | `/expenses/{id}` | Update an expense | `204 No Content` |
| `DELETE` | `/expenses/{id}` | Delete an expense | `204 No Content` |

Error responses: `400` invalid data, `401` bad credentials, `403` user without permission, `404` expense not found or owned by another user.

### Expense format

```json
{
  "id": 1,
  "description": "Supermarket",
  "amount": 45.30,
  "date": "2026-11-03",
  "category": "Food",
  "owner": "lucas"
}
```

`id` and `owner` are set by the server.

### Summary format

```json
{
  "total": 207.30,
  "byCategory": {
    "Education": 150.00,
    "Food": 45.30,
    "Leisure": 12.00
  }
}
```

## Running locally

Requirements: **JDK 21**.

```bash
# Run the application (http://localhost:8080)
./gradlew bootRun

# Run the tests
./gradlew test
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

### Demo users

| User | Password | Role |
|---|---|---|
| `lucas` | `abc123` | Expense owner |
| `ana` | `xyz789` | Expense owner |
| `noowner` | `qrs456` | No access to expenses |

### Example requests

```bash
# Create an expense
curl -u lucas:abc123 -X POST http://localhost:8080/expenses \
  -H "Content-Type: application/json" \
  -d '{"description":"Coffee","amount":1.80,"date":"2026-11-04","category":"Leisure"}'

# List expenses, most expensive first
curl -u lucas:abc123 "http://localhost:8080/expenses?sort=amount,desc"

# Get the summary
curl -u lucas:abc123 http://localhost:8080/expenses/summary
```

## Project structure

```
src/main/java/expense_tracker_api
├── Expense.java                 # Domain model (record) with validation rules
├── ExpenseController.java       # REST endpoints
├── ExpenseRepository.java       # Spring Data JDBC repository
├── ExpenseSummary.java          # Summary response
├── GlobalExceptionHandler.java  # Error responses for invalid input
└── SecurityConfig.java          # Authentication and authorization
```

## Next steps

- Persistent database (PostgreSQL) instead of in-memory H2
- User registration and JWT authentication
- Filters by category and date range
- API documentation with OpenAPI / Swagger
- Docker image and cloud deployment

## Author

**Lucas González Lamas** · Computer Engineering student at Universidade de Vigo
[LinkedIn](https://www.linkedin.com/in/lucasgonzalezlamas)

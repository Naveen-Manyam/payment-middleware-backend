# PhonePe MiddleWare

A comprehensive Spring Boot middleware application for integrating with PhonePe payment services, supporting multiple payment methods including Dynamic QR (DQR), Static QR, Electronic Data Capture (EDC), and Payment Links.

## 🚀 Features

- **Multi-Payment Support**: DQR, Static QR, EDC, Payment Links
- **Robust Error Handling**: Comprehensive exception tracking and logging
- **Retry Mechanism**: Automatic retry for failed API calls with exponential backoff
- **Structured Logging**: JSON-structured logs with correlation IDs
- **Database Integration**: MySQL with JPA/Hibernate
- **API Security**: X-VERIFY signature validation
- **Swagger Documentation**: Interactive API documentation
- **Production Ready**: Comprehensive configuration and monitoring

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Configuration](#configuration)
- [Database Schema](#database-schema)
- [Error Handling](#error-handling)
- [Monitoring & Logging](#monitoring--logging)
- [Deployment](#deployment)
- [Contributing](#contributing)

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Controllers   │    │    Services      │    │   Repositories  │
│                 │    │                  │    │                 │
│ - DQR           │───▶│ - DQR Service    │───▶│ - JPA Repos     │
│ - Static QR     │    │ - Static Service │    │ - MySQL DB      │
│ - EDC           │    │ - EDC Service    │    │                 │
│ - Payment Link  │    │ - Payment Service│    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │
         └───────────────────────┼─────────────────┐
                                 │                 │
                    ┌──────────────────┐  ┌─────────────────┐
                    │   Utilities      │  │  PhonePe APIs   │
                    │                  │  │                 │
                    │ - WebClient      │  │ - Payment Init  │
                    │ - Logging        │  │ - Status Check  │
                    │ - Exception      │  │ - Metadata      │
                    │ - Crypto Utils   │  │ - Callbacks     │
                    └──────────────────┘  └─────────────────┘
```

## 🚀 Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **MySQL 8.0+**
- **PhonePe API Credentials**

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Phonepe-MiddleWare
   ```

2. **Configure Database**
   ```sql
   CREATE DATABASE naveendb;
   ```

3. **Update Configuration**
   ```yaml
   # src/main/resources/application.yml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/naveendb
       username: your_username
       password: your_password
   ```

4. **Set PhonePe Credentials**
   ```yaml
   dqr:
     phonepe:
       saltKey: your_salt_key
       saltIndex: 1
       baseUrl: https://mercury-uat.phonepe.com/enterprise-sandbox
   ```

5. **Build and Run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

6. **Access Application**
   - Application: http://localhost:8081
   - Swagger UI: http://localhost:8081/swagger-ui.html

## 🔌 API Endpoints

### DQR (Dynamic QR) APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/phonepe/dqr/transaction/init` | Initialize DQR transaction |
| POST | `/api/phonepe/dqr/transaction/cancel` | Cancel DQR transaction |
| POST | `/api/phonepe/dqr/transaction/refund` | Refund DQR transaction |
| POST | `/api/phonepe/dqr/transaction/status` | Check DQR transaction status |
| POST | `/api/phonepe/dqr/s2s-callback` | Handle S2S callbacks |

### Static QR APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/phonepe/static-qr/transaction/list` | Get transaction list |
| POST | `/api/phonepe/static-qr/transaction/metadata` | Get transaction metadata |
| POST | `/api/phonepe/static-qr/s2s-callback` | Handle S2S callbacks |

### EDC (Electronic Data Capture) APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/phonepe/edc/transaction/init` | Initialize EDC transaction |
| POST | `/api/phonepe/edc/transaction/status` | Check EDC transaction status |

### Payment Link APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/phonepe/payment-link/init` | Create payment link |

## ⚙️ Configuration

### Application Properties

```yaml
# Server Configuration
server:
  port: 8081

# Database Configuration
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/naveendb
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update

# PhonePe API Configuration
phonepe:
  api:
    connection:
      timeout: 30000      # Connection timeout (ms)
    read:
      timeout: 30000      # Read timeout (ms)
    write:
      timeout: 30000      # Write timeout (ms)
    retry:
      max-attempts: 3     # Maximum retry attempts
      initial-delay: 1000 # Initial retry delay (ms)

# Service-Specific Configuration
dqr:
  phonepe:
    saltKey: your_salt_key
    saltIndex: 1
    baseUrl: https://mercury-uat.phonepe.com/enterprise-sandbox
    endpoint: /v3/qr/init
    # ... other endpoints

static:
  phonepe:
    saltKey: your_salt_key
    saltIndex: 1
    baseUrl: https://mercury-uat.phonepe.com/enterprise-sandbox
    endpoint: /v3/qr/transaction/list
    metadataEndpoint: /v1/merchant/transaction/metadata
```

## 🗄️ Database Schema

The application uses multiple entities to store transaction data:

### Core Entities
- **DqrInitializeTransactionRequest/Response**: DQR transaction initialization
- **DqrCancelTransactionRequest/Response**: DQR cancellation
- **DqrRefundTransactionRequest/Response**: DQR refunds
- **DqrCheckTransactionStatusRequest/Response**: Status checks
- **StaticQRTransactionListRequest/Response**: Static QR transactions
- **StaticQRMetadataRequest/Response**: Transaction metadata
- **EdcInitializeTransactionRequest/Response**: EDC transactions
- **PaymentLinkRequest/Response**: Payment link creation
- **ExceptionTrackResponse**: Exception tracking

### Entity Relationships
```sql
-- Example: StaticQR Metadata with nested transactions
StaticQRMetadataResponse (1) ──── (1) MetadataResponseData
                                        │
                                        │ (1)
                                        ▼ (many)
                                  MetadataTransaction
                                        │
                                        │ (1)
                                        ▼ (many)
                                  PaymentMode
```

## 🛡️ Error Handling

### Exception Hierarchy
- **DqrApiException**: DQR-specific errors
- **StaticQRApiException**: Static QR errors
- **EdcApiException**: EDC-specific errors
- **PaymentLinkApiException**: Payment link errors

### Retry Mechanism
phonepe:
  api:
    retry:
      max-attempts: 3
      initial-delay: 1000

### Global Exception Handling
- **TrackExceptionService**: Logs all exceptions to database
- **StructuredLogger**: Provides correlation ID tracking
- **Response Codes**: Standardized error responses

## 📊 Monitoring & Logging

### Structured Logging
```java
StructuredLogger.forLogger(log)
    .operation("DQR_INIT")
    .transactionId(transactionId)
    .phonepeTransactionId(phonepeTransactionId)
    .responseTime(responseTime)
    .timestamp()
    .info("Transaction initialized successfully");
```

### Log Files
- **Application Logs**: `logs/phonepe-middleware.log`
- **Transaction Logs**: Structured JSON format
- **Error Logs**: Comprehensive error tracking

### Metrics
- API response times
- Success/failure rates
- Retry attempt statistics
- Database performance

## 🚀 Deployment

### Development
```bash
mvn spring-boot:run
```

### Production
```bash
# Build JAR
mvn clean package -DskipTests

# Run with production profile
java -jar target/phonepe-middleware.jar --spring.profiles.active=prod
```

### Docker Deployment
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/phonepe-middleware.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 🔧 Utility Classes

### Key Utilities
- **PhonePeWebClientFactory**: WebClient with timeout and retry configuration
- **GenerateXVerifyKey**: X-VERIFY signature generation
- **GenerateTransactionId**: Unique transaction ID generation
- **StructuredLogger**: Structured logging utility
- **CommonServiceUtils**: Common service operations

## 🧪 Testing

### Running Tests
```bash
# Unit tests
mvn test

# Integration tests
mvn integration-test

# All tests with coverage
mvn clean test jacoco:report
```

### API Testing
Use the provided Swagger UI at `/swagger-ui.html` or import the OpenAPI specification into Postman.

## 📚 Additional Documentation

- [Entity Documentation](docs/ENTITIES.md)
- [Service Layer Documentation](docs/SERVICES.md)
- [Controller Documentation](docs/CONTROLLERS.md)
- [Utility Documentation](docs/UTILITIES.md)
- [Configuration Guide](docs/CONFIGURATION.md)
- [API Examples](docs/API_EXAMPLES.md)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation in the `docs/` folder

---

**Version**: 1.0.0
**Last Updated**: $(date)
**Maintainer**: Development Team
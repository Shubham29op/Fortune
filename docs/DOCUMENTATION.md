# HSBC Fortune - Portfolio Management System Documentation

## Table of Contents
1. [System Overview](#system-overview)
2. [Architecture Diagram](#architecture-diagram)
3. [ER Diagram](#er-diagram)
4. [User Flow Diagram](#user-flow-diagram)
5. [Flowchart](#flowchart)
6. [Client Interaction Diagram](#client-interaction-diagram)
7. [API Endpoints](#api-endpoints)
8. [Database Schema](#database-schema)

---

## System Overview

The Portfolio Management System is a multi-asset portfolio management platform designed for portfolio managers to manage multiple customer portfolios. The system supports various asset types including Equity, Debt, Gold, Real Estate, Crypto, Cash, and Mutual Funds.

**Key Features:**
- Multi-customer portfolio management
- Multi-asset support
- Real-time portfolio tracking
- Transaction history management
- GDPR compliant data handling
- Comprehensive visualizations

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                             │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │         Portfolio Dashboard (Frontend)                    │   │
│  │  - HTML/CSS/JavaScript                                    │   │
│  │  - Canvas-based Charts                                    │   │
│  │  - API Integration (api.js)                              │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP/REST API
                              │
┌─────────────────────────────────────────────────────────────────┐
│                      APPLICATION LAYER                          │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              REST Controllers                            │   │
│  │  - CustomerController                                    │   │
│  │  - PortfolioController                                   │   │
│  │  - DashboardController                                   │   │
│  │  - TransactionController                                 │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Service Layer                                │   │
│  │  - CustomerService                                        │   │
│  │  - PortfolioService                                       │   │
│  │  - DashboardService                                       │   │
│  │  - TransactionService                                     │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Repository Layer                             │   │
│  │  - CustomerRepository                                     │   │
│  │  - PortfolioRepository                                    │   │
│  │  - HoldingRepository                                      │   │
│  │  - TransactionRepository                                  │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ JPA/Hibernate
                              │
┌─────────────────────────────────────────────────────────────────┐
│                       DATA LAYER                                │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              MySQL Database                               │   │
│  │  - customers table                                        │   │
│  │  - portfolios table                                      │   │
│  │  - holdings table                                         │   │
│  │  - transactions table                                     │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    SUPPORTING COMPONENTS                         │
│  - Swagger/OpenAPI Documentation                                │
│  - CORS Configuration                                            │
│  - Data Initialization Service                                  │
│  - GDPR Compliance Layer                                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## ER Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         CUSTOMERS                              │
├─────────────────────────────────────────────────────────────────┤
│ PK │ id                    BIGINT                               │
│    │ client_id            VARCHAR(10) UNIQUE                    │
│    │ name                 VARCHAR(100)                          │
│    │ email                VARCHAR(50)                            │
│    │ type                 ENUM(HNW, REGULAR)                    │
│    │ risk_level           ENUM(LOW, MODERATE, HIGH)             │
│    │ join_date            TIMESTAMP                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ 1
                              │
                              │ has
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        PORTFOLIOS                               │
├─────────────────────────────────────────────────────────────────┤
│ PK │ id                    BIGINT                               │
│ FK │ customer_id           BIGINT ───────────┐                 │
│    │ created_at           TIMESTAMP          │                 │
│    │ (calculated)         total_invested      │                 │
│    │ (calculated)         total_current_value │                 │
│    │ (calculated)         total_gain          │                 │
│    │ (calculated)         total_returns       │                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ 1
                              │
                              │ contains
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         HOLDINGS                               │
├─────────────────────────────────────────────────────────────────┤
│ PK │ id                    BIGINT                               │
│ FK │ portfolio_id         BIGINT ───────────┐                  │
│    │ asset_name           VARCHAR(200)      │                  │
│    │ category              ENUM(EQUITY,      │                  │
│    │                        DEBT, GOLD,      │                  │
│    │                        REAL_ESTATE,     │                  │
│    │                        CRYPTO, CASH,    │                  │
│    │                        MUTUAL_FUND)     │                  │
│    │ quantity              DECIMAL(18,4)     │                  │
│    │ avg_price             DECIMAL(18,2)     │                  │
│    │ current_price         DECIMAL(18,2)     │                  │
│    │ invested_amount       DECIMAL(18,2)     │                  │
│    │ current_value         DECIMAL(18,2)     │                  │
│    │ (calculated)          gain               │                  │
│    │ (calculated)          returns            │                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      TRANSACTIONS                               │
├─────────────────────────────────────────────────────────────────┤
│ PK │ id                    BIGINT                               │
│    │ transaction_id        VARCHAR(50) UNIQUE                  │
│ FK │ customer_id           BIGINT ───────────┐                 │
│    │ type                  ENUM(BUY, SELL)    │                 │
│    │ asset                 VARCHAR(200)        │                 │
│    │ category              ENUM(EQUITY, ...)  │                 │
│    │ quantity              DECIMAL(18,4)      │                 │
│    │ price                 DECIMAL(18,2)      │                 │
│    │ amount                DECIMAL(18,2)      │                 │
│    │ status                ENUM(SUCCESS,       │                 │
│    │                        PENDING, FAILED)   │                 │
│    │ timestamp             TIMESTAMP          │                 │
└─────────────────────────────────────────────────────────────────┘

RELATIONSHIPS:
- Customer (1) ────< (M) Portfolio
- Portfolio (1) ────< (M) Holding
- Customer (1) ────< (M) Transaction
```

---

## User Flow Diagram

```
                    ┌─────────────────────┐
                    │   Portfolio Manager  │
                    │      (User)          │
                    └──────────┬──────────┘
                               │
                               │ 1. Access Dashboard
                               ▼
                    ┌─────────────────────┐
                    │   Login/Authentication│
                    │   (SSO - Future)     │
                    └──────────┬──────────┘
                               │
                               │ 2. Authenticated
                               ▼
                    ┌─────────────────────┐
                    │   Main Dashboard     │
                    │  - View Summary      │
                    │  - Select Customer   │
                    └──────────┬──────────┘
                               │
                ┌──────────────┼──────────────┐
                │              │              │
                ▼              ▼              ▼
    ┌─────────────────┐ ┌──────────────┐ ┌──────────────┐
    │  View Customers  │ │ View Portfolio│ │ View Trans.  │
    │     Page         │ │     Page      │ │    Page      │
    └─────────┬───────┘ └──────┬───────┘ └──────┬───────┘
              │                 │                 │
              │                 │                 │
              ▼                 ▼                 ▼
    ┌─────────────────┐ ┌──────────────┐ ┌──────────────┐
    │  Browse All      │ │ Select       │ │ Filter by   │
    │  Customers       │ │ Customer      │ │ Date/Status │
    │  Filter/Sort     │ │ from Dropdown│ │ View Details │
    └─────────────────┘ └──────┬───────┘ └──────────────┘
                                 │
                                 │ 3. Customer Selected
                                 ▼
                    ┌─────────────────────┐
                    │  Portfolio Details  │
                    │  - Holdings List    │
                    │  - Performance      │
                    │  - Charts           │
                    └──────────┬──────────┘
                               │
                ┌──────────────┼──────────────┐
                │              │              │
                ▼              ▼              ▼
    ┌─────────────────┐ ┌──────────────┐ ┌──────────────┐
    │  Add Holding     │ │ Remove       │ │ View Charts │
    │  - Fill Form     │ │ Holding      │ │ - Performance│
    │  - Submit        │ │ - Confirm    │ │ - Allocation │
    │  - API Call      │ │ - API Call   │ │ - Risk       │
    └─────────┬───────┘ └──────┬───────┘ └──────┬───────┘
              │                 │                 │
              │                 │                 │
              ▼                 ▼                 ▼
    ┌─────────────────┐ ┌──────────────┐ ┌──────────────┐
    │  Success         │ │ Success      │ │ Updated     │
    │  Notification    │ │ Notification  │ │ Visualizations│
    │  Refresh Data    │ │ Refresh Data  │ │             │
    └─────────────────┘ └──────────────┘ └──────────────┘
```

---

## Flowchart

### Main Application Flow

```
START
  │
  ▼
┌─────────────────────┐
│ Initialize App      │
│ - Load API Config    │
│ - Setup Navigation   │
└──────────┬───────────┘
           │
           ▼
┌─────────────────────┐
│ Load Dashboard      │
│ GET /api/dashboard/  │
│      summary        │
└──────────┬───────────┘
           │
           ├─── Success ────┐
           │                │
           ▼                ▼
┌─────────────────────┐ ┌─────────────────────┐
│ Display Dashboard   │ │ Show Error Message  │
│ - Summary Cards     │ │ Retry/Handle Error  │
│ - Charts            │ └─────────────────────┘
│ - Tables            │
└──────────┬───────────┘
           │
           ▼
┌─────────────────────┐
│ Load Customers      │
│ GET /api/customers   │
└──────────┬───────────┘
           │
           ├─── Success ────┐
           │                │
           ▼                ▼
┌─────────────────────┐ ┌─────────────────────┐
│ Populate Dropdown   │ │ Show Error          │
│ Select Customer     │ └─────────────────────┘
└──────────┬───────────┘
           │
           ▼
    ┌──────────────┐
    │ Customer     │
    │ Selected?    │
    └───┬──────┬───┘
        │ Yes  │ No
        │      │
        ▼      ▼
┌──────────────┐ ┌──────────────┐
│ Load         │ │ Show Message │
│ Portfolio    │ │ "Select      │
│ GET /api/    │ │  Customer"   │
│ portfolios/  │ └──────────────┘
│ customer/{id}│
└──────┬───────┘
       │
       ├─── Success ────┐
       │                │
       ▼                ▼
┌──────────────┐ ┌──────────────┐
│ Display      │ │ Show Error   │
│ Portfolio    │ │ Message      │
│ - Holdings  │ └──────────────┘
│ - Summary   │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ User Action? │
└───┬──────┬───┘
    │      │
    ▼      ▼
┌──────────────┐ ┌──────────────┐
│ Add Holding  │ │ Remove       │
│ - Open Modal │ │ Holding      │
│ - Fill Form  │ │ - Confirm    │
│ - Submit     │ │ - API Call   │
└──────┬───────┘ └──────┬───────┘
       │                │
       ▼                ▼
┌──────────────┐ ┌──────────────┐
│ POST /api/   │ │ DELETE /api/ │
│ portfolios/  │ │ portfolios/  │
│ customer/{id}│ │ holdings/{id}│
│ /holdings    │ └──────┬───────┘
└──────┬───────┘        │
       │                │
       ├─── Success ────┤
       │                │
       ▼                ▼
┌──────────────┐ ┌──────────────┐
│ Show Success │ │ Refresh      │
│ Notification │ │ Portfolio    │
│ Refresh Data │ │ Data         │
└──────┬───────┘ └──────┬───────┘
       │                │
       └────────┬───────┘
                │
                ▼
           CONTINUE
```

### Add Holding Flow

```
START (User clicks "Add Asset")
  │
  ▼
┌─────────────────────┐
│ Check Customer      │
│ Selected?           │
└───┬─────────────┬───┘
    │ Yes         │ No
    │             │
    ▼             ▼
┌─────────────┐ ┌─────────────────────┐
│ Open Modal  │ │ Show Error:         │
│ Display Form│ │ "Select Customer"   │
└──────┬──────┘ └─────────────────────┘
       │
       ▼
┌─────────────────────┐
│ User Fills Form     │
│ - Asset Name        │
│ - Category          │
│ - Quantity          │
│ - Avg Price         │
│ - Current Price     │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│ Validate Form       │
└───┬─────────────┬───┘
    │ Valid       │ Invalid
    │             │
    ▼             ▼
┌─────────────┐ ┌─────────────────────┐
│ Submit Form │ │ Show Validation     │
│ POST API    │ │ Errors              │
└──────┬──────┘ └─────────────────────┘
       │
       ├─── Success ────┐
       │                │
       ▼                ▼
┌─────────────┐ ┌─────────────────────┐
│ Show Success│ │ Show Error Message  │
│ Notification│ │ Log Error           │
│ Close Modal │ └─────────────────────┘
│ Refresh Data│
└──────┬──────┘
       │
       ▼
      END
```

---

## Client Interaction Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    PORTFOLIO MANAGER                            │
│                    (Frontend Client)                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP Requests
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    REST API ENDPOINTS                            │
│                    (Spring Boot Backend)                         │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Customer     │    │ Portfolio    │    │ Transaction  │
│ Controller   │    │ Controller    │    │ Controller   │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                    │
       ▼                   ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Customer     │    │ Portfolio    │    │ Transaction  │
│ Service      │    │ Service      │    │ Service      │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                    │
       └───────────────────┼────────────────────┘
                           │
                           ▼
                ┌──────────────────────┐
                │   Dashboard Service  │
                │   (Aggregates Data)  │
                └──────────┬───────────┘
                           │
                           ▼
                ┌──────────────────────┐
                │   Repository Layer   │
                │   (Data Access)       │
                └──────────┬───────────┘
                           │
                           ▼
                ┌──────────────────────┐
                │   MySQL Database     │
                │   (Persistent Store) │
                └──────────────────────┘

SEQUENCE EXAMPLE: View Portfolio
─────────────────────────────────────────────────────────────────
Portfolio Manager    Frontend        Backend API      Database
     │                  │                │               │
     │─── Click ───────>│                │               │
     │   Customer       │                │               │
     │                  │─── GET ───────>│               │
     │                  │  /api/        │               │
     │                  │  portfolios/   │               │
     │                  │  customer/{id} │               │
     │                  │                │─── Query ────>│
     │                  │                │               │
     │                  │                │<── Data ──────│
     │                  │                │               │
     │                  │<── Response ───│               │
     │                  │  JSON Data     │               │
     │<── Display ──────│                │               │
     │   Portfolio      │                │               │
```

### API Request/Response Flow

```
CLIENT REQUEST FLOW:
─────────────────────────────────────────────────────────────────
1. User Action (Click, Form Submit)
   │
   ▼
2. Frontend JavaScript (main.js)
   │
   ▼
3. API Service (api.js)
   │
   ▼
4. HTTP Request (fetch API)
   │
   ▼
5. REST Controller (Spring Boot)
   │
   ▼
6. Service Layer (Business Logic)
   │
   ▼
7. Repository Layer (Data Access)
   │
   ▼
8. Database Query (MySQL)
   │
   ▼
9. Response flows back through layers
   │
   ▼
10. Frontend updates UI
```

---

## API Endpoints

### Dashboard Endpoints

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/api/dashboard/summary` | Get dashboard summary statistics | DashboardSummaryDto |

### Customer Endpoints

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/api/customers` | Get all customers | List<CustomerSummaryDto> |
| GET | `/api/customers/{id}` | Get customer by ID | CustomerSummaryDto |
| DELETE | `/api/customers/{id}` | Delete customer (GDPR) | 204 No Content |

### Portfolio Endpoints

| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|--------------|----------|
| GET | `/api/portfolios/customer/{customerId}` | Get portfolio by customer | - | PortfolioDto |
| POST | `/api/portfolios/customer/{customerId}/holdings` | Add holding to portfolio | CreateHoldingRequest | HoldingDto |
| DELETE | `/api/portfolios/holdings/{holdingId}` | Remove holding | - | 204 No Content |

### Transaction Endpoints

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/api/transactions` | Get all transactions | List<TransactionDto> |
| GET | `/api/transactions/customer/{customerId}` | Get transactions by customer | List<TransactionDto> |

---

## Database Schema

### customers Table
```sql
CREATE TABLE customers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_id VARCHAR(10) UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(50),
    type ENUM('HNW', 'REGULAR'),
    risk_level ENUM('LOW', 'MODERATE', 'HIGH'),
    join_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### portfolios Table
```sql
CREATE TABLE portfolios (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);
```

### holdings Table
```sql
CREATE TABLE holdings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    portfolio_id BIGINT NOT NULL,
    asset_name VARCHAR(200) NOT NULL,
    category ENUM('EQUITY', 'DEBT', 'GOLD', 'REAL_ESTATE', 'CRYPTO', 'CASH', 'MUTUAL_FUND') NOT NULL,
    quantity DECIMAL(18,4) NOT NULL,
    avg_price DECIMAL(18,2) NOT NULL,
    current_price DECIMAL(18,2) NOT NULL,
    invested_amount DECIMAL(18,2) NOT NULL,
    current_value DECIMAL(18,2) NOT NULL,
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE
);
```

### transactions Table
```sql
CREATE TABLE transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transaction_id VARCHAR(50) UNIQUE NOT NULL,
    customer_id BIGINT NOT NULL,
    type ENUM('BUY', 'SELL') NOT NULL,
    asset VARCHAR(200) NOT NULL,
    category ENUM('EQUITY', 'DEBT', 'GOLD', 'REAL_ESTATE', 'CRYPTO', 'CASH', 'MUTUAL_FUND') NOT NULL,
    quantity DECIMAL(18,4) NOT NULL,
    price DECIMAL(18,2) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    status ENUM('SUCCESS', 'PENDING', 'FAILED') NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);
```

---

## GDPR Compliance Features

1. **Data Minimization**: DTOs only expose necessary fields
2. **Right to be Forgotten**: DELETE endpoint for customer removal
3. **Data Separation**: PII separated from financial data
4. **No PII in Logs**: Logging excludes sensitive customer information
5. **Access Control**: API endpoints designed for SSO integration

---

## Technology Stack

### Backend
- Java 21
- Spring Boot 3.5.10
- Spring Data JPA
- MySQL 8.0+
- Lombok
- Swagger/OpenAPI 3.0

### Frontend
- HTML5
- CSS3
- Vanilla JavaScript
- Canvas API (for charts)

---

## Team Information

**Team Name**: Fortune  
**Company**: HSBC  
**Project**: Portfolio Management System

---

## Version History

- **v1.0.0** - Initial release
  - Multi-asset portfolio management
  - Customer management
  - Transaction tracking
  - Dashboard visualizations
  - GDPR compliance

---

*Documentation last updated: 2026*

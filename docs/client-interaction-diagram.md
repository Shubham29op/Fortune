# Client Interaction Diagram

## System Interaction Overview

```mermaid
sequenceDiagram
    participant PM as Portfolio Manager
    participant FE as Frontend<br/>(JavaScript)
    participant API as REST API<br/>(Spring Boot)
    participant SVC as Service Layer
    participant REPO as Repository Layer
    participant DB as MySQL Database
    
    PM->>FE: Click "View Portfolio"
    FE->>API: GET /api/portfolios/customer/{id}
    API->>SVC: getPortfolioByCustomerId(id)
    SVC->>REPO: findByCustomerId(id)
    REPO->>DB: SELECT query
    DB-->>REPO: Portfolio data
    REPO-->>SVC: Portfolio entity
    SVC->>SVC: Calculate totals<br/>Convert to DTO
    SVC-->>API: PortfolioDto
    API-->>FE: JSON Response
    FE->>FE: Update UI<br/>Display holdings
    FE-->>PM: Show portfolio details
```

## Add Holding Sequence

```mermaid
sequenceDiagram
    participant PM as Portfolio Manager
    participant FE as Frontend
    participant API as REST API
    participant SVC as PortfolioService
    participant REPO as Repository
    participant DB as Database
    
    PM->>FE: Fill form & Submit
    FE->>FE: Validate form data
    FE->>API: POST /api/portfolios/customer/{id}/holdings<br/>{assetName, category, quantity, prices}
    API->>SVC: addHolding(customerId, request)
    SVC->>REPO: findById(customerId)
    REPO->>DB: SELECT customer
    DB-->>REPO: Customer data
    REPO-->>SVC: Customer entity
    SVC->>REPO: findByCustomerId(customerId)
    REPO->>DB: SELECT portfolio
    DB-->>REPO: Portfolio data
    REPO-->>SVC: Portfolio entity
    SVC->>SVC: Create Holding entity<br/>Calculate values
    SVC->>REPO: save(holding)
    REPO->>DB: INSERT holding
    DB-->>REPO: Saved holding
    REPO-->>SVC: Holding entity
    SVC->>SVC: Convert to DTO
    SVC-->>API: HoldingDto
    API-->>FE: 201 Created + JSON
    FE->>FE: Show success notification
    FE->>API: GET /api/portfolios/customer/{id}
    API-->>FE: Updated portfolio
    FE-->>PM: Display updated portfolio
```

## Dashboard Summary Sequence

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant API as REST API
    participant DS as DashboardService
    participant CS as CustomerService
    participant TS as TransactionService
    participant DB as Database
    
    FE->>API: GET /api/dashboard/summary
    API->>DS: getDashboardSummary()
    
    DS->>CS: getAllCustomers()
    CS->>DB: SELECT all customers
    DB-->>CS: Customer data
    CS-->>DS: List<CustomerSummaryDto>
    
    DS->>DS: Calculate total AUM<br/>Average returns<br/>Active clients
    
    DS->>TS: countTransactionsToday()
    TS->>DB: COUNT transactions
    DB-->>TS: Count result
    TS-->>DS: Transaction count
    
    DS->>TS: findTop10ByOrderByTimestampDesc()
    TS->>DB: SELECT top 10 transactions
    DB-->>TS: Transaction data
    TS-->>DS: List<TransactionDto>
    
    DS->>DS: Get top 5 clients<br/>Calculate asset allocation
    
    DS-->>API: DashboardSummaryDto
    API-->>FE: JSON Response
    FE->>FE: Update dashboard UI
```

## Client-Server Communication Flow

```mermaid
graph LR
    subgraph "Client Side"
        A[User Interaction] --> B[Event Handler]
        B --> C[API Service<br/>api.js]
        C --> D[HTTP Request<br/>fetch API]
    end
    
    subgraph "Network"
        D --> E[HTTP/REST]
    end
    
    subgraph "Server Side"
        E --> F[REST Controller]
        F --> G[Service Layer]
        G --> H[Repository Layer]
        H --> I[(Database)]
    end
    
    subgraph "Response Flow"
        I --> H
        H --> G
        G --> F
        F --> E
        E --> D
        D --> C
        C --> B
        B --> J[UI Update]
    end
    
    style A fill:#e1f5ff
    style F fill:#fff4e1
    style G fill:#fff4e1
    style H fill:#fff4e1
    style I fill:#e8f5e9
    style J fill:#c8e6c9
```

## API Request/Response Pattern

```
┌─────────────────────────────────────────────────────────────┐
│                    REQUEST FLOW                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. User Action                                             │
│     └─> Click, Form Submit, Page Load                      │
│                                                              │
│  2. Frontend JavaScript (main.js)                           │
│     └─> Event handler triggered                             │
│                                                              │
│  3. API Service (api.js)                                    │
│     └─> Construct API endpoint URL                        │
│     └─> Prepare request payload (if POST/PUT)              │
│                                                              │
│  4. HTTP Request (fetch API)                                │
│     └─> Method: GET/POST/DELETE                             │
│     └─> Headers: Content-Type: application/json            │
│     └─> Body: JSON data (if applicable)                    │
│                                                              │
│  5. REST Controller (Spring Boot)                           │
│     └─> Receive HTTP request                               │
│     └─> Validate request                                    │
│     └─> Map to DTO                                          │
│                                                              │
│  6. Service Layer                                           │
│     └─> Business logic execution                            │
│     └─> Data transformation                                 │
│                                                              │
│  7. Repository Layer                                        │
│     └─> Database query construction                         │
│                                                              │
│  8. Database                                                │
│     └─> Execute SQL query                                   │
│                                                              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   RESPONSE FLOW                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  8. Database                                                │
│     └─> Return query results                                │
│                                                              │
│  7. Repository Layer                                        │
│     └─> Map to Entity objects                              │
│                                                              │
│  6. Service Layer                                           │
│     └─> Convert Entity to DTO                              │
│     └─> Apply business rules                               │
│                                                              │
│  5. REST Controller                                        │
│     └─> Serialize DTO to JSON                              │
│     └─> Set HTTP status code                                │
│                                                              │
│  4. HTTP Response                                           │
│     └─> Status: 200 OK / 201 Created / 204 No Content      │
│     └─> Body: JSON data                                    │
│                                                              │
│  3. API Service                                             │
│     └─> Parse JSON response                                 │
│     └─> Handle errors                                       │
│                                                              │
│  2. Frontend JavaScript                                     │
│     └─> Update UI with received data                        │
│     └─> Show notifications                                  │
│                                                              │
│  1. User                                                    │
│     └─> See updated interface                               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

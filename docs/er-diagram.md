# ER Diagram

```mermaid
erDiagram
    CUSTOMERS ||--o{ PORTFOLIOS : has
    CUSTOMERS ||--o{ TRANSACTIONS : makes
    PORTFOLIOS ||--o{ HOLDINGS : contains
    
    CUSTOMERS {
        bigint id PK
        varchar client_id UK
        varchar name
        varchar email
        enum type
        enum risk_level
        timestamp join_date
    }
    
    PORTFOLIOS {
        bigint id PK
        bigint customer_id FK
        timestamp created_at
    }
    
    HOLDINGS {
        bigint id PK
        bigint portfolio_id FK
        varchar asset_name
        enum category
        decimal quantity
        decimal avg_price
        decimal current_price
        decimal invested_amount
        decimal current_value
    }
    
    TRANSACTIONS {
        bigint id PK
        varchar transaction_id UK
        bigint customer_id FK
        enum type
        varchar asset
        enum category
        decimal quantity
        decimal price
        decimal amount
        enum status
        timestamp timestamp
    }
```

## Relationship Details

- **Customer → Portfolio**: One-to-Many (One customer can have multiple portfolios)
- **Customer → Transaction**: One-to-Many (One customer can have multiple transactions)
- **Portfolio → Holding**: One-to-Many (One portfolio can have multiple holdings)

## Asset Categories

- EQUITY
- DEBT
- GOLD
- REAL_ESTATE
- CRYPTO
- CASH
- MUTUAL_FUND

## Customer Types

- HNW (High Net Worth)
- REGULAR

## Risk Levels

- LOW
- MODERATE
- HIGH

## Transaction Types

- BUY
- SELL

## Transaction Status

- SUCCESS
- PENDING
- FAILED

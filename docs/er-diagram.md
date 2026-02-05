# ER Diagram

```mermaid
erDiagram
    MANAGERS ||--o{ CLIENTS : manages
    CLIENTS ||--o{ CLIENT_HOLDINGS : has
    ASSETS ||--o{ CLIENT_HOLDINGS : "traded as"
    
    MANAGERS {
        bigint manager_id PK
        varchar full_name
        varchar email UK
        varchar password
    }
    
    CLIENTS {
        bigint client_id PK
        bigint manager_id FK
        varchar full_name
        varchar email
        timestamp created_at
    }
    
    ASSETS {
        bigint asset_id PK
        varchar symbol UK
        varchar asset_name
        enum category
        text description
    }
    
    CLIENT_HOLDINGS {
        bigint holding_id PK
        bigint client_id FK
        bigint asset_id FK
        decimal quantity
        decimal avg_buy_price
        date buy_date
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

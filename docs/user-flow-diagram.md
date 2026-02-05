# User Flow Diagram

```mermaid
flowchart TD
    Start([Portfolio Manager<br/>Accesses System]) --> Login{SSO<br/>Authentication}
    Login -->|Authenticated| Dashboard[Main Dashboard<br/>View Summary<br/>Select Customer]
    
    Dashboard --> Customers[View Customers Page<br/>Browse All Customers<br/>Filter/Sort]
    Dashboard --> Portfolio[View Portfolio Page<br/>Select Customer<br/>View Holdings]
    Dashboard --> Transactions[View Transactions Page<br/>Filter by Date/Status<br/>View Details]
    
    Customers --> CustomerSelected[Customer Selected]
    Portfolio --> CustomerSelected
    
    CustomerSelected --> PortfolioDetails[Portfolio Details<br/>Holdings List<br/>Performance<br/>Charts]
    
    PortfolioDetails --> AddHolding[Add Holding<br/>Fill Form<br/>Submit]
    PortfolioDetails --> RemoveHolding[Remove Holding<br/>Confirm<br/>Delete]
    PortfolioDetails --> ViewCharts[View Charts<br/>Performance<br/>Allocation<br/>Risk]
    
    AddHolding --> APICall1[POST /api/portfolios/<br/>customer/{id}/holdings]
    RemoveHolding --> APICall2[DELETE /api/portfolios/<br/>holdings/{id}]
    
    APICall1 --> Success1[Success Notification<br/>Refresh Data]
    APICall2 --> Success2[Success Notification<br/>Refresh Data]
    ViewCharts --> UpdatedCharts[Updated Visualizations]
    
    Success1 --> Dashboard
    Success2 --> Dashboard
    UpdatedCharts --> Dashboard
    
    Transactions --> TransactionDetails[Transaction Details<br/>View History]
    
    style Start fill:#e1f5ff
    style Login fill:#fff4e1
    style Dashboard fill:#e8f5e9
    style PortfolioDetails fill:#e8f5e9
    style Success1 fill:#c8e6c9
    style Success2 fill:#c8e6c9
```

## User Journey Steps

1. **Authentication**: Portfolio Manager logs in via SSO (future implementation)
2. **Dashboard Access**: View overall summary and select customer
3. **Customer Selection**: Choose customer from dropdown or clients page
4. **Portfolio View**: View selected customer's portfolio details
5. **Actions**: Add/remove holdings, view charts, check transactions
6. **Feedback**: Success notifications and data refresh

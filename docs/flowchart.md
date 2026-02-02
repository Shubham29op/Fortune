# Flowchart - Main Application Flow

```mermaid
flowchart TD
    Start([Application Start]) --> Init[Initialize Application<br/>Load API Config<br/>Setup Navigation]
    
    Init --> LoadDashboard[Load Dashboard<br/>GET /api/dashboard/summary]
    
    LoadDashboard -->|Success| DisplayDashboard[Display Dashboard<br/>Summary Cards<br/>Charts<br/>Tables]
    LoadDashboard -->|Error| Error1[Show Error Message<br/>Retry/Handle Error]
    
    DisplayDashboard --> LoadCustomers[Load Customers<br/>GET /api/customers]
    
    LoadCustomers -->|Success| PopulateDropdown[Populate Customer Dropdown<br/>Select Customer]
    LoadCustomers -->|Error| Error2[Show Error]
    
    PopulateDropdown --> CheckCustomer{Customer<br/>Selected?}
    
    CheckCustomer -->|Yes| LoadPortfolio[Load Portfolio<br/>GET /api/portfolios/<br/>customer/{id}]
    CheckCustomer -->|No| ShowMessage[Show Message<br/>Select Customer]
    
    LoadPortfolio -->|Success| DisplayPortfolio[Display Portfolio<br/>Holdings<br/>Summary]
    LoadPortfolio -->|Error| Error3[Show Error Message]
    
    DisplayPortfolio --> UserAction{User Action?}
    
    UserAction -->|Add Holding| AddFlow[Add Holding Flow]
    UserAction -->|Remove Holding| RemoveFlow[Remove Holding Flow]
    UserAction -->|View Charts| ChartFlow[Chart Flow]
    
    AddFlow --> OpenModal[Open Modal<br/>Display Form]
    OpenModal --> FillForm[User Fills Form<br/>Asset Name<br/>Category<br/>Quantity<br/>Prices]
    FillForm --> Validate{Validate<br/>Form?}
    
    Validate -->|Valid| SubmitAdd[POST /api/portfolios/<br/>customer/{id}/holdings]
    Validate -->|Invalid| ValidationError[Show Validation Errors]
    
    SubmitAdd -->|Success| SuccessAdd[Show Success Notification<br/>Close Modal<br/>Refresh Data]
    SubmitAdd -->|Error| ErrorAdd[Show Error Message<br/>Log Error]
    
    RemoveFlow --> Confirm{Confirm<br/>Deletion?}
    Confirm -->|Yes| SubmitRemove[DELETE /api/portfolios/<br/>holdings/{id}]
    Confirm -->|No| Cancel[Cancel Action]
    
    SubmitRemove -->|Success| SuccessRemove[Show Success Notification<br/>Refresh Data]
    SubmitRemove -->|Error| ErrorRemove[Show Error Message]
    
    ChartFlow --> UpdateCharts[Update Charts<br/>Performance<br/>Allocation<br/>Risk]
    
    SuccessAdd --> DisplayPortfolio
    SuccessRemove --> DisplayPortfolio
    UpdateCharts --> DisplayPortfolio
    
    style Start fill:#e1f5ff
    style DisplayDashboard fill:#e8f5e9
    style DisplayPortfolio fill:#e8f5e9
    style SuccessAdd fill:#c8e6c9
    style SuccessRemove fill:#c8e6c9
    style Error1 fill:#ffcdd2
    style Error2 fill:#ffcdd2
    style Error3 fill:#ffcdd2
    style ErrorAdd fill:#ffcdd2
    style ErrorRemove fill:#ffcdd2
```

## Flowchart - Add Holding Process

```mermaid
flowchart TD
    Start([User Clicks Add Asset]) --> CheckCustomer{Customer<br/>Selected?}
    
    CheckCustomer -->|No| ErrorMsg[Show Error:<br/>Select Customer First]
    CheckCustomer -->|Yes| OpenModal[Open Modal<br/>Display Form]
    
    OpenModal --> FillForm[User Fills Form<br/>- Asset Name<br/>- Category<br/>- Quantity<br/>- Avg Price<br/>- Current Price]
    
    FillForm --> Validate{Validate<br/>Form Data?}
    
    Validate -->|Invalid| ShowValidation[Show Validation Errors<br/>Highlight Fields]
    Validate -->|Valid| SubmitForm[Submit Form<br/>POST API Request]
    
    ShowValidation --> FillForm
    
    SubmitForm --> APICall[API Call:<br/>POST /api/portfolios/<br/>customer/{id}/holdings]
    
    APICall -->|Success| Success[Show Success Notification<br/>Close Modal<br/>Reset Form<br/>Refresh Portfolio Data]
    APICall -->|Error| Error[Show Error Message<br/>Log Error Details<br/>Keep Modal Open]
    
    Success --> End([End])
    Error --> FillForm
    ErrorMsg --> End
    
    style Start fill:#e1f5ff
    style Success fill:#c8e6c9
    style Error fill:#ffcdd2
    style ErrorMsg fill:#ffcdd2
    style ShowValidation fill:#fff9c4
```

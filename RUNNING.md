# Running the Backend

## With MySQL

1. **Create the database:**
   ```sql
   CREATE DATABASE portfolio_db;
   ```

2. **Update MySQL credentials** in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.username=your_mysql_username
   spring.datasource.password=your_mysql_password
   ```

3. **Start MySQL**, then run:
   ```bash
   ./mvnw spring-boot:run
   ```

## What Happens on Startup

- **Tables are created automatically** by Spring Boot/Hibernate based on your `@Entity` classes
- **Sample data is loaded** by `DataInitializer` (customers, portfolios, holdings, transactions)
- **Server starts** on port 8080

## Access Points

- **API Base**: http://localhost:8080/api  
- **Swagger UI**: http://localhost:8080/swagger-ui.html  
- **API Endpoints**: 
  - `GET /api/dashboard/summary` - Dashboard statistics
  - `GET /api/customers` - All customers
  - `GET /api/portfolios/customer/{id}` - Customer portfolio
  - `GET /api/transactions` - All transactions

## Troubleshooting

**Error "Access denied for user 'root'@'localhost'"**  
- Check MySQL username and password in `application.properties`
- Ensure MySQL is running
- Verify database `portfolio_db` exists

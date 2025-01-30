# multitenant-datasource
Spring Boot single file multi tenant demo (one database per user)

High-Level Approach
Configuration Database: Store metadata (JDBC URL, username, password, driver) for each customer's databases.
Dynamic Data Source Routing: Implement a RoutingDataSource that determines which database to connect to dynamically.
Tenant Context Management: Store the current customer ID in a ThreadLocal context (or in Spring Security context if applicable).
Database Connection Pooling: Since you have hundreds of database instances, avoid excessive connection creation by leveraging HikariCP connection pooling.
Interceptor or AOP for Tenant Resolution: Use an interceptor to extract tenant info from the request or job execution context.

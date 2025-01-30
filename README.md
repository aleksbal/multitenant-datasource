# multitenant-datasource
Dynamic Multi-Tenant Database Switching in Spring Boot
This project implements a Spring Boot 3 (Java 21) REST API with dynamic database switching for each tenant (user).

Every user has their own database.
If a user's database does not exist, it is created on the fly.
Uses Spring JDBC, HikariCP connection pooling, and ThreadLocal context for multi-tenancy.
🛠 Features
✅ Dynamic Database Switching – Each request connects to the correct tenant database.
✅ Automatic Tenant Database Creation – If the database doesn't exist, it is created automatically.
✅ Efficient Connection Pooling – Uses HikariCP for fast and optimized connections.
✅ Thread-Safe Tenant Management – Uses ThreadLocal for per-request database selection.
✅ Minimalistic REST API – One GET endpoint: /data?username=<tenant>

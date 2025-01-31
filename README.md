# Dynamic Multi-Tenant Database Switching in Spring Boot  

This project implements a **Spring Boot 3 (Java 21) REST API** with sort of **dynamic database switching** for each tenant (user). 
Basic problem was in a rudimentary way described [here](https://www.baeldung.com/spring-abstract-routing-data-source). We'll use Spring AbstractRoutingDatasource.

- **Every client/user has their own tet's say SQL database instance (multi-tenancy).**  
- **If a user's database does not exist, it could be created on the fly.**  
- Uses **Spring JDBC, HikariCP connection pooling, and `ThreadLocal` context** to implement multi-tenancy.  

---

## 🧪 Features  

✅ **Dynamic Database Switching** – Each individual request connects to the correct tenant database.  
✅ **Automatic Tenant Database Creation** – If the database doesn't exist, it is **created automatically**.  
✅ **Efficient Connection Pooling** – Uses **HikariCP** for fast and optimized connections.  
✅ **Thread-Safe Tenant Management** – Uses **`ThreadLocal`** for per-request database selection.  
✅ **Minimalistic REST API** – One `GET` endpoint: `/data?username=<tenant>`  

---

## 📂 Project Structure  

```
/src/main/java/com/example/DynamicDbApi.java  # Single-file Spring Boot application
```

---

## 🚀 How It Works  

1. A user makes a request:  
   ```
   GET /data?username=john_doe
   ```
2. The system **checks if John's database exists**:  
   - ✅ **If it exists:** It connects and retrieves data.  
   - ❌ **If it doesn’t exist:** It creates a **new database & user** dynamically.  
3. The request is executed **on the correct database**.  

---

## 🧪 Setup & Run  

### 1️⃣ Ensure MySQL is Running  

```sql
CREATE USER 'root'@'%' IDENTIFIED BY 'root_password';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
```

### 2️⃣ Run the Spring Boot App  

```sh
mvn spring-boot:run

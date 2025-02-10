# Dynamic Multi-Tenant Database Switching in Spring Boot  

This project implements a **Spring Boot 3 (Java 21) REST API** with sort of **dynamic database switching** for each tenant (user). 
Basic problem was in a rudimentary way described [here](https://www.baeldung.com/spring-abstract-routing-data-source). We'll use Spring AbstractRoutingDatasource.

- **Every client/user has their own SQL database instance (multi-tenancy).**
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

# 📌 Testing Multi-Tenant Database Setup in Spring Boot

## 🚀 Prerequisites

Before running tests, ensure you have the following:

- **Java 21** installed
- **Gradle** installed
- **Spring Boot 3.4.2** project set up
- **H2 in-memory database** configured

## 🛠 Steps to Test

### **1️⃣ Run the Application**

Start the Spring Boot application using:

```sh
./gradlew clean build bootRun
```

### **2️⃣ Verify Initial Tenant Databases**

The application initializes with the following tenants:

- `alice_db`
- `bob_db`
- `default_db`

Check if tenants exist by running:

```sh
curl -X GET "http://localhost:8080/data?username=alice"
```

Expected Response (if data exists):

```json
[
  {"id": 1, "name": "Alice"},
  {"id": 2, "name": "Bob"}
]
```

### **3️⃣ Verify Default Database Handling**

Test an unknown tenant:

```sh
curl -X GET "http://localhost:8080/data?username=unknown"
```

Expected Response:

```json
{
  "error": "User not found!"
}
```

### **4️⃣ Test Adding a New Tenant at Runtime**

If you have an API to add new tenants dynamically, run:

```sh
curl -X POST "http://localhost:8080/addTenant?username=new_tenant"
```

Verify:

```sh
curl -X GET "http://localhost:8080/data?username=new_tenant"
```

Expected Response:

```json
[]
```

### **5️⃣ Debugging Tenant Switching**

If something goes wrong, enable debugging:

```sh
./gradlew bootRun --debug
```

Or check logs:

```sh
tail -f logs/application.log
```

## ✅ Expected Results

| **Test Case**           | **Expected Result**                |
| ----------------------- | ---------------------------------- |
| Access `alice_db`       | Returns data from Alice's database |
| Access `unknown` tenant | Returns error "User not found!"    |
| Add new tenant          | Successfully creates new database  |
| Query new tenant        | Returns empty result set           |

## 🛑 Troubleshooting

- **Issue:** `targetDataSources` error?\
  **Fix:** Ensure `setTargetDataSources()` is called before `afterPropertiesSet()`.
- **Issue:** `Cannot connect to database`?\
  **Fix:** Ensure `org.h2.Driver` is used for H2 databases.
- **Issue:** `User not found!` error?\
  **Fix:** Check if the tenant exists using `getResolvedDataSources()`.

## 🎯 Next Steps

- ✅ Write **JUnit tests** for tenant switching.
- ✅ Implement a **REST API to manage tenants dynamically**.
- ✅ Improve **database connection pooling optimizations**.

🚀 Happy Testing!



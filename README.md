# Dynamic Multi-Tenant Database Switching in Spring Boot  

This project implements a **Spring Boot 3 (Java 21) REST API** with sort of **dynamic database switching** for each tenant (user). 
Basic problem was in a rudimentary way described [here](https://www.baeldung.com/spring-abstract-routing-data-source). We'll use Spring AbstractRoutingDatasource.

- **Every client/user has their own SQL database instance (multi-tenancy).**
- Uses **Spring JDBC, HikariCP connection pooling, and `ThreadLocal` context** to implement multi-tenancy.  

---

## 🧪 Features  

✅ **Dynamic Database Switching** – Each individual request connects to the correct tenant database.  
✅ **Efficient Connection Pooling** – Uses **HikariCP** for fast and optimized connections.  
✅ **Thread-Safe Tenant Management** – Uses **`ThreadLocal`** for per-request database selection.

---

## 📂 Project Structure  

```
/src/org/abl/demo/spb/multitenant/App.java  # Command line Spring Boot application
```

# 📌 Testing Multi-Tenant Database Setup in Spring Boot

## 🚀 Prerequisites

Before running tests, ensure you have the following:

- **Java 21** installed
- **Gradle** installed
- **Spring Boot 3.4.2** project set up
- **H2 in-memory database** configured

## 🛠 Steps to Test

### **1️⃣ Run the Application**

Start the application using:

```sh
./gradlew clean build bootRun
```

### **2️⃣ Verify Initial Tenant Databases**

The application initializes with the following tenants:

- `alice`
- `bob`

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



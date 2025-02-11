package org.abl.demo.spb.multitenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.abl.demo.spb.multitenant.stuff.MultiTenantDataSource;
import org.abl.demo.spb.multitenant.stuff.TenantContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class App {

  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }

	// used to store database access stuff for each tenant
  @Bean(name = "refDataSource")
  public DataSource refDataSource() {
    var config = new HikariConfig();
    config.setJdbcUrl(
        "jdbc:h2:mem:multitenant-db;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'\\;RUNSCRIPT FROM 'classpath:data.sql'");
    config.setDriverClassName("org.h2.Driver");
    config.setUsername("sa");
    config.setPassword("");

    var dataSource = new HikariDataSource(config);

    System.out.println("✅ Fully Configured refDataSource: " + dataSource.getJdbcUrl());
    return dataSource;
  }

  @Bean(name = "multitenantDataSource")
  public DataSource multiTenantDataSource(@Qualifier("refDataSource") DataSource refDs) {

    var multiTenantDataSource = new MultiTenantDataSource(refDs);
    multiTenantDataSource.afterPropertiesSet(); // REQUIRED to initialize
    return multiTenantDataSource;
  }

  @Bean
  public JdbcTemplate jdbcTemplate(@Qualifier("multitenantDataSource") DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  private void runTenantTest(String tenantId, DataSource ds) throws SQLException {

    //✅ The database is switched for tenant from TenantContext (ThreadLocal<String>)
    TenantContext.setTenant(tenantId);

    try ( var stmt = ds.getConnection().createStatement()) {

      // Print the current database (if supported)
      try (ResultSet dbRs = stmt.executeQuery("SELECT DATABASE() AS db_name")) {
        if (dbRs.next()) {
          System.out.println(
              "✅ Connected to database [" + tenantId + "]: " + dbRs.getString("db_name"));
        }
      } catch (SQLException e) {
        System.out.println("⚠️ DATABASE() not supported, skipping database name check.");
      }

      // Fetch all users
      try (var rs = stmt.executeQuery("SELECT * FROM users")) {
        System.out.println("📋 Users in " + tenantId + "'s database:");
        while (rs.next()) {
          System.out.println(" - " + rs.getString("name"));
        }
      }
    }
  }

  private void runTenantTestWithJdbcTemplate(String tenantId, JdbcTemplate jdbcTemplate) {

    //✅ here, database is switched for given tenant
    TenantContext.setTenant(tenantId);

    List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT * FROM users");

    System.out.println("📋 Users in " + tenantId + "'s database:");
    for (Map<String, Object> row : users) {
      System.out.println(" - " + row.get("name"));
    }
  }

  @Bean
  CommandLineRunner runTest(ApplicationContext ctx,
      @Qualifier("multitenantDataSource") DataSource multiDs, JdbcTemplate jdbcTemplate) {
    return args -> {
      System.out.println("🚀 Running command-line test...");

      runTenantTest("alice", multiDs);
      runTenantTest("bob", multiDs);

      runTenantTestWithJdbcTemplate("alice", jdbcTemplate);
      runTenantTestWithJdbcTemplate("bob", jdbcTemplate);

      System.out.println("✅ Test complete. Shutting down.");
      SpringApplication.exit(ctx, () -> 0); // Shut down the application
    };
  }

}


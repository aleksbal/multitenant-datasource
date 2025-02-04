package org.abl.demo.spb.multitenantds;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class MultiTenantDataSource extends AbstractRoutingDataSource {

  private static final String DEFAULT_DB_URL = "jdbc:h2:mem:tenant_registry_db;DB_CLOSE_DELAY=-1;";
  private static final String DEFAULT_DB_USER = "sa";
  private static final String DEFAULT_DB_PASSWORD = "";

  @Autowired
  private final DataSource defaultDataSource;

  //already in AbstractRoutingDataSource
  private final Map<String, DataSource> tenantDataSources = new ConcurrentHashMap<>();

  public MultiTenantDataSource() {
    this.defaultDataSource = createDataSource(DEFAULT_DB_URL, DEFAULT_DB_USER, DEFAULT_DB_PASSWORD);
  }

  private static DataSource createDataSource(String jdbcUrl, String username, String password) {
    var config = new HikariConfig();
    config.setJdbcUrl(jdbcUrl);
    config.setUsername(username);
    config.setPassword(password);
    config.setDriverClassName("org.h2.Driver");
    config.setMaximumPoolSize(10);
    return new HikariDataSource(config);
  }

  private DataSource getOrCreateTenantDataSource(String tenantId) {
    return tenantDataSources.computeIfAbsent(tenantId, this::loadTenantDataSource);
  }

  private DataSource loadTenantDataSource(String tenantId) {
    try (Connection conn = defaultDataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(
            "SELECT jdbc_url, username, password FROM tenants WHERE tenant_id = ?")) {
      stmt.setString(1, tenantId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return createDataSource(rs.getString("jdbc_url"), rs.getString("username"), rs.getString("password"));
        } else {
          throw new RuntimeException("Tenant not found: " + tenantId);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to retrieve tenant data source", e);
    }
  }

  @Override
  protected Object determineCurrentLookupKey() {
    return TenantContext.getTenant();
  }

  @Override
  // this is not really needed, default implementation could be sufficient
  protected DataSource determineTargetDataSource() {
    String tenantId = TenantContext.getTenant();
    return tenantId != null ? getOrCreateTenantDataSource(tenantId) : defaultDataSource;
  }
}

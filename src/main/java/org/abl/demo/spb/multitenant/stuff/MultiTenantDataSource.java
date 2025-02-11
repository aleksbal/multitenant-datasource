package org.abl.demo.spb.multitenant.stuff;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MultiTenantDataSource extends AbstractRoutingDataSource {

  private final DataSource refDataSource;

  private volatile Map<Object, DataSource> resolvedDataSources = new HashMap<>();

  public MultiTenantDataSource(DataSource refDs) {
    this.setTargetDataSources(new HashMap<>());
    this.refDataSource = refDs;
  }

  @Override
  protected Object determineCurrentLookupKey() {

    return TenantContext.getTenant();
  }

  @Override
  public DataSource determineTargetDataSource() {

    if (determineCurrentLookupKey() == null || !(determineCurrentLookupKey() instanceof String))
      throw new RuntimeException("Data source lookup key is null, check if you set the TenantContext!");

    // if the data source doesn't yet exist create one, otherwise just skip and call method from super class
    if (!this.resolvedDataSources.containsKey(determineCurrentLookupKey())) {

      try (var conn = refDataSource.getConnection();

          var stmt = conn.prepareStatement(
              "SELECT jdbc_url, schema_file, data_file FROM tenants WHERE tenant_id = ?")) {

          stmt.setString(1, determineCurrentLookupKey().toString());

          try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
              this.resolvedDataSources.put(determineCurrentLookupKey(),
                  createDataSource(rs.getString("jdbc_url"),
                      rs.getString("schema_file"),
                      rs.getString("data_file")));
              afterPropertiesSet();
            } else {
              throw new RuntimeException("Tenant not found: " + determineCurrentLookupKey());
            }
          }
      } catch (SQLException e) {
        throw new RuntimeException("Failed to retrieve tenant data source", e);
      }
    }
    return resolvedDataSources.get(determineCurrentLookupKey());
  }

  private DataSource createDataSource(String jdbcUrl, String schemaFile, String dataFile) {

    var config = new HikariConfig();

    // Build the INIT script execution part
    var finalJdbcUrl = new StringBuilder(jdbcUrl);

    if ((schemaFile != null && !schemaFile.isEmpty()) || (dataFile != null && !dataFile.isEmpty())) {
      finalJdbcUrl.append(";INIT=");

      if (schemaFile != null && !schemaFile.isEmpty()) {
        finalJdbcUrl.append("RUNSCRIPT FROM 'classpath:").append(schemaFile).append("'");
      }

      if (dataFile != null && !dataFile.isEmpty()) {
        if (schemaFile != null && !schemaFile.isEmpty()) {
          finalJdbcUrl.append(" \\;");
        }
        finalJdbcUrl.append("RUNSCRIPT FROM 'classpath:").append(dataFile).append("'");
      }
    }

    config.setJdbcUrl(finalJdbcUrl.toString());
    config.setUsername("sa");
    config.setPassword("");
    config.setDriverClassName("org.h2.Driver");
    config.setMaximumPoolSize(10);

    return new HikariDataSource(config);
  }
}

package org.abl.demo.spb.multitenantds;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MultiTenantDataSource extends AbstractRoutingDataSource {

  private final DataSource refDataSource;

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

    if (determineCurrentLookupKey() == null || determineCurrentLookupKey() instanceof String)
      throw new RuntimeException("Data source lookup key is null, check if you set the TenantContext!");

    // if the data source doesn't yet exist create one, otherwise just skip and call method from super class
    if (!this.getResolvedDataSources().containsKey(determineCurrentLookupKey())) {

      try (var conn = refDataSource.getConnection();

          var stmt = conn.prepareStatement(
              "SELECT jdbc_url, username, password FROM tenants WHERE tenant_id = ?")) {

          stmt.setString(1, determineCurrentLookupKey().toString());

          try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
              this.getResolvedDataSources().put(determineCurrentLookupKey(), createDataSource(rs.getString("jdbc_url")));
            } else {
              throw new RuntimeException("Tenant not found: " + determineCurrentLookupKey());
            }
          }
      } catch (SQLException e) {
        throw new RuntimeException("Failed to retrieve tenant data source", e);
      }
    }
    return super.determineTargetDataSource();
  }

  //config.setJdbcUrl("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'\\;RUNSCRIPT FROM 'classpath:data.sql'");
  private static DataSource createDataSource(String jdbcUrl) {
    var config = new HikariConfig();
    config.setJdbcUrl(jdbcUrl);
    config.setUsername("sa");
    config.setPassword("");
    config.setDriverClassName("org.h2.Driver");
    config.setMaximumPoolSize(10);
    return new HikariDataSource(config);
  }
}

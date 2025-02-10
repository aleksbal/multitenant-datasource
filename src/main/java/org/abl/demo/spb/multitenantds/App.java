package org.abl.demo.spb.multitenantds;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.datasource.lookup.*;
import com.zaxxer.hikari.*;
import javax.sql.DataSource;
import java.util.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class App {
	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	@Bean
	@Primary
	public DataSource multiTenantDataSource() {
		return new MultiTenantDataSource();
	}

	@Bean
	public JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@RestController
	@RequestMapping("/data")
	public static class DataController {

		private final JdbcTemplate jdbcTemplate;

		public DataController(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
		}

		@GetMapping
		public List<Map<String, Object>> getData(@RequestParam String username) {
			TenantContext.setTenant(username);
			try {
				return jdbcTemplate.queryForList("SELECT * FROM users LIMIT 10");
			} finally {
				TenantContext.clear();
			}
		}
	}

	@Bean(name = "refDataSource")
	@ConfigurationProperties(prefix = "spring.datasource.refDataSource")
	public DataSource refDataSource() {
		return DataSourceBuilder.create().build();
	}

	public class MultiTenantDataSource extends AbstractRoutingDataSource {

		@Autowired
		@Qualifier("refDataSource")
		private DataSource refDataSource;

		@Override
    protected DataSource determineTargetDataSource() {

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

		@Override
		protected Object determineCurrentLookupKey() {
			return TenantContext.getTenant();
		}
	}
}


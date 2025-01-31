package org.abl.demo.multitenant_datasource;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.datasource.lookup.*;
import com.zaxxer.hikari.*;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class MultitenantDatasourceApplication {
	public static void main(String[] args) {
		SpringApplication.run(MultitenantDatasourceApplication.class, args);
	}

	@Bean
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
			if (!MultiTenantDataSource.hasTenant(username)) {
				throw new RuntimeException("User not found!");
			}
			TenantContext.setTenant(username);
			try {
				return jdbcTemplate.queryForList("SELECT * FROM users LIMIT 10");
			} finally {
				TenantContext.clear();
			}
		}
	}

	public static class TenantContext {
		private static final ThreadLocal<String> tenantHolder = new ThreadLocal<>();

		public static void setTenant(String tenantId) {
			tenantHolder.set(tenantId);
		}

		public static String getTenant() {
			return tenantHolder.get();
		}

		public static void clear() {
			tenantHolder.remove();
		}
	}

	@Component
	public static class MultiTenantDataSource extends AbstractRoutingDataSource {

		private static final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
		private static final DataSource DEFAULT_DATASOURCE = createDataSource(
				"jdbc:mysql://localhost:3306/default_db", "default_user", "password"
		);

		static {
			// todo: static creation, the databases shall be stored in a configuration database and data sources shall be created on demand
			dataSources.put("alice", createDataSource("jdbc:mysql://localhost:3306/alice_db", "alice_user", "password"));
			dataSources.put("bob", createDataSource("jdbc:mysql://localhost:3306/bob_db", "bob_user", "password"));
		}

		private static DataSource createDataSource(String url, String username, String password) {
			var config = new HikariConfig();
			config.setJdbcUrl(url);
			config.setUsername(username);
			config.setPassword(password);
			config.setDriverClassName("com.mysql.cj.jdbc.Driver");
			config.setMaximumPoolSize(10);
			config.setMinimumIdle(2);
			config.setIdleTimeout(30000);
			config.setMaxLifetime(1800000);
			return new HikariDataSource(config);
		}

		public static boolean hasTenant(String tenantId) {
			return dataSources.containsKey(tenantId);
		}

		@Override
		protected Object determineCurrentLookupKey() {
			return TenantContext.getTenant();
		}

		@Override
		protected DataSource determineTargetDataSource() {
			String tenantId = TenantContext.getTenant();
			return (tenantId != null && dataSources.containsKey(tenantId)) ? dataSources.get(tenantId) : DEFAULT_DATASOURCE;
		}
	}
}


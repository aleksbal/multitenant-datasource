package org.abl.demo.multitenant_datasource;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
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
public class MultitenantDatasourceApplication {
	public static void main(String[] args) {
		SpringApplication.run(MultitenantDatasourceApplication.class, args);
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

	public class MultiTenantDataSource extends AbstractRoutingDataSource {

		public MultiTenantDataSource() {
			// Create the tenant data sources
			Map<Object, Object> tenantDataSources = new HashMap<>();
			tenantDataSources.put("alice", createDataSource("alice_db"));
			tenantDataSources.put("bob", createDataSource("bob_db"));
			tenantDataSources.put("aleks", createDataSource("aleks_db"));

			// Set them directly in the superclass
			setDefaultTargetDataSource(createDataSource("default_db"));
			setTargetDataSources(tenantDataSources);

			// Initialize after setting data sources
			super.afterPropertiesSet();
		}

		private static DataSource createDataSource(String dbName) {
			var config = new HikariConfig();
			config.setJdbcUrl("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;");
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

		public void addTenant(String tenantId) {
			if (!getResolvedDataSources().containsKey(tenantId)) {
				DataSource newDataSource = createDataSource(tenantId);
				Map<Object, Object> updatedDataSources = new HashMap<>(getResolvedDataSources());
				updatedDataSources.put(tenantId, newDataSource);

				// Update target data sources and reinitialize
				setTargetDataSources(updatedDataSources);
				super.afterPropertiesSet();
			}
		}
	}

}


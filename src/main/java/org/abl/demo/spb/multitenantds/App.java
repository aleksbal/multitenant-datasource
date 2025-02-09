package org.abl.demo.spb.multitenantds;

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

	public class MultiTenantDataSource extends AbstractRoutingDataSource {

		public MultiTenantDataSource() {
			// Create the tenant data sources
			Map<Object, Object> tds = new HashMap<>();
			tds.put("alice", createDataSource("alice_db"));
			tds.put("bob", createDataSource("bob_db"));
			tds.put("aleks", createDataSource("aleks_db"));

			// Set them directly in the superclass
			setDefaultTargetDataSource(createDataSource("default_db"));
			setTargetDataSources(tds);

			// Initialize after setting data sources
			super.afterPropertiesSet();
		}

		private static DataSource createDataSource(String dbName) {
			var config = new HikariConfig();
			config.setJdbcUrl("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'\\;RUNSCRIPT FROM 'classpath:data.sql'");
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


package org.abl.demo.spb.multitenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import java.sql.SQLException;
import org.abl.demo.spb.multitenant.stuff.MultiTenantDataSource;
import org.abl.demo.spb.multitenant.stuff.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.*;
import javax.sql.DataSource;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class App {

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	@Bean(name = "refDataSource")
	public DataSource refDataSource() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:h2:mem:multitenant-db;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'\\;RUNSCRIPT FROM 'classpath:data.sql'");
		config.setDriverClassName("org.h2.Driver");
		config.setUsername("sa");
		config.setPassword("");

		HikariDataSource dataSource = new HikariDataSource(config);

		System.out.println("✅ Fully Configured refDataSource: " + dataSource.getJdbcUrl());
		return dataSource;
	}

	@Component
	public class DebugDataSource {

		@Value("${spring.datasource.ref-data-source.url:NOT_SET}")
		private String refDatasourceUrl;

		@PostConstruct
		public void printLoadedProperties() {
			System.out.println("Property Loaded: " + refDatasourceUrl);
		}

		@Autowired
		public DebugDataSource(@Qualifier("refDataSource") DataSource refDs,
				@Qualifier("multitenantDataSource") DataSource multiDs) throws SQLException {

			System.out.println("Multitenant datasource Initialized: " + multiDs);

			TenantContext.setTenant("alice");
			System.out.println("refDataSource Initialized: " + multiDs.getConnection());

			TenantContext.setTenant("bob");
			System.out.println("refDataSource Initialized: " + multiDs.getConnection());

		}

	}

	@Bean(name = "multitenantDataSource")
	public DataSource multiTenantDataSource(@Qualifier("refDataSource") DataSource refDs) {

		var multiTenantDataSource = new MultiTenantDataSource(refDs);
		multiTenantDataSource.afterPropertiesSet(); // REQUIRED to initialize
		return multiTenantDataSource;
	}

	@Bean
	public JdbcTemplate jdbcTemplate(@Qualifier("multitenantDataSource")  DataSource dataSource) {
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
}


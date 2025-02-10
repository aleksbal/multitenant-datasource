package org.abl.demo.spb.multitenantds;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.*;
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

	@Bean(name = "refDataSource")
	@ConfigurationProperties(prefix = "spring.datasource.ref-data-source")
	public DataSource refDataSource() {
		return DataSourceBuilder.create().build();
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
				return jdbcTemplate.queryForList("SELECT * FROM jobs LIMIT 10");
			} finally {
				TenantContext.clear();
			}
		}
	}
}


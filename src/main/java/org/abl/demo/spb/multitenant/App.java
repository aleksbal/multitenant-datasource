package org.abl.demo.spb.multitenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.abl.demo.spb.multitenant.stuff.MultiTenantDataSource;
import org.abl.demo.spb.multitenant.stuff.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.*;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class App {

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	private void runTenantTest(String tenant, DataSource multiDs) throws SQLException {

		TenantContext.setTenant(tenant);

		try (Connection conn = multiDs.getConnection();
				Statement stmt = conn.createStatement()) {

			// Print the current database (if supported)
			try (ResultSet dbRs = stmt.executeQuery("SELECT DATABASE() AS db_name")) {  // Works for MySQL
				if (dbRs.next()) {
					System.out.println("✅ Connected to database [" + tenant + "]: " + dbRs.getString("db_name"));
				}
			} catch (SQLException e) {
				System.out.println("⚠️ DATABASE() not supported, skipping database name check.");
			}

			// Fetch all users
			try (ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
				System.out.println("📋 Users in " + tenant + "'s database:");
				while (rs.next()) {
					System.out.println(" - " + rs.getString("name"));
				}
			}
		}
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

	@Component
	public class DebugDataSource {

		@Value("${spring.datasource.ref-data-source.url:NOT_SET}")
		private String refDatasourceUrl;

		@PostConstruct
		public void printLoadedProperties() {
			System.out.println("Property Loaded: " + refDatasourceUrl);
		}
	}

	@Bean
	CommandLineRunner runTest(ApplicationContext ctx, @Qualifier("multitenantDataSource") DataSource multiDs) {
		return args -> {
			System.out.println("🚀 Running command-line test...");

			runTenantTest("alice", multiDs);
			runTenantTest("bob", multiDs);

			System.out.println("✅ Test complete. Shutting down.");
			SpringApplication.exit(ctx, () -> 0); // Shut down the application
		};
	}

}


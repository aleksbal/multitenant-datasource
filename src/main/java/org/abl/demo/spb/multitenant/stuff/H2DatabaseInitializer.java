package org.abl.demo.spb.multitenant.stuff;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

//@Component
//@Configuration
//@EnableConfigurationProperties
public class H2DatabaseInitializer {
  //@Autowired
  public H2DatabaseInitializer(@Qualifier("refDataSource") DataSource dataSource) {
    try (Connection conn = dataSource.getConnection()) {
      System.out.println("Successfully connected to H2: " + conn.getMetaData().getURL());
      ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM INFORMATION_SCHEMA.TABLES");
      while (rs.next()) {
        System.out.println("Table Found: " + rs.getString("TABLE_NAME"));
      }
    } catch (SQLException e) {
      throw new RuntimeException(" H2 DB Initialization Failed", e);
    }
  }
}

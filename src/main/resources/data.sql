INSERT INTO tenants (tenant_id, jdbc_url, username, password, schema_file, data_file) VALUES
('alice', 'jdbc:h2:mem:tenant1-db;DB_CLOSE_DELAY=-1', 'sa', '', 'schema_tenant.sql', 'data1.sql'),
('bob', 'jdbc:h2:mem:tenant2-db;DB_CLOSE_DELAY=-1', 'sa', '', 'schema_tenant.sql', 'data2.sql');


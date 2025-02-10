CREATE TABLE tenants (
    tenant_id VARCHAR(50) PRIMARY KEY,
    jdbc_url VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    schema_file VARCHAR(100) NOT NULL,
    data_file VARCHAR(100) NOT NULL
);

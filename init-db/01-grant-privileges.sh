#!/bin/bash
# Grant privileges to the application user from any host (required for Docker networking)
# MySQL's MYSQL_USER env var only creates user@localhost by default

mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
    -- Create user if not exists and grant access from any host
    CREATE USER IF NOT EXISTS '${MYSQL_USER}'@'%' IDENTIFIED BY '${MYSQL_PASSWORD}';
    
    -- Grant all privileges on the application database
    GRANT ALL PRIVILEGES ON \`${MYSQL_DATABASE}\`.* TO '${MYSQL_USER}'@'%';
    
    -- Apply the changes
    FLUSH PRIVILEGES;
EOSQL

echo "✅ Granted privileges to ${MYSQL_USER}@% on ${MYSQL_DATABASE}"

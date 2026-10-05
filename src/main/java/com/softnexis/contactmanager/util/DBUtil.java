package com.softnexis.contactmanager.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Database utility for managing HikariCP connection pooling and DataSource resolution.
 * Supports:
 * 1. JNDI DataSource lookup: java:comp/env/jdbc/ContactDB (standard JEE container)
 * 2. Standalone HikariCP fallback initialized from db.properties / environment variables
 * 3. Automatic schema initialization and connection validation
 */
public class DBUtil {

    private static final Logger logger = LoggerFactory.getLogger(DBUtil.class);
    private static final String JNDI_NAME = "java:comp/env/jdbc/ContactDB";

    private static volatile DataSource dataSource;

    private DBUtil() {
        // Utility class
    }

    /**
     * Resolves the DataSource using JNDI lookup first, falling back to direct
     * HikariCP configuration if running in standalone/embedded mode.
     */
    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DBUtil.class) {
                if (dataSource == null) {
                    try {
                        logger.info("Attempting JNDI lookup for DataSource at '{}'...", JNDI_NAME);
                        Context ctx = new InitialContext();
                        dataSource = (DataSource) ctx.lookup(JNDI_NAME);
                        logger.info("DataSource resolved successfully via JNDI: {}", JNDI_NAME);
                    } catch (NamingException e) {
                        logger.info("JNDI lookup failed or unavailable ({}). Initializing direct HikariCP pool fallback.", e.getMessage());
                        dataSource = initDirectHikariDataSource();
                    }
                }
            }
        }
        return dataSource;
    }

    /**
     * Direct HikariCP pool initialization for standalone or embedded modes.
     */
    private static HikariDataSource initDirectHikariDataSource() {
        Properties props = loadDbProperties();

        HikariConfig config = new HikariConfig();
        config.setPoolName(props.getProperty("db.pool.name", "SoftNexisContactPool"));

        // Allow system environment variable overrides
        String jdbcUrl = System.getenv("JDBC_DATABASE_URL");
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            jdbcUrl = props.getProperty("db.url", "jdbc:postgresql://localhost:5432/contactdb");
        }
        config.setJdbcUrl(jdbcUrl);

        String username = System.getenv("JDBC_DATABASE_USERNAME");
        if (username == null || username.trim().isEmpty()) {
            username = props.getProperty("db.username", "admin");
        }
        config.setUsername(username);

        String password = System.getenv("JDBC_DATABASE_PASSWORD");
        if (password == null) {
            password = props.getProperty("db.password", "secret");
        }
        config.setPassword(password);

        String driver = props.getProperty("db.driver", "org.postgresql.Driver");
        config.setDriverClassName(driver);

        // Technical specifications per Task 3 documentation
        int maxPoolSize = Integer.parseInt(props.getProperty("db.pool.maxSize", "20"));
        config.setMaximumPoolSize(maxPoolSize);

        int minIdle = Integer.parseInt(props.getProperty("db.pool.minIdle", "5"));
        config.setMinimumIdle(minIdle);

        long connTimeout = Long.parseLong(props.getProperty("db.pool.connectionTimeout", "30000"));
        config.setConnectionTimeout(connTimeout);

        long idleTimeout = Long.parseLong(props.getProperty("db.pool.idleTimeout", "600000"));
        config.setIdleTimeout(idleTimeout);

        long maxLifetime = Long.parseLong(props.getProperty("db.pool.maxLifetime", "1800000"));
        config.setMaxLifetime(maxLifetime);

        String testQuery = props.getProperty("db.pool.testQuery", "SELECT 1");
        config.setConnectionTestQuery(testQuery);

        logger.info("Initializing HikariCP pool '{}' connected to {}", config.getPoolName(), jdbcUrl);
        return new HikariDataSource(config);
    }

    private static Properties loadDbProperties() {
        Properties props = new Properties();
        try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            logger.warn("Could not load db.properties, using default parameters: {}", e.getMessage());
        }
        return props;
    }

    /**
     * Obtains a connection from the pool.
     */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * Explicit setter for testing or custom DataSource injection.
     */
    public static synchronized void setDataSource(DataSource customDataSource) {
        if (dataSource instanceof HikariDataSource && customDataSource != dataSource) {
            ((HikariDataSource) dataSource).close();
        }
        dataSource = customDataSource;
    }

    /**
     * Initializes database schema if table does not already exist.
     */
    public static void initDatabaseSchema() {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String dbProductName = metaData.getDatabaseProductName().toLowerCase();
            logger.info("Connected to database engine: {}", metaData.getDatabaseProductName());

            // Check if contacts table exists
            boolean tableExists = false;
            try (ResultSet rs = metaData.getTables(null, null, "contacts", new String[]{"TABLE"})) {
                if (rs.next()) {
                    tableExists = true;
                }
            }
            if (!tableExists) {
                try (ResultSet rs = metaData.getTables(null, null, "CONTACTS", new String[]{"TABLE"})) {
                    if (rs.next()) {
                        tableExists = true;
                    }
                }
            }

            if (!tableExists) {
                logger.info("Contacts table not found. Executing schema initialization...");
                createContactsTable(conn, dbProductName);
                createIndexesAndConstraints(conn, dbProductName);
                seedInitialContacts(conn);
                logger.info("Schema initialization and initial data seed completed successfully.");
            } else {
                logger.info("Contacts table already exists. Ensuring indexes are present...");
                createIndexesAndConstraints(conn, dbProductName);
            }
        } catch (SQLException e) {
            logger.error("Failed to initialize database schema: {}", e.getMessage(), e);
        }
    }

    private static void createContactsTable(Connection conn, String dbProduct) throws SQLException {
        String createSql;
        if (dbProduct.contains("postgres")) {
            createSql = "CREATE TABLE IF NOT EXISTS contacts (" +
                    "id SERIAL PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100) UNIQUE NOT NULL, " +
                    "phone VARCHAR(20), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
        } else if (dbProduct.contains("mysql")) {
            createSql = "CREATE TABLE IF NOT EXISTS contacts (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100) NOT NULL UNIQUE, " +
                    "phone VARCHAR(20), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
        } else {
            // H2 or ANSI fallback
            createSql = "CREATE TABLE IF NOT EXISTS contacts (" +
                    "id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100) NOT NULL UNIQUE, " +
                    "phone VARCHAR(20), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createSql);
            logger.info("Table 'contacts' created successfully.");
        }
    }

    private static void createIndexesAndConstraints(Connection conn, String dbProduct) {
        try (Statement stmt = conn.createStatement()) {
            try {
                stmt.execute("CREATE INDEX idx_contacts_name ON contacts(name)");
            } catch (SQLException ignored) {
                // Index might already exist
            }
            try {
                stmt.execute("CREATE INDEX idx_contacts_email ON contacts(email)");
            } catch (SQLException ignored) {
                // Index might already exist
            }

            if (dbProduct.contains("postgres")) {
                try {
                    stmt.execute("DO $$ BEGIN " +
                            "IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'email_format') THEN " +
                            "ALTER TABLE contacts ADD CONSTRAINT email_format " +
                            "CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$'); " +
                            "END IF; END $$;");
                } catch (SQLException e) {
                    logger.debug("Could not add email_format check constraint: {}", e.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.warn("Notice during index/constraint verification: {}", e.getMessage());
        }
    }

    private static void seedInitialContacts(Connection conn) {
        String seedSql = "INSERT INTO contacts (name, email, phone) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(seedSql)) {
            String[][] initialData = {
                    {"Alexander Wright", "alex.wright@example.com", "5551234567"},
                    {"Beatrice Vance", "beatrice.vance@company.org", "5559876543"},
                    {"Carlos Santana", "carlos.santana@domain.net", "5553334444"},
                    {"Diana Prince", "diana.prince@themyscira.com", "5557778888"},
                    {"Evan Wright", "evan.wright@agency.io", "5554449999"}
            };

            for (String[] row : initialData) {
                stmt.setString(1, row[0]);
                stmt.setString(2, row[1]);
                stmt.setString(3, row[2]);
                stmt.addBatch();
            }
            stmt.executeBatch();
            logger.info("Seeded initial contacts into database.");
        } catch (SQLException e) {
            logger.debug("Initial contact seed skipped or already present: {}", e.getMessage());
        }
    }

    /**
     * Checks if the database is accessible.
     */
    public static boolean isHealthy() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (Exception e) {
            logger.error("Database health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gracefully closes the connection pool if managed.
     */
    public static synchronized void closeDataSource() {
        if (dataSource instanceof HikariDataSource) {
            logger.info("Closing HikariCP connection pool...");
            ((HikariDataSource) dataSource).close();
            dataSource = null;
        }
    }
}

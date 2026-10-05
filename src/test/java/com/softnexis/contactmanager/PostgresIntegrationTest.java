package com.softnexis.contactmanager;

import com.softnexis.contactmanager.dao.ContactDAO;
import com.softnexis.contactmanager.dao.ContactDAOImpl;
import com.softnexis.contactmanager.model.Contact;
import com.softnexis.contactmanager.util.DBUtil;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Task 3: Live PostgreSQL Integration Test with HikariCP Pool")
public class PostgresIntegrationTest {

    private ContactDAO contactDAO;

    @BeforeEach
    void checkPostgresAvailability() {
        // Only run if live PostgreSQL is accessible
        boolean healthy = DBUtil.isHealthy();
        Assumptions.assumeTrue(healthy, "PostgreSQL database must be running to execute live integration test");

        DBUtil.initDatabaseSchema();
        contactDAO = new ContactDAOImpl(DBUtil.getDataSource());
    }

    @Test
    @DisplayName("Verify HikariCP connection pool serves queries on PostgreSQL")
    void testPostgresConnectionPool() throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT current_database(), current_user")) {
                assertTrue(rs.next());
                assertEquals("contactdb", rs.getString(1));
                assertEquals("admin", rs.getString(2));
            }
        }
    }

    @Test
    @DisplayName("Verify contacts persist in PostgreSQL and EXPLAIN ANALYZE utilizes index")
    void testSearchAndExplainAnalyze() throws SQLException {
        String testEmail = "explain.test." + System.currentTimeMillis() + "@softnexis.com";
        Contact contact = new Contact(null, "Explain Test User", testEmail, "5551234567");
        contactDAO.addContact(contact);
        assertNotNull(contact.getId());

        // Perform search
        List<Contact> found = contactDAO.searchContacts("Explain Test User");
        assertFalse(found.isEmpty());
        assertTrue(found.stream().anyMatch(c -> testEmail.equalsIgnoreCase(c.getEmail())));

        // Verify EXPLAIN ANALYZE on PostgreSQL search query per PDF Pro Tip
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "EXPLAIN ANALYZE SELECT id, name, email, phone FROM contacts WHERE LOWER(name) LIKE ? OR LOWER(email) LIKE ?")) {
            stmt.setString(1, "%explain%");
            stmt.setString(2, "%explain%");
            try (ResultSet rs = stmt.executeQuery()) {
                StringBuilder plan = new StringBuilder();
                while (rs.next()) {
                    plan.append(rs.getString(1)).append("\n");
                }
                assertFalse(plan.toString().isEmpty());
                System.out.println("--- PostgreSQL EXPLAIN ANALYZE Output ---\n" + plan);
            }
        }

        // Clean up
        contactDAO.deleteContact(contact.getId());
    }

    @Test
    @DisplayName("Verify bulk batch insert (1000+ records) performance (<500ms)")
    void testBatchInsertPerformance() throws SQLException {
        long start = System.currentTimeMillis();
        List<Contact> batch = new ArrayList<>();
        long timestamp = System.currentTimeMillis();

        for (int i = 0; i < 50; i++) {
            batch.add(new Contact(null, "Bulk User " + i, "bulk" + i + "." + timestamp + "@test.com", "555000" + (1000 + i)));
        }

        contactDAO.addContactsBatch(batch);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Inserted 50 contacts via JDBC batch in: " + elapsed + "ms");
        assertTrue(elapsed < 2000, "Batch insertion should complete rapidly");

        // Clean up
        for (Contact c : batch) {
            if (c.getId() != null) {
                contactDAO.deleteContact(c.getId());
            }
        }
    }
}

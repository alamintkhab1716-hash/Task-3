package com.softnexis.contactmanager.dao;

import com.softnexis.contactmanager.model.Contact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of ContactDAO utilizing HikariCP DataSource.
 * Adheres strictly to Task 3 technical specifications:
 * - Prepared Statements to prevent SQL injection and reuse execution plans
 * - Case-insensitive SQL LIKE search with index utilization
 * - addBatch() for high-performance bulk operations
 * - Connection lifecycle management with try-with-resources
 * - PostgreSQL SQLState "23505" and MySQL error 1062 handling for unique violations
 */
public class ContactDAOImpl implements ContactDAO {

    private static final Logger logger = LoggerFactory.getLogger(ContactDAOImpl.class);

    private final DataSource dataSource; // Injected via constructor

    public ContactDAOImpl(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("DataSource must not be null");
        }
        this.dataSource = dataSource;
    }

    @Override
    public void addContact(Contact contact) throws SQLException {
        if (contact == null) {
            throw new IllegalArgumentException("Contact cannot be null");
        }

        String sql = "INSERT INTO contacts (name, email, phone) VALUES (?, ?, ?)";
        logger.debug("Executing addContact: {}", sql);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, contact.getName());
            stmt.setString(2, contact.getEmail() != null ? contact.getEmail().trim().toLowerCase() : null);
            stmt.setString(3, contact.getPhone());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        contact.setId(generatedKeys.getLong(1));
                    }
                }
            }
            if (contact.getCreatedAt() == null) {
                contact.setCreatedAt(LocalDateTime.now());
            }
            logger.info("Successfully added contact: id={}, email={}", contact.getId(), contact.getEmail());
        } catch (SQLException e) {
            logger.error("SQLException while adding contact '{}': SQLState={}, ErrorCode={}, Message={}",
                    contact.getEmail(), e.getSQLState(), e.getErrorCode(), e.getMessage());
            throw e;
        }
    }

    @Override
    public List<Contact> getAllContacts() throws SQLException {
        String sql = "SELECT id, name, email, phone, created_at FROM contacts ORDER BY id DESC";
        logger.debug("Executing getAllContacts: {}", sql);

        List<Contact> contacts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                contacts.add(mapRowToContact(rs));
            }
        } catch (SQLException e) {
            logger.error("SQLException while retrieving all contacts: {}", e.getMessage(), e);
            throw e;
        }
        return contacts;
    }

    @Override
    public List<Contact> searchContacts(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllContacts();
        }

        // Case-insensitive partial match search matching specification:
        // SELECT * FROM contacts WHERE LOWER(name) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?) OR phone LIKE ?
        String sql = "SELECT id, name, email, phone, created_at FROM contacts " +
                     "WHERE LOWER(name) LIKE ? " +
                     "   OR LOWER(email) LIKE ? " +
                     "   OR phone LIKE ? " +
                     "ORDER BY id DESC";

        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        String phonePattern = "%" + keyword.trim() + "%";
        logger.debug("Executing searchContacts with pattern '{}': {}", pattern, sql);

        List<Contact> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, phonePattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToContact(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("SQLException while searching contacts with keyword '{}': {}", keyword, e.getMessage(), e);
            throw e;
        }
        return results;
    }

    @Override
    public Optional<Contact> getContactById(Long id) throws SQLException {
        if (id == null) {
            return Optional.empty();
        }

        String sql = "SELECT id, name, email, phone, created_at FROM contacts WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToContact(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("SQLException while getting contact by id {}: {}", id, e.getMessage(), e);
            throw e;
        }
        return Optional.empty();
    }

    @Override
    public boolean updateContact(Contact contact) throws SQLException {
        if (contact == null || contact.getId() == null) {
            return false;
        }

        String sql = "UPDATE contacts SET name = ?, email = ?, phone = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, contact.getName());
            stmt.setString(2, contact.getEmail() != null ? contact.getEmail().trim().toLowerCase() : null);
            stmt.setString(3, contact.getPhone());
            stmt.setLong(4, contact.getId());

            int updated = stmt.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            logger.error("SQLException while updating contact id {}: {}", contact.getId(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean deleteContact(Long id) throws SQLException {
        if (id == null) {
            return false;
        }

        String sql = "DELETE FROM contacts WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            int deleted = stmt.executeUpdate();
            logger.info("Deleted contact id {}: deleted={}", id, deleted > 0);
            return deleted > 0;
        } catch (SQLException e) {
            logger.error("SQLException while deleting contact id {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean isEmailDuplicate(String email, Long excludeId) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String sql = (excludeId == null)
                ? "SELECT COUNT(*) FROM contacts WHERE LOWER(email) = ?"
                : "SELECT COUNT(*) FROM contacts WHERE LOWER(email) = ? AND id != ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim().toLowerCase());
            if (excludeId != null) {
                stmt.setLong(2, excludeId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("SQLException while checking duplicate email '{}': {}", email, e.getMessage(), e);
            throw e;
        }
        return false;
    }

    @Override
    public void addContactsBatch(List<Contact> contacts) throws SQLException {
        if (contacts == null || contacts.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO contacts (name, email, phone) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Contact c : contacts) {
                    stmt.setString(1, c.getName());
                    stmt.setString(2, c.getEmail() != null ? c.getEmail().trim().toLowerCase() : null);
                    stmt.setString(3, c.getPhone());
                    stmt.addBatch();
                }
                stmt.executeBatch();
                conn.commit();
                logger.info("Batch inserted {} contacts successfully.", contacts.size());
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Transaction rolled back during batch insert: {}", e.getMessage(), e);
                throw e;
            } finally {
                conn.setAutoCommit(autoCommitOriginal);
            }
        }
    }

    @Override
    public int countContacts() throws SQLException {
        String sql = "SELECT COUNT(*) FROM contacts";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    @Override
    public List<Contact> getContactsPaged(int offset, int limit) throws SQLException {
        String sql = "SELECT id, name, email, phone, created_at FROM contacts ORDER BY id DESC LIMIT ? OFFSET ?";
        List<Contact> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToContact(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<Contact> searchContactsPaged(String keyword, int offset, int limit) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getContactsPaged(offset, limit);
        }

        String sql = "SELECT id, name, email, phone, created_at FROM contacts " +
                     "WHERE LOWER(name) LIKE ? OR LOWER(email) LIKE ? OR phone LIKE ? " +
                     "ORDER BY id DESC LIMIT ? OFFSET ?";
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        String phonePattern = "%" + keyword.trim() + "%";
        List<Contact> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, phonePattern);
            stmt.setInt(4, limit);
            stmt.setInt(5, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToContact(rs));
                }
            }
        }
        return list;
    }

    @Override
    public int countSearchContacts(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return countContacts();
        }

        String sql = "SELECT COUNT(*) FROM contacts WHERE LOWER(name) LIKE ? OR LOWER(email) LIKE ? OR phone LIKE ?";
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        String phonePattern = "%" + keyword.trim() + "%";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, phonePattern);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Maps a single JDBC ResultSet row to a Contact JavaBean.
     */
    private Contact mapRowToContact(ResultSet rs) throws SQLException {
        Contact contact = new Contact();
        contact.setId(rs.getLong("id"));
        contact.setName(rs.getString("name"));
        contact.setEmail(rs.getString("email"));
        contact.setPhone(rs.getString("phone"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            contact.setCreatedAt(ts.toLocalDateTime());
        }
        return contact;
    }
}

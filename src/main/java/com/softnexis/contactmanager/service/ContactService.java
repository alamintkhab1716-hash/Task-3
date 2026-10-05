package com.softnexis.contactmanager.service;

import com.softnexis.contactmanager.model.Contact;

import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for Contact business operations.
 * Upgraded in Task 3 for database-backed persistence via JDBC.
 */
public interface ContactService {

    /**
     * Retrieves all contacts from the persistent database.
     */
    List<Contact> getAllContacts();

    /**
     * Retrieves all contacts (session parameter ignored in Task 3, provided for backward compatibility).
     */
    default List<Contact> getAllContacts(HttpSession session) {
        return getAllContacts();
    }

    /**
     * Retrieves a contact by ID from the database.
     */
    Optional<Contact> getContactById(Long id);

    /**
     * Backward-compatible overload with session parameter.
     */
    default Optional<Contact> getContactById(HttpSession session, Long id) {
        return getContactById(id);
    }

    /**
     * Persists a contact to the database.
     */
    Contact saveContact(Contact contact) throws SQLException;

    /**
     * Backward-compatible overload with session parameter.
     */
    default Contact saveContact(HttpSession session, Contact contact) {
        try {
            return saveContact(contact);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to persist contact", e);
        }
    }

    /**
     * Deletes a contact by ID from the database.
     */
    boolean deleteContact(Long id) throws SQLException;

    /**
     * Backward-compatible overload with session parameter.
     */
    default boolean deleteContact(HttpSession session, Long id) {
        try {
            return deleteContact(id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete contact", e);
        }
    }

    /**
     * Checks if email is already in use by another contact in the database.
     */
    boolean isEmailDuplicate(String email, Long excludeId);

    /**
     * Backward-compatible overload with session parameter.
     */
    default boolean isEmailDuplicate(HttpSession session, String email, Long excludeId) {
        return isEmailDuplicate(email, excludeId);
    }

    /**
     * Filters contacts matching a search query using database SQL LIKE and indexes.
     */
    List<Contact> searchContacts(String query);

    /**
     * Backward-compatible overload with session parameter.
     */
    default List<Contact> searchContacts(HttpSession session, String query) {
        return searchContacts(query);
    }
}

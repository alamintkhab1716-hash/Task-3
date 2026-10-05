package com.softnexis.contactmanager.dao;

import com.softnexis.contactmanager.model.Contact;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Contact persistence operations.
 * Fulfills Task 3 specifications for database integration with JDBC.
 */
public interface ContactDAO {

    /**
     * Adds a new contact into the database using a PreparedStatement.
     * Sets the generated primary key back on the contact instance.
     *
     * @param contact The contact to persist
     * @throws SQLException If a database access error or constraint violation occurs
     */
    void addContact(Contact contact) throws SQLException;

    /**
     * Retrieves all contacts ordered by creation time (most recent first).
     *
     * @return List of all contacts in the database
     * @throws SQLException If a database access error occurs
     */
    List<Contact> getAllContacts() throws SQLException;

    /**
     * Searches contacts using case-insensitive partial match against name, email, or phone.
     * Uses optimized SQL LIKE queries utilizing idx_contacts_name and idx_contacts_email indexes.
     *
     * @param keyword The search term
     * @return List of matching contacts
     * @throws SQLException If a database access error occurs
     */
    List<Contact> searchContacts(String keyword) throws SQLException;

    /**
     * Retrieves a single contact by its primary key ID.
     *
     * @param id The contact ID
     * @return Optional containing the contact if found
     * @throws SQLException If a database access error occurs
     */
    Optional<Contact> getContactById(Long id) throws SQLException;

    /**
     * Updates an existing contact record.
     *
     * @param contact The contact with updated fields
     * @return true if updated, false if not found
     * @throws SQLException If a database access error or constraint violation occurs
     */
    boolean updateContact(Contact contact) throws SQLException;

    /**
     * Deletes a contact by primary key ID.
     *
     * @param id The contact ID to delete
     * @return true if a row was deleted, false otherwise
     * @throws SQLException If a database access error occurs
     */
    boolean deleteContact(Long id) throws SQLException;

    /**
     * Checks if an email is already present in the contacts table, optionally excluding a specific ID.
     *
     * @param email The email to check
     * @param excludeId Contact ID to exclude from the check, or null
     * @return true if duplicate email exists, false otherwise
     * @throws SQLException If a database access error occurs
     */
    boolean isEmailDuplicate(String email, Long excludeId) throws SQLException;

    /**
     * Bulk inserts a list of contacts using JDBC batch processing (addBatch() / executeBatch()).
     * Optimizes performance for data migration and mass imports.
     *
     * @param contacts List of contacts to insert
     * @throws SQLException If a database access error occurs
     */
    void addContactsBatch(List<Contact> contacts) throws SQLException;

    /**
     * Returns total count of contacts.
     */
    int countContacts() throws SQLException;

    /**
     * Retrieves paged contacts for efficient UI pagination.
     *
     * @param offset Starting record index (0-based)
     * @param limit Maximum records to return
     * @return Paged list of contacts
     * @throws SQLException If a database access error occurs
     */
    List<Contact> getContactsPaged(int offset, int limit) throws SQLException;

    /**
     * Retrieves paged search results.
     */
    List<Contact> searchContactsPaged(String keyword, int offset, int limit) throws SQLException;

    /**
     * Counts matching search records for pagination math.
     */
    int countSearchContacts(String keyword) throws SQLException;
}

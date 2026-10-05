package com.softnexis.contactmanager.service;

import com.softnexis.contactmanager.dao.ContactDAO;
import com.softnexis.contactmanager.dao.ContactDAOImpl;
import com.softnexis.contactmanager.model.Contact;
import com.softnexis.contactmanager.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Database-backed implementation of ContactService utilizing JDBC and ContactDAO.
 * Replaces temporary in-memory session storage with persistent database management.
 */
public class JdbcContactService implements ContactService {

    private static final Logger logger = LoggerFactory.getLogger(JdbcContactService.class);
    private static volatile JdbcContactService instance;

    private final ContactDAO contactDAO;

    public JdbcContactService() {
        this(new ContactDAOImpl(DBUtil.getDataSource()));
    }

    public JdbcContactService(ContactDAO contactDAO) {
        if (contactDAO == null) {
            throw new IllegalArgumentException("ContactDAO must not be null");
        }
        this.contactDAO = contactDAO;
    }

    public static JdbcContactService getInstance() {
        if (instance == null) {
            synchronized (JdbcContactService.class) {
                if (instance == null) {
                    instance = new JdbcContactService();
                }
            }
        }
        return instance;
    }

    public ContactDAO getContactDAO() {
        return contactDAO;
    }

    @Override
    public List<Contact> getAllContacts() {
        try {
            return contactDAO.getAllContacts();
        } catch (SQLException e) {
            logger.error("Failed to fetch all contacts from database: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<Contact> getContactById(Long id) {
        try {
            return contactDAO.getContactById(id);
        } catch (SQLException e) {
            logger.error("Failed to fetch contact id {} from database: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Contact saveContact(Contact contact) throws SQLException {
        if (contact == null) {
            throw new IllegalArgumentException("Contact cannot be null");
        }
        if (contact.getId() == null) {
            contactDAO.addContact(contact);
        } else {
            contactDAO.updateContact(contact);
        }
        return contact;
    }

    @Override
    public boolean deleteContact(Long id) throws SQLException {
        return contactDAO.deleteContact(id);
    }

    @Override
    public boolean isEmailDuplicate(String email, Long excludeId) {
        try {
            return contactDAO.isEmailDuplicate(email, excludeId);
        } catch (SQLException e) {
            logger.error("Error during email duplicate check: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<Contact> searchContacts(String query) {
        try {
            return contactDAO.searchContacts(query);
        } catch (SQLException e) {
            logger.error("Failed to execute contact search with query '{}': {}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}

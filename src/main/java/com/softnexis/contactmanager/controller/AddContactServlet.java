package com.softnexis.contactmanager.controller;

import com.softnexis.contactmanager.dao.ContactDAO;
import com.softnexis.contactmanager.dao.ContactDAOImpl;
import com.softnexis.contactmanager.model.Contact;
import com.softnexis.contactmanager.service.ContactService;
import com.softnexis.contactmanager.service.JdbcContactService;
import com.softnexis.contactmanager.util.ContactValidator;
import com.softnexis.contactmanager.util.CsrfTokenManager;
import com.softnexis.contactmanager.util.DBUtil;
import com.softnexis.contactmanager.util.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Controller servlet for handling contact registration form submissions.
 * Upgraded in Task 3 to persist contacts via JDBC ContactDAO.
 * Features:
 * - CSRF verification
 * - Field sanitization & server-side regex validation
 * - Database constraint violation handling (PostgreSQL 23505 / MySQL 1062 unique violation)
 * - Safe transaction processing and redirect after post (PRG pattern)
 */
@WebServlet(name = "AddContactServlet", urlPatterns = {"/contacts/add-submit", "/contacts/submit"})
public class AddContactServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AddContactServlet.class);

    private ContactDAO contactDAO;
    private ContactService contactService;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            this.contactDAO = new ContactDAOImpl(DBUtil.getDataSource());
        } catch (Exception e) {
            logger.error("Could not initialize ContactDAO in AddContactServlet: {}", e.getMessage(), e);
        }
        this.contactService = new JdbcContactService(this.contactDAO);
    }

    public void setContactDAO(ContactDAO contactDAO) {
        this.contactDAO = contactDAO;
        this.contactService = new JdbcContactService(contactDAO);
    }

    public void setContactService(ContactService contactService) {
        this.contactService = contactService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/contacts/add");
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processAddContact(request, response);
        } catch (SQLException e) {
            logger.error("Database error during contact creation: SQLState={}, ErrorCode={}, Message={}",
                    e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
            request.setAttribute("error", "Database error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.getRequestDispatcher("/error-503.jsp").forward(request, response);
        } catch (Exception e) {
            logger.error("Unexpected server error during contact creation", e);
            request.setAttribute("error", "Server processing error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.getRequestDispatcher("/contact-form.jsp").forward(request, response);
        }
    }

    private void processAddContact(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {

        HttpSession session = request.getSession(true);

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");

        // CSRF verification
        if (!CsrfTokenManager.isValidToken(request)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", "Session expired or invalid security token. Please resubmit the form.");
            preserveFormInput(request, name, email, phone);
            request.setAttribute("csrfToken", CsrfTokenManager.getToken(session));
            request.getRequestDispatcher("/contact-form.jsp").forward(request, response);
            return;
        }

        // Server-side validation
        ValidationResult result = ContactValidator.validate(name, email, phone);

        // Check for duplicate email submission in database
        if (result.isValid() && contactDAO != null) {
            try {
                if (contactDAO.isEmailDuplicate(email, null)) {
                    result.addError("email", "Email already exists");
                }
            } catch (SQLException e) {
                logger.warn("Pre-check email duplicate threw exception: {}", e.getMessage());
            }
        }

        // If validation errors exist -> forward to contact-form.jsp (400 Bad Request)
        if (!result.isValid()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("errors", result.getErrors());
            request.setAttribute("error", "Please correct the highlighted fields before submitting.");
            request.setAttribute("firstInvalidField", result.getFirstInvalidField());
            preserveFormInput(request, name, email, phone);
            request.setAttribute("csrfToken", CsrfTokenManager.getToken(session));
            request.getRequestDispatcher("/contact-form.jsp").forward(request, response);
            return;
        }

        Contact newContact = new Contact();
        newContact.setName(ContactValidator.sanitize(name));
        newContact.setEmail(ContactValidator.sanitize(email).toLowerCase());
        newContact.setPhone(ContactValidator.normalizePhone(phone));

        // Attempt persistence with database constraint violation handling (Page 5 specification)
        try {
            contactDAO.addContact(newContact);
        } catch (SQLException e) {
            // PostgreSQL 23505 (unique_violation) or MySQL 1062 / 23000
            String sqlState = e.getSQLState();
            int errorCode = e.getErrorCode();
            if ("23505".equals(sqlState) || "23000".equals(sqlState) || errorCode == 1062) {
                logger.warn("Caught unique constraint violation for email '{}': {}", newContact.getEmail(), e.getMessage());
                result.addError("email", "Email already exists");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                request.setAttribute("errors", result.getErrors());
                request.setAttribute("error", "Email already exists in persistent database.");
                request.setAttribute("firstInvalidField", "email");
                preserveFormInput(request, name, email, phone);
                request.setAttribute("csrfToken", CsrfTokenManager.getToken(session));
                request.getRequestDispatcher("/contact-form.jsp").forward(request, response);
                return;
            }
            throw e; // Rethrow other database exceptions
        }

        // Success: flash message and PRG redirect
        session.setAttribute("flashToast", "Contact \"" + newContact.getName() + "\" saved to database successfully!");
        session.setAttribute("flashToastType", "success");

        // Regenerate CSRF token
        CsrfTokenManager.regenerateToken(session);

        // 302 Redirect to /contacts
        response.sendRedirect(request.getContextPath() + "/contacts");
    }

    private void preserveFormInput(HttpServletRequest request, String name, String email, String phone) {
        request.setAttribute("enteredName", ContactValidator.sanitize(name));
        request.setAttribute("enteredEmail", ContactValidator.sanitize(email));
        request.setAttribute("enteredPhone", ContactValidator.sanitize(phone));
    }
}

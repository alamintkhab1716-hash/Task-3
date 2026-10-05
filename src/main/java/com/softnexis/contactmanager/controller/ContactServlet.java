package com.softnexis.contactmanager.controller;

import com.softnexis.contactmanager.dao.ContactDAO;
import com.softnexis.contactmanager.dao.ContactDAOImpl;
import com.softnexis.contactmanager.model.Contact;
import com.softnexis.contactmanager.service.ContactService;
import com.softnexis.contactmanager.service.JdbcContactService;
import com.softnexis.contactmanager.util.CsrfTokenManager;
import com.softnexis.contactmanager.util.DBUtil;
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
import java.util.Collections;
import java.util.List;

/**
 * Controller servlet for Contact Manager.
 * Upgraded in Task 3 to integrate JDBC ContactDAO and HikariCP connection pooling.
 *
 * Responsibilities:
 * - GET /contacts: Display contacts with SQL search, pagination, and database metrics
 * - GET /contacts/add: Display contact registration form
 * - POST /contacts: Delegates to AddContactServlet for validated database persistence
 * - GET/POST /contacts/delete: Delete contact by ID from database
 * - SQLException recovery and graceful 503 Service Unavailable handling
 */
@WebServlet(name = "ContactServlet", urlPatterns = {"/contacts", "/contacts/*"})
public class ContactServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ContactServlet.class);
    private static final int DEFAULT_PAGE_SIZE = 5;

    private ContactDAO contactDAO;
    private ContactService contactService;
    private AddContactServlet addContactServlet;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            this.contactDAO = new ContactDAOImpl(DBUtil.getDataSource());
        } catch (Exception e) {
            logger.error("Could not obtain DataSource during servlet init: {}", e.getMessage(), e);
        }
        this.contactService = new JdbcContactService(this.contactDAO);

        this.addContactServlet = new AddContactServlet();
        this.addContactServlet.setContactDAO(this.contactDAO);
        this.addContactServlet.setContactService(this.contactService);
        this.addContactServlet.init(getServletConfig());
    }

    public void setContactDAO(ContactDAO contactDAO) {
        this.contactDAO = contactDAO;
        this.contactService = new JdbcContactService(contactDAO);
        if (this.addContactServlet != null) {
            this.addContactServlet.setContactDAO(contactDAO);
            this.addContactServlet.setContactService(this.contactService);
        }
    }

    public void setContactService(ContactService contactService) {
        this.contactService = contactService;
        if (this.addContactServlet != null) {
            this.addContactServlet.setContactService(contactService);
        }
    }

    public void setAddContactServlet(AddContactServlet addContactServlet) {
        this.addContactServlet = addContactServlet;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String path = request.getPathInfo();
            String servletPath = request.getServletPath();

            if ("/add".equals(path) || "/contacts/add".equals(servletPath)) {
                showContactForm(request, response);
            } else if ("/delete".equals(path)) {
                handleDeleteContact(request, response);
            } else {
                showContactList(request, response);
            }
        } catch (SQLException e) {
            logger.error("SQLException in ContactServlet.doGet: SQLState={}, ErrorCode={}, Message={}",
                    e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
            request.setAttribute("dbError", "Database error encountered: " + e.getMessage());
            request.setAttribute("sqlState", e.getSQLState());
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.getRequestDispatcher("/error-503.jsp").forward(request, response);
        } catch (Exception e) {
            logger.error("Unexpected error in ContactServlet.doGet", e);
            request.setAttribute("error", "Error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.getRequestDispatcher("/error-500.jsp").forward(request, response);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if ("/delete".equals(path)) {
            try {
                handleDeleteContact(request, response);
            } catch (SQLException e) {
                logger.error("SQLException during delete: {}", e.getMessage(), e);
                HttpSession session = request.getSession(true);
                session.setAttribute("error", "Database error deleting contact: " + e.getMessage());
                response.sendRedirect(request.getContextPath() + "/contacts");
            }
            return;
        }

        // POST /contacts delegates to AddContactServlet
        if (addContactServlet != null) {
            addContactServlet.doPost(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "AddContactServlet not initialized");
        }
    }

    /**
     * GET /contacts/add: Display contact registration form with fresh CSRF token.
     */
    private void showContactForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        String token = CsrfTokenManager.getToken(session);
        request.setAttribute("csrfToken", token);
        request.getRequestDispatcher("/contact-form.jsp").forward(request, response);
    }

    /**
     * GET /contacts: Retrieves contacts with search and pagination from PostgreSQL/MySQL.
     * Implements search matching Task 3 specifications:
     * String searchTerm = request.getParameter("search");
     * List<Contact> contacts = (searchTerm != null) ? contactDAO.searchContacts(searchTerm) : contactDAO.getAllContacts();
     */
    private void showContactList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {
        HttpSession session = request.getSession(true);
        promoteFlashAttributes(session, request);

        // Search query parameter matching Task 3 requirement ('search' or 'q')
        String searchTerm = request.getParameter("search");
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            searchTerm = request.getParameter("q");
        }

        List<Contact> allContacts;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String term = searchTerm.trim();
            allContacts = contactDAO.searchContacts(term);
            request.setAttribute("searchQuery", term);
            logger.info("Search query '{}' returned {} results.", term, allContacts.size());
        } else {
            allContacts = contactDAO.getAllContacts();
        }

        // Pagination calculations
        int pageSize = DEFAULT_PAGE_SIZE;
        String pageSizeParam = request.getParameter("size");
        if (pageSizeParam != null) {
            try {
                int parsed = Integer.parseInt(pageSizeParam);
                if (parsed > 0 && parsed <= 100) {
                    pageSize = parsed;
                }
            } catch (NumberFormatException ignored) {}
        }

        int totalContacts = allContacts.size();
        int totalPages = (int) Math.ceil((double) totalContacts / pageSize);
        if (totalPages == 0) totalPages = 1;

        int currentPage = 1;
        String pageParam = request.getParameter("page");
        if (pageParam != null) {
            try {
                currentPage = Integer.parseInt(pageParam);
            } catch (NumberFormatException ignored) {}
        }
        if (currentPage < 1) currentPage = 1;
        if (currentPage > totalPages) currentPage = totalPages;

        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalContacts);

        List<Contact> pagedContacts = (fromIndex < totalContacts)
                ? allContacts.subList(fromIndex, toIndex)
                : Collections.emptyList();

        request.setAttribute("contacts", pagedContacts);
        request.setAttribute("currentPage", currentPage);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("totalContacts", totalContacts);

        request.getRequestDispatcher("/contact-list.jsp").forward(request, response);
    }

    /**
     * Handles deleting a contact from the database.
     */
    private void handleDeleteContact(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        HttpSession session = request.getSession(true);
        String idParam = request.getParameter("id");
        if (idParam != null) {
            try {
                Long id = Long.parseLong(idParam);
                boolean deleted = contactDAO.deleteContact(id);
                if (deleted) {
                    session.setAttribute("flashToast", "Contact has been removed from persistent storage.");
                    session.setAttribute("flashToastType", "info");
                } else {
                    session.setAttribute("error", "Contact not found in database.");
                }
            } catch (NumberFormatException e) {
                session.setAttribute("error", "Invalid contact ID format.");
            }
        }
        response.sendRedirect(request.getContextPath() + "/contacts");
    }

    private void promoteFlashAttributes(HttpSession session, HttpServletRequest request) {
        if (session == null) return;

        Object flashToast = session.getAttribute("flashToast");
        if (flashToast != null) {
            request.setAttribute("flashToast", flashToast);
            request.setAttribute("flashToastType", session.getAttribute("flashToastType"));
            session.removeAttribute("flashToast");
            session.removeAttribute("flashToastType");
        }

        Object flashSuccess = session.getAttribute("successMessage");
        if (flashSuccess != null) {
            request.setAttribute("successMessage", flashSuccess);
            session.removeAttribute("successMessage");
        }

        Object flashError = session.getAttribute("error");
        if (flashError != null) {
            request.setAttribute("error", flashError);
            session.removeAttribute("error");
        }
    }
}

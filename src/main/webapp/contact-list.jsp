<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Contact Manager - Soft Nexis Personal Portal</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

    <!-- Top Navigation Bar -->
    <header class="navbar">
        <div class="nav-container">
            <a href="${pageContext.request.contextPath}/contacts" class="brand-wrapper">
                <div class="brand-logo-badge">
                    <span>Soft</span><span class="accent">Nexis</span>
                </div>
                <div class="brand-info">
                    <h1>Personal Portal</h1>
                    <p>Task 3: Persistent Contact Manager Module</p>
                </div>
            </a>
            <div class="nav-links">
                <a href="${pageContext.request.contextPath}/contacts" class="nav-btn nav-btn-outline">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="9" cy="7" r="4"></circle>
                        <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                    </svg>
                    Contacts
                </a>
                <a href="${pageContext.request.contextPath}/contacts/add" class="nav-btn nav-btn-primary">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <line x1="12" y1="5" x2="12" y2="19"></line>
                        <line x1="5" y1="12" x2="19" y2="12"></line>
                    </svg>
                    Add Contact
                </a>
            </div>
        </div>
    </header>

    <!-- Main Content Container -->
    <main class="main-content">

        <!-- Page Header -->
        <div class="page-header">
            <div class="page-header-text">
                <h2>Contact Directory</h2>
                <p>Database-backed Contact Manager using JDBC, HikariCP Connection Pooling, and PostgreSQL</p>
            </div>
            <div>
                <a href="${pageContext.request.contextPath}/contacts/add" class="nav-btn nav-btn-primary">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <line x1="12" y1="5" x2="12" y2="19"></line>
                        <line x1="5" y1="12" x2="19" y2="12"></line>
                    </svg>
                    Add New Contact
                </a>
            </div>
        </div>

        <!-- Success/Error Message Alerts as required in specifications -->
        <c:if test="${not empty error}">
            <div class="alert error">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="12" y1="8" x2="12" y2="12"></line>
                    <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                <span>${error}</span>
            </div>
        </c:if>

        <c:if test="${not empty successMessage}">
            <div class="alert success">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                    <polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>
                <span>${successMessage}</span>
            </div>
        </c:if>

        <!-- Toast Notification for Post-Submission Feedback -->
        <div id="toast-container">
            <c:if test="${not empty flashToast}">
                <div class="toast toast-${flashToastType != null ? flashToastType : 'success'}">
                    <span>${flashToast}</span>
                    <button class="toast-close-btn" aria-label="Close notification">&times;</button>
                </div>
            </c:if>
        </div>

        <!-- Card Container with Table and Controls -->
        <div class="card">
            <!-- Task 3: SQL Indexed Search Form & Stats Toolbar -->
            <div class="table-toolbar">
                <form action="${pageContext.request.contextPath}/contacts" method="get" class="search-form-wrapper" style="display: flex; gap: 0.5rem; flex: 1; max-width: 520px;">
                    <div class="search-box-wrapper" style="flex: 1;">
                        <span class="search-icon">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <circle cx="11" cy="11" r="8"></circle>
                                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                            </svg>
                        </span>
                        <input type="text"
                               name="search"
                               id="searchContactsInput"
                               class="search-input"
                               placeholder="Search contacts (name, email, phone)..."
                               value="<c:out value='${searchQuery}'/>"
                               autocomplete="off">
                        <c:if test="${not empty searchQuery}">
                            <a href="${pageContext.request.contextPath}/contacts" class="search-clear-btn" aria-label="Clear search" style="text-decoration: none; display: flex; align-items: center; justify-content: center;">&times;</a>
                        </c:if>
                    </div>
                    <button type="submit" class="nav-btn nav-btn-primary" style="padding: 0.5rem 1rem; border-radius: 8px;">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="11" cy="11" r="8"></circle>
                            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                        </svg>
                        Search
                    </button>
                </form>

                <div class="toolbar-stats">
                    <span class="badge badge-success" style="background: rgba(16, 185, 129, 0.15); color: #059669; font-weight: 600;">
                        &#x25CF; PostgreSQL + HikariCP
                    </span>
                    Showing <span id="filteredCountBadge" class="badge badge-primary">${fn:length(contacts)}</span> of
                    <span class="badge">${totalContacts}</span> contacts
                </div>
            </div>

            <!-- Contacts Table or Empty Placeholder -->
            <c:choose>
                <c:when test="${empty contacts}">
                    <!-- "No contacts" placeholder when empty per requirement -->
                    <div class="empty-placeholder">
                        <div class="empty-icon-wrap">
                            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                <circle cx="9" cy="7" r="4"></circle>
                                <line x1="17" y1="11" x2="23" y2="11"></line>
                            </svg>
                        </div>
                        <h3>No contacts found</h3>
                        <p>There are currently no contacts stored in this session. Start by submitting your first visitor contact entry.</p>
                        <a href="${pageContext.request.contextPath}/contacts/add" class="nav-btn nav-btn-primary">
                            + Add First Contact
                        </a>
                    </div>
                </c:when>

                <c:otherwise>
                    <!-- Responsive Table with XSS protection per requirement -->
                    <div class="table-responsive">
                        <table class="contacts-table">
                            <thead>
                                <tr>
                                    <th style="width: 60px;">#</th>
                                    <th>Contact Name</th>
                                    <th>Email Address</th>
                                    <th>Phone Number</th>
                                    <th>Added On</th>
                                    <th style="text-align: right;">Action</th>
                                </tr>
                            </thead>
                            <tbody id="contactsTableBody">
                                <c:forEach var="contact" items="${contacts}" varStatus="status">
                                    <tr class="contact-row"
                                        data-name="<c:out value='${contact.name}'/>"
                                        data-email="<c:out value='${contact.email}'/>"
                                        data-phone="<c:out value='${contact.phone}'/>">
                                        <td style="color: var(--text-muted); font-weight: 500;">
                                            <c:out value="${(currentPage - 1) * pageSize + status.count}"/>
                                        </td>
                                        <td>
                                            <div class="contact-user-cell">
                                                <div class="avatar-circle">
                                                    <c:out value="${contact.initials}"/>
                                                </div>
                                                <div>
                                                    <div class="contact-name-title">
                                                        <c:out value="${contact.name}"/>
                                                    </div>
                                                </div>
                                            </div>
                                        </td>
                                        <td>
                                            <a href="mailto:<c:out value='${contact.email}'/>" class="contact-email-link">
                                                <c:out value="${contact.email}"/>
                                            </a>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty contact.phone}">
                                                    <a href="tel:<c:out value='${contact.phone}'/>" class="contact-phone-link">
                                                        <c:out value="${contact.formattedPhone}"/>
                                                    </a>
                                                </c:when>
                                                <c:otherwise>
                                                    <span style="color: var(--text-muted);">&mdash;</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <span class="date-text">
                                                <c:out value="${contact.formattedDate}"/>
                                            </span>
                                        </td>
                                        <td style="text-align: right;">
                                            <div class="action-buttons" style="justify-content: flex-end;">
                                                <a href="${pageContext.request.contextPath}/contacts/delete?id=${contact.id}"
                                                   class="btn-delete"
                                                   onclick="return confirm('Are you sure you want to delete contact: <c:out value="${contact.name}"/>?');"
                                                   title="Delete Contact">
                                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                                        <polyline points="3 6 5 6 21 6"></polyline>
                                                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                                    </svg>
                                                    Delete
                                                </a>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>

                                <!-- Row shown when search-as-you-type has 0 matches -->
                                <tr id="noFilterResultsRow" style="display: none;">
                                    <td colspan="6" style="text-align: center; padding: 2.5rem; color: var(--text-secondary);">
                                        No contacts match your filter criteria.
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>

                    <!-- Contact List Pagination per requirement -->
                    <div class="pagination-container">
                        <div class="pagination-info">
                            Page <strong>${currentPage}</strong> of <strong>${totalPages}</strong>
                            (Total: ${totalContacts} records)
                        </div>
                        <div class="pagination-controls">
                            <!-- Previous Page -->
                            <c:choose>
                                <c:when test="${currentPage > 1}">
                                    <a href="${pageContext.request.contextPath}/contacts?page=${currentPage - 1}&size=${pageSize}"
                                       class="page-btn" aria-label="Previous page">&laquo; Prev</a>
                                </c:when>
                                <c:otherwise>
                                    <span class="page-btn disabled">&laquo; Prev</span>
                                </c:otherwise>
                            </c:choose>

                            <!-- Numeric Page Buttons -->
                            <c:forEach var="p" begin="1" end="${totalPages}">
                                <a href="${pageContext.request.contextPath}/contacts?page=${p}&size=${pageSize}"
                                   class="page-btn ${p == currentPage ? 'active' : ''}">
                                    ${p}
                                </a>
                            </c:forEach>

                            <!-- Next Page -->
                            <c:choose>
                                <c:when test="${currentPage < totalPages}">
                                    <a href="${pageContext.request.contextPath}/contacts?page=${currentPage + 1}&size=${pageSize}"
                                       class="page-btn" aria-label="Next page">Next &raquo;</a>
                                </c:when>
                                <c:otherwise>
                                    <span class="page-btn disabled">Next &raquo;</span>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <!-- "Add New Contact" Floating Action Button per requirement -->
        <a href="${pageContext.request.contextPath}/contacts/add" class="fab-btn" title="Add New Contact" aria-label="Add New Contact">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            <span class="fab-tooltip">Add New Contact</span>
        </a>

    </main>

    <!-- Footer -->
    <footer class="footer">
        <p>&copy; 2026 Soft Nexis Personal Portal &bull; Task 3: Database Integration with JDBC &bull; HikariCP Connection Pooling &bull; PostgreSQL Persistent Storage</p>
    </footer>

    <!-- App Scripts -->
    <script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>

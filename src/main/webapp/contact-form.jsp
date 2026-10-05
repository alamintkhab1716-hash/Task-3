<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Add New Contact - Soft Nexis Personal Portal</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

    <!-- Header Navigation -->
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
                        <line x1="19" y1="12" x2="5" y2="12"></line>
                        <polyline points="12 19 5 12 12 5"></polyline>
                    </svg>
                    Back to Contacts
                </a>
            </div>
        </div>
    </header>

    <!-- Main Content -->
    <main class="main-content">
        <div class="form-card card">
            <div class="form-header">
                <h2>Add New Contact</h2>
                <p>Fill out the form below to register a new contact in persistent PostgreSQL storage.</p>
            </div>

            <div class="form-body">
                <!-- User Feedback: Error message alert per specification -->
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

                <!-- Submission Form: POST /contacts -->
                <form id="contactForm"
                      action="${pageContext.request.contextPath}/contacts"
                      method="POST"
                      novalidate
                      data-first-invalid="<c:out value='${firstInvalidField}'/>">

                    <!-- CSRF Protection & Duplicate Submission Prevention -->
                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">

                    <!-- Contact Name Field -->
                    <div class="form-group">
                        <label for="name" class="form-label">
                            Full Name <span class="required-indicator">*</span>
                        </label>
                        <div class="input-wrapper">
                            <input type="text"
                                   id="name"
                                   name="name"
                                   class="form-control ${not empty errors.name ? 'is-invalid' : ''}"
                                   placeholder="e.g. Jane Doe"
                                   maxlength="50"
                                   value="<c:out value='${enteredName != null ? enteredName : param.name}'/>"
                                   required>
                        </div>
                        <div id="name-error"
                             class="field-error"
                             style="${not empty errors.name ? 'display:flex;' : 'display:none;'}">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="8" x2="12" y2="12"></line>
                                <line x1="12" y1="16" x2="12.01" y2="16"></line>
                            </svg>
                            <span>${not empty errors.name ? errors.name : 'Please enter valid name'}</span>
                        </div>
                        <div class="field-hint">Must be 2 to 50 characters in length.</div>
                    </div>

                    <!-- Email Field -->
                    <div class="form-group">
                        <label for="email" class="form-label">
                            Email Address <span class="required-indicator">*</span>
                        </label>
                        <div class="input-wrapper">
                            <input type="email"
                                   id="email"
                                   name="email"
                                   class="form-control ${not empty errors.email ? 'is-invalid' : ''}"
                                   placeholder="e.g. jane.doe@example.com"
                                   value="<c:out value='${enteredEmail != null ? enteredEmail : param.email}'/>"
                                   required>
                        </div>
                        <div id="email-error"
                             class="field-error"
                             style="${not empty errors.email ? 'display:flex;' : 'display:none;'}">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="8" x2="12" y2="12"></line>
                                <line x1="12" y1="16" x2="12.01" y2="16"></line>
                            </svg>
                            <span>${not empty errors.email ? errors.email : 'Invalid email address'}</span>
                        </div>
                        <div class="field-hint">Valid email format required (e.g. name@domain.com).</div>
                    </div>

                    <!-- Phone Field -->
                    <div class="form-group">
                        <label for="phone" class="form-label">
                            Phone Number <span style="font-weight: 400; color: var(--text-muted);">(Optional)</span>
                        </label>
                        <div class="input-wrapper">
                            <input type="tel"
                                   id="phone"
                                   name="phone"
                                   class="form-control ${not empty errors.phone ? 'is-invalid' : ''}"
                                   placeholder="e.g. 5551234567 or 555-123-4567"
                                   maxlength="14"
                                   value="<c:out value='${enteredPhone != null ? enteredPhone : param.phone}'/>">
                        </div>
                        <div id="phone-error"
                             class="field-error"
                             style="${not empty errors.phone ? 'display:flex;' : 'display:none;'}">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="8" x2="12" y2="12"></line>
                                <line x1="12" y1="16" x2="12.01" y2="16"></line>
                            </svg>
                            <span>${not empty errors.phone ? errors.phone : 'Use 10-digit format'}</span>
                        </div>
                        <div class="field-hint">Must be 10 digits if provided.</div>
                    </div>

                    <!-- Actions -->
                    <div class="form-actions">
                        <a href="${pageContext.request.contextPath}/contacts" class="btn-secondary">
                            Cancel
                        </a>
                        <button type="submit" id="submitBtn" class="btn-submit">
                            <span class="spinner" aria-hidden="true"></span>
                            <span class="btn-text">Save Contact</span>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </main>

    <!-- Footer -->
    <footer class="footer">
        <p>&copy; 2026 Soft Nexis Personal Portal &bull; Task 3: Database Integration with JDBC &bull; Persistent Storage &bull; Real-Time Form Validation</p>
    </footer>

    <!-- Scripts -->
    <script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>500 - Server Error | Soft Nexis</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <header class="navbar">
        <div class="nav-container">
            <a href="${pageContext.request.contextPath}/contacts" class="brand-wrapper">
                <div class="brand-logo-badge">
                    <span>Soft</span><span class="accent">Nexis</span>
                </div>
                <div class="brand-info">
                    <h1>Personal Portal</h1>
                    <p>Contact Manager Module</p>
                </div>
            </a>
        </div>
    </header>
    <main class="main-content">
        <div class="card empty-placeholder">
            <div class="empty-icon-wrap" style="background: #fef2f2; color: #ef4444;">
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="12" y1="8" x2="12" y2="12"></line>
                    <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
            </div>
            <h3>500 - Internal Server Error</h3>
            <p>An unexpected condition was encountered on the server. Null pointers or expired sessions are safely contained.</p>
            <a href="${pageContext.request.contextPath}/contacts" class="nav-btn nav-btn-primary">Return to Contacts</a>
        </div>
    </main>
</body>
</html>

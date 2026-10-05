<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>503 Service Unavailable - Soft Nexis Contact Manager</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

    <!-- Header -->
    <header class="navbar">
        <div class="nav-container">
            <a href="${pageContext.request.contextPath}/contacts" class="brand-wrapper">
                <div class="brand-logo-badge">
                    <span>Soft</span><span class="accent">Nexis</span>
                </div>
                <div class="brand-info">
                    <h1>Personal Portal</h1>
                    <p>Task 3: Database Integration with JDBC</p>
                </div>
            </a>
        </div>
    </header>

    <!-- Main Container -->
    <main class="main-content" style="max-width: 600px; margin: 4rem auto; text-align: center;">
        <div class="card" style="padding: 3rem 2rem;">
            <div style="font-size: 4rem; color: #f59e0b; margin-bottom: 1rem;">
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: inline-block;">
                    <ellipse cx="12" cy="5" rx="9" ry="3"></ellipse>
                    <path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"></path>
                    <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"></path>
                    <line x1="3" y1="12" x2="21" y2="12"></line>
                </svg>
            </div>

            <h2 style="font-size: 2rem; margin-bottom: 0.75rem; color: var(--text-primary, #1e293b);">503 - Service Unavailable</h2>
            <p style="color: var(--text-secondary, #64748b); font-size: 1.05rem; line-height: 1.6; margin-bottom: 1.5rem;">
                The contact management database is temporarily unavailable or experiencing connection pool timeout.
                The application connection pool (HikariCP) is performing automatic recovery.
            </p>

            <c:if test="${not empty dbError}">
                <div class="alert error" style="text-align: left; margin-bottom: 1.5rem;">
                    <strong>Database Error:</strong> <c:out value="${dbError}"/>
                    <c:if test="${not empty sqlState}">
                        <br><small>SQLState: <c:out value="${sqlState}"/></small>
                    </c:if>
                </div>
            </c:if>

            <div style="display: flex; gap: 1rem; justify-content: center;">
                <a href="${pageContext.request.contextPath}/contacts" class="nav-btn nav-btn-primary">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="1 4 1 10 7 10"></polyline>
                        <polyline points="23 20 23 14 17 14"></polyline>
                        <path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15"></path>
                    </svg>
                    Retry Connection
                </a>
            </div>
        </div>
    </main>

    <footer class="footer">
        <p>&copy; 2026 Soft Nexis Personal Portal &bull; Task 3: Database Integration with JDBC &bull; HikariCP Connection Pooling</p>
    </footer>

</body>
</html>

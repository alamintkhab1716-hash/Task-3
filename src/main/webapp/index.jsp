<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // Redirect root URL to contacts manager servlet
    response.sendRedirect(request.getContextPath() + "/contacts");
%>

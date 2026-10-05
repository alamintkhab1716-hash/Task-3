package com.softnexis.contactmanager.listener;

import com.softnexis.contactmanager.dao.ContactDAO;
import com.softnexis.contactmanager.dao.ContactDAOImpl;
import com.softnexis.contactmanager.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

/**
 * Application Lifecycle Listener.
 * Initializes database connection pool, verifies schema and indexes,
 * and handles graceful pool shutdown when the web application stops.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(AppContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Initializing Soft Nexis Contact Manager (Task 3)...");
        try {
            DataSource ds = DBUtil.getDataSource();
            logger.info("HikariCP connection pool initialized successfully.");

            // Initialize database schema (table, indexes, check constraint, sample data)
            DBUtil.initDatabaseSchema();

            ContactDAO contactDAO = new ContactDAOImpl(ds);
            sce.getServletContext().setAttribute("contactDAO", contactDAO);

            logger.info("Soft Nexis Contact Manager initialization completed successfully.");
        } catch (Exception e) {
            logger.error("Error during application initialization: {}", e.getMessage(), e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Shutting down Soft Nexis Contact Manager...");
        try {
            DBUtil.closeDataSource();
            logger.info("HikariCP connection pool closed cleanly.");
        } catch (Exception e) {
            logger.error("Error closing database connection pool: {}", e.getMessage(), e);
        }
    }
}

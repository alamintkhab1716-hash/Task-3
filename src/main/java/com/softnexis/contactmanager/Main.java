package com.softnexis.contactmanager;

import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;

/**
 * Embedded Tomcat server launcher.
 * Allows running the full Java EE application with a single command:
 *   mvn compile exec:java
 */
public class Main {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.trim().isEmpty()) {
            try {
                port = Integer.parseInt(portEnv.trim());
            } catch (NumberFormatException ignored) {}
        }

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector(); // Initialize default HTTP connector
        tomcat.enableNaming(); // Enable JNDI naming for jdbc/ContactDB Resource

        File baseDir = new File(System.getProperty("java.io.tmpdir"), "tomcat." + port);
        tomcat.setBaseDir(baseDir.getAbsolutePath());

        // Locate webapp directory
        File webappDir = new File("src/main/webapp");
        if (!webappDir.exists()) {
            webappDir = new File("webapp");
        }

        Context context = tomcat.addWebapp("", webappDir.getAbsolutePath());
        context.setParentClassLoader(Main.class.getClassLoader());

        // Configure class resources so embedded Tomcat finds compiled servlet classes
        File classesDir = new File("target/classes");
        if (classesDir.exists()) {
            WebResourceRoot resources = new StandardRoot(context);
            resources.addPreResources(new DirResourceSet(
                    resources,
                    "/WEB-INF/classes",
                    classesDir.getAbsolutePath(),
                    "/"
            ));
            context.setResources(resources);
        }

        System.out.println("==================================================================");
        System.out.println(" Soft Nexis - Task 3: Database Integration with JDBC");
        System.out.println(" Component: Persistent Contact Manager with HikariCP Connection Pool");
        System.out.println(" Application URL: http://localhost:" + port + "/contacts");
        System.out.println(" Add Contact Form: http://localhost:" + port + "/contacts/add");
        System.out.println(" Database: PostgreSQL / MySQL with JDBC PreparedStatement & Indexes");
        System.out.println("==================================================================");

        tomcat.start();
        tomcat.getServer().await();
    }
}

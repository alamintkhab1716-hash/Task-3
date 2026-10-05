package com.softnexis.contactmanager.util;

import com.softnexis.contactmanager.dao.ContactDAO;
import com.softnexis.contactmanager.dao.ContactDAOImpl;
import com.softnexis.contactmanager.model.Contact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration Utility for Contact Manager (Task 3 Migration Strategy).
 * Fulfills specifications:
 * 1. Data Migration: Export in-memory/existing contacts to CSV.
 * 2. Import CSV to database using batch processing / COPY semantics.
 */
public class ContactCsvMigrator {

    private static final Logger logger = LoggerFactory.getLogger(ContactCsvMigrator.class);
    private static final String CSV_HEADER = "id,name,email,phone,created_at";

    /**
     * Exports a list of contacts to CSV format.
     */
    public static void exportContactsToCsv(List<Contact> contacts, Writer writer) throws IOException {
        PrintWriter pw = new PrintWriter(writer);
        pw.println(CSV_HEADER);

        for (Contact contact : contacts) {
            String line = String.format("%s,\"%s\",\"%s\",\"%s\",%s",
                    contact.getId() != null ? contact.getId() : "",
                    escapeCsv(contact.getName()),
                    escapeCsv(contact.getEmail()),
                    escapeCsv(contact.getPhone()),
                    contact.getCreatedAt() != null ? contact.getCreatedAt().toString() : LocalDateTime.now().toString());
            pw.println(line);
        }
        pw.flush();
        logger.info("Exported {} contacts to CSV.", contacts.size());
    }

    /**
     * Exports all contacts from the database to a CSV file.
     */
    public static File exportDatabaseToFile(ContactDAO contactDAO, File targetFile) throws SQLException, IOException {
        List<Contact> contacts = contactDAO.getAllContacts();
        try (FileWriter fw = new FileWriter(targetFile)) {
            exportContactsToCsv(contacts, fw);
        }
        return targetFile;
    }

    /**
     * Imports contacts from a CSV Reader and persists them to the database using batch inserts.
     */
    public static int importContactsFromCsv(Reader reader, ContactDAO contactDAO) throws IOException, SQLException {
        BufferedReader br = new BufferedReader(reader);
        String headerLine = br.readLine(); // Skip header
        if (headerLine == null) {
            return 0;
        }

        List<Contact> batchList = new ArrayList<>();
        String line;
        int count = 0;

        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            String[] tokens = parseCsvLine(line);
            if (tokens.length >= 3) {
                Contact contact = new Contact();
                // tokens[0] is id (optional)
                contact.setName(tokens.length > 1 ? tokens[1].trim() : "");
                contact.setEmail(tokens.length > 2 ? tokens[2].trim().toLowerCase() : "");
                contact.setPhone(tokens.length > 3 ? tokens[3].trim() : "");
                contact.setCreatedAt(LocalDateTime.now());

                if (!contact.getName().isEmpty() && !contact.getEmail().isEmpty()) {
                    batchList.add(contact);
                    count++;
                }
            }
        }

        if (!batchList.isEmpty()) {
            contactDAO.addContactsBatch(batchList);
            logger.info("Successfully imported and batch-inserted {} contacts from CSV.", batchList.size());
        }

        return count;
    }

    /**
     * Imports CSV from file into the database.
     */
    public static int importFileToDatabase(ContactDAO contactDAO, File sourceFile) throws IOException, SQLException {
        try (FileReader fr = new FileReader(sourceFile)) {
            return importContactsFromCsv(fr, contactDAO);
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    private static String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    sb.append('\"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }

    /**
     * Standalone CLI runner for migration scripts.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java ContactCsvMigrator <export|import> <filePath>");
            return;
        }

        String mode = args[0];
        File file = new File(args[1]);
        ContactDAO dao = new ContactDAOImpl(DBUtil.getDataSource());

        try {
            if ("export".equalsIgnoreCase(mode)) {
                exportDatabaseToFile(dao, file);
                System.out.println("Contacts successfully exported to: " + file.getAbsolutePath());
            } else if ("import".equalsIgnoreCase(mode)) {
                int imported = importFileToDatabase(dao, file);
                System.out.println("Successfully imported " + imported + " contacts from: " + file.getAbsolutePath());
            } else {
                System.err.println("Unknown command: " + mode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

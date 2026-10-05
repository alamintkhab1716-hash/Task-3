package com.softnexis.contactmanager;

import com.softnexis.contactmanager.dao.ContactDAO;
import com.softnexis.contactmanager.dao.ContactDAOImpl;
import com.softnexis.contactmanager.model.Contact;
import com.softnexis.contactmanager.util.DBUtil;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Task 3: Success Metrics Verification (Concurrency & Performance)")
public class ConcurrencyAndPerformanceTest {

    private ContactDAO contactDAO;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(DBUtil.isHealthy(), "PostgreSQL database must be accessible");
        DBUtil.initDatabaseSchema();
        contactDAO = new ContactDAOImpl(DBUtil.getDataSource());
    }

    @Test
    @DisplayName("Success Metric: Connection pool serves 50+ concurrent queries simultaneously")
    void testConcurrentUsers() throws Exception {
        int concurrentUsers = 50;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        List<Callable<Integer>> tasks = new ArrayList<>();

        for (int i = 0; i < concurrentUsers; i++) {
            tasks.add(() -> {
                List<Contact> contacts = contactDAO.searchContacts("Alexander");
                return contacts.size();
            });
        }

        long start = System.currentTimeMillis();
        List<Future<Integer>> futures = executor.invokeAll(tasks, 10, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(concurrentUsers, futures.size());
        for (Future<Integer> f : futures) {
            assertTrue(f.isDone());
            assertNotNull(f.get());
        }

        System.out.println("HikariCP successfully served " + concurrentUsers + " concurrent user requests in " + elapsed + "ms");
        assertTrue(elapsed < 5000, "50 concurrent queries must be serviced within 5 seconds");
    }

    @Test
    @DisplayName("Success Metric: Search returns results in <500ms")
    void testSearchSpeedUnder500ms() throws Exception {
        long start = System.currentTimeMillis();
        List<Contact> results = contactDAO.searchContacts("wright");
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("Search query executed in: " + elapsed + "ms");
        assertNotNull(results);
        assertTrue(elapsed < 500, "Search query must execute in < 500ms per success metric");
    }
}

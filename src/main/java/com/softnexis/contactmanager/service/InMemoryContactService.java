package com.softnexis.contactmanager.service;

import com.softnexis.contactmanager.model.Contact;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Thread-safe In-Memory implementation of ContactService.
 * Synchronizes with HTTP Session attribute "contacts" to fulfill the state management specification.
 */
public class InMemoryContactService implements ContactService {

    public static final String SESSION_CONTACTS_KEY = "contacts";
    private static final InMemoryContactService INSTANCE = new InMemoryContactService();

    private final List<Contact> globalStore = new CopyOnWriteArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(100);

    public InMemoryContactService() {
        seedInitialData();
    }

    public static InMemoryContactService getInstance() {
        return INSTANCE;
    }

    private void seedInitialData() {
        if (globalStore.isEmpty()) {
            globalStore.add(new Contact(1L, "Alexander Wright", "alex.wright@example.com", "5551234567", LocalDateTime.now().minusDays(5)));
            globalStore.add(new Contact(2L, "Beatrice Vance", "beatrice.vance@company.org", "5559876543", LocalDateTime.now().minusDays(3)));
            globalStore.add(new Contact(3L, "Carlos Santana", "carlos.santana@domain.net", "5553334444", LocalDateTime.now().minusDays(2)));
            globalStore.add(new Contact(4L, "Diana Prince", "diana.prince@themyscira.com", "5557778888", LocalDateTime.now().minusDays(1)));
            globalStore.add(new Contact(5L, "Evan Wright", "evan.wright@agency.io", "5554449999", LocalDateTime.now().minusHours(4)));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Contact> getAllContacts(HttpSession session) {
        if (session == null) {
            return new ArrayList<>(globalStore);
        }

        List<Contact> sessionList = (List<Contact>) session.getAttribute(SESSION_CONTACTS_KEY);
        if (sessionList == null) {
            sessionList = new CopyOnWriteArrayList<>(globalStore);
            session.setAttribute(SESSION_CONTACTS_KEY, sessionList);
        }
        return new ArrayList<>(sessionList);
    }

    @Override
    public Optional<Contact> getContactById(HttpSession session, Long id) {
        if (id == null) return Optional.empty();
        return getAllContacts(session).stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Contact saveContact(HttpSession session, Contact contact) {
        if (contact == null) {
            throw new IllegalArgumentException("Contact cannot be null");
        }

        if (contact.getId() == null) {
            contact.setId(idGenerator.incrementAndGet());
            contact.setCreatedAt(LocalDateTime.now());
        }

        globalStore.removeIf(c -> c.getId().equals(contact.getId()));
        globalStore.add(0, contact);

        if (session != null) {
            List<Contact> sessionList = (List<Contact>) session.getAttribute(SESSION_CONTACTS_KEY);
            if (sessionList == null) {
                sessionList = new CopyOnWriteArrayList<>(globalStore);
            } else {
                sessionList.removeIf(c -> c.getId().equals(contact.getId()));
                sessionList.add(0, contact);
            }
            session.setAttribute(SESSION_CONTACTS_KEY, sessionList);
        }

        return contact;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean deleteContact(HttpSession session, Long id) {
        if (id == null) return false;

        boolean removedFromGlobal = globalStore.removeIf(c -> c.getId().equals(id));
        boolean removedFromSession = false;

        if (session != null) {
            List<Contact> sessionList = (List<Contact>) session.getAttribute(SESSION_CONTACTS_KEY);
            if (sessionList != null) {
                removedFromSession = sessionList.removeIf(c -> c.getId().equals(id));
                session.setAttribute(SESSION_CONTACTS_KEY, sessionList);
            }
        }

        return removedFromGlobal || removedFromSession;
    }

    @Override
    public boolean isEmailDuplicate(HttpSession session, String email, Long excludeId) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String normalizedEmail = email.trim().toLowerCase();
        return getAllContacts(session).stream()
                .filter(c -> excludeId == null || !excludeId.equals(c.getId()))
                .anyMatch(c -> c.getEmail() != null && c.getEmail().trim().equalsIgnoreCase(normalizedEmail));
    }

    @Override
    public List<Contact> searchContacts(HttpSession session, String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllContacts(session);
        }
        String q = query.trim().toLowerCase();
        return getAllContacts(session).stream()
                .filter(c -> (c.getName() != null && c.getName().toLowerCase().contains(q))
                        || (c.getEmail() != null && c.getEmail().toLowerCase().contains(q))
                        || (c.getPhone() != null && c.getPhone().contains(q)))
                .collect(Collectors.toList());
    }

    @Override
    public List<Contact> getAllContacts() {
        return getAllContacts(null);
    }

    @Override
    public Optional<Contact> getContactById(Long id) {
        return getContactById(null, id);
    }

    @Override
    public Contact saveContact(Contact contact) {
        return saveContact(null, contact);
    }

    @Override
    public boolean deleteContact(Long id) {
        return deleteContact(null, id);
    }

    @Override
    public boolean isEmailDuplicate(String email, Long excludeId) {
        return isEmailDuplicate(null, email, excludeId);
    }

    @Override
    public List<Contact> searchContacts(String query) {
        return searchContacts(null, query);
    }
}

package com.softnexis.contactmanager;

import com.softnexis.contactmanager.model.Contact;
import com.softnexis.contactmanager.service.InMemoryContactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Contact Service Tests")
public class ContactServiceTest {

    private InMemoryContactService contactService;
    private HttpSession session;
    private Map<String, Object> sessionAttributes;

    @BeforeEach
    void setUp() {
        contactService = new InMemoryContactService();
        session = mock(HttpSession.class);
        sessionAttributes = new HashMap<>();

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return sessionAttributes.get(key);
        }).when(session).getAttribute(any());

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            sessionAttributes.put(key, value);
            return null;
        }).when(session).setAttribute(any(), any());
    }

    @Test
    @DisplayName("Retrieve contacts from initial store / session")
    void testGetAllContacts() {
        List<Contact> contacts = contactService.getAllContacts(session);
        assertNotNull(contacts);
        assertFalse(contacts.isEmpty(), "Default contacts should be seeded");
        assertTrue(sessionAttributes.containsKey(InMemoryContactService.SESSION_CONTACTS_KEY));
    }

    @Test
    @DisplayName("Valid submission -> Contact appears in list")
    void testSaveAndRetrieveContact() {
        Contact newContact = new Contact(null, "Samantha Reed", "samantha@domain.com", "5558889999");
        Contact saved = contactService.saveContact(session, newContact);

        assertNotNull(saved.getId(), "Generated ID must not be null");
        assertEquals("Samantha Reed", saved.getName());

        List<Contact> contacts = contactService.getAllContacts(session);
        boolean found = contacts.stream().anyMatch(c -> c.getEmail().equals("samantha@domain.com"));
        assertTrue(found, "Saved contact must appear in the retrieved contact list");
    }

    @Test
    @DisplayName("Detect duplicate email addresses")
    void testDuplicateEmail() {
        Contact newContact = new Contact(null, "Unique Person", "unique@domain.com", "5551112222");
        contactService.saveContact(session, newContact);

        boolean isDuplicate = contactService.isEmailDuplicate(session, "unique@domain.com", null);
        assertTrue(isDuplicate, "Should detect duplicate email address");

        boolean isCaseInsensitiveDuplicate = contactService.isEmailDuplicate(session, "UNIQUE@DOMAIN.COM", null);
        assertTrue(isCaseInsensitiveDuplicate, "Duplicate check should be case-insensitive");

        boolean isNotDuplicate = contactService.isEmailDuplicate(session, "another@domain.com", null);
        assertFalse(isNotDuplicate, "Should not flag different email as duplicate");
    }

    @Test
    @DisplayName("Delete contact successfully")
    void testDeleteContact() {
        Contact contact = new Contact(null, "Temporary Person", "temp@domain.com", "5550001111");
        Contact saved = contactService.saveContact(session, contact);

        boolean deleted = contactService.deleteContact(session, saved.getId());
        assertTrue(deleted, "Delete should return true for existing contact");

        Optional<Contact> found = contactService.getContactById(session, saved.getId());
        assertFalse(found.isPresent(), "Deleted contact should not be found");
    }

    @Test
    @DisplayName("Search-as-you-type filter matches name, email, or phone")
    void testSearchContacts() {
        Contact c1 = new Contact(null, "Walter White", "heisenberg@abq.com", "5051234567");
        Contact c2 = new Contact(null, "Jesse Pinkman", "jesse@abq.com", "5059876543");
        contactService.saveContact(session, c1);
        contactService.saveContact(session, c2);

        List<Contact> searchByName = contactService.searchContacts(session, "Walter");
        assertEquals(1, searchByName.size());
        assertEquals("Walter White", searchByName.get(0).getName());

        List<Contact> searchByEmail = contactService.searchContacts(session, "heisenberg");
        assertEquals(1, searchByEmail.size());

        List<Contact> searchByPhone = contactService.searchContacts(session, "505987");
        assertEquals(1, searchByPhone.size());
        assertEquals("Jesse Pinkman", searchByPhone.get(0).getName());
    }
}

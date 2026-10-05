-- ====================================================================
-- Soft Nexis - Task 3: Database Integration with JDBC
-- Project Component: Persistent Contact Manager
-- Database Schema Definition (PostgreSQL / MySQL compatible)
-- ====================================================================

-- 1. Create Normalized Contacts Table
CREATE TABLE IF NOT EXISTS contacts (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_contacts_email UNIQUE (email)
);

-- 2. Indexes for Search Optimization
CREATE INDEX IF NOT EXISTS idx_contacts_name ON contacts(name);
CREATE INDEX IF NOT EXISTS idx_contacts_email ON contacts(email);

-- 3. Initial Seed Contacts for Verification
INSERT INTO contacts (name, email, phone, created_at)
VALUES 
    ('Alexander Wright', 'alex.wright@example.com', '5551234567', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    ('Beatrice Vance', 'beatrice.vance@company.org', '5559876543', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    ('Carlos Santana', 'carlos.santana@domain.net', '5553334444', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    ('Diana Prince', 'diana.prince@themyscira.com', '5557778888', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    ('Evan Wright', 'evan.wright@agency.io', '5554449999', CURRENT_TIMESTAMP - INTERVAL '4 hours')
ON CONFLICT (email) DO NOTHING;

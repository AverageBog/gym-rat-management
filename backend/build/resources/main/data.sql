INSERT INTO membership_plan (id, name, duration_months, price, description) VALUES
  (1, 'Monthly',   1,  39.99, 'Rolling monthly membership'),
  (2, 'Quarterly', 3,  99.99, 'Three-month membership'),
  (3, 'Annual',   12, 299.99, 'Full-year membership');

INSERT INTO member (id, name, email, phone, join_date, status, membership_plan_id) VALUES
  (1, 'Alex Rivera',  'alex@example.com',   '555-0101', '2025-01-10', 'ACTIVE',   1),
  (2, 'Jordan Lee',   'jordan@example.com', '555-0102', '2025-02-14', 'ACTIVE',   3),
  (3, 'Sam Patel',    'sam@example.com',    '555-0103', '2024-11-01', 'INACTIVE', 2);

INSERT INTO attendance (id, member_id, check_in_time) VALUES
  (1, 1, '2026-03-10T08:30:00'),
  (2, 1, '2026-03-12T09:00:00'),
  (3, 2, '2026-03-13T07:45:00');

INSERT INTO member_note (id, member_id, content, created_at) VALUES
  (1, 1, 'Prefers morning sessions. Interested in personal training.', '2026-03-10T10:00:00'),
  (2, 2, 'Competes in powerlifting. Needs access to competition platform.', '2026-03-13T11:00:00');

INSERT INTO merchandise_item (id, name, quantity, price, description) VALUES
  (1, 'Gym T-Shirt (M)',  20, 24.99, 'Classic logo tee'),
  (2, 'Shaker Bottle',    15, 14.99, 'BPA-free 700ml shaker'),
  (3, 'Resistance Bands',  8, 19.99, 'Set of 5 resistance bands');

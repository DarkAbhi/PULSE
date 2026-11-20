BEGIN;

ALTER TABLE financial_horizon_categories
    ADD COLUMN parent_id BIGINT REFERENCES financial_horizon_categories(id),
    ADD COLUMN allows_subcategories BOOLEAN NOT NULL DEFAULT TRUE,
    ADD CONSTRAINT horizon_category_child_is_leaf CHECK (parent_id IS NULL OR NOT allows_subcategories);

DROP INDEX horizon_categories_user_name_idx;
CREATE UNIQUE INDEX horizon_categories_user_name_idx
    ON financial_horizon_categories (COALESCE(user_id, 0), COALESCE(parent_id, 0), LOWER(name));
CREATE INDEX horizon_categories_parent_idx ON financial_horizon_categories (parent_id);

-- Older API writes could supply just a label. Restore an ID when it identifies one visible category.
WITH label_matches AS (
    SELECT t.id AS transaction_id, min(c.id) AS category_id
    FROM financial_horizon_transactions t
    JOIN financial_horizon_categories c ON lower(c.name) = lower(trim(t.category_name))
        AND (c.user_id = t.user_id OR (c.user_id IS NULL AND c.is_default))
    WHERE t.category_id IS NULL AND lower(trim(t.category_name)) <> 'other'
    GROUP BY t.id
    HAVING count(*) = 1
)
UPDATE financial_horizon_transactions t SET category_id = m.category_id
FROM label_matches m WHERE t.id = m.transaction_id;

-- Reuse the original IDs so existing transactions, subscriptions and import rules retain their assignments.
UPDATE financial_horizon_categories AS c SET name = renamed.new_name
FROM (VALUES
    ('Food & Dining', 'Food and Drinks'),
    ('Bills & Utilities', 'Bill'),
    ('Housing', 'Home'),
    ('Transportation', 'Transport'),
    ('Subscriptions', 'Subscription'),
    ('Other', 'Miscellaneous')
) AS renamed(old_name, new_name)
WHERE c.is_default AND c.user_id IS NULL AND c.name = renamed.old_name;

-- Keep the two legacy categories whose historical transactions could belong to multiple new categories.
UPDATE financial_horizon_categories SET allows_subcategories = FALSE
WHERE is_default AND user_id IS NULL AND name IN ('Health & Fitness', 'Investments & Savings');

INSERT INTO financial_horizon_categories (name, icon, color, is_default, allows_subcategories)
SELECT name, icon, color, TRUE, allows_subcategories
FROM (VALUES
    ('Food and Drinks', 'utensils', '#f97316', true),
    ('Transport', 'car', '#3b82f6', true),
    ('Shopping', 'shopping-bag', '#ec4899', true),
    ('Groceries', 'shopping-bag', '#65a30d', true),
    ('Home', 'home', '#8b5cf6', true),
    ('Entertainment', 'film', '#14b8a6', true),
    ('Events', 'tag', '#db2777', true),
    ('Travel', 'tag', '#0284c7', true),
    ('Medical', 'heart-pulse', '#06b6d4', true),
    ('Personal', 'tag', '#9333ea', true),
    ('Fitness', 'heart-pulse', '#0891b2', true),
    ('Services', 'tag', '#475569', true),
    ('Bill', 'receipt', '#ef4444', true),
    ('Subscription', 'credit-card', '#6366f1', true),
    ('EMI', 'credit-card', '#7c3aed', true),
    ('Credit Bill', 'credit-card', '#b91c1c', true),
    ('Investment', 'trending-up', '#22c55e', true),
    ('Support', 'tag', '#c026d3', true),
    ('Insurance', 'tag', '#0d9488', true),
    ('Tax', 'receipt', '#a16207', true),
    ('Top-up', 'credit-card', '#0369a1', true),
    ('Children', 'tag', '#be185d', true),
    ('Pet Care', 'tag', '#b45309', true),
    ('Business', 'tag', '#1d4ed8', true),
    ('Miscellaneous', 'tag', '#64748b', true),
    ('Self-transfer', 'tag', '#64748b', false),
    ('Savings', 'tag', '#64748b', false),
    ('Gift', 'tag', '#64748b', false),
    ('Donation', 'tag', '#64748b', false),
    ('Hidden Charges', 'tag', '#64748b', false),
    ('Cash Withdrawal', 'tag', '#64748b', false),
    ('Return', 'tag', '#64748b', false)
) AS root(name, icon, color, allows_subcategories)
ON CONFLICT DO NOTHING;

INSERT INTO financial_horizon_categories (name, icon, color, is_default, parent_id, allows_subcategories)
SELECT child.name, p.icon, p.color, TRUE, p.id, FALSE
FROM (VALUES
    ('Food and Drinks', ARRAY['eating out', 'takeaway', 'tea and coffee', 'fast food', 'snacks', 'Swiggy', 'Zomato', 'sweet treat', 'liquor', 'beverages', 'date', 'pizza', 'tiffin', 'others']),
    ('Transport', ARRAY['Uber', 'Rappido', 'auto', 'cab', 'train', 'metro', 'bus', 'bike', 'fuel', 'EV charge', 'flights', 'parking', 'Fastag', 'tolls', 'launch', 'fine', 'others']),
    ('Shopping', ARRAY['clothes', 'footwear', 'electronics', 'festival', 'luggage', 'video games', 'books', 'plants', 'jewelry', 'furniture', 'appliances', 'utensils', 'vehicle', 'cosmetics', 'toys', 'stationery', 'glasses', 'devotional', 'others']),
    ('Groceries', ARRAY['staples', 'vegetables', 'fruits', 'meat', 'eggs', 'bakery', 'dairy', 'Zappo', 'others']),
    ('Home', ARRAY['essentials', 'toiletries', 'decor', 'cleaning', 'upkeep', 'painting', 'renovation', 'pest control', 'construction', 'others']),
    ('Entertainment', ARRAY['movies', 'shows', 'bowling', 'others']),
    ('Events', ARRAY['party', 'spiritual', 'wedding', 'others']),
    ('Travel', ARRAY['activities', 'camping', 'hotel', 'commute', 'meals', 'visa fees', 'hostel', 'Airbnb', 'others']),
    ('Medical', ARRAY['medicines', 'hospital', 'clinic', 'dentist', 'lab test', 'hygiene', 'others']),
    ('Personal', ARRAY['self-care', 'grooming', 'hobbies', 'vices', 'therapy', 'others']),
    ('Fitness', ARRAY['gym', 'badminton', 'football', 'cricket', 'classes', 'equipment', 'nutrition', 'others']),
    ('Services', ARRAY['laundry', 'tailor', 'courier', 'carpenter', 'lumber', 'mechanic', 'photographer', 'driver', 'vehicle wash', 'electrician', 'painting', 'print', 'Xerox', 'legal advisor', 'repair', 'logistics', 'others']),
    ('Bill', ARRAY['phone', 'rent', 'water', 'electricity', 'gas', 'internet', 'house help', 'education', 'diet', 'cook', 'maintenance', 'others']),
    ('Subscription', ARRAY['fold', 'software', 'news', 'Netflix', 'Prime', 'YouTube', 'Spotify', 'Google', 'Learning', 'Apple', 'Bumble', 'Jio', 'Hotstar', 'Google Play', 'Xbox', 'PlayStation', 'Disney Plus', 'Zee5', 'ChatGPT', 'Claude', 'others']),
    ('EMI', ARRAY['electronics', 'house', 'vehicle', 'education', 'others']),
    ('Credit Bill', ARRAY['credit card', 'Slice', 'Amazon Pay', 'others']),
    ('Investment', ARRAY['mutual funds', 'stocks', 'IPO', 'PPF', 'NPS', 'fixed deposit', 'recurring deposit', 'assets', 'crypto', 'gold', 'US stocks', 'others']),
    ('Support', ARRAY['parents', 'spouse', 'mom', 'dad', 'pocket money', 'others']),
    ('Insurance', ARRAY['health', 'vehicle', 'life', 'electronics', 'others']),
    ('Tax', ARRAY['income tax', 'GST', 'property tax', 'others']),
    ('Top-up', ARRAY['UPI Lite', 'ATM', 'Amazon', 'PhonePe', 'wallet', 'others']),
    ('Children', ARRAY['nutrition', 'necessities', 'toys', 'medical', 'care', 'tuition fee', 'classes fee', 'school fee', 'college fee', 'others']),
    ('Pet Care', ARRAY['food', 'toys', 'grooming', 'weight', 'others']),
    ('Business', ARRAY['salary', 'inventory', 'rent', 'logistics', 'software', 'marketing', 'tax', 'insurance', 'services', 'others']),
    ('Miscellaneous', ARRAY['tip', 'verification', 'bribe', 'forex', 'deposit', 'gift card', 'others'])
) AS catalog(parent_name, children)
JOIN financial_horizon_categories p ON p.name = catalog.parent_name
    AND p.user_id IS NULL AND p.is_default AND p.parent_id IS NULL
CROSS JOIN LATERAL unnest(catalog.children) AS child(name)
ON CONFLICT DO NOTHING;

-- Keep the snapshot large enough to hold two 100-character names and the separator.
ALTER TABLE financial_horizon_transactions ALTER COLUMN category_name TYPE VARCHAR(203);

COMMIT;

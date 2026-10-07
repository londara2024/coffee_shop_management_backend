-- =====================================================================
-- Minimum reference data the system needs to work. No demo/sample data:
-- that lives in db/demo and only loads in tests or with the 'demo' profile.
--
--   * 1 store       checkout, settings and inventory need a store; rename it in the settings page
--   * categories    a product cannot be created without one
--   * milk options  used by drink pricing (default: oat)
--   * toppings      the shared default toppings (product_id IS NULL)
-- =====================================================================

INSERT INTO stores (id, name, address, hours_label) VALUES
    ('ST001', 'Free Shop Coffee', '742 Pine Hill Court, Suite 100', 'Mon – Sun • 6:30 AM – 7:00 PM');

INSERT INTO categories (id, label, label_km, eyebrow, title, title_km, note, note_km, sort_order) VALUES
    ('coffee',   'Coffee',          'កាហ្វេ',         'Single Origin & Specialty Espresso',    'Coffee & Extraction Lab',       'កាហ្វេ',         NULL,                           NULL,                              0),
    ('tea',      'Tea & Beverages', 'តែ និងភេសជ្ជៈ',  'Ceremonial Leaf & Cold-Pressed Blends', 'Tea & Beverages',               'តែ និងភេសជ្ជៈ',  NULL,                           NULL,                              1),
    ('food',     'Food',            'អាហារ',          'Kitchen & Hearth',                      'Artisanal Food & Warm Plates',  'អាហារ',          'Served until 3:00 PM daily',   'បម្រើរហូតដល់ម៉ោង ៣ រសៀល រាល់ថ្ងៃ', 2),
    ('desserts', 'Desserts',        'បង្អែម',         'Pastry Laboratory',                     'Handcrafted Desserts & Sweets', 'បង្អែម',         'Baked in small daily batches', 'ដុតនំជាបាច់តូចៗរាល់ថ្ងៃ',         3);

INSERT INTO milk_options (code, label, label_km, price_delta, sort_order) VALUES
    ('oat',     'Oat Milk',                  'ទឹកដោះគោអូត',               0.00, 0),
    ('whole',   'Whole Milk',                'ទឹកដោះគោសុទ្ធ',              0.00, 1),
    ('almond',  'Almond Milk',               'ទឹកដោះគោអាល់ម៉ុន',           0.60, 2),
    ('coconut', 'Coconut Milk',              'ទឹកដោះគោដូង',               0.60, 3),
    ('breve',   'Organic Breve Half & Half', 'ទឹកដោះគោសរីរាង្គ Breve កន្លះ', 0.80, 4);

-- Global default toppings (product_id IS NULL = shared defaultToppings)
INSERT INTO product_toppings (id, product_id, label, label_km, price_delta, is_locked) VALUES
    ('TP001', NULL, 'Ceylon Cinnamon Dusting',        'រោយម្សៅស៊ីណាមុនស៊ីឡុង',     0.00, true),
    ('TP002', NULL, 'Extra Wildflower Honey Drizzle', 'ស្រោចទឹកឃ្មុំផ្កាព្រៃបន្ថែម',  0.50, false),
    ('TP003', NULL, 'Salted Caramel Drizzle',         'ស្រោចការ៉ាមែលអំបិល',       0.75, false),
    ('TP004', NULL, 'Whipped Vanilla Sweet Foam',     'ពពុះវ៉ានីឡាផ្អែមវាយក្រែម',   1.00, false),
    ('TP005', NULL, 'Crushed Toasted Pecans',         'គ្រាប់ភីខិនអាំងកិន',        0.85, false),
    ('TP006', NULL, 'Brown Sugar Boba Pearls',        'គុជពែលប៊ូបាស្ករត្នោត',      1.20, false);

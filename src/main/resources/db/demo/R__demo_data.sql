-- =====================================================================
-- DEMO / SAMPLE DATA - never loaded by default.
--
-- Loaded only when db/demo is on the Flyway path:
--   * tests (src/test/resources/application.properties)
--   * locally with:  SPRING_PROFILES_ACTIVE=demo  (application-demo.yml)
--
-- Repeatable migration (R__): Flyway runs it after all versioned
-- migrations. The guard below makes it insert only once, even if Flyway
-- re-runs it after this file is edited.
--
-- Contents (ported from the frontend mocks): a second store, staff,
-- 18 menu items with tags/sizes, pairings, demo customer
-- (elena.rostova@botanica.com / Password123), promo codes, discounts,
-- suppliers, inventory with stock, a draft PO, 2 open orders + KDS tickets.
-- =====================================================================

DO $demo$
BEGIN
    IF EXISTS (SELECT 1 FROM customers WHERE id = 'CU001') THEN
        RETURN; -- demo data already loaded
    END IF;

    -- ---------------------------------------------------------------------
    -- Stores & staff
    -- ---------------------------------------------------------------------
    INSERT INTO stores (id, name, address, hours_label) VALUES
            ('ST002', 'Timberyard Workshop', '1208 Craft Lane, Dockside',      'Tue – Sun • 7:00 AM – 5:00 PM');

    INSERT INTO staff (id, store_id, name, role) VALUES
        ('SF001', 'ST001', 'Mateo Silva',   'staff'    ),
        ('SF002', 'ST001', 'Elena Vasquez', 'manager'),
        ('SF003', 'ST001', 'Marco',         'staff'  );


    -- ---------------------------------------------------------------------
    -- Products (every coffee/tea drink is customizable, matching the
    -- frontend configurator's behaviour)
    -- ---------------------------------------------------------------------
    INSERT INTO products (slug, category_id, name, name_km, kicker, kicker_km, base_price, image_url, badge, image_tag, extra, customizable, description, description_km) VALUES
        ('honey-cinnamon-oat-latte', 'coffee', 'Honey Cinnamon Oat Latte', 'ឡាតេទឹកឃ្មុំ ស៊ីណាមុន និងទឹកដោះអូ៊ត', 'House Signature', 'ម្ហូបពិសេសរបស់ហាង', 6.25, '/images/honey-cinnamon-oat-latte.jpg', 'ត្រជាក់/ក្ដៅ', 'Barista Pick', NULL, true,
         'Rich organic espresso blended with velvety steamed oat milk, pure wildflower honey, and fresh-cracked Sri Lankan Ceylon cinnamon bark.',
         'អេស្ប្រេសូសរីរាង្គខាប់ ចម្រុះជាមួយទឹកដោះអូ៊តចំហុយម៉ដ្តម៉ៃ ទឹកឃ្មុំផ្កាព្រៃសុទ្ធ និងសំបកឈើស៊ីណាមុនស៊ីឡូនស្រីលង្កាដាំថ្មីៗ។'),
        ('brown-sugar-oat-shaken-espresso', 'coffee', 'Brown Sugar Oat Shaken Espresso', 'អេស្ប្រេសូគ្រើបស្ករត្នោត និងទឹកដោះអូ៊ត', 'Seasonal Brew', 'កាហ្វេតាមរដូវ', 6.50, '/images/brown-sugar-shaken-espresso.jpg', 'ត្រជាក់/ក្ដៅ', 'V · GF', NULL, true, NULL, NULL),
        ('cardamom-pistachio-cortado', 'coffee', 'Cardamom Pistachio Cortado', 'កូតាដូក្រវាញ់ និងភីស្តាស្យូ', 'Aromatic Reserve', 'កាហ្វេក្រអូបពិសេស', 5.85, '/images/cardamom-pistachio-cortado.jpg', 'ក្ដៅ', 'Spiced Nut', NULL, true, NULL, NULL),
        ('bourbon-barrel-cold-brew', 'coffee', 'Bourbon Barrel Cold Brew', 'កាហ្វេស្រង់ត្រជាក់ ធុងឈើបួរបុន', 'Aged Cask', 'ជ្រលក់ក្នុងធុងឈើចាស់', 6.75, '/images/bourbon-barrel-cold-brew.jpg', 'ត្រជាក់', '20h Barrel Steep', NULL, true, NULL, NULL),
        ('velvet-flat-white', 'coffee', 'Velvet Flat White', 'ហ្វ្លាតវ៉ែតម៉ដ្តម៉ៃ', 'Traditional Classics', 'រូបមន្តបុរាណ', 4.95, '/images/velvet-flat-white.jpg', 'ក្ដៅ', 'Double Ristretto', '6 oz', true, NULL, NULL),
        ('ceremonial-matcha-espresso-fusion', 'coffee', 'Ceremonial Matcha Espresso Fusion', 'ម៉ាចាពិធីការ លាយអេស្ប្រេសូ', 'Botanical Fusion', 'លាយបញ្ចូលរុក្ខជាតិ', 6.25, '/images/matcha-espresso-fusion.jpg', 'ត្រជាក់/ក្ដៅ', '1st Harvest Uji', NULL, true, NULL, NULL),
        ('ethiopian-yirgacheffe-pour-over', 'coffee', 'Ethiopian Yirgacheffe Pour Over', 'កាហ្វេចាក់ដៃ អេត្យូពី យ៉ែហ្គាឆេហ្វេ', 'Origin Pour Over', 'កាហ្វេចាក់ដៃដើមកំណើត', 5.25, '/images/ethiopian-pour-over.jpg', 'ក្ដៅ', 'Grade 1 Washed', '2,100m', true, NULL, NULL),
        ('toasted-hazelnut-mocha', 'coffee', 'Toasted Hazelnut Mocha', 'ម៉ូខា គ្រាប់ហាហ្សែលនាត់ដុតក្រហម', 'Signature Dessert', 'បង្អែមពិសេសរបស់ហាង', 6.10, '/images/toasted-hazelnut-mocha.jpg', 'ត្រជាក់/ក្ដៅ', 'House Confection', 'Nut Allergens', true, NULL, NULL),
        ('matcha-ceremonial-glow', 'tea', 'Matcha Ceremonial Glow', 'ម៉ាចាពិធីការ ភ្លឺរស់', 'Superfood Elixir', 'ភេសជ្ជៈអាហារឧត្តម', 8.25, '/images/matcha-ceremonial-glow.jpg', 'ត្រជាក់/ក្ដៅ', 'Lion''s Mane', NULL, true, NULL, NULL),
        ('golden-turmeric-mango-smoothie', 'tea', 'Golden Turmeric Mango Smoothie', 'ស្មូធីស្វាយ ខ្ញីលឿង', 'Wellness Elixir', 'ភេសជ្ជៈសុខភាព', 7.25, '/images/golden-turmeric-mango.jpg', 'ត្រជាក់', 'Adaptogenic', NULL, true, NULL, NULL),
        ('acai-blueberry-protein-blast', 'tea', 'Açaí Blueberry Protein Blast', 'អាសៃ ប៊្លូបឺរី ប្រូតេអ៊ីនពេញកម្លាំង', 'Power Blend', 'លាយថាមពល', 7.95, '/images/acai-protein-blast.jpg', 'ត្រជាក់', '22g Clean Protein', NULL, true, NULL, NULL),
        ('pitaya-cold-pressed-botanical', 'tea', 'Pitaya Cold-Pressed Botanical', 'ស្រកានាគ សង្កត់ត្រជាក់ រុក្ខជាតិ', 'Vitality Tonic', 'ភេសជ្ជៈបំប៉នកម្លាំង', 7.50, '/images/pitaya-botanical.jpg', 'ត្រជាក់', 'Vitamin C', NULL, true, NULL, NULL),
        ('raw-cacao-maca-recovery', 'tea', 'Raw Cacao Maca Recovery', 'កាកាវឆៅ និងម៉ាកា ស្តារកម្លាំង', 'Energy Elixir', 'ភេសជ្ជៈថាមពល', 8.50, '/images/raw-cacao-maca.jpg', 'ត្រជាក់', 'Plant Protein', NULL, true, NULL, NULL),
        ('spirulina-cleanse-chlorophyll', 'tea', 'Spirulina Cleanse & Chlorophyll', 'ស្ព្យារូលីណា សម្អាតរាងកាយ និងក្លរ៉ូហ្វីល', 'Green Tonic', 'ភេសជ្ជៈបៃតង', 7.75, '/images/spirulina-cleanse.jpg', 'ត្រជាក់', 'Raw Chlorophyll', NULL, true, NULL, NULL),
        ('avocado-jammy-egg-sourdough', 'food', 'Avocado & Jammy Egg Sourdough', 'នំបុ័ងស៊ូដោ អាវ៉ូកាដូ និងស៊ុតលឿងទន់', 'Kitchen Special', 'ម្ហូបពិសេសផ្ទះបាយ', 9.50, '/images/avocado-sourdough.jpg', 'ក្ដៅ', 'Breakfast Tartine', NULL, false, NULL, NULL),
        ('prosciutto-gruyere-croissant', 'food', 'Prosciutto Gruyère Warm Croissant', 'ក្រូវ៉ាសង់ក្តៅ ប្រូស្យូតូ និងឈីសហ្គ្រុយអែរ', 'Warm Pastry', 'នំក្តៅស្រស់', 7.80, '/images/prosciutto-croissant.jpg', 'ក្ដៅ', 'Artisanal Hearth', NULL, false, NULL, NULL),
        ('basque-burnt-cheesecake', 'desserts', 'Basque Burnt Cheesecake', 'ឈីសខេកបាស្គ ដុតក្រៀម', 'Pastry Lab', 'មន្ទីរពិសោធន៍នំបុ័ង', 6.50, '/images/basque-cheesecake.jpg', 'ត្រជាក់', 'Gluten-Free', NULL, false, NULL, NULL),
        ('espresso-tiramisu-jar', 'desserts', 'Espresso Tiramisu Jar', 'ធីរ៉ាមីស៊ូអេស្ប្រេសូ ក្នុងកែវ', 'House Confection', 'នំផ្អែមធ្វើដោយហាង', 6.00, '/images/tiramisu-jar.jpg', 'ត្រជាក់', 'Individual Jar', NULL, false, NULL, NULL);

    INSERT INTO product_tags (product_id, label, label_km, tone)
    SELECT p.id, t.label, t.label_km, t.tone
    FROM (VALUES
        ('honey-cinnamon-oat-latte',          'Velvety & Warm',   'ម៉ដ្តម៉ៃ និងកក្តៅ',   'amber'),
        ('brown-sugar-oat-shaken-espresso',   'Vegan',            'អាហារវែហ្គិន',       'forest'),
        ('cardamom-pistachio-cortado',        'Signature',        'ពិសេសរបស់ហាង',      'amber'),
        ('bourbon-barrel-cold-brew',          'Oak & Vanilla',    'ឈើអូក និងវ៉ានីឡា',   'amber'),
        ('velvet-flat-white',                 'Intense & Smooth', 'ខាប់ និងរលោង',       'amber'),
        ('ceremonial-matcha-espresso-fusion', 'Antioxidant',      'ប្រឆាំងអុកស៊ីតកម្ម',  'forest'),
        ('ceremonial-matcha-espresso-fusion', 'Vegan',            'អាហារវែហ្គិន',       'neutral'),
        ('ethiopian-yirgacheffe-pour-over',   'Light Roast',      'ដុតស្រាល',          'amber'),
        ('toasted-hazelnut-mocha',            'Indulgent',        'ស្កប់ស្កល់',         'amber'),
        ('matcha-ceremonial-glow',            'Antioxidant',      'ប្រឆាំងអុកស៊ីតកម្ម',  'forest'),
        ('golden-turmeric-mango-smoothie',    'Plant Based',      'ផលិតផលពីរុក្ខជាតិ',   'forest'),
        ('acai-blueberry-protein-blast',      'High Protein',     'ប្រូតេអ៊ីនខ្ពស់',     'amber'),
        ('pitaya-cold-pressed-botanical',     'Vegan',            'អាហារវែហ្គិន',       'forest'),
        ('raw-cacao-maca-recovery',           'Adaptogen',        'សារធាតុសម្របខ្លួន',   'amber'),
        ('spirulina-cleanse-chlorophyll',     'Detox',            'សម្អាតជាតិពុល',       'forest'),
        ('avocado-jammy-egg-sourdough',       'Vegetarian',       'អាហារបួស',          'forest'),
        ('prosciutto-gruyere-croissant',      'Savory',           'រសជាតិប្រៃ',         'amber'),
        ('basque-burnt-cheesecake',           'Signature',        'ពិសេសរបស់ហាង',      'amber'),
        ('espresso-tiramisu-jar',             'Artisanal',        'សិប្បកម្ម',          'amber')
    ) AS t (slug, label, label_km, tone)
    JOIN products p ON p.slug = t.slug;

    -- Sizes: every customizable drink gets small / regular / large priced
    -- relative to base (-0.50 / +0.00 / +0.60), same as the configurator.
    -- sort_order: 0 = small, 1 = regular, 2 = large (DATABASE_DESIGN.md).
    INSERT INTO product_sizes (product_id, size_label, sort_order, price)
    SELECT p.id, s.size_label, s.sort_order, p.base_price + s.delta
    FROM products p
    CROSS JOIN (VALUES
        ('តូច',   0, -0.50),
        ('ធម្មតា', 1,  0.00),
        ('ធំ',    2,  0.60)
    ) AS s (size_label, sort_order, delta)
    WHERE p.customizable = true;



    INSERT INTO pairings (slug, name, name_km, description, description_km, price, image_url, cta_label, cta_label_km) VALUES
        ('cardamom-morning-bun', 'Cardamom Morning Bun', 'នំបុ័ងព្រឹក ក្រវាញ់',
         'Laminated dough with spiced Swedish cardamom butter.', 'ម្សៅជាន់ជាស្រទាប់ ជាមួយប៊ឺក្រវាញ់ម៉ូតស៊ុយអែត។',
         4.50, '/images/cardamom-bun.jpg', 'Add Pairing', 'បន្ថែមម្ហូបផ្គូផ្គង'),
        ('cooperativa-santa-teresa-12oz', 'Cooperativa Santa Teresa (12oz)', 'កូអូភើរ៉ាទីវ៉ា សាន់តាតេរេសា (១២ អោន់)',
         'The exact roasted beans used for this signature craft latte.', 'គ្រាប់កាហ្វេដុតដូចគ្នាបេះបិទ ដែលប្រើសម្រាប់ធ្វើឡាតេពិសេសនេះ។',
         21.00, '/images/colombia-bean-bag.jpg', 'Add Whole Bean Bag', 'បន្ថែមថង់គ្រាប់កាហ្វេទាំងមូល'),
        ('wild-honey-almond-biscotti', 'Wild Honey Almond Biscotti', 'ប៊ីស្កូទីអាម៉ុង ទឹកឃ្មុំព្រៃ',
         'Double baked with roasted local almonds and honey crunch.', 'ដុតពីរដង ជាមួយអាម៉ុងក្នុងស្រុកអាំង និងទឹកឃ្មុំក្រែម។',
         3.25, '/images/almond-biscotti.jpg', 'Add Pairing', 'បន្ថែមម្ហូបផ្គូផ្គង');

    -- ---------------------------------------------------------------------
    -- Demo customer (password: Password123)
    -- ---------------------------------------------------------------------
    INSERT INTO customers (id, name, phone, email, avatar_url, password_hash) VALUES
        ('CU001', 'Elena Rostova', '(555) 234-8901', 'elena.rostova@botanica.com',
         '/images/customer-portrait.png', '$2a$10$6zymWPYoTazitezYyHqnEe4CbBiSMyI6JbgO0U34eoZLa5ZsYFma2');

    INSERT INTO loyalty_accounts (customer_id, tier, stars, stars_goal) VALUES
        ('CU001', 'Gold Member', 178, 200);

    -- ---------------------------------------------------------------------
    -- Promotions
    -- ---------------------------------------------------------------------
    INSERT INTO promo_codes (code, label, amount) VALUES
        ('AURAWARMTH', 'AURAWARMTH', 0.50),
        ('WELCOME5',   'WELCOME5',   0.75),
        ('FIRSTCUP',   'FIRSTCUP',   1.00);

    INSERT INTO discounts (id, label, amount) VALUES
        ('DC001', 'បញ្ចុះតម្លៃ $1.00', 1.00),
        ('DC002', 'បញ្ចុះតម្លៃ $2.00', 2.00),
        ('DC005', 'បញ្ចុះតម្លៃ $5.00', 5.00);

    -- ---------------------------------------------------------------------
    -- Suppliers, inventory, stock & a draft purchase order
    -- ---------------------------------------------------------------------
    INSERT INTO suppliers (name, note, relationship_type) VALUES
        ('Finca La Esperanza',      'ជួញដូរផ្ទាល់ បានចុះហត្ថលេខា', 'direct_trade'),
        ('Minor Figures Dist.',     'ខ្សែសង្វាក់ត្រជាក់ក្នុងតំបន់',  'distributor'),
        ('Local Bee Guild',         'Sonoma សរីរាង្គ',            'distributor'),
        ('Spice Collective Direct', 'ចម្ការតែមួយ Sri Lanka',       'direct_trade'),
        ('Heritage Bakehouse',      'បញ្ជាទិញប្រចាំថ្ងៃ',           'distributor'),
        ('EcoWare Global',          'បានទទួលវិញ្ញាបនបត្រ B-Corp',   'distributor'),
        ('Aura Central Roastery',   'Lot #91',                   'direct_trade');

    INSERT INTO inventory_items (store_id, supplier_id, name, meta, group_name, unit_label, par_level, image_url)
    SELECT 'ST001', s.id, i.name, i.meta, i.group_name, i.unit_label, i.par_level, i.image_url
    FROM (VALUES
        ('Colombian Pink Bourbon',       'Huila · Anaerobic Washed · Lot #248',          'Single Origin Beans', 'kg',     25,   '/images/green-beans.jpg',     'Finca La Esperanza'),
        ('Minor Figures Barista Oat',    'កេស Tetra Pak ស្ងួតគ្មានមេរោគ 6× 1L',           'Milk & Dairy',        'case',   15,   '/images/oat-milk-carton.jpg', 'Minor Figures Dist.'),
        ('Wildflower Honey Reserve',     'ដកស្រង់ដោយដៃ មិនច្រោះ ច្រូតកាត់ក្នុងស្រុក (500g)',  'Syrups & Botanicals', 'bottle', 10,   NULL,                          'Local Bee Guild'),
        ('Ceylon Cinnamon Bark',         'កិនដោយថ្ម សរីរាង្គ · ថ្នាក់ ''Alba''',              'Syrups & Botanicals', 'kg',     3,    NULL,                          'Spice Collective Direct'),
        ('Country Sourdough Batards',    'ដម្កល់ត្រជាក់ 48 ម៉ោង · ដុតស្រស់រាល់ថ្ងៃ',           'Bakery',              'loaf',   10,   NULL,                          'Heritage Bakehouse'),
        ('12oz & 16oz Compostable Cups', 'ជាតិសរសៃឫស្សី ស្រោបជាមួយ PLA ស្នាមឡូហ្គោចាំង',      'Packaging',           'unit',   1000, NULL,                          'EcoWare Global')
    ) AS i (name, meta, group_name, unit_label, par_level, image_url, supplier_name)
    JOIN suppliers s ON s.name = i.supplier_name;

    INSERT INTO inventory_stock_snapshots (inventory_item_id, on_hand_qty, pct_of_par, state)
    SELECT ii.id, v.on_hand, v.pct, v.state
    FROM (VALUES
        ('Colombian Pink Bourbon',       14,  56,  'low'),
        ('Minor Figures Barista Oat',    6,   40,  'critical'),
        ('Wildflower Honey Reserve',     18,  180, 'healthy'),
        ('Ceylon Cinnamon Bark',         4.5, 150, 'healthy'),
        ('Country Sourdough Batards',    12,  120, 'fresh'),
        ('12oz & 16oz Compostable Cups', 850, 85,  'adequate')
    ) AS v (name, on_hand, pct, state)
    JOIN inventory_items ii ON ii.name = v.name;

    INSERT INTO purchase_orders (id, po_code, store_id, status, funding_account, created_by_staff_id) VALUES
        ('PO084', 'PO-2025-084', 'ST001', 'draft',
         'Flagship Roastery Operating Fund (#***849)', 'SF002');

    INSERT INTO purchase_order_lines (purchase_order_id, inventory_item_id, description, supplier_id, qty, price)
    SELECT 'PO084',
           (SELECT id FROM inventory_items WHERE name = l.item_name),
           l.description,
           (SELECT id FROM suppliers WHERE name = l.supplier_name),
           l.qty, l.price
    FROM (VALUES
        ('Minor Figures Barista Oat (12cs)',  'Minor Figures Barista Oat', 'Minor Figures Dist.',   12, 336.00),
        ('House Espresso Blend Green (20kg)', NULL,                        'Aura Central Roastery', 1,  304.00)
    ) AS l (description, item_name, supplier_name, qty, price);

    -- ---------------------------------------------------------------------
    -- Demo orders + KDS tickets (kds-board.tsx)
    -- ---------------------------------------------------------------------
    INSERT INTO orders (id, order_code, customer_id, store_id, fulfillment_method, fulfillment_timing,
                        subtotal, tax, tip, total, status, placed_at) VALUES
        ('OR942', 'ACR-8942', 'CU001',
         'ST001', 'store', 'asap', 22.25, 0, 0, 22.25, 'brewing', now() - interval '4 minutes'),
        ('OR944', 'ACR-8944', NULL,
         'ST001', 'store', 'asap', 13.20, 0, 0, 13.20, 'new', now() - interval '1 minute');

    INSERT INTO payments (order_id, method, method_label, amount, paid_at) VALUES
        ('OR942', 'aba',  'ABA ឬស្កេន QR', 22.25, now() - interval '4 minutes'),
        ('OR944', 'cash', 'សាច់ប្រាក់',     13.20, NULL);

    INSERT INTO order_items (id, order_id, product_id, qty, unit_price, details_label, station)
    SELECT v.id, v.order_id, p.id, v.qty, v.unit_price, v.details_label, v.station
    FROM (VALUES
        ('OI421', 'OR942', 'honey-cinnamon-oat-latte',        1, 6.25, 'ធម្មតា · Hot · Oat Milk · Extra Wildflower Honey Drizzle', 'Bar A'),
        ('OI422', 'OR942', 'avocado-jammy-egg-sourdough',     1, 9.50, 'Organic sourdough · Chili flakes · Extra fresh herbs',      'Hearth'),
        ('OI423', 'OR942', 'basque-burnt-cheesecake',         1, 6.50, 'Custard center · Blackberry-elderberry coulis',             'Bakery'),
        ('OI441', 'OR944', 'ethiopian-yirgacheffe-pour-over', 1, 5.25, 'ធម្មតា · Hot · Oat Milk',                                    'Bar B'),
        ('OI442', 'OR944', 'acai-blueberry-protein-blast',    1, 7.95, 'ធម្មតា · Iced · Oat Milk',                                   'Cold Lab')
    ) AS v (id, order_id, slug, qty, unit_price, details_label, station)
    JOIN products p ON p.slug = v.slug;

    INSERT INTO order_item_notes (order_item_id, note, sort_order) VALUES
        ('OI421', 'ពែងសេរ៉ាមិច 12oz ចំហុយក្តៅ 145°F', 0),
        ('OI421', 'Minor Figures Barista Oat Milk', 1),
        ('OI421', 'រោយម្សៅស៊ីណាមុន Ceylon + ទឹកឃ្មុំផ្កាព្រៃបន្ថែម', 2),
        ('OI422', 'នំបុ័ង Sourdough ជនបទ ស៊ុតទឹកកណ្ដាល អំបិល Maldon កំទេចម្ទេសក្រហម', 0),
        ('OI423', 'ចំណិតត្រជាក់ លាបជាមួយផលិតផលកក់ក្រូចឆ្មារ Meyer', 0),
        ('OI441', 'វិធីស្រង់៖ Chemex 3-Cup Classic', 0),
        ('OI442', 'ប្រូតេអ៊ីនសណ្ដែក 22g ផ្លែប៊្លូបឺរីព្រៃ ទឹកដូង គ្រាប់ Hemp', 0);

    INSERT INTO kitchen_tickets (id, order_id, status, channel_label, sla_minutes, started_at, flag_tone, flag_text, assigned_staff_id) VALUES
        ('KT942', 'OR942', 'brewing', 'អាទិភាពរហ័ស', 6,
         now() - interval '222 seconds', 'neutral',
         'ចំណង់ចំណូលចិត្តអតិថិជនអេកូ៖ កញ្ចប់អេកូកាត់បន្ថយសំណល់ — មិនប្រើគម្របឧបករណ៍ និងក្រដាសអត់ដៃបន្ថែម។',
         'SF001'),
        ('KT944', 'OR944', 'new', 'ទទួលនៅកន្លែងបញ្ជរ', 8,
         now() - interval '65 seconds', 'neutral', 'កន្លែងព្រុះកាហ្វេ Chemex កំពុងទំនេរ។ អាចរួចរាល់លឿន។', NULL);

    INSERT INTO kitchen_ticket_items (ticket_id, order_item_id, station)
    SELECT CASE WHEN oi.order_id = 'OR942'
                THEN 'KT942'
                ELSE 'KT944' END,
           oi.id, oi.station
    FROM order_items oi
    WHERE oi.order_id IN ('OR942', 'OR944');
END
$demo$;

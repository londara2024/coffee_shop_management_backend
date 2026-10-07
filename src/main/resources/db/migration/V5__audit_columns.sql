-- =====================================================================
-- Auditing: every table gets created_at / updated_at / created_by / updated_by.
-- Filled by Spring Data JPA auditing (common.persistence.Auditable).
-- Columns that already exist (e.g. products.created_at) are kept as-is.
-- Existing rows are backfilled: timestamps = now(), actor = 'system'.
-- Business timestamps (orders.placed_at, payments.paid_at, ...) are separate and unchanged.
-- =====================================================================

DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'stores', 'staff',
        'categories', 'products', 'product_sizes', 'product_toppings', 'product_tags', 'milk_options', 'pairings',
        'customers', 'loyalty_accounts',
        'carts', 'cart_items',
        'promo_codes', 'discounts',
        'orders', 'order_items', 'order_item_notes', 'payments',
        'kitchen_tickets', 'kitchen_ticket_items',
        'suppliers', 'inventory_items', 'inventory_stock_snapshots', 'purchase_orders', 'purchase_order_lines',
        'brand_colors'
    ]
    LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS created_at timestamptz NOT NULL DEFAULT now()', t);
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now()', t);
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS created_by varchar(100) DEFAULT ''system''', t);
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS updated_by varchar(100) DEFAULT ''system''', t);
    END LOOP;
END $$;

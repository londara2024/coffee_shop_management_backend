-- =====================================================================
-- Aura Coffee Roasters - initial schema (PostgreSQL 16)
-- Implements DATABASE_DESIGN.md section 3 table-by-table.
-- Additions beyond the design doc (needed by the API) are marked [ext].
-- Primary keys are 5-character ids like 'aZ3x9' from short_id(table) below.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Short ids: 5 random characters from [0-9A-Za-z] (62^5 ~ 916 million).
-- With so few possible values random collisions become likely once a
-- table holds tens of thousands of rows, so the function checks the
-- table and retries until it finds an unused id. Used as the default
-- of every primary key and by the Java ShortIdGenerator.
-- ---------------------------------------------------------------------
CREATE FUNCTION short_id(tbl text) RETURNS varchar(5)
    LANGUAGE plpgsql VOLATILE AS
$fn$
DECLARE
    alphabet  constant text := '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
    candidate text;
    taken     boolean;
BEGIN
    LOOP
        candidate := '';
        FOR i IN 1..5 LOOP
            candidate := candidate || substr(alphabet, 1 + floor(random() * 62)::int, 1);
        END LOOP;
        EXECUTE format('SELECT EXISTS (SELECT 1 FROM %I WHERE id = $1)', tbl) INTO taken USING candidate;
        IF NOT taken THEN
            RETURN candidate;
        END IF;
    END LOOP;
END
$fn$;

-- ---------------------------------------------------------------------
-- 3.6 Stores & staff (created first: referenced by most other tables)
-- ---------------------------------------------------------------------
CREATE TABLE stores (
    id           varchar(5) PRIMARY KEY DEFAULT short_id('stores'),
    name         varchar(150) NOT NULL,
    address      varchar(250) NOT NULL,
    hours_label  varchar(100)
);

CREATE TABLE staff (
    id        varchar(5) PRIMARY KEY DEFAULT short_id('staff'),
    store_id  varchar(5)         NOT NULL REFERENCES stores (id),
    name      varchar(150) NOT NULL,
    role      varchar(40)  NOT NULL
);
CREATE INDEX idx_staff_store ON staff (store_id);

-- ---------------------------------------------------------------------
-- 3.1 Catalog
-- ---------------------------------------------------------------------
CREATE TABLE categories (
    id          varchar(20) PRIMARY KEY,
    label       varchar(100) NOT NULL,
    label_km    varchar(100),
    eyebrow     varchar(150),
    title       varchar(150),
    title_km    varchar(150),
    note        varchar(200),
    note_km     varchar(200),
    sort_order  smallint     NOT NULL DEFAULT 0
);

CREATE TABLE products (
    id                   varchar(5) PRIMARY KEY DEFAULT short_id('products'),
    slug                 varchar(150)   NOT NULL UNIQUE,
    category_id          varchar(20)    NOT NULL REFERENCES categories (id),
    name                 varchar(150)   NOT NULL,
    name_km              varchar(150),
    kicker               varchar(150),
    kicker_km            varchar(150),
    description          text,
    description_km       text,
    base_price           numeric(10, 2) NOT NULL CHECK (base_price >= 0),
    image_url            varchar(300)   NOT NULL,
    image_tag            varchar(80),
    badge                varchar(40),
    extra                varchar(40),
    customizable         boolean        NOT NULL DEFAULT false,
    is_custom            boolean        NOT NULL DEFAULT false,
    is_live              boolean        NOT NULL DEFAULT true,
    created_by_staff_id  varchar(5) REFERENCES staff (id),
    created_at           timestamptz    NOT NULL DEFAULT now(),
    updated_at           timestamptz    NOT NULL DEFAULT now()
);
CREATE INDEX idx_products_category ON products (category_id);

CREATE TABLE product_sizes (
    id          varchar(5) PRIMARY KEY DEFAULT short_id('product_sizes'),
    product_id  varchar(5)           NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    size_label  varchar(20)    NOT NULL,
    sort_order  smallint       NOT NULL,
    price       numeric(10, 2) NOT NULL CHECK (price >= 0),
    CONSTRAINT uq_product_size_order UNIQUE (product_id, sort_order)
);

-- product_id NULL = global default topping offered on every customizable drink
CREATE TABLE product_toppings (
    id           varchar(5) PRIMARY KEY DEFAULT short_id('product_toppings'),
    product_id   varchar(5) REFERENCES products (id) ON DELETE CASCADE,
    label        varchar(80)    NOT NULL,
    label_km     varchar(80),
    price_delta  numeric(10, 2) NOT NULL DEFAULT 0 CHECK (price_delta >= 0),
    is_locked    boolean        NOT NULL DEFAULT false
);
CREATE INDEX idx_product_toppings_product ON product_toppings (product_id);

CREATE TABLE product_tags (
    id          varchar(5) PRIMARY KEY DEFAULT short_id('product_tags'),
    product_id  varchar(5)        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    label       varchar(60) NOT NULL,
    label_km    varchar(60),
    tone        varchar(10) NOT NULL CHECK (tone IN ('forest', 'amber', 'neutral'))
);
CREATE INDEX idx_product_tags_product ON product_tags (product_id);

-- [ext] Milk choice changes the drink price in product-configurator.tsx,
-- so it needs to live server-side for authoritative pricing.
CREATE TABLE milk_options (
    code         varchar(20) PRIMARY KEY,
    label        varchar(80)    NOT NULL,
    label_km     varchar(80),
    price_delta  numeric(10, 2) NOT NULL DEFAULT 0 CHECK (price_delta >= 0),
    sort_order   smallint       NOT NULL DEFAULT 0
);

CREATE TABLE pairings (
    id              varchar(5) PRIMARY KEY DEFAULT short_id('pairings'),
    slug            varchar(150)   NOT NULL UNIQUE,
    name            varchar(150)   NOT NULL,
    name_km         varchar(150),
    description     text,
    description_km  text,
    price           numeric(10, 2) NOT NULL CHECK (price >= 0),
    image_url       varchar(300)   NOT NULL,
    cta_label       varchar(60),
    cta_label_km    varchar(60)
);

-- ---------------------------------------------------------------------
-- 3.2 Customers & loyalty
-- ---------------------------------------------------------------------
CREATE TABLE customers (
    id             varchar(5) PRIMARY KEY DEFAULT short_id('customers'),
    name           varchar(150) NOT NULL,
    phone          varchar(30)  UNIQUE,
    email          varchar(200) UNIQUE,
    avatar_url     varchar(300),
    password_hash  varchar(100),            -- [ext] BCrypt hash for sign-in / sign-up
    created_at     timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE loyalty_accounts (
    customer_id  varchar(5) PRIMARY KEY REFERENCES customers (id) ON DELETE CASCADE,
    tier         varchar(40) NOT NULL DEFAULT 'Member',
    stars        int         NOT NULL DEFAULT 0 CHECK (stars >= 0),
    stars_goal   int         NOT NULL DEFAULT 200 CHECK (stars_goal > 0),
    updated_at   timestamptz NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- 3.3 Cart & checkout
-- ---------------------------------------------------------------------
CREATE TABLE carts (
    id           varchar(5) PRIMARY KEY DEFAULT short_id('carts'),
    customer_id  varchar(5) REFERENCES customers (id) ON DELETE SET NULL,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_carts_customer ON carts (customer_id);

CREATE TABLE cart_items (
    id             varchar(5) PRIMARY KEY DEFAULT short_id('cart_items'),
    cart_id        varchar(5)           NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    product_id     varchar(5)           NOT NULL REFERENCES products (id),
    variant_key    varchar(200)   NOT NULL,
    unit_price     numeric(10, 2) NOT NULL CHECK (unit_price >= 0),
    qty            int            NOT NULL CHECK (qty > 0),
    details_label  varchar(300),
    tag            varchar(60),
    CONSTRAINT uq_cart_variant UNIQUE (cart_id, variant_key)
);

CREATE TABLE promo_codes (
    code    varchar(40) PRIMARY KEY,
    label   varchar(60)    NOT NULL,
    amount  numeric(10, 2) NOT NULL CHECK (amount >= 0),
    active  boolean        NOT NULL DEFAULT true
);

CREATE TABLE discounts (
    id      varchar(5) PRIMARY KEY DEFAULT short_id('discounts'),
    label   varchar(60)    NOT NULL,
    amount  numeric(10, 2) NOT NULL CHECK (amount >= 0)
);

-- Human-friendly order codes (ACR-8946, ...). Demo seeds use 8942/8944.
CREATE SEQUENCE order_code_seq START WITH 8946;

CREATE TABLE orders (
    id                     varchar(5) PRIMARY KEY DEFAULT short_id('orders'),
    order_code             varchar(20)    NOT NULL UNIQUE,
    customer_id            varchar(5) REFERENCES customers (id),
    store_id               varchar(5)           NOT NULL REFERENCES stores (id),
    fulfillment_method     varchar(20)    NOT NULL CHECK (fulfillment_method IN ('store', 'curbside')),
    fulfillment_timing     varchar(20)    NOT NULL DEFAULT 'asap' CHECK (fulfillment_timing IN ('asap', 'later')),
    scheduled_for          timestamptz,
    subtotal               numeric(10, 2) NOT NULL,
    tax                    numeric(10, 2) NOT NULL DEFAULT 0,
    tip                    numeric(10, 2) NOT NULL DEFAULT 0,
    discount_id            varchar(5) REFERENCES discounts (id),
    discount_amount        numeric(10, 2) NOT NULL DEFAULT 0,
    promo_code             varchar(40) REFERENCES promo_codes (code),
    promo_discount_amount  numeric(10, 2) NOT NULL DEFAULT 0,
    total                  numeric(10, 2) NOT NULL CHECK (total >= 0),
    status                 varchar(20)    NOT NULL DEFAULT 'new'
                           CHECK (status IN ('new', 'brewing', 'ready', 'completed', 'cancelled')),
    placed_at              timestamptz    NOT NULL DEFAULT now(),
    CONSTRAINT chk_orders_scheduled CHECK (fulfillment_timing = 'asap' OR scheduled_for IS NOT NULL)
);
CREATE INDEX idx_orders_customer ON orders (customer_id);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_items (
    id             varchar(5) PRIMARY KEY DEFAULT short_id('order_items'),
    order_id       varchar(5)           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id     varchar(5)           NOT NULL REFERENCES products (id),
    qty            int            NOT NULL CHECK (qty > 0),
    unit_price     numeric(10, 2) NOT NULL,
    details_label  varchar(300),
    station        varchar(20)
);
CREATE INDEX idx_order_items_order ON order_items (order_id);

CREATE TABLE order_item_notes (
    id             varchar(5) PRIMARY KEY DEFAULT short_id('order_item_notes'),
    order_item_id  varchar(5)         NOT NULL REFERENCES order_items (id) ON DELETE CASCADE,
    note           varchar(300) NOT NULL,
    sort_order     smallint     NOT NULL DEFAULT 0
);
CREATE INDEX idx_order_item_notes_item ON order_item_notes (order_item_id);

CREATE TABLE payments (
    id            varchar(5) PRIMARY KEY DEFAULT short_id('payments'),
    order_id      varchar(5)           NOT NULL UNIQUE REFERENCES orders (id) ON DELETE CASCADE,
    method        varchar(20)    NOT NULL CHECK (method IN ('aba', 'cash')),
    method_label  varchar(80)    NOT NULL,
    amount        numeric(10, 2) NOT NULL,
    paid_at       timestamptz
);

-- ---------------------------------------------------------------------
-- 3.4 Kitchen display (KDS)
-- ---------------------------------------------------------------------
CREATE TABLE kitchen_tickets (
    id                 varchar(5) PRIMARY KEY DEFAULT short_id('kitchen_tickets'),
    order_id           varchar(5)        NOT NULL UNIQUE REFERENCES orders (id) ON DELETE CASCADE,
    status             varchar(20) NOT NULL CHECK (status IN ('new', 'brewing', 'ready', 'scheduled')),
    channel_label      varchar(80),
    sla_minutes        int         NOT NULL DEFAULT 10 CHECK (sla_minutes > 0),
    started_at         timestamptz NOT NULL DEFAULT now(),
    ready_at           timestamptz,
    flag_tone          varchar(10) CHECK (flag_tone IN ('amber', 'neutral')),
    flag_text          varchar(300),
    assigned_staff_id  varchar(5) REFERENCES staff (id)
);
CREATE INDEX idx_kitchen_tickets_status ON kitchen_tickets (status);

CREATE TABLE kitchen_ticket_items (
    id             varchar(5) PRIMARY KEY DEFAULT short_id('kitchen_ticket_items'),
    ticket_id      varchar(5)        NOT NULL REFERENCES kitchen_tickets (id) ON DELETE CASCADE,
    order_item_id  varchar(5)        NOT NULL REFERENCES order_items (id) ON DELETE CASCADE,
    station        varchar(20) NOT NULL CHECK (station IN ('Bar A', 'Bar B', 'Hearth', 'Bakery', 'Cold Lab', 'Tea Bar')),
    done           boolean     NOT NULL DEFAULT false
);
CREATE INDEX idx_kitchen_ticket_items_ticket ON kitchen_ticket_items (ticket_id);

-- ---------------------------------------------------------------------
-- 3.5 Inventory & purchasing
-- ---------------------------------------------------------------------
CREATE TABLE suppliers (
    id                 varchar(5) PRIMARY KEY DEFAULT short_id('suppliers'),
    name               varchar(150) NOT NULL UNIQUE,
    note               varchar(200),
    relationship_type  varchar(20)  NOT NULL CHECK (relationship_type IN ('direct_trade', 'distributor'))
);

CREATE TABLE inventory_items (
    id           varchar(5) PRIMARY KEY DEFAULT short_id('inventory_items'),
    store_id     varchar(5)           NOT NULL REFERENCES stores (id),
    supplier_id  varchar(5) REFERENCES suppliers (id),
    name         varchar(150)   NOT NULL,
    meta         varchar(250),
    group_name   varchar(40)    NOT NULL
                 CHECK (group_name IN ('Single Origin Beans', 'Milk & Dairy', 'Syrups & Botanicals', 'Bakery', 'Packaging')),
    unit_label   varchar(20)    NOT NULL DEFAULT 'kg',
    par_level    numeric(10, 2) NOT NULL CHECK (par_level > 0),
    image_url    varchar(300),
    is_custom    boolean        NOT NULL DEFAULT false,
    created_at   timestamptz    NOT NULL DEFAULT now(),
    CONSTRAINT uq_inventory_store_name UNIQUE (store_id, name)
);
CREATE INDEX idx_inventory_items_supplier ON inventory_items (supplier_id);

CREATE TABLE inventory_stock_snapshots (
    id                 varchar(5) PRIMARY KEY DEFAULT short_id('inventory_stock_snapshots'),
    inventory_item_id  varchar(5)           NOT NULL REFERENCES inventory_items (id) ON DELETE CASCADE,
    on_hand_qty        numeric(10, 2) NOT NULL CHECK (on_hand_qty >= 0),
    pct_of_par         smallint       NOT NULL,
    state              varchar(20)    NOT NULL CHECK (state IN ('low', 'critical', 'healthy', 'fresh', 'adequate')),
    recorded_at        timestamptz    NOT NULL DEFAULT now()
);
CREATE INDEX idx_stock_snapshots_item_time ON inventory_stock_snapshots (inventory_item_id, recorded_at DESC);

-- PO-<year>-<seq>. Demo seed uses PO-2025-084.
CREATE SEQUENCE po_code_seq START WITH 85;

CREATE TABLE purchase_orders (
    id                   varchar(5) PRIMARY KEY DEFAULT short_id('purchase_orders'),
    po_code              varchar(20) NOT NULL UNIQUE,
    store_id             varchar(5)        NOT NULL REFERENCES stores (id),
    status               varchar(20) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'approved', 'sent')),
    funding_account      varchar(100),
    created_by_staff_id  varchar(5) REFERENCES staff (id),
    created_at           timestamptz NOT NULL DEFAULT now(),
    approved_at          timestamptz
);
CREATE INDEX idx_purchase_orders_status ON purchase_orders (status);

CREATE TABLE purchase_order_lines (
    id                 varchar(5) PRIMARY KEY DEFAULT short_id('purchase_order_lines'),
    purchase_order_id  varchar(5)           NOT NULL REFERENCES purchase_orders (id) ON DELETE CASCADE,
    inventory_item_id  varchar(5) REFERENCES inventory_items (id) ON DELETE SET NULL,
    description        varchar(200)   NOT NULL,
    supplier_id        varchar(5) REFERENCES suppliers (id),
    qty                int            NOT NULL DEFAULT 1 CHECK (qty > 0),
    price              numeric(10, 2) NOT NULL CHECK (price > 0)
);
CREATE INDEX idx_po_lines_po ON purchase_order_lines (purchase_order_id);

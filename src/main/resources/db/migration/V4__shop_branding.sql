-- =====================================================================
-- Shop branding (admin settings page: logo + brand colour)
--   * brand_colors: the preset palette offered in the colour picker
--   * stores.logo_url / stores.brand_color: the shop's chosen branding,
--     applied site-wide by the frontend (LogoMark, brand-color-style.tsx)
-- =====================================================================

CREATE TABLE brand_colors (
    code        varchar(20) PRIMARY KEY,
    label       varchar(60) NOT NULL,
    label_km    varchar(60),
    hex         varchar(7)  NOT NULL UNIQUE CHECK (hex ~ '^#[0-9a-f]{6}$'),
    is_default  boolean     NOT NULL DEFAULT false,
    sort_order  smallint    NOT NULL DEFAULT 0
);
-- at most one default colour
CREATE UNIQUE INDEX uq_brand_colors_default ON brand_colors (is_default) WHERE is_default;

INSERT INTO brand_colors (code, label, label_km, hex, is_default, sort_order) VALUES
    ('amber',      'Amber (default)', 'អំបរ (លំនាំដើម)', '#8d4f06', true,  0),
    ('forest',     'Forest',          'បៃតង',           '#3a5a40', false, 1),
    ('burgundy',   'Burgundy',        'ក្រហមទទឹម',       '#7a2e2e', false, 2),
    ('navy',       'Navy',            'ខៀវចាស់',         '#1f3a5f', false, 3),
    ('terracotta', 'Terracotta',      'ឥដ្ឋដី',          '#b5542d', false, 4),
    ('plum',       'Plum',            'ស្វាយខ្ចី',        '#5b3a62', false, 5);

ALTER TABLE stores
    ADD COLUMN logo_url    varchar(300),
    ADD COLUMN brand_color varchar(7) NOT NULL DEFAULT '#8d4f06' REFERENCES brand_colors (hex);

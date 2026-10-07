-- =====================================================================
-- Shop settings (admin/settings page, shop-settings.ts)
--   * stores: map coordinates + open/closed switch          [ext]
--   * staff.role: the two roles the settings page manages   (manager | staff)
--   * removing a staff member keeps history: references are set to NULL
-- =====================================================================

ALTER TABLE stores
    ADD COLUMN latitude  double precision CHECK (latitude BETWEEN -90 AND 90),
    ADD COLUMN longitude double precision CHECK (longitude BETWEEN -180 AND 180),
    ADD COLUMN is_open   boolean NOT NULL DEFAULT true,
    ADD CONSTRAINT chk_stores_coordinates CHECK ((latitude IS NULL) = (longitude IS NULL));

-- default location from shop-settings.ts
UPDATE stores SET latitude = 13.640854603672658, longitude = 103.1190348130734
WHERE id = 'ST001';

ALTER TABLE staff ADD CONSTRAINT chk_staff_role CHECK (role IN ('manager', 'staff'));

ALTER TABLE products DROP CONSTRAINT products_created_by_staff_id_fkey,
    ADD CONSTRAINT products_created_by_staff_id_fkey
        FOREIGN KEY (created_by_staff_id) REFERENCES staff (id) ON DELETE SET NULL;

ALTER TABLE kitchen_tickets DROP CONSTRAINT kitchen_tickets_assigned_staff_id_fkey,
    ADD CONSTRAINT kitchen_tickets_assigned_staff_id_fkey
        FOREIGN KEY (assigned_staff_id) REFERENCES staff (id) ON DELETE SET NULL;

ALTER TABLE purchase_orders DROP CONSTRAINT purchase_orders_created_by_staff_id_fkey,
    ADD CONSTRAINT purchase_orders_created_by_staff_id_fkey
        FOREIGN KEY (created_by_staff_id) REFERENCES staff (id) ON DELETE SET NULL;

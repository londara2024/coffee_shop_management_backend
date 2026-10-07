# Aura Coffee Roasters — Backend

Monolithic Spring Boot REST API for the Aura Coffee Roasters Next.js frontend (`../coffeeshop`).
The schema implements [DATABASE_DESIGN.md](DATABASE_DESIGN.md).

| | |
|---|---|
| Java | 17 |
| Framework | Spring Boot 3.3 (Web, Data JPA, Validation, Actuator) |
| Database | PostgreSQL 16 |
| Migrations | Flyway (`src/main/resources/db/migration`) |
| API docs | springdoc-openapi / Swagger UI |
| Tests | JUnit 5 + Testcontainers (real PostgreSQL 16) |

## Run locally

```bash
docker compose up -d          # PostgreSQL 16 on localhost:5432 (db/user/password: coffeeshop)
mvn spring-boot:run           # API on http://localhost:8080
```

- Swagger UI: http://localhost:8080/swagger-ui.html (groups: *All*, *Storefront*, *Back office*)
- OpenAPI JSON: http://localhost:8080/v3/api-docs/0-all
- Health: http://localhost:8080/actuator/health

Flyway runs automatically on startup. Hibernate uses `ddl-auto: validate`, so it never changes the schema; it only checks that the entities match it.

| Env var | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/coffeeshop` |
| `DB_USERNAME` / `DB_PASSWORD` | `coffeeshop` / `coffeeshop` |
| `SERVER_PORT` | `8080` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` |
| `UPLOAD_DIR` | `./uploads` (where uploaded product images are written) |
| `UPLOAD_PUBLIC_URL` | `http://localhost:8080/uploads` (base URL saved in `image_url`; set to the public host in production) |

**A fresh database has no sample data.** It holds only what the system needs to work: one store (`ST001` "Free Shop Coffee", which you rename in the settings page), the 4 menu categories, milk options, default toppings and the brand color presets. You add your menu, staff and so on through the admin API.

**Demo data (optional):** to explore the API with a sample menu, the demo customer, orders, inventory and staff, start with the `demo` profile:

```bash
SPRING_PROFILES_ACTIVE=demo mvn spring-boot:run     # Windows PowerShell: $env:SPRING_PROFILES_ACTIVE="demo"
```

This loads `db/demo/R__demo_data.sql` once. The demo login is `elena.rostova@botanica.com` / `Password123`. Never use the demo profile in production. The tests always run with the demo data.

**IDs:** every record's ID is 5 random characters from `[0-9A-Za-z]`, e.g. `aZ3x9`. The database function `short_id(table)` generates them, checks the table, and retries until it finds an unused ID. That check matters because there are only about 916 million possible IDs. The Java entities use the `@ShortId` annotation, which calls the same function. Categories and milk options keep readable keys (`coffee`, `oat`), and orders keep their display code (`ACR-8946`).

### Tests

```bash
mvn test    # needs Docker running (Testcontainers starts postgres:16-alpine)
```

## Architecture

A single deployable split into feature modules. Every module uses the same layout:

```
<module>/
├── *.java          enums (with their JPA converters), domain events, helpers
├── controller/     REST controllers (@RestController)
├── dto/            request / response records
├── entity/         JPA entities (@Entity)
├── repository/     Spring Data JPA repositories
└── service/        business logic (transactions live here)
```

```
com.aura.coffeeshop
├── common      config (OpenAPI, CORS, JPA auditing), RFC 7807 error handling, Auditable base entity,
│               CodedEnum converter, paging,
│               storage/ (ImageStorage + LocalImageStorage for uploaded images)
├── store       stores, staff                                   service/StoreService
├── setting     admin settings page: shop info, open/closed,    service/ShopSettingsService
│               staff roles                                     (uses store's entities & repositories)
├── catalog     categories, products, sizes, toppings, tags,    service/CatalogService, PricingService
│               milk options, pairings (customer-facing menu)
├── product     admin menu management: create / edit / publish  service/ProductAdminService
│               / delete items, product image upload            (uses catalog's entities & repositories)
├── customer    customers, loyalty, sign-up / sign-in (BCrypt)  service/CustomerService
├── cart        carts, cart items (server-side priced)          service/CartService
├── order       orders, items, notes, payments, promos,         service/OrderService
│               discounts
├── kitchen     KDS tickets (listens to OrderPlacedEvent)       service/KitchenService
└── inventory   suppliers, stock items, snapshots, POs          service/InventoryService, PurchaseOrderService
```

Controllers depend only on their module's `service`. Services use their own `repository` and may call other modules' services, e.g. `OrderService` uses `CartService`, `CustomerService` and `StoreService`.

Key flows:

1. **Pricing is server-side.** Clients send product and configuration (size, temperature, milk, toppings, sweetness, notes), never prices. `PricingService` follows the rules in `product-configurator.tsx`. Identical configurations merge into one cart line through `variant_key`.
2. **Checkout** (`POST /api/v1/orders`) snapshots prices into `order_items`, splits `details_label` into `order_item_notes` (KDS prep bullets), routes each line to a station by category, records a pending `payments` row, and empties the cart. It then publishes `OrderPlacedEvent`.
3. **Kitchen**: `kitchen` handles that event in the same transaction and opens a ticket (`scheduled` for later orders, otherwise `new`). Bumping the ticket keeps `orders.status` in sync: `new → brewing → ready → completed`. Completion awards loyalty stars (1 per whole dollar).
4. **Inventory**: each stock count is a new `inventory_stock_snapshots` row. The newest row is the current stock, and its state is derived from % of par: ≤40 critical, <70 low, <100 adequate, else healthy.
5. **Purchasing**: `draft` (lines editable) → `approved` → `sent`.
6. **Shop settings** (admin settings page): edit the shop name, address, hours and map location, set the shop's branding, open or close the shop, and manage staff with two roles, `manager` (អ្នកគ្រប់គ្រងហាង) and `staff` (បុគ្គលិក).
   - **Branding:** upload or remove a logo (`PUT`/`DELETE /api/v1/admin/settings/logo`, stored in `uploads/logos/`), and pick a brand color from the 6 presets in `brand_colors` (`PATCH /api/v1/admin/settings` with `brandColor`). Any color that isn't a preset gets 400. Both are also returned on the public `GET /api/v1/stores` data, so the storefront header and theme can use them.
   - While the shop is closed, `POST /api/v1/orders` returns 409.
   - A store must keep at least one manager, so demoting or removing the last one returns 409.
   - Removing a staff member keeps the history: their tickets, purchase orders and menu items stay, with the staff link cleared.
7. **Product images** (multipart field `file`):
   - Creating a product: `POST /api/v1/admin/products/images` uploads the image and returns its `url`. Send that `url` as `imageUrl` in `POST /api/v1/admin/products`.
   - Changing a product's photo: `PUT /api/v1/admin/products/{id}/image`.
   - Only JPEG, PNG, WebP and GIF are accepted, detected from the file's bytes rather than its name or `Content-Type`, so a renamed file is rejected with 400. The limit is 5 MB; larger files get 413.
   - Files get random names in `uploads/products/` and are served at `http://localhost:8080/uploads/products/<file>`, cacheable for a year.
   - Replacing an image deletes the old uploaded file once the change is committed; deleting a product deletes its file. Seed images under `/images/...` belong to the frontend and are never touched.
   - The storage is behind the `ImageStorage` interface, so S3 or another cloud store can replace `LocalImageStorage` without changing callers.
   - If the frontend renders these URLs with `next/image`, allow the backend host in `next.config.ts` (`images.remotePatterns`).

## API overview

| Area | Endpoints |
|---|---|
| Catalog | `GET /api/v1/categories`, `GET /api/v1/products?category&q`, `GET /api/v1/products/{slug}`, `GET /api/v1/catalog/options`, `GET /api/v1/pairings` |
| Stores | `GET /api/v1/stores`, `GET /api/v1/stores/{id}`, `GET /api/v1/stores/{id}/staff` |
| Auth / customers | `POST /api/v1/auth/sign-up`, `POST /api/v1/auth/sign-in`, `GET /api/v1/customers/{id}` |
| Cart | `POST /api/v1/carts`, `GET /api/v1/carts/{id}`, `POST /api/v1/carts/{id}/items`, `PATCH|DELETE /api/v1/carts/{id}/items/{itemId}`, `DELETE /api/v1/carts/{id}/items` |
| Checkout & orders | `POST /api/v1/checkout/quote`, `POST /api/v1/orders`, `GET /api/v1/orders?customerId&status`, `GET /api/v1/orders/{code}`, `POST /api/v1/orders/{code}/cancel`, `POST /api/v1/orders/{code}/payment/confirm` |
| Promotions | `GET /api/v1/promo-codes`, `GET /api/v1/promo-codes/{code}`, `GET /api/v1/discounts` |
| Kitchen (KDS) | `GET /api/v1/kitchen/tickets?status&station`, `GET /api/v1/kitchen/summary`, `POST /api/v1/kitchen/tickets/{id}/advance|recall|release`, `PATCH /api/v1/kitchen/tickets/{id}`, `PATCH /api/v1/kitchen/tickets/{id}/items/{itemId}` |
| Admin – menu | `GET|POST /api/v1/admin/products`, `GET|PUT|DELETE /api/v1/admin/products/{id}`, `PATCH /api/v1/admin/products/{id}/live`, `POST /api/v1/admin/products/images`, `PUT /api/v1/admin/products/{id}/image` |
| Admin – inventory | `GET|POST /api/v1/admin/suppliers`, `GET|POST /api/v1/admin/inventory/items`, `POST /api/v1/admin/inventory/items/quick-add`, `GET|POST /api/v1/admin/inventory/items/{id}/stock` |
| Admin – purchasing | `GET|POST /api/v1/admin/purchase-orders`, `GET /api/v1/admin/purchase-orders/{id}`, `POST|DELETE …/{id}/lines`, `POST …/{id}/approve`, `POST …/{id}/send` |
| Admin – settings | `GET|PATCH /api/v1/admin/settings`, `PATCH /api/v1/admin/settings/status`, `PUT|DELETE /api/v1/admin/settings/logo`, `POST /api/v1/admin/settings/staff`, `PATCH|DELETE /api/v1/admin/settings/staff/{staffId}` (all accept optional `?storeId`, default = the main store `ST001`) |

**Base URL.** `/api/v1` is set once in `application.yml` (`app.api.base-path`). `WebConfig` adds it to every controller in the application, so controllers declare only their own path, e.g. `@RequestMapping("/carts")`. CORS and the Swagger groups use the same property. Swagger UI, `/v3/api-docs` and actuator are not prefixed.

**Errors** are handled globally by `GlobalExceptionHandler` (extends Spring's `ResponseEntityExceptionHandler`). Every error, including Spring's own 404/405/415 responses, is returned as RFC 7807 `application/problem+json` with `type, title, status, detail, instance, timestamp`:

| Status | When |
|---|---|
| 400 | Body validation (with a per-field `errors` map), invalid query params (`?size=1000`, `?page=abc`, unknown enum), malformed JSON (no internals leaked) |
| 401 | Bad sign-in |
| 404 | Unknown id/code/slug, or an unknown URL |
| 405 / 415 | Wrong HTTP method / unsupported `Content-Type` |
| 409 | Business rule (e.g. cancelling a brewing order) or data conflict (duplicate, still referenced) |
| 413 | Uploaded image larger than the limit |
| 500 | Any unexpected exception. Logged server-side; the client gets a generic message only |

To raise an error from new code, throw `NotFoundException.of("Order", code)` or `new BusinessRuleException("...")` (409, or `BusinessRuleException.badRequest(...)` for 400).

Enum values in JSON and in the database use the codes from the design doc, e.g. `brewing`, `store`, `aba`, `Bar A`, `Single Origin Beans`, `direct_trade`. Query parameters accept either the code or the constant name.

## Migrations

| Version | Content |
|---|---|
`db/migration/` always runs:

| Version | Content |
|---|---|
| `V1__init_schema.sql` | The `short_id()` ID generator; all tables, constraints, indexes and code sequences from DATABASE_DESIGN.md §3 |
| `V2__reference_data.sql` | Minimum data the system needs: 1 store (`ST001`), 4 categories, 5 milk options, 6 default toppings |
| `V3__shop_settings.sql` | Store coordinates and open/closed flag; staff roles limited to `manager`/`staff`; staff foreign keys switched to `ON DELETE SET NULL` |
| `V4__shop_branding.sql` | `brand_colors` preset palette (6 colors); `stores.logo_url` and `stores.brand_color` (foreign key to the palette) |
| `V5__audit_columns.sql` | `created_at`, `updated_at`, `created_by`, `updated_by` on every table |

`db/demo/` runs only with the `demo` profile and in tests:

| File | Content |
|---|---|
| `R__demo_data.sql` | Sample data from the frontend mocks: a second store, staff, 18 menu items, pairings, the demo customer, promo codes, discounts, suppliers, inventory, a draft PO, 2 open orders with KDS tickets. It is a repeatable migration that loads only once. |

Never edit an applied migration. Add a new `V6__...sql` instead.

### Auditing

Every entity extends `common.persistence.Auditable`, and Spring Data JPA auditing (`JpaAuditingConfig`) fills these columns on every insert and update. Never set them by hand.

| Column | Set when |
|---|---|
| `created_at` / `created_by` | The row is inserted; they never change afterwards |
| `updated_at` / `updated_by` | The row is inserted or updated |

- **Actor:** there is no login yet, so the actor is always `system`. Once Spring Security is added, change `JpaAuditingConfig.auditorAware()` to return the signed-in user; nothing else needs to change.
- **Child-only changes:** when a change touches only child rows (e.g. a cart's lines), call `markModified()` on the parent so its `updated_at` still moves. `Cart.touch()` already does this.
- **Business timestamps are separate** and still set by the domain: `orders.placed_at`, `payments.paid_at`, `kitchen_tickets.started_at`, `inventory_stock_snapshots.recorded_at`.

### Differences from DATABASE_DESIGN.md

The schema follows the design doc table for table. These are the only additions, each marked `[ext]` in V1:

- `customers.password_hash`: needed for sign-up and sign-in.
- `milk_options` table: milk choice changes the drink price, so the server needs it for pricing.
- `order_code_seq` / `po_code_seq` sequences: generate `ACR-xxxx` and `PO-yyyy-nnn` codes.
- CHECK constraints for the enum-like columns, using the values listed in the doc.
- IDs are `varchar(5)` short IDs instead of the doc's `uuid` (see **IDs** above).
- V3 `stores.latitude` / `longitude` / `is_open`: the settings page shows a map link and an open/closed switch.
- V3 `staff.role`: the doc shows free text (`shift_lead`, `barista`); it is now limited to the settings page's two roles, `manager` and `staff`.
- V4 `brand_colors` table plus `stores.logo_url` / `stores.brand_color`: shop branding from the settings page.
- V5 audit columns on every table (see [Auditing](#auditing)).

Archived KDS tickets keep the doc's `ready` status, and the order moves to `completed`. The doc's ticket status list has no `completed` value.

## Not yet implemented

- **No authentication or authorization on endpoints.** Sign-in only checks credentials and returns the profile. `/api/v1/admin/**` and `/api/v1/kitchen/**` must be protected (e.g. Spring Security + JWT and staff roles) before any real deployment.
- Payment gateway integration (ABA/KHQR). Payments are only confirmed manually.
- Real-time KDS push (SSE/WebSocket). The board currently has to poll `GET /api/v1/kitchen/tickets`.

"""Build docs/API_Endpoints.xlsx from the live OpenAPI spec of the coffeeshop backend."""
import json
import sys
from collections import OrderedDict

from openpyxl import Workbook
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.table import Table, TableStyleInfo

SPEC_PATH, OUT_PATH = sys.argv[1], sys.argv[2]
spec = json.load(open(SPEC_PATH, encoding="utf-8"))
SCHEMAS = spec["components"]["schemas"]

FONT = "Arial"
HEADER_FILL = PatternFill("solid", start_color="3B2A20")  # espresso
HEADER_FONT = Font(name=FONT, bold=True, color="FFFFFF", size=10)
BODY_FONT = Font(name=FONT, size=10)
MONO_FONT = Font(name="Consolas", size=9)
THIN = Side(style="thin", color="D9D2C5")
BORDER = Border(left=THIN, right=THIN, top=THIN, bottom=THIN)
WRAP_TOP = Alignment(wrap_text=True, vertical="top")
METHOD_FILL = {
    "GET": "D9EAD3", "POST": "CFE2F3", "PUT": "FCE5CD", "PATCH": "E6D9F2", "DELETE": "F4CCCC",
}

MODULE_ORDER = ["Catalog", "Stores", "Auth", "Customers", "Cart", "Checkout & Orders", "Promotions",
                "Kitchen (KDS)", "Admin - Menu", "Admin - Inventory", "Admin - Purchasing", "Admin - Settings"]

# ----------------------------------------------------------------------------- examples
U = {
    "store": "ST001",
    "customer": "CU001",
    "staff": "SF002",
    "discount": "DC001",
    "po": "PO084",
    "topping": "TP002",
    "order": "nRsSC",
    "item": "Ot5Hd",
    "ticket": "KT942",
    "product": "YaUDe",
    "size": "AxDa9",
    "cart": "Ck7Qp",
    "cartItem": "Lm4Tz",
    "supplier": "Sp2Xw",
    "inventory": "Iv8Rb",
    "generic": "aZ3x9",
}

# Hand-written request bodies: realistic and valid (e.g. no discountId + customDiscountAmount together).
REQUEST_EXAMPLES = {
    "SignUpRequest": {"name": "Sophia Vance", "email": "sophia.vance@botanica.com", "phone": "+1 (555) 234-5678",
                      "password": "Botanica2026", "acceptedTerms": True},
    "SignInRequest": {"email": "elena.rostova@botanica.com", "password": "Password123"},
    "CreateCartRequest": {"customerId": U["customer"]},
    "AddCartItemRequest": {"productSlug": "honey-cinnamon-oat-latte", "qty": 1,
                           "customization": {"sizeId": U["size"], "temperature": "iced", "milkCode": "almond",
                                             "toppingIds": [U["topping"]], "sweetness": "50% Half",
                                             "notes": "Extra hot, 155°F"}},
    "UpdateCartItemRequest": {"qty": 2},
    "QuoteRequest": {"cartId": U["cart"], "tip": 2.00, "promoCode": "WELCOME5", "discountId": U["discount"]},
    "PlaceOrderRequest": {"cartId": U["cart"], "customerId": U["customer"], "storeId": U["store"],
                          "fulfillmentMethod": "store", "fulfillmentTiming": "asap", "paymentMethod": "aba",
                          "tip": 2.00, "promoCode": "WELCOME5", "discountId": U["discount"]},
    "UpdateTicketRequest": {"assignedStaffId": U["staff"], "flagTone": "amber",
                            "flagText": "Guest arrived at curbside bay 3 minutes ago.", "slaMinutes": 8},
    "ItemDoneRequest": {"done": True},
    "ProductUpsertRequest": {"slug": "lavender-honey-latte", "categoryId": "coffee", "name": "Lavender Honey Latte",
                             "nameKm": "ឡាតេទឹកឃ្មុំ ឡាវេនឌឺ", "kicker": "Seasonal Brew", "kickerKm": "កាហ្វេតាមរដូវ",
                             "description": "Espresso, steamed oat milk, wildflower honey and culinary lavender.",
                             "descriptionKm": None, "basePrice": 6.40, "imageUrl": "/images/latte-cup.jpg",
                             "imageTag": "Barista Pick", "badge": "ក្ដៅ", "extra": None, "customizable": True,
                             "live": True, "createdByStaffId": U["staff"],
                             "sizes": [{"sizeLabel": "តូច", "price": 5.90}, {"sizeLabel": "ធម្មតា", "price": 6.40},
                                       {"sizeLabel": "ធំ", "price": 7.00}],
                             "toppings": [{"label": "Lavender Sugar Rim", "labelKm": None, "priceDelta": 0.50,
                                           "locked": False}],
                             "tags": [{"label": "Floral", "labelKm": "ក្លិនផ្កា", "tone": "forest"}]},
    "LiveRequest": {"live": False},
    "UpdateShopRequest": {"shopName": "Free Shop Coffee", "address": "742 Pine Hill Court, Suite 100",
                          "hoursLabel": "Mon – Sun • 6:30 AM – 7:00 PM", "latitude": 13.640854603672658,
                          "longitude": 103.1190348130734, "brandColor": "#3a5a40"},
    "ShopStatusRequest": {"open": False},
    "CreateStaffRequest": {"name": "Sophia Vance", "role": "staff"},
    "UpdateStaffRequest": {"role": "manager"},
    "SupplierRequest": {"name": "Highland Coop Direct", "note": "Direct trade, signed", "relationshipType": "direct_trade"},
    "CreateInventoryItemRequest": {"storeId": U["store"], "supplierId": U["supplier"], "name": "Guatemala Huehuetenango",
                                   "meta": "Washed · Lot #112", "group": "Single Origin Beans", "unitLabel": "kg",
                                   "parLevel": 25, "imageUrl": "/images/green-beans.jpg", "initialOnHandQty": 18},
    "QuickAddMaterialsRequest": {"storeId": U["store"], "group": "Packaging",
                                 "names": ["Bamboo Stir Sticks", "Kraft Pastry Bags"], "supplierName": "EcoWare Global",
                                 "unitLabel": "unit", "parLevel": 500},
    "RecordStockRequest": {"onHandQty": 14, "state": None},
    "PoLineRequest": {"inventoryItemId": U["inventory"], "description": "Minor Figures Barista Oat (12cs)",
                      "supplierId": U["supplier"], "qty": 12, "price": 336.00},
    "CreatePurchaseOrderRequest": {"storeId": U["store"], "fundingAccount": "Flagship Roastery Operating Fund (#***849)",
                                   "createdByStaffId": U["staff"],
                                   "lines": [{"inventoryItemId": U["inventory"], "qty": 12, "price": 336.00},
                                             {"description": "House Espresso Blend Green (20kg)", "qty": 1,
                                              "price": 304.00}]},
}

# Field-name driven sample values for synthesized responses.
FIELD_SAMPLES = {
    "id": "coffee",  # only reached for non-UUID ids (categories)
    "orderCode": "ACR-8946", "poCode": "PO-2026-085", "slug": "honey-cinnamon-oat-latte",
    "productSlug": "honey-cinnamon-oat-latte", "categoryId": "coffee", "name": "Honey Cinnamon Oat Latte",
    "nameKm": "ឡាតេទឹកឃ្មុំ ស៊ីណាមុន និងទឹកដោះអូ៊ត", "kicker": "House Signature", "kickerKm": "ម្ហូបពិសេសរបស់ហាង",
    "description": "Rich organic espresso blended with velvety steamed oat milk and wildflower honey.",
    "descriptionKm": "អេស្ប្រេសូសរីរាង្គខាប់ ចម្រុះជាមួយទឹកដោះអូ៊ត។", "imageUrl": "/images/honey-cinnamon-oat-latte.jpg",
    "imageTag": "Barista Pick", "badge": "ត្រជាក់/ក្ដៅ", "extra": "6 oz", "label": "Coffee", "labelKm": "កាហ្វេ",
    "eyebrow": "Single Origin & Specialty Espresso", "title": "Coffee & Extraction Lab", "titleKm": "កាហ្វេ",
    "note": "Served until 3:00 PM daily", "noteKm": "បម្រើរហូតដល់ម៉ោង ៣ រសៀល", "sizeLabel": "ធម្មតា",
    "ctaLabel": "Add Pairing", "ctaLabelKm": "បន្ថែមម្ហូបផ្គូផ្គង", "code": "WELCOME5", "promoCode": "WELCOME5",
    "address": "742 Pine Hill Court, Suite 100", "hoursLabel": "Mon – Sun • 6:30 AM – 7:00 PM",
    "storeName": "Free Shop Coffee", "role": "shift_lead", "email": "elena.rostova@botanica.com",
    "phone": "(555) 234-8901", "avatarUrl": "/images/customer-portrait.png", "tier": "Gold Member",
    "customerName": "Elena Rostova", "guest": "Elena Rostova", "where": "Pick-Up Bar A",
    "channel": "ទទួលនៅកន្លែងបញ្ជរ", "pickupLabel": "Pick-Up Bar A", "methodLabel": "ABA ឬស្កេន QR",
    "detailsLabel": "ធំ · Iced · Almond Milk · Extra Wildflower Honey Drizzle", "tag": "House",
    "variantKey": "2-iced-almond-3b1f0c9e-6a1d", "text": "Guest arrived at curbside bay 3 minutes ago.",
    "assignedStaffName": "Mateo Silva", "createdBy": "Elena Vasquez", "supplierName": "Minor Figures Dist.",
    "meta": "Huila · Anaerobic Washed · Lot #248", "unitLabel": "kg",
    "fundingAccount": "Flagship Roastery Operating Fund (#***849)", "notes": "Minor Figures Barista Oat Milk",
    "shopName": "Free Shop Coffee", "url": "http://localhost:8080/uploads/products/3f2c9a1e-7b4d-4c1a-9e2f-5a6b7c8d9e0f.jpg",
    "contentType": "image/jpeg", "brandColor": "#8d4f06", "hex": "#8d4f06",
    "logoUrl": "http://localhost:8080/uploads/logos/7c1d2e3f-4a5b-4c6d-8e9f-0a1b2c3d4e5f.png", "roleLabel": "Store Manager", "roleLabelKm": "អ្នកគ្រប់គ្រងហាង",
    "mapsUrl": "https://www.google.com/maps/search/?api=1&query=13.640854603672658,103.1190348130734",
}
NUMBER_SAMPLES = {
    "price": 6.25, "unitPrice": 7.95, "lineTotal": 15.90, "subtotal": 22.40, "tax": 0.00, "tip": 2.00,
    "promoDiscountAmount": 0.75, "discountAmount": 1.00, "total": 22.65, "amount": 22.65, "priceDelta": 0.50,
    "basePrice": 6.25, "onHandQty": 14, "parLevel": 25, "qty": 2, "itemCount": 3, "stars": 178,
    "starsGoal": 200, "starsToNextReward": 22, "sortOrder": 1, "productCount": 8, "slaMinutes": 8,
    "elapsedSeconds": 222, "pctOfPar": 56, "page": 0, "size": 20, "totalElements": 1, "totalPages": 1,
    "latitude": 13.640854603672658, "longitude": 103.1190348130734,
    "all": 3, "queued": 1, "brewing": 1, "ready": 1, "completedToday": 42,
}
NULL_IN_TYPICAL_RESPONSE = {"scheduledFor", "readyAt", "approvedAt", "paidAt"}
UUID_BY_FIELD = {
    "customerId": U["customer"], "storeId": U["store"], "staffId": U["staff"], "productId": U["product"], "discountId": U["discount"],
    "assignedStaffId": U["staff"], "supplierId": U["supplier"], "inventoryItemId": U["inventory"],
}
UUID_BY_SCHEMA = {
    "OrderResponse": U["order"], "OrderSummaryResponse": U["order"], "CartResponse": U["cart"],
    "CartItemResponse": U["cartItem"], "TicketResponse": U["ticket"], "StoreResponse": U["store"],
    "StaffResponse": U["staff"], "CustomerResponse": U["customer"], "ProductSummaryResponse": U["product"],
    "ProductDetailResponse": U["product"], "SizeResponse": U["size"], "ToppingResponse": U["topping"],
    "DiscountResponse": U["discount"], "PurchaseOrderResponse": U["po"], "SupplierResponse": U["supplier"],
    "InventoryItemResponse": U["inventory"], "OrderItemResponse": U["item"],
}


def ref_name(schema):
    return schema["$ref"].rsplit("/", 1)[-1] if schema and "$ref" in schema else None


def resolve(schema):
    name = ref_name(schema)
    return (SCHEMAS[name], name) if name else (schema, None)


def sample(schema, field=None, owner=None, depth=0):
    schema, name = resolve(schema)
    owner = name or owner
    if depth > 6:
        return None
    t = schema.get("type")
    if "enum" in schema:
        values = schema["enum"]
        for preferred in ("new", "draft"):
            if preferred in values:
                return preferred
        return values[0]
    if t == "object" or "properties" in schema:
        # Fields that are null in the typical case are omitted, exactly as the API does (Jackson non_null).
        return OrderedDict((k, sample(v, k, owner, depth + 1)) for k, v in schema.get("properties", {}).items()
                           if k not in NULL_IN_TYPICAL_RESPONSE)
    if t == "array":
        return [sample(schema.get("items", {}), field, owner, depth + 1)]
    if "example" in schema:
        return schema["example"]
    fmt = schema.get("format")
    if fmt == "uuid":
        if field == "id":
            return UUID_BY_SCHEMA.get(owner, U["generic"])
        return UUID_BY_FIELD.get(field, U["generic"])
    if fmt == "date-time":
        return "2026-10-07T08:15:00Z"
    if t == "boolean":
        return field in {"customizable", "live", "locked", "acceptedTerms", "open"}
    if t in ("number", "integer"):
        if owner == "ImageUploadResponse" and field == "size":
            return 245760  # bytes
        return NUMBER_SAMPLES.get(field, 1)
    if t == "string":
        if field == "notes":
            return FIELD_SAMPLES["notes"]
        # ids are 5-character strings ("aZ3x9"); categories use readable keys ("coffee")
        if field == "id" and owner != "CategoryResponse":
            return UUID_BY_SCHEMA.get(owner, U["generic"])
        if field and (field.endswith("Id") or field.endswith("Ids")) and field != "categoryId":
            return UUID_BY_FIELD.get(field, U["generic"])
        return FIELD_SAMPLES.get(field, "string")
    return None


def to_json(value):
    return json.dumps(value, ensure_ascii=False, indent=2)


# ----------------------------------------------------------------------------- field flattening
def describe_type(schema):
    schema, name = resolve(schema)
    t = schema.get("type", "object")
    if t == "array":
        inner, inner_name = resolve(schema.get("items", {}))
        return f"array<{inner_name or describe_type(schema.get('items', {}))}>"
    if name:
        return name
    fmt = schema.get("format")
    return f"{t} ({fmt})" if fmt else t


def constraints(schema):
    parts = []
    for key, label in (("minLength", "min length"), ("maxLength", "max length"), ("minimum", "min"),
                       ("maximum", "max"), ("minItems", "min items"), ("maxItems", "max items"),
                       ("pattern", "pattern")):
        if key in schema:
            parts.append(f"{label} {schema[key]}")
    if schema.get("exclusiveMinimum") is True:
        parts.append("exclusive min")
    if schema.get("format") == "email":
        parts.append("valid email")
    return "; ".join(parts)


def flatten(schema_name, prefix="", required_parent=True, seen=None, rows=None):
    """Rows for every field of a schema, recursing into nested objects and arrays with dotted paths."""
    rows = [] if rows is None else rows
    seen = set() if seen is None else seen
    if schema_name in seen:
        return rows
    seen = seen | {schema_name}
    schema = SCHEMAS[schema_name]
    required = set(schema.get("required", []))
    for field, prop in schema.get("properties", {}).items():
        path = f"{prefix}{field}"
        resolved, nested = resolve(prop)
        items_nested = None
        if resolved.get("type") == "array":
            _, items_nested = resolve(resolved.get("items", {}))
            item_schema, _ = resolve(resolved.get("items", {}))
        else:
            item_schema = None
        enum = resolved.get("enum") or (item_schema or {}).get("enum")
        rows.append({
            "path": path,
            "type": describe_type(prop),
            "required": "Yes" if field in required else "No",
            "constraints": "; ".join(c for c in [constraints(resolved), constraints(item_schema or {})] if c),
            "allowed": " | ".join(str(e) for e in enum) if enum else "",
            "default": str(resolved.get("default", "")) if "default" in resolved else "",
            "example": "" if resolved.get("example") is None else (
                json.dumps(resolved["example"], ensure_ascii=False) if not isinstance(resolved["example"], str)
                else resolved["example"]),
            "description": resolved.get("description", "") or prop.get("description", ""),
        })
        if nested:
            flatten(nested, f"{path}.", field in required, seen, rows)
        elif items_nested:
            flatten(items_nested, f"{path}[].", field in required, seen, rows)
    return rows


# ----------------------------------------------------------------------------- endpoints
# Error statuses that come from business rules in the services (validation 400 / not-found 404 are derived).
EXTRA_ERRORS = {
    ("POST", "/api/v1/auth/sign-up"): {409: "Email or phone already registered"},
    ("POST", "/api/v1/auth/sign-in"): {401: "Invalid email or password"},
    ("POST", "/api/v1/carts/{cartId}/items"): {409: "Product withdrawn from the menu",
                                               400: "Unknown size / milk / topping, or line qty > 50"},
    ("POST", "/api/v1/checkout/quote"): {400: "Invalid promo code, or discountId + customDiscountAmount together"},
    ("POST", "/api/v1/orders"): {400: "Empty cart, scheduledFor missing/past, invalid promo code",
                                 409: "Shop is closed, or a cart item was withdrawn from the menu"},
    ("PATCH", "/api/v1/admin/settings"): {400: "latitude/longitude not sent together, or brandColor not one of the presets"},
    ("PUT", "/api/v1/admin/settings/logo"): {400: "Empty file, missing 'file' part, or not a JPEG/PNG/WebP/GIF",
                                               413: "Image larger than 5 MB"},
    ("POST", "/api/v1/admin/products/images"): {400: "Empty file, missing 'file' part, or not a JPEG/PNG/WebP/GIF",
                                                413: "Image larger than 5 MB"},
    ("PUT", "/api/v1/admin/products/{productId}/image"): {400: "Empty file, missing 'file' part, or not a JPEG/PNG/WebP/GIF",
                                                          413: "Image larger than 5 MB"},
    ("PATCH", "/api/v1/admin/settings/staff/{staffId}"): {409: "Demoting the store's last manager"},
    ("DELETE", "/api/v1/admin/settings/staff/{staffId}"): {409: "Removing the store's last manager"},
    ("POST", "/api/v1/orders/{orderCode}/cancel"): {409: "Order is no longer 'new'"},
    ("POST", "/api/v1/orders/{orderCode}/payment/confirm"): {409: "Already paid, or order cancelled"},
    ("POST", "/api/v1/kitchen/tickets/{ticketId}/advance"): {409: "Ticket is scheduled, or order already closed"},
    ("POST", "/api/v1/kitchen/tickets/{ticketId}/recall"): {409: "Ticket is not 'brewing'"},
    ("POST", "/api/v1/kitchen/tickets/{ticketId}/release"): {409: "Ticket is not 'scheduled'"},
    ("PATCH", "/api/v1/kitchen/tickets/{ticketId}"): {409: "Order already closed"},
    ("PATCH", "/api/v1/kitchen/tickets/{ticketId}/items/{itemId}"): {409: "Order already closed"},
    ("POST", "/api/v1/admin/products"): {409: "Slug already used"},
    ("PUT", "/api/v1/admin/products/{productId}"): {409: "Slug already used"},
    ("DELETE", "/api/v1/admin/products/{productId}"): {409: "Product is referenced by orders or carts — withdraw it instead"},
    ("POST", "/api/v1/admin/suppliers"): {409: "Supplier name already exists"},
    ("POST", "/api/v1/admin/inventory/items"): {409: "Item name already exists in the store"},
    ("POST", "/api/v1/admin/purchase-orders/{poId}/lines"): {409: "Purchase order is not a draft"},
    ("DELETE", "/api/v1/admin/purchase-orders/{poId}/lines/{lineId}"): {409: "Purchase order is not a draft"},
    ("POST", "/api/v1/admin/purchase-orders/{poId}/approve"): {409: "Not a draft, or has no lines"},
    ("POST", "/api/v1/admin/purchase-orders/{poId}/send"): {409: "Purchase order is not approved"},
}

PATH_SAMPLES = {"orderCode": "ACR-8942", "slug": "honey-cinnamon-oat-latte", "cartId": U["cart"],
                "itemId": U["cartItem"], "staffId": U["staff"], "ticketId": U["ticket"], "customerId": U["customer"],
                "storeId": U["store"], "staffId": U["staff"], "productId": U["product"], "poId": U["po"], "lineId": U["generic"],
                "code": "WELCOME5"}

endpoints = []
request_schemas = OrderedDict()
response_schemas = OrderedDict()

for path, ops in spec["paths"].items():
    for method, op in ops.items():
        method = method.upper()
        tag = op.get("tags", ["Other"])[0]
        params = op.get("parameters", [])
        path_params = [p for p in params if p["in"] == "path"]
        query_params = [p for p in params if p["in"] == "query"]

        def fmt_param(p):
            s = p.get("schema", {})
            bits = [describe_type(s)]
            if p.get("required"):
                bits.append("required")
            if "default" in s:
                bits.append(f"default {s['default']}")
            c = constraints(s)
            if c:
                bits.append(c)
            if s.get("enum"):
                bits.append("values: " + " | ".join(s["enum"]))
            desc = p.get("description")
            return f"{p['name']} ({', '.join(bits)})" + (f" — {desc}" if desc and not s.get('enum') else "")

        body_schema = None
        multipart_label = None
        body = op.get("requestBody")
        if body:
            media_type, media = next(iter(body["content"].items()))
            body_schema = ref_name(media["schema"])
            if body_schema:
                request_schemas.setdefault(body_schema, []).append(f"{method} {path}")
            else:
                # Inline schema, e.g. multipart/form-data file uploads
                parts = media["schema"].get("properties", {})
                multipart_label = media_type + "\n" + "\n".join(
                    f"{name}: {'file (binary)' if p.get('format') == 'binary' else describe_type(p)}"
                    + (" — required" if name in media["schema"].get("required", []) else "")
                    for name, p in parts.items())

        status, resp = next(iter(op["responses"].items()))
        resp_schema_obj = next(iter(resp.get("content", {}).values()), {}).get("schema") if resp.get("content") else None
        if resp_schema_obj and resp_schema_obj.get("type") == "array":
            item = ref_name(resp_schema_obj["items"])
            resp_label = f"array<{item}>"
            resp_root = item
        else:
            resp_root = ref_name(resp_schema_obj) if resp_schema_obj else None
            resp_label = resp_root or "(no body)"
        if resp_root:
            response_schemas.setdefault(resp_root, []).append(f"{method} {path}")

        errors = {}
        if body or query_params:
            errors[400] = "Validation failed / malformed value"
        if path_params:
            errors[404] = "Resource not found"
        for code, text in EXTRA_ERRORS.get((method, path), {}).items():
            errors[code] = text if code not in errors or code != 400 else f"{errors[code]}; {text}"
        error_text = "\n".join(f"{c} — {t}" for c, t in sorted(errors.items()))

        example_path = path
        for name, value in PATH_SAMPLES.items():
            example_path = example_path.replace("{" + name + "}", value)
        example_query = "&".join(f"{p['name']}={p['schema'].get('default', '')}" for p in query_params
                                 if "default" in p.get("schema", {}))
        if example_query:
            example_path += "?" + example_query

        req_example = ""
        if body_schema:
            req_example = to_json(REQUEST_EXAMPLES.get(body_schema) or sample({"$ref": f"#/components/schemas/{body_schema}"}))
        elif multipart_label:
            req_example = ("Content-Type: multipart/form-data\n"
                           "file = <latte.jpg>   (JPEG, PNG, WebP or GIF, max 5 MB)\n\n"
                           f"curl -X {method} -F \"file=@latte.jpg\" \\\n  http://localhost:8080{example_path}")
        resp_example = to_json(sample(resp_schema_obj)) if resp_schema_obj else "(empty — 204 No Content)"

        endpoints.append({
            "module": tag, "method": method, "path": path, "summary": op.get("summary", ""),
            "description": op.get("description", ""),
            "path_params": "\n".join(fmt_param(p) for p in path_params),
            "query_params": "\n".join(fmt_param(p) for p in query_params),
            "request": body_schema or multipart_label or "—", "status": f"{status} {resp.get('description', '')}".strip(),
            "response": resp_label, "errors": error_text, "example_call": f"{method} {example_path}",
            "req_example": req_example, "resp_example": resp_example,
        })

method_rank = {m: i for i, m in enumerate(["GET", "POST", "PUT", "PATCH", "DELETE"])}
endpoints.sort(key=lambda e: (MODULE_ORDER.index(e["module"]) if e["module"] in MODULE_ORDER else 99,
                              e["path"], method_rank.get(e["method"], 9)))

# ----------------------------------------------------------------------------- workbook
wb = Workbook()


def header(ws, row, headers, widths):
    for col, (title, width) in enumerate(zip(headers, widths), start=1):
        c = ws.cell(row=row, column=col, value=title)
        c.font, c.fill, c.border = HEADER_FONT, HEADER_FILL, BORDER
        c.alignment = Alignment(wrap_text=True, vertical="center")
        ws.column_dimensions[get_column_letter(col)].width = width
    ws.row_dimensions[row].height = 30


def body_cell(ws, row, col, value, mono=False, bold=False):
    c = ws.cell(row=row, column=col, value=value)
    c.font = MONO_FONT if mono else Font(name=FONT, size=10, bold=bold)
    c.alignment, c.border = WRAP_TOP, BORDER
    return c


def add_table(ws, name, last_col, last_row, first_row=1):
    ref = f"A{first_row}:{get_column_letter(last_col)}{last_row}"
    table = Table(displayName=name, ref=ref)
    table.tableStyleInfo = TableStyleInfo(name="TableStyleLight1", showRowStripes=False)
    ws.add_table(table)


# --- Endpoints sheet
ws_e = wb.active
ws_e.title = "Endpoints"
cols = ["No", "Module", "Method", "Path", "Summary", "Description", "Path Params", "Query Params",
        "Request Body", "Success Status", "Response Body", "Error Responses", "Example Call",
        "Request Example (JSON)", "Response Example (JSON)"]
widths = [5, 16, 9, 42, 30, 40, 30, 40, 24, 14, 30, 38, 46, 55, 65]
header(ws_e, 1, cols, widths)
for i, e in enumerate(endpoints, start=1):
    r = i + 1
    values = [i, e["module"], e["method"], e["path"], e["summary"], e["description"], e["path_params"],
              e["query_params"], e["request"], e["status"], e["response"], e["errors"], e["example_call"],
              e["req_example"], e["resp_example"]]
    for col, v in enumerate(values, start=1):
        body_cell(ws_e, r, col, v, mono=col in (4, 13, 14, 15), bold=col == 3)
    ws_e.cell(row=r, column=3).fill = PatternFill("solid", start_color=METHOD_FILL[e["method"]])
    ws_e.cell(row=r, column=3).alignment = Alignment(horizontal="center", vertical="top")
    lines = max(v.count("\n") + 1 for v in [e["req_example"], e["resp_example"], e["errors"],
                                             e["query_params"], e["path_params"]] if v)
    ws_e.row_dimensions[r].height = min(409, max(30, lines * 12.5))
ws_e.freeze_panes = "E2"
add_table(ws_e, "Endpoints", len(cols), len(endpoints) + 1)


def field_sheet(title, table_name, schema_map, intro):
    ws = wb.create_sheet(title)
    ws.cell(row=1, column=1, value=intro).font = Font(name=FONT, italic=True, size=10, color="5C4A3D")
    ws.merge_cells(start_row=1, start_column=1, end_row=1, end_column=10)
    hdr = ["Schema", "Field (dotted path)", "Type", "Required", "Constraints", "Allowed Values", "Default",
           "Example", "Description", "Used By"]
    header(ws, 2, hdr, [26, 34, 26, 10, 26, 34, 10, 26, 44, 46])
    r = 3
    for schema_name in sorted(schema_map):
        used_by = "\n".join(schema_map[schema_name])
        for row in flatten(schema_name):
            values = [schema_name, row["path"], row["type"], row["required"], row["constraints"], row["allowed"],
                      row["default"], row["example"], row["description"], used_by]
            for col, v in enumerate(values, start=1):
                body_cell(ws, r, col, v, mono=col == 2)
            if row["required"] == "Yes":
                ws.cell(row=r, column=4).font = Font(name=FONT, size=10, bold=True, color="9C2B1E")
            r += 1
    ws.freeze_panes = "C3"
    add_table(ws, table_name, len(hdr), r - 1, first_row=2)
    return ws


field_sheet("Request Fields", "RequestFields", request_schemas,
            "Every field a client can send in a request body. Nested objects/arrays use dotted paths "
            "(e.g. customization.toppingIds, lines[].price). Required = must be present and non-null.")
field_sheet("Response Fields", "ResponseFields", response_schemas,
            "Every field returned in a response body. Null-valued fields are omitted from the JSON "
            "(Jackson non_null), so treat any field as optional when absent.")

# --- Errors sheet
ws_err = wb.create_sheet("Error Format")
ws_err["A1"] = "All errors use RFC 7807 Problem Details (Content-Type: application/problem+json)."
ws_err["A1"].font = Font(name=FONT, bold=True, size=11)
header(ws_err, 3, ["Field", "Type", "Description"], [16, 14, 80])
problem_fields = [
    ("type", "string", "Problem type URI (defaults to about:blank)"),
    ("title", "string", "Short summary: Not Found, Validation Failed, Business Rule Violation, Data Conflict, Bad Request, Malformed Request Body"),
    ("status", "integer", "HTTP status code"),
    ("detail", "string", "Human-readable explanation of this occurrence"),
    ("instance", "string", "Request path that produced the error"),
    ("errors", "object", "Validation only (400): map of field name → message, e.g. {\"password\": \"Use at least 8 characters.\"}"),
]
for i, row in enumerate(problem_fields, start=4):
    for col, v in enumerate(row, start=1):
        body_cell(ws_err, i, col, v, mono=col == 1)
header(ws_err, 11, ["Status", "Title", "When"], [16, 14, 80])
statuses = [
    (400, "Validation Failed / Bad Request", "Bean-validation failure, bad enum or UUID value, malformed JSON, or a rule such as an invalid promo code"),
    (401, "Business Rule Violation", "Sign-in with a wrong email or password"),
    (404, "Not Found", "Unknown id / code / slug (including withdrawn products on the customer menu)"),
    (409, "Business Rule Violation / Data Conflict", "Invalid state transition (e.g. cancel a brewing order), duplicate unique value, or deleting a referenced row"),
]
for i, row in enumerate(statuses, start=12):
    for col, v in enumerate(row, start=1):
        body_cell(ws_err, i, col, v)
ws_err.column_dimensions["B"].width = 34
ws_err["A18"] = "Example (400):"
ws_err["A18"].font = Font(name=FONT, bold=True, size=10)
example_problem = {"type": "about:blank", "title": "Validation Failed", "status": 400,
                   "detail": "One or more fields are invalid", "instance": "/api/v1/auth/sign-up",
                   "errors": {"password": "Include both letters and numbers.",
                              "acceptedTerms": "Please accept the Terms of Service and Privacy Policy."}}
ws_err["A19"] = to_json(example_problem)
ws_err["A19"].font, ws_err["A19"].alignment = MONO_FONT, WRAP_TOP
ws_err.merge_cells("A19:C19")
ws_err.row_dimensions[19].height = 140

# --- Overview sheet (first tab)
ws_o = wb.create_sheet("Overview", 0)
ws_o.column_dimensions["A"].width = 30
ws_o.column_dimensions["B"].width = 14
ws_o.column_dimensions["C"].width = 70
ws_o["A1"] = "Aura Coffee Roasters API — Endpoint Reference"
ws_o["A1"].font = Font(name=FONT, bold=True, size=16, color="3B2A20")
ws_o["A2"] = f"Generated from the live OpenAPI spec ({spec['info'].get('version', 'v1')}). Re-generate after API changes."
ws_o["A2"].font = Font(name=FONT, italic=True, size=10, color="5C4A3D")

info = [
    ("Base URL (local)", "http://localhost:8080"),
    ("Swagger UI", "http://localhost:8080/swagger-ui.html"),
    ("OpenAPI JSON", "http://localhost:8080/v3/api-docs/0-all"),
    ("Content type", "application/json (UTF-8)"),
    ("Money", "USD, 2 decimals (e.g. 22.65)"),
    ("Timestamps", "ISO-8601 UTC (e.g. 2026-10-07T08:15:00Z)"),
    ("Enum values", "Lower-case codes from DATABASE_DESIGN.md (brewing, store, aba, Bar A, Single Origin Beans, ...)"),
    ("Paging", "page (0-based) & size query params; responses: content, page, size, totalElements, totalPages"),
    ("Authentication", "None yet — /api/v1/admin/** and /api/v1/kitchen/** must be secured before production"),
    ("Demo login", "elena.rostova@botanica.com / Password123"),
]
r = 4
for k, v in info:
    ws_o.cell(row=r, column=1, value=k).font = Font(name=FONT, bold=True, size=10)
    c = ws_o.cell(row=r, column=3, value=v)
    c.font, c.alignment = BODY_FONT, Alignment(wrap_text=True)
    r += 1

r += 1
header_row = r
for col, (title, width) in enumerate(zip(["Module", "Endpoints", "Scope"], [30, 14, 70]), start=1):
    c = ws_o.cell(row=r, column=col, value=title)
    c.font, c.fill, c.border = HEADER_FONT, HEADER_FILL, BORDER
scope = {
    "Catalog": "Customer menu: categories, products, configurator options, pairings",
    "Stores": "Store locations and staff",
    "Auth": "Customer sign-up / sign-in",
    "Customers": "Profile and loyalty",
    "Cart": "Cart lines priced by the server",
    "Checkout & Orders": "Quote, place, track, cancel, confirm payment",
    "Promotions": "Promo codes and staff discounts",
    "Kitchen (KDS)": "Kitchen display tickets and prep workflow (staff)",
    "Admin - Menu": "Create / edit / publish / delete menu items (staff)",
    "Admin - Inventory": "Suppliers, stock items, stock counts (staff)",
    "Admin - Purchasing": "Purchase orders: draft → approved → sent (staff)",
    "Admin - Settings": "Shop name, address, map location, open/closed switch, staff roles (staff)",
}
first_mod = r + 1
for module in MODULE_ORDER:
    r += 1
    body_cell(ws_o, r, 1, module)
    # Live count from the Endpoints sheet so the summary stays in sync if rows are edited.
    body_cell(ws_o, r, 2, f'=COUNTIF(Endpoints!$B:$B,A{r})')
    body_cell(ws_o, r, 3, scope[module])
last_mod = r
r += 1
body_cell(ws_o, r, 1, "Total", bold=True)
body_cell(ws_o, r, 2, f"=SUM(B{first_mod}:B{last_mod})", bold=True)
body_cell(ws_o, r, 3, "")

r += 2
ws_o.cell(row=r, column=1, value="Sheets").font = Font(name=FONT, bold=True, size=11)
for name, desc in [("Endpoints", "One row per endpoint: params, request/response schema, errors, JSON examples"),
                   ("Request Fields", "Every request field with type, required flag, constraints and allowed values"),
                   ("Response Fields", "Every response field with type and allowed values"),
                   ("Error Format", "Problem Details error body and status code meanings")]:
    r += 1
    ws_o.cell(row=r, column=1, value=name).font = Font(name=FONT, size=10, bold=True)
    ws_o.cell(row=r, column=3, value=desc).font = BODY_FONT

r += 2
ws_o.cell(row=r, column=1, value="Method colours").font = Font(name=FONT, bold=True, size=11)
for m, fill in METHOD_FILL.items():
    r += 1
    c = ws_o.cell(row=r, column=1, value=m)
    c.font, c.fill = Font(name=FONT, bold=True, size=10), PatternFill("solid", start_color=fill)

for ws in wb.worksheets:
    ws.sheet_view.zoomScale = 90
wb.calculation.fullCalcOnLoad = True
wb.save(OUT_PATH)
print(f"{len(endpoints)} endpoints, {len(request_schemas)} request schemas, {len(response_schemas)} response schemas")



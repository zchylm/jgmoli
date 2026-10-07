INSERT INTO products (id, category_id, slug, brand, name, subtype, short_description, image_key, sort_order) VALUES
    ('20000000-0000-0000-0000-000000000024', '10000000-0000-0000-0000-000000000004', 'moli-arena-core', 'JG MOLI', 'JG MOLI Arena Core', 'Interactive arenas', 'A complete two-player home projection system with 60 permanently licensed offline games.', 'moli-arena-core', 8),
    ('20000000-0000-0000-0000-000000000025', '10000000-0000-0000-0000-000000000004', 'moli-arena-signature', 'JG MOLI', 'JG MOLI Arena Signature', 'Interactive arenas', 'A four-player home projection system with blackout works, content updates and AI Studio.', 'moli-arena-signature', 9),
    ('20000000-0000-0000-0000-000000000026', '10000000-0000-0000-0000-000000000004', 'moli-arena-venue', 'JG MOLI', 'JG MOLI Arena Venue', 'Interactive arenas', 'A six-player commercial projection system with timer controls and an operations dashboard.', 'moli-arena-venue', 10);

INSERT INTO product_variants (id, product_id, sku, price_cents) VALUES
    ('30000000-0000-0000-0000-000000000024', '20000000-0000-0000-0000-000000000024', 'JGM-ARENA-CORE', 999900),
    ('30000000-0000-0000-0000-000000000025', '20000000-0000-0000-0000-000000000025', 'JGM-ARENA-SIGNATURE', 1399900),
    ('30000000-0000-0000-0000-000000000026', '20000000-0000-0000-0000-000000000026', 'JGM-ARENA-VENUE', 1799900);

INSERT INTO inventory_items (id, catalog_variant_id, sku, brand, name, item_type) VALUES
    ('50000000-0000-0000-0000-000000000024', '30000000-0000-0000-0000-000000000024', 'JGM-ARENA-CORE', 'JG MOLI', 'JG MOLI Arena Core', 'Interactive arenas'),
    ('50000000-0000-0000-0000-000000000025', '30000000-0000-0000-0000-000000000025', 'JGM-ARENA-SIGNATURE', 'JG MOLI', 'JG MOLI Arena Signature', 'Interactive arenas'),
    ('50000000-0000-0000-0000-000000000026', '30000000-0000-0000-0000-000000000026', 'JGM-ARENA-VENUE', 'JG MOLI', 'JG MOLI Arena Venue', 'Interactive arenas');

INSERT INTO inventory_balances (item_id, location_id, on_hand, reserved, reorder_level) VALUES
    ('50000000-0000-0000-0000-000000000024', '40000000-0000-0000-0000-000000000001', 0, 0, 0),
    ('50000000-0000-0000-0000-000000000025', '40000000-0000-0000-0000-000000000001', 0, 0, 0),
    ('50000000-0000-0000-0000-000000000026', '40000000-0000-0000-0000-000000000001', 0, 0, 0);

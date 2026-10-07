INSERT INTO products (id, category_id, slug, brand, name, subtype, short_description, image_key, sort_order) VALUES
    ('20000000-0000-0000-0000-000000000021', '10000000-0000-0000-0000-000000000004', 'moli-racer-core', 'JG MOLI', 'MOLI Racer Core', 'Complete racers', 'A complete entry motion-racing machine with direct-drive control and a single display.', 'moli-racer-core', 5),
    ('20000000-0000-0000-0000-000000000022', '10000000-0000-0000-0000-000000000004', 'moli-racer-signature', 'JG MOLI', 'MOLI Racer Signature', 'Complete racers', 'The core 4DOF racing system with a 49-inch QLED display and calibrated controls.', 'moli-racer-signature', 6),
    ('20000000-0000-0000-0000-000000000023', '10000000-0000-0000-0000-000000000004', 'moli-racer-elite', 'JG MOLI', 'MOLI Racer Elite', 'Complete racers', 'The flagship motion racer with a 57-inch ultrawide display and advanced controls.', 'moli-racer-elite', 7);

INSERT INTO product_variants (id, product_id, sku, price_cents) VALUES
    ('30000000-0000-0000-0000-000000000021', '20000000-0000-0000-0000-000000000021', 'JGM-RACER-CORE', 2690000),
    ('30000000-0000-0000-0000-000000000022', '20000000-0000-0000-0000-000000000022', 'JGM-RACER-SIGNATURE', 3490000),
    ('30000000-0000-0000-0000-000000000023', '20000000-0000-0000-0000-000000000023', 'JGM-RACER-ELITE', 4490000);

INSERT INTO inventory_items (id, catalog_variant_id, sku, brand, name, item_type) VALUES
    ('50000000-0000-0000-0000-000000000021', '30000000-0000-0000-0000-000000000021', 'JGM-RACER-CORE', 'JG MOLI', 'MOLI Racer Core', 'Complete racers'),
    ('50000000-0000-0000-0000-000000000022', '30000000-0000-0000-0000-000000000022', 'JGM-RACER-SIGNATURE', 'JG MOLI', 'MOLI Racer Signature', 'Complete racers'),
    ('50000000-0000-0000-0000-000000000023', '30000000-0000-0000-0000-000000000023', 'JGM-RACER-ELITE', 'JG MOLI', 'MOLI Racer Elite', 'Complete racers');

INSERT INTO inventory_balances (item_id, location_id, on_hand, reserved, reorder_level) VALUES
    ('50000000-0000-0000-0000-000000000021', '40000000-0000-0000-0000-000000000001', 0, 0, 0),
    ('50000000-0000-0000-0000-000000000022', '40000000-0000-0000-0000-000000000001', 0, 0, 0),
    ('50000000-0000-0000-0000-000000000023', '40000000-0000-0000-0000-000000000001', 0, 0, 0);

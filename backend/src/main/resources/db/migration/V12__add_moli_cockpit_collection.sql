INSERT INTO products (id, category_id, slug, brand, name, subtype, short_description, image_key, sort_order) VALUES
    ('20000000-0000-0000-0000-000000000017', '10000000-0000-0000-0000-000000000004', 'moli-cockpit', 'JG MOLI', 'MOLI Cockpit', 'Complete cockpits', 'A compact complete cockpit shaped around comfort, focus and everyday play.', 'moli-cockpit', 1),
    ('20000000-0000-0000-0000-000000000018', '10000000-0000-0000-0000-000000000004', 'moli-cockpit-plus', 'JG MOLI', 'MOLI Cockpit Plus', 'Complete cockpits', 'An enveloping personal setup with upgraded support and a wider visual field.', 'moli-cockpit-plus', 2),
    ('20000000-0000-0000-0000-000000000019', '10000000-0000-0000-0000-000000000004', 'moli-cockpit-pro', 'JG MOLI', 'MOLI Cockpit Pro', 'Complete cockpits', 'The core enthusiast cockpit with wraparound displays and advanced controls.', 'moli-cockpit-pro', 3),
    ('20000000-0000-0000-0000-000000000020', '10000000-0000-0000-0000-000000000004', 'moli-cockpit-ultra', 'JG MOLI', 'MOLI Cockpit Ultra', 'Complete cockpits', 'The flagship seating, control and display environment with an adaptive platform.', 'moli-cockpit-ultra', 4);

INSERT INTO product_variants (id, product_id, sku, price_cents) VALUES
    ('30000000-0000-0000-0000-000000000017', '20000000-0000-0000-0000-000000000017', 'JGM-COCKPIT', 1199900),
    ('30000000-0000-0000-0000-000000000018', '20000000-0000-0000-0000-000000000018', 'JGM-COCKPIT-PLUS', 2499900),
    ('30000000-0000-0000-0000-000000000019', '20000000-0000-0000-0000-000000000019', 'JGM-COCKPIT-PRO', 2999900),
    ('30000000-0000-0000-0000-000000000020', '20000000-0000-0000-0000-000000000020', 'JGM-COCKPIT-ULTRA', 3999900);

INSERT INTO inventory_items (id, catalog_variant_id, sku, brand, name, item_type) VALUES
    ('50000000-0000-0000-0000-000000000017', '30000000-0000-0000-0000-000000000017', 'JGM-COCKPIT', 'JG MOLI', 'MOLI Cockpit', 'Complete cockpits'),
    ('50000000-0000-0000-0000-000000000018', '30000000-0000-0000-0000-000000000018', 'JGM-COCKPIT-PLUS', 'JG MOLI', 'MOLI Cockpit Plus', 'Complete cockpits'),
    ('50000000-0000-0000-0000-000000000019', '30000000-0000-0000-0000-000000000019', 'JGM-COCKPIT-PRO', 'JG MOLI', 'MOLI Cockpit Pro', 'Complete cockpits'),
    ('50000000-0000-0000-0000-000000000020', '30000000-0000-0000-0000-000000000020', 'JGM-COCKPIT-ULTRA', 'JG MOLI', 'MOLI Cockpit Ultra', 'Complete cockpits');

INSERT INTO inventory_balances (item_id, location_id, on_hand, reserved, reorder_level) VALUES
    ('50000000-0000-0000-0000-000000000017', '40000000-0000-0000-0000-000000000001', 0, 0, 0),
    ('50000000-0000-0000-0000-000000000018', '40000000-0000-0000-0000-000000000001', 0, 0, 0),
    ('50000000-0000-0000-0000-000000000019', '40000000-0000-0000-0000-000000000001', 0, 0, 0),
    ('50000000-0000-0000-0000-000000000020', '40000000-0000-0000-0000-000000000001', 0, 0, 0);

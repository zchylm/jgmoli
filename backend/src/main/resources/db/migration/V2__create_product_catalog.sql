CREATE TABLE product_categories (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES product_categories(id),
    slug VARCHAR(160) NOT NULL UNIQUE,
    brand VARCHAR(100) NOT NULL,
    name VARCHAR(180) NOT NULL,
    subtype VARCHAR(80) NOT NULL,
    short_description VARCHAR(320) NOT NULL,
    image_key VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_category_status ON products(category_id, status, sort_order);

CREATE TABLE product_variants (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL DEFAULT 'Standard',
    price_cents INTEGER NOT NULL CHECK (price_cents >= 0),
    compare_at_price_cents INTEGER CHECK (compare_at_price_cents IS NULL OR compare_at_price_cents >= price_cents),
    currency CHAR(3) NOT NULL DEFAULT 'AUD',
    gst_rate NUMERIC(5,4) NOT NULL DEFAULT 0.1000 CHECK (gst_rate >= 0),
    price_includes_gst BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_variants_product_active ON product_variants(product_id, active);

INSERT INTO product_categories (id, code, name, sort_order) VALUES
    ('10000000-0000-0000-0000-000000000001', 'displays', 'Displays', 10),
    ('10000000-0000-0000-0000-000000000002', 'controls', 'Controls', 20),
    ('10000000-0000-0000-0000-000000000003', 'audio', 'Audio', 30),
    ('10000000-0000-0000-0000-000000000004', 'sim', 'Sim Gear', 40),
    ('10000000-0000-0000-0000-000000000005', 'furniture', 'Furniture', 50);

INSERT INTO products (id, category_id, slug, brand, name, subtype, short_description, image_key, sort_order) VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'lg-ultragear-27gs60f', 'LG', 'UltraGear 27GS60F 27-inch 180Hz Monitor', 'Gaming monitors', 'Fast Full HD motion for competitive play.', 'display-high-refresh', 10),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'alienware-aw3423dwf', 'Alienware', 'AW3423DWF 34-inch QD-OLED Monitor', 'Gaming monitors', 'An ultrawide OLED view built for immersive worlds.', 'display-ultrawide', 20),
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'brateck-heavy-duty-monitor-arm', 'Brateck', 'Heavy-Duty Gaming Monitor Arm', 'Monitor arms', 'Position larger displays around posture and play space.', 'display-arm', 30),
    ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'belkin-usb-c-hdmi-adapter', 'Belkin', 'USB-C to HDMI 2.1 Adapter', 'Cables & adapters', 'Connect a laptop to a high-resolution gaming display.', 'display-connect', 40),
    ('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000002', 'logitech-g515-lightspeed', 'Logitech G', 'G515 LIGHTSPEED TKL Keyboard', 'Keyboards', 'Low-profile wireless control with a compact competitive layout.', 'control-keyboard', 10),
    ('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000002', 'logitech-pro-x-superlight-2', 'Logitech G', 'PRO X SUPERLIGHT 2 Mouse', 'Mice', 'A lightweight wireless mouse shaped for consistent aim.', 'control-mouse', 20),
    ('20000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000002', 'xbox-wireless-controller', 'Xbox', 'Wireless Controller', 'Controllers', 'Familiar wireless control across Xbox and Windows.', 'control-controller', 30),
    ('20000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000003', 'hyperx-cloud-iii-wireless', 'HyperX', 'Cloud III Wireless Headset', 'Headsets', 'Long-session comfort with clear positional game audio.', 'audio-headset', 10),
    ('20000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000003', 'logitech-g560', 'Logitech G', 'G560 LIGHTSYNC Gaming Speakers', 'Speakers', 'Room-filling desktop sound with reactive lighting.', 'audio-speakers', 20),
    ('20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000003', 'hyperx-quadcast-2', 'HyperX', 'QuadCast 2 USB Microphone', 'Microphones', 'Clearer voice for teams, streams and creation.', 'audio-microphone', 30),
    ('20000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000004', 'logitech-g923', 'Logitech G', 'G923 Racing Wheel & Pedals', 'Wheels', 'Force feedback and responsive control for console and PC racing.', 'sim-wheel', 10),
    ('20000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000004', 'moza-crp2-pedals', 'MOZA Racing', 'CRP2 Load Cell Pedals', 'Pedals', 'Repeatable braking through a load-cell pedal system.', 'sim-pedals', 20),
    ('20000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000004', 'playseat-evolution-actifit', 'Playseat', 'Evolution ActiFit Racing Cockpit', 'Cockpits', 'Align seat, wheel and pedals on a stable racing frame.', 'sim-cockpit', 30),
    ('20000000-0000-0000-0000-000000000014', '10000000-0000-0000-0000-000000000005', 'secretlab-magnus-evo', 'Secretlab', 'MAGNUS Evo Gaming Desk', 'Desks', 'A cable-managed adjustable desk for a considered setup.', 'furniture-desk', 10),
    ('20000000-0000-0000-0000-000000000015', '10000000-0000-0000-0000-000000000005', 'secretlab-titan-evo', 'Secretlab', 'TITAN Evo Gaming Chair', 'Seating', 'Adjustable support for longer sessions at the desk.', 'furniture-seat', 20),
    ('20000000-0000-0000-0000-000000000016', '10000000-0000-0000-0000-000000000005', 'nanoleaf-pc-screen-mirror-lightstrip', 'Nanoleaf', 'PC Screen Mirror Lightstrip', 'Lighting', 'Extend on-screen colour into the room without visual clutter.', 'furniture-light', 30);

INSERT INTO product_variants (id, product_id, sku, price_cents) VALUES
    ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'DEMO-LG-27GS60F', 29900),
    ('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'DEMO-AW3423DWF', 129900),
    ('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', 'DEMO-BRATECK-ARM', 12900),
    ('30000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000004', 'DEMO-BELKIN-HDMI21', 7995),
    ('30000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000005', 'DEMO-G515-TKL', 24900),
    ('30000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000006', 'DEMO-GPX2', 14900),
    ('30000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000007', 'DEMO-XBOX-CONTROLLER', 8995),
    ('30000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000008', 'DEMO-CLOUD3-WL', 27900),
    ('30000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000009', 'DEMO-G560', 29900),
    ('30000000-0000-0000-0000-000000000010', '20000000-0000-0000-0000-000000000010', 'DEMO-QUADCAST2', 22900),
    ('30000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000011', 'DEMO-G923', 39900),
    ('30000000-0000-0000-0000-000000000012', '20000000-0000-0000-0000-000000000012', 'DEMO-MOZA-CRP2', 64900),
    ('30000000-0000-0000-0000-000000000013', '20000000-0000-0000-0000-000000000013', 'DEMO-PLAYSEAT-EVO', 59900),
    ('30000000-0000-0000-0000-000000000014', '20000000-0000-0000-0000-000000000014', 'DEMO-MAGNUS-EVO', 84900),
    ('30000000-0000-0000-0000-000000000015', '20000000-0000-0000-0000-000000000015', 'DEMO-TITAN-EVO', 69900),
    ('30000000-0000-0000-0000-000000000016', '20000000-0000-0000-0000-000000000016', 'DEMO-NANOLEAF-PC', 6999);

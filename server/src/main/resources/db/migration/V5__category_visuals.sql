-- Category & command group visual fields (icons + accent colors)

ALTER TABLE categories
    ADD COLUMN icon_url TEXT,
    ADD COLUMN accent_color VARCHAR(7),
    ADD COLUMN accent_color_dark VARCHAR(7);

ALTER TABLE command_groups
    ADD COLUMN icon_url TEXT,
    ADD COLUMN accent_color VARCHAR(7),
    ADD COLUMN accent_color_dark VARCHAR(7);

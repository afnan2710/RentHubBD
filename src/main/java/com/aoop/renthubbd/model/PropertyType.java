package com.aoop.renthubbd.model;

public enum PropertyType {
    // Residential
    APARTMENT("Apartment", PropertyCategory.RESIDENTIAL),
    HOUSE("House", PropertyCategory.RESIDENTIAL),
    ROOM("Single Room", PropertyCategory.RESIDENTIAL),
    HOSTEL("Hostel / Bachelor Mess", PropertyCategory.RESIDENTIAL),
    FLAT_SHARE("Flat Share", PropertyCategory.RESIDENTIAL),

    // Commercial
    OFFICE("Office", PropertyCategory.COMMERCIAL),
    SHOP("Shop", PropertyCategory.COMMERCIAL),
    SHOWROOM("Showroom", PropertyCategory.COMMERCIAL),
    WAREHOUSE("Warehouse", PropertyCategory.COMMERCIAL),
    RESTAURANT_SPACE("Restaurant Space", PropertyCategory.COMMERCIAL),

    // Industrial & Storage
    FACTORY("Factory", PropertyCategory.INDUSTRIAL),
    GODOWN("Godown", PropertyCategory.INDUSTRIAL),
    STORAGE_UNIT("Storage Unit", PropertyCategory.INDUSTRIAL),

    // Other
    PARKING("Parking Space", PropertyCategory.OTHER),
    LAND("Land", PropertyCategory.OTHER),
    EVENT_SPACE("Event Space", PropertyCategory.OTHER),
    OTHER_SPACE("Other", PropertyCategory.OTHER);

    private final String label;
    private final PropertyCategory category;

    PropertyType(String label, PropertyCategory category) {
        this.label = label;
        this.category = category;
    }
    public String getLabel() { return label; }
    public PropertyCategory getCategory() { return category; }
}
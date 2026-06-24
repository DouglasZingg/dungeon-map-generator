public enum MapSizePreset {
    SMALL_14x28("14 x 28", 14, 28, 8),
    SMALL_24x36("24 x 36", 24, 36, 14),
    MEDIUM_36x48("36 x 48", 36, 48, 30);

    private final String label;
    private final int width;
    private final int height;
    private final int recommendedRooms;

    MapSizePreset(String label, int width, int height, int recommendedRooms) {
        this.label = label;
        this.width = width;
        this.height = height;
        this.recommendedRooms = recommendedRooms;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRecommendedRooms() {
        return recommendedRooms;
    }

    @Override
    public String toString() {
        return label;
    }
}

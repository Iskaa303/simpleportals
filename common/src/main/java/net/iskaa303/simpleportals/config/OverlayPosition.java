package net.iskaa303.simpleportals.config;

public enum OverlayPosition implements ConfigEnum {
    TOP_LEFT("top_left"), MID_LEFT("mid_left"), BOTTOM_LEFT("bottom_left"),
    TOP_CENTER("top_center"), MID_CENTER("mid_center"), BOTTOM_CENTER("bottom_center"),
    TOP_RIGHT("top_right"), MID_RIGHT("mid_right"), BOTTOM_RIGHT("bottom_right");

    private final String name;

    OverlayPosition(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public static OverlayPosition fromName(String name) {
        for (OverlayPosition pos : values()) {
            if (pos.name.equals(name)) {
                return pos;
            }
        }
        return BOTTOM_RIGHT;
    }

    public int getX(int screenWidth, int textWidth) {
        return switch (this) {
            case TOP_LEFT, MID_LEFT, BOTTOM_LEFT -> 5;
            case TOP_CENTER, MID_CENTER, BOTTOM_CENTER -> (screenWidth - textWidth) / 2;
            case TOP_RIGHT, MID_RIGHT, BOTTOM_RIGHT -> screenWidth - textWidth - 5;
        };
    }

    public int getY(int screenHeight, int totalHeight) {
        return switch (this) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 5;
            case MID_LEFT, MID_CENTER, MID_RIGHT -> (screenHeight - totalHeight) / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - totalHeight - 5;
        };
    }
}

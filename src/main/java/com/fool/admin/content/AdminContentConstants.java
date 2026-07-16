package com.fool.admin.content;

public final class AdminContentConstants {
    public static final int MAX_DISPLAY_NAME_LENGTH = 64;
    public static final int MAX_WAYPOINTS = 64;
    public static final int MAX_ZONE_BLOCKS = 65_536;
    public static final int MIN_BRUSH_RADIUS = 1;
    public static final int MAX_BRUSH_RADIUS = 8;
    public static final int DEFAULT_BRUSH_RADIUS = 3;
    public static final double WAYPOINT_ARRIVAL_DISTANCE_SQ = 4.0D;
    public static final double NPC_MOVE_SPEED = 0.45D;
    public static final double BOSS_IDLE_MOVE_SPEED = 0.65D;
    public static final int BOSS_COMBAT_MEMORY_TICKS = 100;
    public static final int BOSS_ATTRACTION_REPATH_TICKS = 20;
    public static final String MANAGED_TAG_PREFIX = "foolsadmin:managed:";

    public static final int MAX_DIALOGUE_LINES = 32;
    public static final int MAX_DIALOGUE_LINE_LENGTH = 256;
    public static final int MIN_LINE_DELAY_TICKS = 0;
    public static final int MAX_LINE_DELAY_TICKS = 20 * 60 * 5;
    public static final int DEFAULT_LINE_DELAY_TICKS = 40;
    public static final int DIALOGUE_RETRIGGER_COOLDOWN_TICKS = 20;
    public static final int MAX_WAYPOINT_DWELL_TICKS = 20 * 60 * 10;
    public static final double NPC_FACE_PLAYER_RANGE = 16.0D;

    private AdminContentConstants() {
    }
}

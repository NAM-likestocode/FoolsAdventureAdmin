package com.fool.admin.map;

public final class MapTileConstants {
    public static final int TILE_BLOCKS = 128;
    public static final int TILE_PIXELS = 128;
    public static final int TILE_BYTES = TILE_PIXELS * TILE_PIXELS;
    public static final int CHUNKS_PER_TILE = TILE_BLOCKS / 16;
    public static final int FILE_VERSION = 1;
    public static final byte[] FILE_MAGIC = new byte[]{'F', 'A', 'M', '1'};

    public static final int MAX_TILES_PER_REQUEST = 24;
    public static final int MAX_REQUESTS_PER_SECOND = 3;
    public static final int SAMPLING_BUDGET_PER_TICK = 4096;

    private MapTileConstants() {
    }
}

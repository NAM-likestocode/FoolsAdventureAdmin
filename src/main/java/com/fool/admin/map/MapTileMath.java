package com.fool.admin.map;

public final class MapTileMath {
    private MapTileMath() {
    }

    public static int blockToTile(int blockCoord) {
        return Math.floorDiv(blockCoord, MapTileConstants.TILE_BLOCKS);
    }

    public static int tileOrigin(int tileCoord) {
        return tileCoord * MapTileConstants.TILE_BLOCKS;
    }

    public static int blockToLocalPixel(int blockCoord) {
        return Math.floorMod(blockCoord, MapTileConstants.TILE_BLOCKS);
    }

    public static int pixelIndex(int localX, int localZ) {
        return localX + localZ * MapTileConstants.TILE_PIXELS;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clampBlocksPerPixel(double blocksPerPixel) {
        return clamp(blocksPerPixel, 0.25D, 16.0D);
    }

    public static ZoomAnchorResult zoomAroundPoint(
            double centerX,
            double centerZ,
            double blocksPerPixel,
            double zoomFactor,
            double anchorScreenX,
            double anchorScreenY,
            double viewportWidth,
            double viewportHeight
    ) {
        double worldBeforeX = screenToWorldX(anchorScreenX, centerX, blocksPerPixel, viewportWidth);
        double worldBeforeZ = screenToWorldZ(anchorScreenY, centerZ, blocksPerPixel, viewportHeight);

        double newBlocksPerPixel = clampBlocksPerPixel(blocksPerPixel * zoomFactor);

        double worldAfterX = screenToWorldX(anchorScreenX, centerX, newBlocksPerPixel, viewportWidth);
        double worldAfterZ = screenToWorldZ(anchorScreenY, centerZ, newBlocksPerPixel, viewportHeight);

        return new ZoomAnchorResult(
                centerX + worldBeforeX - worldAfterX,
                centerZ + worldBeforeZ - worldAfterZ,
                newBlocksPerPixel
        );
    }

    public static double screenToWorldX(double screenX, double centerX, double blocksPerPixel, double viewportWidth) {
        return centerX + (screenX - viewportWidth / 2.0D) * blocksPerPixel;
    }

    public static double screenToWorldZ(double screenY, double centerZ, double blocksPerPixel, double viewportHeight) {
        return centerZ + (screenY - viewportHeight / 2.0D) * blocksPerPixel;
    }

    public record ZoomAnchorResult(double centerX, double centerZ, double blocksPerPixel) {
    }
}

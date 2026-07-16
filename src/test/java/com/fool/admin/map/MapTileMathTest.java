package com.fool.admin.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapTileMathTest {
    @Test
    void blockToTileHandlesNegativeCoordinates() {
        assertEquals(-1, MapTileMath.blockToTile(-1));
        assertEquals(-1, MapTileMath.blockToTile(-128));
        assertEquals(-2, MapTileMath.blockToTile(-129));
        assertEquals(0, MapTileMath.blockToTile(0));
        assertEquals(0, MapTileMath.blockToTile(127));
        assertEquals(1, MapTileMath.blockToTile(128));
    }

    @Test
    void tileOriginReturnsExpectedBlockOrigin() {
        assertEquals(-128, MapTileMath.tileOrigin(-1));
        assertEquals(128, MapTileMath.tileOrigin(1));
    }

    @Test
    void zoomAroundPointKeepsAnchorStable() {
        MapTileMath.ZoomAnchorResult result = MapTileMath.zoomAroundPoint(
                100.0D,
                200.0D,
                1.0D,
                0.5D,
                150.0D,
                120.0D,
                300.0D,
                200.0D
        );

        double beforeX = MapTileMath.screenToWorldX(150.0D, 100.0D, 1.0D, 300.0D);
        double beforeZ = MapTileMath.screenToWorldZ(120.0D, 200.0D, 1.0D, 200.0D);
        double afterX = MapTileMath.screenToWorldX(150.0D, result.centerX(), result.blocksPerPixel(), 300.0D);
        double afterZ = MapTileMath.screenToWorldZ(120.0D, result.centerZ(), result.blocksPerPixel(), 200.0D);

        assertEquals(beforeX, afterX, 0.001D);
        assertEquals(beforeZ, afterZ, 0.001D);
        assertEquals(0.5D, result.blocksPerPixel(), 0.001D);
    }

    @Test
    void blocksPerPixelIsClamped() {
        assertEquals(0.25D, MapTileMath.clampBlocksPerPixel(0.1D));
        assertEquals(16.0D, MapTileMath.clampBlocksPerPixel(32.0D));
    }
}

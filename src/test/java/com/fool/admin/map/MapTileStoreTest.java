package com.fool.admin.map;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapTileStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void roundTripsTileBytes() {
        TestableMapTileStore store = new TestableMapTileStore(tempDir);
        MapTileKey key = new MapTileKey(testDimension(), 2, -3);
        byte[] colors = MapTileSampler.createEmptyTile();
        colors[10] = 42;
        colors[500] = (byte) 200;

        store.saveDirect(key, colors);
        byte[] loaded = store.loadDirect(key).orElseThrow();

        assertArrayEquals(colors, loaded);
    }

    @Test
    void rejectsCorruptFiles() {
        TestableMapTileStore store = new TestableMapTileStore(tempDir);
        MapTileKey key = new MapTileKey(testDimension(), 0, 0);
        store.writeRaw(key, new byte[] {1, 2, 3});
        assertTrue(store.loadDirect(key).isEmpty());
    }

    private static ResourceKey<Level> testDimension() {
        return ResourceKey.create(
                ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("minecraft", "dimension")),
                Identifier.fromNamespaceAndPath("minecraft", "overworld")
        );
    }

    private static final class TestableMapTileStore {
        private final Path tileDirectory;

        private TestableMapTileStore(Path root) {
            this.tileDirectory = root.resolve("tiles");
        }

        void saveDirect(MapTileKey key, byte[] colors) {
            try {
                java.nio.file.Files.createDirectories(tileDirectory);
                byte[] payload = new byte[8 + MapTileConstants.TILE_BYTES];
                System.arraycopy(MapTileConstants.FILE_MAGIC, 0, payload, 0, 4);
                payload[4] = (byte) MapTileConstants.FILE_VERSION;
                System.arraycopy(colors, 0, payload, 8, MapTileConstants.TILE_BYTES);
                java.nio.file.Files.write(tileDirectory.resolve(key.storageFileName()), payload);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        void writeRaw(MapTileKey key, byte[] bytes) {
            try {
                java.nio.file.Files.createDirectories(tileDirectory);
                java.nio.file.Files.write(tileDirectory.resolve(key.storageFileName()), bytes);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        java.util.Optional<byte[]> loadDirect(MapTileKey key) {
            try {
                Path file = tileDirectory.resolve(key.storageFileName());
                if (!java.nio.file.Files.exists(file)) {
                    return java.util.Optional.empty();
                }
                byte[] fileBytes = java.nio.file.Files.readAllBytes(file);
                if (fileBytes.length != 8 + MapTileConstants.TILE_BYTES) {
                    return java.util.Optional.empty();
                }
                for (int i = 0; i < MapTileConstants.FILE_MAGIC.length; i++) {
                    if (fileBytes[i] != MapTileConstants.FILE_MAGIC[i]) {
                        return java.util.Optional.empty();
                    }
                }
                return java.util.Optional.of(java.util.Arrays.copyOfRange(fileBytes, 8, 8 + MapTileConstants.TILE_BYTES));
            } catch (Exception exception) {
                return java.util.Optional.empty();
            }
        }
    }
}

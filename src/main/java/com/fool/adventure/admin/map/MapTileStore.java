package com.fool.adventure.admin.map;

import com.fool.adventure.FoolsAdventure;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class MapTileStore {
    private final Path tileDirectory;
    private final Set<MapTileKey> corruptLogged = ConcurrentHashMap.newKeySet();

    public MapTileStore(ServerLevel level) {
        this.tileDirectory = level.getServer()
                .getWorldPath(LevelResource.DATA)
                .resolve("foolsadventure_admin_map")
                .resolve(level.dimension().identifier().getNamespace())
                .resolve(level.dimension().identifier().getPath())
                .resolve("tiles");
    }

    public Optional<byte[]> load(MapTileKey key) {
        Path file = tileDirectory.resolve(key.storageFileName());
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        try {
            byte[] fileBytes = Files.readAllBytes(file);
            if (!isValidFile(fileBytes)) {
                logCorruptOnce(key);
                return Optional.empty();
            }
            return Optional.of(Arrays.copyOfRange(fileBytes, 8, 8 + MapTileConstants.TILE_BYTES));
        } catch (IOException exception) {
            logCorruptOnce(key);
            FoolsAdventure.LOGGER.warn("Failed to read admin map tile {}: {}", key.storageFileName(), exception.toString());
            return Optional.empty();
        }
    }

    public void save(MapTileKey key, byte[] colors) {
        if (colors.length != MapTileConstants.TILE_BYTES) {
            throw new IllegalArgumentException("Tile color buffer must be " + MapTileConstants.TILE_BYTES + " bytes");
        }

        try {
            Files.createDirectories(tileDirectory);
            byte[] payload = new byte[8 + MapTileConstants.TILE_BYTES];
            System.arraycopy(MapTileConstants.FILE_MAGIC, 0, payload, 0, 4);
            payload[4] = (byte) (MapTileConstants.FILE_VERSION & 0xFF);
            payload[5] = 0;
            payload[6] = 0;
            payload[7] = 0;
            System.arraycopy(colors, 0, payload, 8, MapTileConstants.TILE_BYTES);

            Path target = tileDirectory.resolve(key.storageFileName());
            Path temp = tileDirectory.resolve(key.storageFileName() + ".tmp");
            Files.write(temp, payload, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            FoolsAdventure.LOGGER.warn("Failed to save admin map tile {}: {}", key.storageFileName(), exception.toString());
        }
    }

    private static boolean isValidFile(byte[] fileBytes) {
        if (fileBytes.length != 8 + MapTileConstants.TILE_BYTES) {
            return false;
        }
        for (int i = 0; i < MapTileConstants.FILE_MAGIC.length; i++) {
            if (fileBytes[i] != MapTileConstants.FILE_MAGIC[i]) {
                return false;
            }
        }
        return fileBytes[4] == MapTileConstants.FILE_VERSION;
    }

    private void logCorruptOnce(MapTileKey key) {
        if (corruptLogged.add(key)) {
            FoolsAdventure.LOGGER.warn("Ignoring corrupt or missing admin map tile {}", key.storageFileName());
        }
    }
}

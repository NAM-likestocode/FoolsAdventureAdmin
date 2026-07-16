package com.fool.admin.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record Waypoint(int x, int y, int z, int dwellTicks) {
    public static final Codec<Waypoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(Waypoint::x),
            Codec.INT.fieldOf("y").forGetter(Waypoint::y),
            Codec.INT.fieldOf("z").forGetter(Waypoint::z),
            Codec.INT.optionalFieldOf("dwell_ticks", 0).forGetter(Waypoint::dwellTicks)
    ).apply(instance, Waypoint::new));

    public Waypoint(int x, int y, int z) {
        this(x, y, z, 0);
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    public static Waypoint from(BlockPos pos) {
        return new Waypoint(pos.getX(), pos.getY(), pos.getZ(), 0);
    }

    public Waypoint withDwellTicks(int dwellTicks) {
        return new Waypoint(x, y, z, dwellTicks);
    }
}

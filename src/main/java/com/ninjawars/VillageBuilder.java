package com.ninjawars;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class VillageBuilder {
    private static final int R = 12;
    private VillageBuilder() {}

    private static void set(ServerLevel l, int x, int y, int z, Block b) {
        l.setBlock(new BlockPos(x, y, z), b.defaultBlockState(), 2);
    }

    /** c = posição dos pés do jogador; o chão fica em c.y - 1. */
    public static void build(ServerLevel l, BlockPos c, Village v) {
        int cx = c.getX(), cy = c.getY(), cz = c.getZ();
        for (int x = -R - 1; x <= R + 1; x++) {
            for (int z = -R - 1; z <= R + 1; z++) {
                for (int dy = 0; dy <= 9; dy++) set(l, cx + x, cy + dy, cz + z, Blocks.AIR);
                for (int dy = 2; dy <= 8; dy++) {
                    BlockPos p = new BlockPos(cx + x, cy - dy, cz + z);
                    BlockState s = l.getBlockState(p);
                    if (s.isAir() || !s.canOcclude()) set(l, p.getX(), p.getY(), p.getZ(), Blocks.DIRT);
                }
                boolean inside = Math.abs(x) <= R && Math.abs(z) <= R;
                if (inside) set(l, cx + x, cy - 1, cz + z, (x == 0 || z == 0) ? Blocks.DIRT_PATH : v.floor);
            }
        }
        // muralha
        for (int i = -R; i <= R; i++) {
            for (int dy = 0; dy < 4; dy++) {
                set(l, cx + i, cy + dy, cz - R, v.wall);
                set(l, cx + i, cy + dy, cz + R, v.wall);
                set(l, cx - R, cy + dy, cz + i, v.wall);
                set(l, cx + R, cy + dy, cz + i, v.wall);
            }
        }
        for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy < 3; dy++) set(l, cx + dx, cy + dy, cz + R, Blocks.AIR);
        for (int sx : new int[]{-R, R}) for (int sz : new int[]{-R, R}) {
            set(l, cx + sx, cy + 4, cz + sz, v.roof);
            set(l, cx + sx, cy + 5, cz + sz, Blocks.LANTERN);
        }
        // quartel do Kage + quadro de missões
        house(l, cx, cz, cy, 3, 4, v, +1);
        set(l, cx, cy, cz - 2, Blocks.LECTERN);
        set(l, cx - 2, cy, cz - 2, Blocks.LANTERN);
        set(l, cx + 2, cy, cz - 2, Blocks.LANTERN);
        // casas
        for (int sx : new int[]{-8, 8}) for (int sz : new int[]{-7, 7}) {
            house(l, cx + sx, cz + sz, cy, 2, 3, v, sz > 0 ? -1 : +1);
        }
        // tochas da entrada
        set(l, cx - 2, cy, cz + R, Blocks.LANTERN);
        set(l, cx + 2, cy, cz + R, Blocks.LANTERN);
    }

    private static void house(ServerLevel l, int hx, int hz, int y, int half, int h, Village v, int doorDir) {
        for (int x = -half; x <= half; x++) {
            for (int z = -half; z <= half; z++) {
                boolean edge = Math.abs(x) == half || Math.abs(z) == half;
                set(l, hx + x, y - 1, hz + z, v.floor);
                for (int dy = 0; dy < h; dy++) set(l, hx + x, y + dy, hz + z, edge ? v.wall : Blocks.AIR);
            }
        }
        // porta
        int dz = hz + doorDir * half;
        set(l, hx, y, dz, Blocks.AIR);
        set(l, hx, y + 1, dz, Blocks.AIR);
        // janelas
        set(l, hx - half, y + 1, hz, Blocks.GLASS_PANE);
        set(l, hx + half, y + 1, hz, Blocks.GLASS_PANE);
        // telhado em camadas
        for (int i = 0; i <= half; i++) {
            int s = half + 1 - i;
            for (int x = -s; x <= s; x++) for (int z = -s; z <= s; z++) set(l, hx + x, y + h + i, hz + z, v.roof);
        }
    }
}

package com.ninjawars;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class WarManager {
    private static final int[] GARRISON = {4, 3, 2, 1, 1, 0, 0, 0};
    private WarManager() {}

    public static void broadcast(ServerLevel l, String msg) {
        l.getServer().getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
    }

    private static boolean active(ServerLevel l, BlockPos p) { return l.isPositionEntityTicking(p); }

    public static List<NinjaEntity> ninjas(ServerLevel l, BlockPos c, int radius, Village v, boolean invader) {
        return l.getEntitiesOfClass(NinjaEntity.class, new AABB(c).inflate(radius),
                n -> n.getVillage() == v && n.isInvader() == invader && n.isAlive());
    }

    public static NinjaEntity spawn(ServerLevel l, Village v, int rank, int x, int z) {
        if (!l.isLoaded(new BlockPos(x, 64, z))) return null;
        int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        NinjaEntity n = NinjaWars.NINJA.get().create(l);
        if (n == null) return null;
        n.moveTo(x + 0.5, y, z + 0.5, l.random.nextFloat() * 360f, 0f);
        n.setup(v, rank);
        l.addFreshEntity(n);
        return n;
    }

    public static void tick(ServerLevel l) {
        WarData d = WarData.get(l);
        // guarnições
        for (var e : d.centers.entrySet()) {
            Village v = e.getKey();
            BlockPos c = e.getValue();
            if (!active(l, c)) continue;
            int[] have = new int[5], want = new int[5];
            for (NinjaEntity n : ninjas(l, c, 60, v, false)) have[n.getRank()]++;
            for (int r : GARRISON) want[r]++;
            for (int r = 0; r < 5; r++) {
                for (int i = have[r]; i < want[r]; i++) {
                    NinjaEntity n = spawn(l, v, r, c.getX() + l.random.nextInt(11) - 5, c.getZ() + l.random.nextInt(11) - 5);
                    if (n != null) n.setHome(c);
                }
            }
        }
        // diplomacia: uma chance por 5 minutos
        if (l.getGameTime() % 6000 < 200 && d.wars.size() < 2 && d.centers.size() >= 2 && l.random.nextFloat() < 0.25f) {
            List<Village> vs = new ArrayList<>(d.centers.keySet());
            Village a = vs.get(l.random.nextInt(vs.size()));
            Village b = vs.get(l.random.nextInt(vs.size()));
            if (d.declare(a, b)) broadcast(l, "§c§l[Guerra] §r§c" + a.display + " declarou guerra contra " + b.display + "!");
        }
        // guerras
        Iterator<WarData.War> it = d.wars.iterator();
        while (it.hasNext()) {
            WarData.War w = it.next();
            BlockPos dc = d.centers.get(w.def);
            if (dc == null || !d.centers.containsKey(w.atk)) { it.remove(); d.setDirty(); continue; }
            if (!active(l, dc)) continue;
            w.age++;
            d.setDirty();
            if (w.waves > 0) {
                if (w.cooldown <= 0) {
                    spawnWave(l, w, dc);
                    w.waves--; w.started = true; w.cooldown = 3;
                } else w.cooldown--;
                continue;
            }
            int inv = ninjas(l, dc, 150, w.atk, true).size();
            int gar = ninjas(l, dc, 60, w.def, false).size();
            if (inv == 0) {
                broadcast(l, "§a§l[Guerra] §r§a" + w.def.display + " repeliu a invasão de " + w.atk.display + "! Paz restaurada.");
                finish(l, w, dc); it.remove();
            } else if (gar == 0) {
                broadcast(l, "§4§l[Guerra] §r§4" + w.atk.display + " dominou " + w.def.display + "! A vila está ocupada.");
                finish(l, w, dc); it.remove();
            } else if (w.age > 150) {
                broadcast(l, "§e[Guerra] Impasse entre " + w.atk.display + " e " + w.def.display + ". Trégua declarada.");
                finish(l, w, dc); it.remove();
            }
        }
    }

    private static void finish(ServerLevel l, WarData.War w, BlockPos dc) {
        for (NinjaEntity n : ninjas(l, dc, 200, w.atk, true)) n.discard();
    }

    private static void spawnWave(ServerLevel l, WarData.War w, BlockPos dc) {
        int[] comp = {3, 2, 1, 1, 0, 0};
        int size = 4 + (2 - Math.min(2, w.waves - 1 < 0 ? 0 : w.waves - 1));
        int spawned = 0;
        for (int i = 0; i < Math.min(size, comp.length); i++) {
            double ang = l.random.nextDouble() * Math.PI * 2;
            int x = dc.getX() + (int) (Math.cos(ang) * 30), z = dc.getZ() + (int) (Math.sin(ang) * 30);
            NinjaEntity n = spawn(l, w.atk, comp[i], x, z);
            if (n != null) { n.setInvader(dc); spawned++; }
        }
        broadcast(l, "§c[Guerra] Onda de invasão de " + w.atk.display + " chegando em " + w.def.display + " (" + spawned + " ninjas).");
    }
}

package com.ninjawars;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class WarManager {
    private static final int[] GARRISON = {4, 3, 2, 1, 1, 0, 0, 0};
    private static final String VILLAGE_TAG = "ninjawars.village.";
    private static final String INVADER_TAG = "ninjawars.invader";
    private WarManager() {}

    public static void broadcast(ServerLevel l, String msg) {
        l.getServer().getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
    }

    private static boolean active(ServerLevel l, BlockPos p) {
        return l.isPositionEntityTicking(p);
    }

    public static Village taggedVillage(Entity entity) {
        for (Village v : Village.values()) {
            if (entity.getTags().contains(VILLAGE_TAG + v.id)) return v;
        }
        return null;
    }

    public static boolean isNarutotoWarrior(Entity entity) {
        return taggedVillage(entity) != null;
    }

    public static List<Mob> ninjas(ServerLevel l, BlockPos center, int radius, Village village, boolean invader) {
        return l.getEntitiesOfClass(Mob.class, new AABB(center).inflate(radius),
                mob -> mob.isAlive()
                        && mob.getTags().contains(VILLAGE_TAG + village.id)
                        && mob.getTags().contains(INVADER_TAG) == invader);
    }

    private static String entityId(Village village, int rank) {
        String suffix = rank <= 0 ? "genin" : rank == 1 ? "chunin" : "jonin";
        if (village == Village.KONOHA && rank == 3) return "konoha_anbu";
        // Narutoto 0.4.1 não possui entidade Kage; ranks superiores usam o Jonin.
        return switch (village) {
            case KONOHA -> "konoha_" + suffix;
            case SUNA -> "suna_" + suffix;
            case KIRI -> "kiri_" + suffix;
            case KUMO -> "kumo_" + suffix;
            case IWA -> "iwa_" + suffix;
        };
    }

    public static Mob spawn(ServerLevel level, Village village, int rank, int x, int z) {
        BlockPos probe = new BlockPos(x, level.getMinBuildHeight(), z);
        if (!level.hasChunkAt(probe)) return null;

        ResourceLocation id = new ResourceLocation("narutoto", entityId(village, rank));
        var type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) {
            broadcast(level, "§c[Ninja Wars] Entidade Narutoto não encontrada: " + id);
            return null;
        }

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        Entity created = type.create(level);
        if (!(created instanceof Mob mob)) return null;

        mob.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360f, 0f);
        mob.addTag(VILLAGE_TAG + village.id);
        mob.addTag("ninjawars.rank." + rank);
        mob.setPersistenceRequired();
        mob.setCustomName(Component.literal(rank >= 4 ? village.kageTitle + " de " + village.display
                : Ranks.NAMES[Math.min(rank, 4)] + " de " + village.display));
        mob.setCustomNameVisible(rank >= 3);
        level.addFreshEntity(mob);
        return mob;
    }

    private static boolean isEnemyOf(Mob mob, LivingEntity candidate, WarData data) {
        Village ownVillage = taggedVillage(mob);
        if (ownVillage == null || candidate == mob || !candidate.isAlive()) return false;

        Village otherVillage = candidate instanceof ServerPlayer player
                ? PlayerData.village(player) : taggedVillage(candidate);
        return otherVillage != null && otherVillage != ownVillage
                && data.atWar(ownVillage, otherVillage);
    }

    private static void directArmies(ServerLevel level, WarData data, WarData.War war, BlockPos battleCenter) {
        AABB area = new AABB(battleCenter).inflate(100);
        List<Mob> warriors = level.getEntitiesOfClass(Mob.class, area,
                mob -> mob.isAlive() && taggedVillage(mob) != null);
        List<LivingEntity> possibleTargets = level.getEntitiesOfClass(LivingEntity.class, area,
                LivingEntity::isAlive);

        for (Mob warrior : warriors) {
            Village side = taggedVillage(warrior);
            if (side != war.atk && side != war.def) continue;

            LivingEntity current = warrior.getTarget();
            if (current != null && isEnemyOf(warrior, current, data)) continue;

            LivingEntity nearest = null;
            double nearestDistance = 48.0 * 48.0;
            for (LivingEntity candidate : possibleTargets) {
                if (candidate == warrior || !isEnemyOf(warrior, candidate, data)) continue;
                double distance = warrior.distanceToSqr(candidate);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = candidate;
                }
            }
            if (nearest != null) warrior.setTarget(nearest);
        }
    }

    public static void tick(ServerLevel level) {
        WarData data = WarData.get(level);

        // Reforços das vilas: usa Genin, Chunin, Jonin e ANBU reais do Narutoto.
        for (var entry : data.centers.entrySet()) {
            Village village = entry.getKey();
            BlockPos center = entry.getValue();
            if (!active(level, center)) continue;

            int[] have = new int[5], want = new int[5];
            for (Mob mob : ninjas(level, center, 60, village, false)) {
                int rank = 0;
                for (int r = 0; r <= 4; r++) {
                    if (mob.getTags().contains("ninjawars.rank." + r)) {
                        rank = r;
                        break;
                    }
                }
                have[rank]++;
            }
            for (int rank : GARRISON) want[rank]++;
            for (int rank = 0; rank < 5; rank++) {
                for (int i = have[rank]; i < want[rank]; i++) {
                    Mob mob = spawn(level, village, rank,
                            center.getX() + level.random.nextInt(11) - 5,
                            center.getZ() + level.random.nextInt(11) - 5);
                    if (mob != null) mob.addTag("ninjawars.garrison");
                }
            }
        }

        // Diplomacia automática: chance periódica, limitada a duas guerras simultâneas.
        if (level.getGameTime() % 6000 < 200 && data.wars.size() < 2
                && data.centers.size() >= 2 && level.random.nextFloat() < 0.25f) {
            List<Village> villages = new ArrayList<>(data.centers.keySet());
            Village a = villages.get(level.random.nextInt(villages.size()));
            Village b = villages.get(level.random.nextInt(villages.size()));
            if (data.declare(a, b)) broadcast(level, "§c§l[Guerra] §r§c" + a.display + " declarou guerra contra " + b.display + "!");
        }

        Iterator<WarData.War> iterator = data.wars.iterator();
        while (iterator.hasNext()) {
            WarData.War war = iterator.next();
            BlockPos defenderCenter = data.centers.get(war.def);
            if (defenderCenter == null || !data.centers.containsKey(war.atk)) {
                iterator.remove();
                data.setDirty();
                continue;
            }
            if (!active(level, defenderCenter)) continue;

            war.age++;
            data.setDirty();
            directArmies(level, data, war, defenderCenter);

            if (war.waves > 0) {
                if (war.cooldown <= 0) {
                    spawnWave(level, war, defenderCenter);
                    war.waves--;
                    war.started = true;
                    war.cooldown = 3;
                } else {
                    war.cooldown--;
                }
                continue;
            }

            int invaders = ninjas(level, defenderCenter, 150, war.atk, true).size();
            int defenders = ninjas(level, defenderCenter, 60, war.def, false).size();
            if (invaders == 0) {
                broadcast(level, "§a§l[Guerra] §r§a" + war.def.display + " repeliu a invasão de " + war.atk.display + "!");
                finish(level, war, defenderCenter);
                iterator.remove();
            } else if (defenders == 0) {
                broadcast(level, "§4§l[Guerra] §r§4" + war.atk.display + " dominou " + war.def.display + "!");
                finish(level, war, defenderCenter);
                iterator.remove();
            } else if (war.age > 150) {
                broadcast(level, "§e[Guerra] Impasse entre " + war.atk.display + " e " + war.def.display + ". Trégua declarada.");
                finish(level, war, defenderCenter);
                iterator.remove();
            }
        }
    }

    public static void removeVillageWarriors(ServerLevel level, Village village, BlockPos center, int radius) {
        for (Mob mob : ninjas(level, center, radius, village, false)) mob.discard();
        for (Mob mob : ninjas(level, center, radius, village, true)) mob.discard();
    }

    private static void finish(ServerLevel level, WarData.War war, BlockPos defenderCenter) {
        for (Mob mob : ninjas(level, defenderCenter, 200, war.atk, true)) mob.discard();
    }

    private static void spawnWave(ServerLevel level, WarData.War war, BlockPos defenderCenter) {
        int[] composition = {2, 1, 1, 0, 0, 0};
        int spawned = 0;
        for (int rank : composition) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            int x = defenderCenter.getX() + (int) (Math.cos(angle) * 30);
            int z = defenderCenter.getZ() + (int) (Math.sin(angle) * 30);
            Mob mob = spawn(level, war.atk, rank, x, z);
            if (mob != null) {
                mob.addTag(INVADER_TAG);
                mob.addTag("ninjawars.wave");
                spawned++;
            }
        }
        broadcast(level, "§c[Guerra] Reforços de " + war.atk.display + " chegando perto de "
                + war.def.display + " (" + spawned + " ninjas do Narutoto).");
    }
}

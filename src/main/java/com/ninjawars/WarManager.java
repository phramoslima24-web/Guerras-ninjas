
package com.ninjawars;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class WarManager {

    private static final int[] GARRISON = {3, 2, 2, 1, 1, 0, 0, 0};

    private static final String VILLAGE_TAG = "ninjawars.village.";
    private static final String INVADER_TAG = "ninjawars.invader";
    private static final String LEADER_TAG = "ninjawars.leader";

    // Limite de blocos analisados em cada execução.
    // A verificação de uma vila grande será distribuída por vários ticks.
    private static final int DESTRUCTION_SCAN_BUDGET = 4096;

    // Guarda o progresso temporário da verificação de cada vila.
    private static final Map<Village, DestructionScan> DESTRUCTION_SCANS =
            new EnumMap<>(Village.class);

    private WarManager() {}

    private static class DestructionScan {
        final BoundingBox box;

        int x;
        int y;
        int z;

        DestructionScan(BoundingBox box) {
            this.box = box;
            this.x = box.minX();
            this.y = box.minY();
            this.z = box.minZ();
        }

        boolean finished() {
            return x > box.maxX();
        }

        void advance() {
            z++;

            if (z > box.maxZ()) {
                z = box.minZ();
                y++;

                if (y > box.maxY()) {
                    y = box.minY();
                    x++;
                }
            }
        }
    }

    public static void broadcast(ServerLevel level, String message) {
        level.getServer().getPlayerList()
                .broadcastSystemMessage(Component.literal(message), false);
    }

    private static boolean active(ServerLevel level, BlockPos pos) {
        return level.isPositionEntityTicking(pos);
    }

    public static Village taggedVillage(Entity entity) {
        for (Village village : Village.values()) {
            if (entity.getTags().contains(VILLAGE_TAG + village.id)) {
                return village;
            }
        }
        return null;
    }

    public static boolean isNarutotoWarrior(Entity entity) {
        return taggedVillage(entity) != null;
    }

    public static List<Mob> ninjas(
            ServerLevel level,
            BlockPos center,
            int radius,
            Village village,
            boolean invader) {

        return level.getEntitiesOfClass(
                Mob.class,
                new AABB(center).inflate(radius),
                mob -> mob.isAlive()
                        && mob.getTags().contains(VILLAGE_TAG + village.id)
                        && !mob.getTags().contains(LEADER_TAG)
                        && mob.getTags().contains(INVADER_TAG) == invader
        );
    }

    private static String entityId(Village village, int rank) {
        String suffix = rank <= 0
                ? "genin"
                : rank == 1 ? "chunin" : "jonin";

        if (village == Village.KONOHA && rank == 3) {
            return "konoha_anbu";
        }

        return switch (village) {
            case KONOHA -> "konoha_" + suffix;
            case SUNA -> "suna_" + suffix;
            case KIRI -> "kiri_" + suffix;
            case KUMO -> "kumo_" + suffix;
            case IWA -> "iwa_" + suffix;
        };
    }

    public static Mob spawn(
            ServerLevel level,
            Village village,
            int rank,
            int x,
            int z) {

        BlockPos probe = new BlockPos(x, level.getMinBuildHeight(), z);

        if (!level.hasChunkAt(probe)) {
            return null;
        }

        if (WarData.get(level).destroyed.contains(village)) {
            return null;
        }

        ResourceLocation id = new ResourceLocation(
                "narutoto",
                entityId(village, rank)
        );

        var type = ForgeRegistries.ENTITY_TYPES.getValue(id);

        if (type == null) {
            broadcast(level,
                    "§c[Ninja Wars] Entidade Narutoto não encontrada: " + id);
            return null;
        }

        int y = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                x,
                z
        );

        Entity created = type.create(level);

        if (!(created instanceof Mob mob)) {
            return null;
        }

        mob.moveTo(
                x + 0.5,
                y,
                z + 0.5,
                level.random.nextFloat() * 360f,
                0f
        );

        mob.addTag(VILLAGE_TAG + village.id);
        mob.addTag("ninjawars.rank." + rank);
        mob.setPersistenceRequired();

        mob.setCustomName(Component.literal(
                rank >= 4
                        ? village.kageTitle + " de " + village.display
                        : Ranks.NAMES[Math.min(rank, 4)] + " de " + village.display
        ));

        mob.setCustomNameVisible(rank >= 3);

        level.addFreshEntity(mob);

        return mob;
    }

    public static void spawnLeader(
            ServerLevel level,
            Village village,
            BlockPos center) {

        WarData data = WarData.get(level);

        if (data.destroyed.contains(village)
                || data.leadersSpawned.contains(village)) {
            return;
        }

        String id = switch (village) {
            case KONOHA -> "minato_npc";
            case SUNA -> "gaara_npc";
            case KUMO -> "kumo_jonin";
            default -> null;
        };

        if (id == null) {
            return;
        }

        var type = ForgeRegistries.ENTITY_TYPES.getValue(
                new ResourceLocation("narutoto", id)
        );

        if (type == null) {
            broadcast(level,
                    "§c[Ninja Wars] Não foi possível encontrar o líder "
                            + id + " no Narutoto.");
            return;
        }

        int x = center.getX();
        int z = center.getZ();

        int y = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                x,
                z
        );

        Entity created = type.create(level);

        if (!(created instanceof Mob leader)) {
            return;
        }

        leader.moveTo(
                x + 0.5,
                y,
                z + 0.5,
                level.random.nextFloat() * 360f,
                0f
        );

        leader.addTag(VILLAGE_TAG + village.id);
        leader.addTag(LEADER_TAG);

        int rank = village == Village.KUMO ? 2 : 4;

        leader.addTag("ninjawars.rank." + rank);
        leader.setPersistenceRequired();

        leader.setCustomName(Component.literal(switch (village) {
            case KONOHA -> "Minato — Hokage";
            case SUNA -> "Gaara — Kazekage";
            case KUMO -> "Jōnin líder de Kumo";
            default -> "Líder de " + village.display;
        }));

        leader.setCustomNameVisible(true);

        if (level.addFreshEntity(leader)) {
            data.leadersSpawned.add(village);
            data.setDirty();
        }
    }

    private static boolean isEnemyOf(
            Mob mob,
            LivingEntity candidate,
            WarData data) {

        Village ownVillage = taggedVillage(mob);

        if (ownVillage == null
                || candidate == mob
                || !candidate.isAlive()
                || candidate.getTags().contains(LEADER_TAG)) {
            return false;
        }

        // Konoha não escolhe jogadores como alvos.
        if (ownVillage == Village.KONOHA
                && candidate instanceof ServerPlayer) {
            return false;
        }

        Village otherVillage = candidate instanceof ServerPlayer player
                ? PlayerData.village(player)
                : taggedVillage(candidate);

        return otherVillage != null
                && otherVillage != ownVillage
                && data.atWar(ownVillage, otherVillage);
    }

    private static void directArmies(
            ServerLevel level,
            WarData data,
            WarData.War war,
            BlockPos battleCenter) {

        AABB area = new AABB(battleCenter).inflate(100);

        List<Mob> warriors = level.getEntitiesOfClass(
                Mob.class,
                area,
                mob -> mob.isAlive()
                        && taggedVillage(mob) != null
                        && !mob.getTags().contains(LEADER_TAG)
        );

        List<LivingEntity> possibleTargets =
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        LivingEntity::isAlive
                );

        for (Mob warrior : warriors) {
            Village side = taggedVillage(warrior);

            if (side != war.atk && side != war.def) {
                continue;
            }

            LivingEntity current = warrior.getTarget();

            if (current != null
                    && isEnemyOf(warrior, current, data)) {
                continue;
            }

            LivingEntity nearest = null;
            double nearestDistance = 48.0 * 48.0;

            for (LivingEntity candidate : possibleTargets) {
                if (candidate == warrior
                        || !isEnemyOf(warrior, candidate, data)) {
                    continue;
                }

                double distance = warrior.distanceToSqr(candidate);

                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = candidate;
                }
            }

            if (nearest != null) {
                warrior.setTarget(nearest);
            }
        }
    }

    private static boolean naturalBlock(
            net.minecraft.world.level.block.state.BlockState state) {

        return state.isAir()
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.WATER)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.BEDROCK)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.MOSS_BLOCK);
    }

    /**
     * Confere se todos os chunks que contêm a estrutura estão carregados.
     * Não força o carregamento de chunks.
     */
    private static boolean allChunksLoaded(
            ServerLevel level,
            BoundingBox box) {

        int minChunkX = box.minX() >> 4;
        int maxChunkX = box.maxX() >> 4;
        int minChunkZ = box.minZ() >> 4;
        int maxChunkZ = box.maxZ() >> 4;

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                BlockPos probe = new BlockPos(
                        (x << 4) + 8,
                        box.minY(),
                        (z << 4) + 8
                );

                if (!level.hasChunkAt(probe)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Inicia e processa verificações em pequenos lotes.
     * Cada execução analisa no máximo DESTRUCTION_SCAN_BUDGET blocos.
     */
    private static void checkDestroyed(
            ServerLevel level,
            WarData data) {

        // Inicia uma nova rodada de verificações a cada 6000 ticks.
        if (level.getGameTime() % 6000 == 0) {
            for (var entry : data.areas.entrySet()) {
                Village village = entry.getKey();

                if (data.destroyed.contains(village)) {
                    continue;
                }

                DESTRUCTION_SCANS.putIfAbsent(
                        village,
                        new DestructionScan(entry.getValue())
                );
            }
        }

        if (DESTRUCTION_SCANS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<Village, DestructionScan>> iterator =
                DESTRUCTION_SCANS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Village, DestructionScan> entry = iterator.next();

            Village village = entry.getKey();
            DestructionScan scan = entry.getValue();

            if (data.destroyed.contains(village)) {
                iterator.remove();
                continue;
            }

            // Se a estrutura mudou, reinicia a verificação na próxima rodada.
            BoundingBox currentBox = data.areas.get(village);

            if (currentBox == null || !currentBox.equals(scan.box)) {
                iterator.remove();
                continue;
            }

            // Nunca tenta ler blocos de chunks descarregados.
            if (!allChunksLoaded(level, scan.box)) {
                iterator.remove();
                continue;
            }

            int checked = 0;
            boolean hasConstruction = false;

            while (!scan.finished()
                    && checked < DESTRUCTION_SCAN_BUDGET) {

                BlockPos pos = new BlockPos(
                        scan.x,
                        scan.y,
                        scan.z
                );

                if (!naturalBlock(level.getBlockState(pos))) {
                    hasConstruction = true;
                    break;
                }

                scan.advance();
                checked++;
            }

            // Encontrou algum bloco que não é natural:
            // a vila ainda não foi completamente destruída.
            if (hasConstruction) {
                iterator.remove();
                continue;
            }

            // Ainda há blocos para verificar. Continua na próxima execução.
            if (!scan.finished()) {
                continue;
            }

            // Todos os blocos foram verificados e são naturais.
            data.destroyed.add(village);

            data.wars.removeIf(
                    war -> war.atk == village || war.def == village
            );

            BlockPos center = data.centers.remove(village);

            if (center != null) {
                removeVillageWarriors(level, village, center, 200);

                for (Mob mob : level.getEntitiesOfClass(
                        Mob.class,
                        new AABB(center).inflate(200),
                        m -> m.getTags().contains(VILLAGE_TAG + village.id)
                                && m.getTags().contains(LEADER_TAG)
                )) {
                    mob.discard();
                }
            }

            data.setDirty();

            broadcast(
                    level,
                    "§4§l[Ninja Wars] " + village.display
                            + " foi completamente destruída! "
                            + "Não nascerão mais ninjas nessa vila."
            );

            iterator.remove();
        }
    }

    public static void tick(ServerLevel level) {
        WarData data = WarData.get(level);

        // Agora a verificação é processada em lotes pequenos.
        checkDestroyed(level, data);

        for (var entry : data.centers.entrySet()) {
            if (active(level, entry.getValue())) {
                spawnLeader(
                        level,
                        entry.getKey(),
                        entry.getValue()
                );
            }
        }

        // Reforços das vilas.
        for (var entry : data.centers.entrySet()) {
            Village village = entry.getKey();
            BlockPos center = entry.getValue();

            if (data.destroyed.contains(village)
                    || !active(level, center)) {
                continue;
            }

            int[] have = new int[5];
            int[] want = new int[5];

            for (Mob mob : ninjas(
                    level,
                    center,
                    60,
                    village,
                    false
            )) {
                int rank = 0;

                for (int r = 0; r <= 4; r++) {
                    if (mob.getTags().contains("ninjawars.rank." + r)) {
                        rank = r;
                        break;
                    }
                }

                have[rank]++;
            }

            for (int rank : GARRISON) {
                want[rank]++;
            }

            // O único Jōnin de Kumo é o líder especial.
            if (village == Village.KUMO) {
                want[2] = 0;
                want[3] = 0;
            }

            for (int rank = 0; rank < 5; rank++) {
                for (int i = have[rank]; i < want[rank]; i++) {
                    Mob mob = spawn(
                            level,
                            village,
                            rank,
                            center.getX() + level.random.nextInt(11) - 5,
                            center.getZ() + level.random.nextInt(11) - 5
                    );

                    if (mob != null) {
                        mob.addTag("ninjawars.garrison");
                    }
                }
            }
        }

        // Guerras só começam por comando explícito do administrador.
        Iterator<WarData.War> iterator = data.wars.iterator();

        while (iterator.hasNext()) {
            WarData.War war = iterator.next();

            BlockPos defenderCenter = data.centers.get(war.def);

            if (defenderCenter == null
                    || !data.centers.containsKey(war.atk)) {
                iterator.remove();
                data.setDirty();
                continue;
            }

            if (!active(level, defenderCenter)) {
                continue;
            }

            war.age++;
            data.setDirty();

            directArmies(
                    level,
                    data,
                    war,
                    defenderCenter
            );

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

            int invaders = ninjas(
                    level,
                    defenderCenter,
                    150,
                    war.atk,
                    true
            ).size();

            int defenders = ninjas(
                    level,
                    defenderCenter,
                    60,
                    war.def,
                    false
            ).size();

            if (invaders == 0) {
                broadcast(
                        level,
                        "§a§l[Guerra] §r§a" + war.def.display
                                + " repeliu a invasão de "
                                + war.atk.display + "!"
                );

                finish(level, war, defenderCenter);
                iterator.remove();

            } else if (defenders == 0) {
                broadcast(
                        level,
                        "§4§l[Guerra] §r§4" + war.atk.display
                                + " dominou " + war.def.display + "!"
                );

                finish(level, war, defenderCenter);
                iterator.remove();

            } else if (war.age > 150) {
                broadcast(
                        level,
                        "§e[Guerra] Impasse entre "
                                + war.atk.display + " e "
                                + war.def.display
                                + ". Trégua declarada."
                );

                finish(level, war, defenderCenter);
                iterator.remove();
            }
        }
    }

    public static void removeVillageWarriors(
            ServerLevel level,
            Village village,
            BlockPos center,
            int radius) {

        for (Mob mob : ninjas(
                level,
                center,
                radius,
                village,
                false
        )) {
            mob.discard();
        }

        for (Mob mob : ninjas(
                level,
                center,
                radius,
                village,
                true
        )) {
            mob.discard();
        }
    }

    private static void finish(
            ServerLevel level,
            WarData.War war,
            BlockPos defenderCenter) {

        for (Mob mob : ninjas(
                level,
                defenderCenter,
                200,
                war.atk,
                true
        )) {
            mob.discard();
        }
    }

    private static void spawnWave(
            ServerLevel level,
            WarData.War war,
            BlockPos defenderCenter) {

        // Kumo mantém apenas o Jōnin líder.
        int[] composition = war.atk == Village.KUMO
                ? new int[]{1, 1, 1, 0, 0, 0}
                : new int[]{2, 1, 1, 0, 0, 0};

        int spawned = 0;

        for (int rank : composition) {
            double angle = level.random.nextDouble() * Math.PI * 2;

            int x = defenderCenter.getX()
                    + (int) (Math.cos(angle) * 30);

            int z = defenderCenter.getZ()
                    + (int) (Math.sin(angle) * 30);

            Mob mob = spawn(
                    level,
                    war.atk,
                    rank,
                    x,
                    z
            );

            if (mob != null) {
                mob.addTag(INVADER_TAG);
                mob.addTag("ninjawars.wave");
                spawned++;
            }
        }

        broadcast(
                level,
                "§c[Guerra] Reforços de " + war.atk.display
                        + " chegando perto de " + war.def.display
                        + " (" + spawned + " ninjas do Narutoto)."
        );
    }
}

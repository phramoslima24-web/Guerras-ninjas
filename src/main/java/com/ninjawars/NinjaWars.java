
package com.ninjawars;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(NinjaWars.MODID)
public class NinjaWars {

    public static final String MODID = "ninjawars";

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<EntityType<NinjaEntity>> NINJA =
            ENTITIES.register("ninja",
                    () -> EntityType.Builder.of(NinjaEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(10)
                            .build("ninja"));

    public NinjaWars() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ENTITIES.register(bus);
        bus.addListener(this::attributes);

        MinecraftForge.EVENT_BUS.register(new GameEvents());
    }

    private void attributes(EntityAttributeCreationEvent e) {
        e.put(NINJA.get(), NinjaEntity.createAttributes().build());
    }

    public static class GameEvents {

        /**
         * Detecta automaticamente as estruturas reais de Konoha,
         * Suna e Kumo do mod Narutoto.
         */
        @SubscribeEvent
        public void villageChunk(ChunkEvent.Load event) {
            if (!(event.getLevel() instanceof ServerLevel level)) return;

            var chunk = event.getChunk();
            WarData data = WarData.get(level);

            var structureRegistry = level.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE);

            for (var entry : chunk.getAllStarts().entrySet()) {

                String id = String.valueOf(
                        structureRegistry.getKey(entry.getKey())
                );

                Village village = switch (id) {
                    case "narutoto:konoha_village" -> Village.KONOHA;
                    case "narutoto:suna_village" -> Village.SUNA;
                    case "narutoto:kumo_village" -> Village.KUMO;
                    default -> null;
                };

                if (village == null
                        || data.destroyed.contains(village)
                        || data.centers.containsKey(village)) {
                    continue;
                }

                StructureStart start = entry.getValue();
                var box = start.getBoundingBox();

                int x = (box.minX() + box.maxX()) / 2;
                int z = (box.minZ() + box.maxZ()) / 2;

                int y = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        x,
                        z
                );

                data.centers.put(village, new BlockPos(x, y, z));
                data.areas.put(village, box);
                data.setDirty();
            }
        }

        @SubscribeEvent
        public void commands(RegisterCommandsEvent e) {
            NWCommands.register(e.getDispatcher());
        }

        @SubscribeEvent
        public void tick(TickEvent.ServerTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            if (e.getServer().getTickCount() % 200 != 0) return;

            WarManager.tick(e.getServer().overworld());
        }

        /**
         * Bloqueia cópias dos líderes gerados naturalmente pelo Narutoto.
         */
        @SubscribeEvent
        public void uniqueVillageLeaders(EntityJoinLevelEvent event) {
            var entity = event.getEntity();

            var id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

            if (id == null || !"narutoto".equals(id.getNamespace())) return;

            String path = id.getPath();

            if ((path.equals("gaara_npc")
                    || path.equals("minato_npc")
                    || path.equals("kumo_jonin"))
                    && WarManager.taggedVillage(entity) == null) {

                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void friendlyKonoha(
                net.minecraftforge.event.entity.living.LivingAttackEvent event) {

            var target = event.getEntity();
            var attacker = event.getSource().getEntity();

            boolean targetKonoha =
                    WarManager.taggedVillage(target) == Village.KONOHA;

            boolean attackerKonoha =
                    attacker != null
                            && WarManager.taggedVillage(attacker) == Village.KONOHA;

            if ((targetKonoha && attacker instanceof ServerPlayer)
                    || (attackerKonoha && target instanceof ServerPlayer)) {

                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void death(LivingDeathEvent e) {
            if (e.getSource().getEntity() instanceof ServerPlayer p) {
                Missions.onKill(p, e.getEntity());
            }
        }

        /**
         * Atril dentro de uma vila = quadro de missões.
         */
        @SubscribeEvent
        public void board(PlayerInteractEvent.RightClickBlock e) {
            if (e.getHand() != InteractionHand.MAIN_HAND) return;
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            if (!e.getLevel().getBlockState(e.getPos()).is(Blocks.LECTERN)) return;

            WarData d = WarData.get((ServerLevel) e.getLevel());

            for (var en : d.centers.entrySet()) {
                BlockPos c = en.getValue();

                if (Math.abs(c.getX() - e.getPos().getX()) <= 14
                        && Math.abs(c.getZ() - e.getPos().getZ()) <= 14) {

                    if (PlayerData.village(p) == null) {
                        PlayerData.setVillage(p, en.getKey());

                        p.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal(
                                        "§aVocê se juntou a "
                                                + en.getKey().display + "!"
                                )
                        );
                    }

                    Missions.giveOrShow(p);
                    e.setCanceled(true);
                    return;
                }
            }
        }
    }
}

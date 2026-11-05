package com.ninjawars;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
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
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final RegistryObject<EntityType<NinjaEntity>> NINJA = ENTITIES.register("ninja",
            () -> EntityType.Builder.of(NinjaEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f).clientTrackingRange(10).build("ninja"));

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
        @SubscribeEvent
        public void commands(RegisterCommandsEvent e) { NWCommands.register(e.getDispatcher()); }

        @SubscribeEvent
        public void tick(TickEvent.ServerTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            if (e.getServer().getTickCount() % 200 != 0) return;
            WarManager.tick(e.getServer().overworld());
        }

        @SubscribeEvent
        public void death(LivingDeathEvent e) {
            if (e.getSource().getEntity() instanceof ServerPlayer p) Missions.onKill(p, e.getEntity());
        }

        /** Atril dentro de uma vila = quadro de missões. */
        @SubscribeEvent
        public void board(PlayerInteractEvent.RightClickBlock e) {
            if (e.getHand() != InteractionHand.MAIN_HAND) return;
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            if (!e.getLevel().getBlockState(e.getPos()).is(Blocks.LECTERN)) return;
            WarData d = WarData.get((ServerLevel) e.getLevel());
            for (var en : d.centers.entrySet()) {
                BlockPos c = en.getValue();
                if (Math.abs(c.getX() - e.getPos().getX()) <= 14 && Math.abs(c.getZ() - e.getPos().getZ()) <= 14) {
                    if (PlayerData.village(p) == null) {
                        PlayerData.setVillage(p, en.getKey());
                        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aVocê se juntou a " + en.getKey().display + "!"));
                    }
                    Missions.giveOrShow(p);
                    e.setCanceled(true);
                    return;
                }
            }
        }
    }
}

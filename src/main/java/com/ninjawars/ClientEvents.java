package com.ninjawars;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NinjaWars.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(NinjaWars.NINJA.get(), NinjaRenderer::new);
    }

    public static class NinjaRenderer extends HumanoidMobRenderer<NinjaEntity, HumanoidModel<NinjaEntity>> {
        public NinjaRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
            addLayer(new HumanoidArmorLayer<>(this,
                    new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                    new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                    ctx.getModelManager()));
        }

        @Override
        public ResourceLocation getTextureLocation(NinjaEntity e) {
            ResourceLocation villageTexture = e.getVillage().texture(e.getRank());
            // Se o Narutoto não estiver instalado ou não fornecer o recurso, usa Steve.
            return net.minecraft.client.Minecraft.getInstance().getResourceManager()
                    .getResource(villageTexture).isPresent()
                    ? villageTexture : e.getVillage().fallbackTexture();
        }
    }
}

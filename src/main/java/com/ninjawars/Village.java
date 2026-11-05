package com.ninjawars;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;

public enum Village {
    KONOHA("konoha", "Konoha", "Hokage", 0xE8731A, "steve", Jutsu.FIRE, ParticleTypes.FLAME,
            Blocks.STONE_BRICKS, Blocks.SPRUCE_PLANKS, Blocks.RED_TERRACOTTA),
    SUNA("suna", "Suna", "Kazekage", 0xD9B96A, "steve", Jutsu.WIND, ParticleTypes.CLOUD,
            Blocks.SMOOTH_SANDSTONE, Blocks.SANDSTONE, Blocks.CUT_SANDSTONE),
    KIRI("kiri", "Kiri", "Mizukage", 0x2E6FD0, "steve", Jutsu.WATER, ParticleTypes.SPLASH,
            Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE, Blocks.DARK_PRISMARINE),
    KUMO("kumo", "Kumo", "Raikage", 0xF2E14B, "steve", Jutsu.LIGHTNING, ParticleTypes.ELECTRIC_SPARK,
            Blocks.POLISHED_ANDESITE, Blocks.WHITE_CONCRETE, Blocks.LIGHT_BLUE_TERRACOTTA),
    IWA("iwa", "Iwa", "Tsuchikage", 0x8A5A3C, "steve", Jutsu.EARTH, ParticleTypes.POOF,
            Blocks.POLISHED_DEEPSLATE, Blocks.BRICKS, Blocks.ORANGE_TERRACOTTA);

    public enum Jutsu { FIRE, WIND, WATER, LIGHTNING, EARTH }

    public final String id, display, kageTitle, skin;
    public final int color;
    public final Jutsu jutsu;
    public final ParticleOptions particle;
    public final Block floor, wall, roof;
    Village(String id, String display, String kageTitle, int color, String skin, Jutsu jutsu,
            ParticleOptions particle, Block floor, Block wall, Block roof) {
        this.id = id; this.display = display; this.kageTitle = kageTitle; this.color = color;
        this.skin = skin; this.jutsu = jutsu; this.particle = particle;
        this.floor = floor; this.wall = wall; this.roof = roof;
    }

    /**
     * Usa as texturas fornecidas pelo Narutoto quando ele estiver instalado.
     * Não copiamos os arquivos do outro mod: apenas referenciamos seu namespace.
     * Rank: 0 Genin, 1 Chunin, 2 Jonin, 3 Anbu, 4 Kage.
     */
    public ResourceLocation texture(int rank) {
        String file = switch (this) {
            case KONOHA -> switch (Math.min(rank, 2)) {
                case 0 -> "anbu_konoha1";
                case 1 -> "anbu_konoha2";
                default -> "anbu_konoha3";
            };
            case SUNA -> switch (Math.min(rank, 2)) {
                case 0 -> "sha_yin_xia_ren_";
                case 1 -> "sha_yin_zhong_ren_";
                default -> "sha_yin_shang_ren_";
            };
            case KIRI -> switch (Math.min(rank, 2)) {
                case 0 -> "kiri_genin";
                case 1 -> "kiri_chunin";
                default -> "kiri_jonin";
            };
            case KUMO -> switch (Math.min(rank, 2)) {
                case 0 -> "kumo_genin";
                case 1 -> "kumo_chunin";
                default -> "kumo_jonin";
            };
            case IWA -> switch (Math.min(rank, 2)) {
                case 0 -> "iwa_genin";
                case 1 -> "iwa_chunin";
                default -> "iwa_jonin";
            };
        };
        return new ResourceLocation("narutoto", "textures/entities/" + file + ".png");
    }

    public ResourceLocation fallbackTexture() {
        return new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");
    }

    public static Village byId(String s) {
        for (Village v : values()) if (v.id.equalsIgnoreCase(s)) return v;
        return null;
    }

    public static Village byIndex(int i) {
        Village[] v = values();
        return v[Math.floorMod(i, v.length)];
    }

    public static Iterable<String> ids() {
        return Arrays.stream(values()).map(v -> v.id).toList();
    }
}

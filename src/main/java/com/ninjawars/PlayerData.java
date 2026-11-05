package com.ninjawars;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class PlayerData {
    private PlayerData() {}

    public static CompoundTag tag(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, 10)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag per = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!per.contains("ninjawars", 10)) per.put("ninjawars", new CompoundTag());
        return per.getCompound("ninjawars");
    }

    public static Village village(Player p) { return Village.byId(tag(p).getString("village")); }
    public static void setVillage(Player p, Village v) { tag(p).putString("village", v.id); }
    public static int xp(Player p) { return tag(p).getInt("xp"); }
    public static int rank(Player p) { return Ranks.forXp(xp(p)); }

    public static void addXp(Player p, int amount) {
        int before = rank(p);
        tag(p).putInt("xp", xp(p) + amount);
        int after = rank(p);
        p.sendSystemMessage(Component.literal("§6+" + amount + " XP ninja §7(" + xp(p) + ")"));
        if (after > before) p.sendSystemMessage(Component.literal("§a§lPromoção! §rAgora você é " + Ranks.NAMES[after] + "."));
    }
}

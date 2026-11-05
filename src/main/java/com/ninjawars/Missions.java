package com.ninjawars;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class Missions {
    private Missions() {}

    public static void giveOrShow(ServerPlayer p) {
        CompoundTag t = PlayerData.tag(p);
        Village v = PlayerData.village(p);
        if (v == null) { p.sendSystemMessage(Component.literal("§cEntre numa vila primeiro: /ninjawars join <vila>")); return; }
        if (t.getInt("mtype") != 0) { show(p); return; }
        ServerLevel l = (ServerLevel) p.level();
        boolean war = WarData.get(l).wars.stream().anyMatch(w -> w.atk == v || w.def == v);
        int rank = PlayerData.rank(p);
        int type = (war && l.random.nextBoolean()) ? 1 : 2;
        int target = 4 + l.random.nextInt(5) + rank * 2;
        t.putInt("mtype", type); t.putInt("mtarget", target); t.putInt("mprog", 0);
        p.sendSystemMessage(Component.literal("§eNova missão aceita!"));
        show(p);
    }

    public static void show(ServerPlayer p) {
        CompoundTag t = PlayerData.tag(p);
        String what = t.getInt("mtype") == 1 ? "ninjas inimigos (vilas em guerra com a sua)" : "monstros hostis";
        p.sendSystemMessage(Component.literal("§bMissão: derrote " + t.getInt("mtarget") + " " + what
                + " §7[" + t.getInt("mprog") + "/" + t.getInt("mtarget") + "]"));
    }

    public static void onKill(ServerPlayer p, LivingEntity victim) {
        CompoundTag t = PlayerData.tag(p);
        int type = t.getInt("mtype");
        if (type == 0) return;
        Village pv = PlayerData.village(p);
        boolean counts;
        if (type == 1) {
            counts = victim instanceof NinjaEntity n && pv != null && n.getVillage() != pv
                    && WarData.get((ServerLevel) p.level()).atWar(pv, n.getVillage());
        } else {
            counts = victim instanceof Monster;
        }
        if (!counts) return;
        int prog = t.getInt("mprog") + 1;
        t.putInt("mprog", prog);
        int target = t.getInt("mtarget");
        if (prog < target) { show(p); return; }
        int emeralds = 3 + PlayerData.rank(p) * 2 + target / 5;
        int xp = target * 8;
        t.putInt("mtype", 0); t.putInt("mtarget", 0); t.putInt("mprog", 0);
        p.getInventory().add(new ItemStack(Items.EMERALD, emeralds));
        p.sendSystemMessage(Component.literal("§a§lMissão concluída! §r+" + emeralds + " esmeraldas."));
        PlayerData.addXp(p, xp);
    }
}

package com.ninjawars;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class WarData extends SavedData {
    public static class War {
        public Village atk, def;
        public int waves = 3, cooldown = 0, age = 0;
        public boolean started = false;
        public War(Village a, Village d) { atk = a; def = d; }
    }

    public final Map<Village, BlockPos> centers = new EnumMap<>(Village.class);
    public final List<War> wars = new ArrayList<>();

    public static WarData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(WarData::load, WarData::new, "ninjawars");
    }

    public boolean atWar(Village a, Village b) {
        for (War w : wars) if ((w.atk == a && w.def == b) || (w.atk == b && w.def == a)) return true;
        return false;
    }

    public boolean declare(Village atk, Village def) {
        if (atk == def || atWar(atk, def) || !centers.containsKey(atk) || !centers.containsKey(def)) return false;
        wars.add(new War(atk, def));
        setDirty();
        return true;
    }

    public static WarData load(CompoundTag tag) {
        WarData d = new WarData();
        for (Tag t : tag.getList("centers", 10)) {
            CompoundTag c = (CompoundTag) t;
            Village v = Village.byId(c.getString("v"));
            if (v != null) d.centers.put(v, NbtUtils.readBlockPos(c.getCompound("pos")));
        }
        for (Tag t : tag.getList("wars", 10)) {
            CompoundTag c = (CompoundTag) t;
            Village a = Village.byId(c.getString("atk")), b = Village.byId(c.getString("def"));
            if (a == null || b == null) continue;
            War w = new War(a, b);
            w.waves = c.getInt("waves"); w.cooldown = c.getInt("cd"); w.age = c.getInt("age");
            w.started = c.getBoolean("started");
            d.wars.add(w);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag cl = new ListTag();
        centers.forEach((v, p) -> {
            CompoundTag c = new CompoundTag();
            c.putString("v", v.id);
            c.put("pos", NbtUtils.writeBlockPos(p));
            cl.add(c);
        });
        tag.put("centers", cl);
        ListTag wl = new ListTag();
        for (War w : wars) {
            CompoundTag c = new CompoundTag();
            c.putString("atk", w.atk.id); c.putString("def", w.def.id);
            c.putInt("waves", w.waves); c.putInt("cd", w.cooldown); c.putInt("age", w.age);
            c.putBoolean("started", w.started);
            wl.add(c);
        }
        tag.put("wars", wl);
        return tag;
    }
}

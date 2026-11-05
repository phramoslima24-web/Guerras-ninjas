package com.ninjawars;

public final class Ranks {
    public static final String[] NAMES = {"Genin", "Chunin", "Jonin", "Anbu", "Kage"};
    public static final int[] XP = {0, 100, 300, 700, 1500};
    private Ranks() {}

    public static int forXp(int xp) {
        int r = 0;
        for (int i = 0; i < XP.length; i++) if (xp >= XP[i]) r = i;
        return r;
    }
}

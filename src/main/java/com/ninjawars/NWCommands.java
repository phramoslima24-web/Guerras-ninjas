package com.ninjawars;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class NWCommands {
    private static final SimpleCommandExceptionType BAD = new SimpleCommandExceptionType(Component.literal("Vila desconhecida."));
    private static final SimpleCommandExceptionType NOT_OW = new SimpleCommandExceptionType(Component.literal("Use no Overworld."));
    private NWCommands() {}

    private static RequiredArgumentBuilder<CommandSourceStack, String> vArg(String n) {
        return Commands.argument(n, StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Village.ids(), b));
    }

    private static Village v(CommandContext<CommandSourceStack> c, String n) throws CommandSyntaxException {
        Village v = Village.byId(StringArgumentType.getString(c, n));
        if (v == null) throw BAD.create();
        return v;
    }

    private static void msg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("ninjawars")
                .then(Commands.literal("list").executes(NWCommands::list))
                .then(Commands.literal("status").executes(NWCommands::status))
                .then(Commands.literal("mission").executes(c -> { Missions.giveOrShow(c.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("join").then(vArg("village").executes(NWCommands::join)))
                .then(Commands.literal("village").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("set").then(vArg("village").executes(NWCommands::setVillage)))
                        // "create" fica como alias de compatibilidade, mas não constrói uma vila nova.
                        .then(Commands.literal("create").then(vArg("village").executes(NWCommands::setVillage)))
                        .then(Commands.literal("remove").then(vArg("village").executes(NWCommands::remove))))
                .then(Commands.literal("war").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("declare").then(vArg("a").then(vArg("b").executes(NWCommands::declare))))
                        .then(Commands.literal("peace").then(vArg("a").then(vArg("b").executes(NWCommands::peace)))))
                .then(Commands.literal("spawn").requires(s -> s.hasPermission(2))
                        .then(vArg("village").then(Commands.argument("rank", IntegerArgumentType.integer(0, 4)).executes(NWCommands::spawn)))));
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        WarData d = WarData.get(c.getSource().getLevel());
        StringBuilder sb = new StringBuilder("§6== Vilas ==");
        for (Village v : Village.values()) {
            BlockPos p = d.centers.get(v);
            sb.append("\n§e").append(v.display).append(" §7(").append(v.kageTitle).append(") ")
              .append(p == null ? "§8não fundada" : "§f" + p.getX() + " " + p.getY() + " " + p.getZ());
        }
        sb.append("\n§c== Guerras ==");
        if (d.wars.isEmpty()) sb.append("\n§aNenhuma");
        for (WarData.War w : d.wars) sb.append("\n§c").append(w.atk.display).append(" → ").append(w.def.display)
                .append(" §7(ondas restantes: ").append(w.waves).append(")");
        msg(c, sb.toString());
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Village v = PlayerData.village(p);
        int xp = PlayerData.xp(p), r = PlayerData.rank(p);
        String next = r < 4 ? " (próximo rank: " + Ranks.XP[r + 1] + " XP)" : "";
        msg(c, "§6Vila: §f" + (v == null ? "nenhuma" : v.display) + " §6Rank: §f" + Ranks.NAMES[r] + " §6XP: §f" + xp + next);
        return 1;
    }

    private static int join(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Village v = v(c, "village");
        PlayerData.setVillage(p, v);
        msg(c, "§aVocê agora é ninja de " + v.display + ".");
        return 1;
    }

    private static int setVillage(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        if (player.level().dimension() != Level.OVERWORLD) throw NOT_OW.create();
        Village village = v(c, "village");
        ServerLevel level = (ServerLevel) player.level();
        BlockPos position = player.blockPosition();

        WarData data = WarData.get(level);
        data.centers.put(village, position);
        data.setDirty();

        msg(c, "§aCentro de " + village.display + " registrado em "
                + position.getX() + " " + position.getY() + " " + position.getZ()
                + ". O Ninja Wars usará a vila e os ninjas do Narutoto; nenhum bloco foi construído.");
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Village v = v(c, "village");
        ServerLevel l = c.getSource().getServer().overworld();
        WarData d = WarData.get(l);
        BlockPos pos = d.centers.remove(v);
        d.wars.removeIf(w -> w.atk == v || w.def == v);
        d.setDirty();
        if (pos != null) WarManager.removeVillageWarriors(l, v, pos, 200);
        msg(c, "§eVila " + v.display + " removida do registro (blocos permanecem).");
        return 1;
    }

    private static int declare(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Village a = v(c, "a"), b = v(c, "b");
        ServerLevel l = c.getSource().getServer().overworld();
        if (WarData.get(l).declare(a, b)) WarManager.broadcast(l, "§c§l[Guerra] §r§c" + a.display + " declarou guerra contra " + b.display + "!");
        else msg(c, "§cNão foi possível (vilas iguais, sem fundação ou já em guerra).");
        return 1;
    }

    private static int peace(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Village a = v(c, "a"), b = v(c, "b");
        ServerLevel l = c.getSource().getServer().overworld();
        WarData d = WarData.get(l);
        d.wars.removeIf(w -> (w.atk == a && w.def == b) || (w.atk == b && w.def == a));
        d.setDirty();
        for (Village x : new Village[]{a, b}) {
            BlockPos p = d.centers.get(x);
            if (p != null) WarManager.removeVillageWarriors(l, x, p, 200);
        }
        WarManager.broadcast(l, "§a[Paz] " + a.display + " e " + b.display + " firmaram a paz.");
        return 1;
    }

    private static int spawn(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Village v = v(c, "village");
        int rank = IntegerArgumentType.getInteger(c, "rank");
        var ninja = WarManager.spawn((ServerLevel) p.level(), v, rank, p.getBlockX(), p.getBlockZ());
        msg(c, ninja == null ? "§cNão foi possível criar o ninja. Confira se o Narutoto 0.4.1 está instalado."
                : "§aNinja do Narutoto criado.");
        return 1;
    }
}

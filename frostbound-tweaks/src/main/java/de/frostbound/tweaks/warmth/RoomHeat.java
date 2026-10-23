package de.frostbound.tweaks.warmth;

import com.momosoftworks.coldsweat.api.event.core.init.DefaultTempModifiersEvent;
import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import com.momosoftworks.coldsweat.api.event.vanilla.BlockStateChangedEvent;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Matcher;
import com.momosoftworks.coldsweat.api.util.placement.Placement;
import com.momosoftworks.coldsweat.util.entity.DummyPlayer;
import de.frostbound.tweaks.FrostboundTweaks;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Raumwärme: Öfen, Herde, Lagerfeuer, Fackeln, Kerzen, Laternen und der Hearth heizen den geschlossenen Raum,
 * in dem sie stehen. Wärme = Summe(Leistung × Ladung) / Raumvolumen × Leckfaktor, als eigener Cold-Sweat-Modifier.
 * Wärmespeicher-Quellen laden sich beim Brennen auf und kühlen danach langsam ab (HeatStore), Ziegel und Stein
 * um die Feuerstelle verlängern das. Nur geladen, wenn Cold Sweat da ist.
 */
public final class RoomHeat {
    static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(FrostboundTweaks.MOD_ID, "room_heat");
    private static final int PRUNE_INTERVAL = 6000;

    /** Letzte Vermessung je Spieler. */
    private record Scan(ResourceKey<Level> dimension, long pos, long tick, RoomScanner.Room room) {}

    private static final Map<UUID, Scan> SCANS = new HashMap<>();

    private RoomHeat() {}

    public static void init() {
        NeoForge.EVENT_BUS.register(RoomHeat.class);
    }

    @SubscribeEvent
    public static void registerModifier(TempModifierRegisterEvent event) {
        event.register(MODIFIER_ID, RoomHeatTempModifier::new);
    }

    @SubscribeEvent
    public static void addToPlayers(DefaultTempModifiersEvent event) {
        if (event.getEntity() instanceof Player && !(event.getEntity() instanceof DummyPlayer)) {
            event.addModifier(Temperature.Trait.WORLD, new RoomHeatTempModifier().tickRate(20),
                    Placement.LAST.noDuplicates(Matcher.SAME_CLASS));
        }
    }

    /** Wärmespeicher mitführen, wenn eine Quelle an- oder ausgeht oder abgebaut wird (auch ohne Spieler daneben). */
    @SubscribeEvent
    public static void onBlockChanged(BlockStateChangedEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        RoomHeatConfig.Source oldSource = RoomHeatConfig.storageFor(event.getOldState());
        RoomHeatConfig.Source newSource = RoomHeatConfig.storageFor(event.getNewState());
        if (oldSource == null && newSource == null) return;
        HeatStore store = HeatStore.get(level);
        long pos = event.getPosition().asLong();
        if (newSource == null) {
            store.remove(pos);
        } else {
            store.setActive(pos, newSource.isActive(event.getNewState()), level.getGameTime());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % PRUNE_INTERVAL == 0) {
            HeatStore.get(level).prune(level, level.getGameTime());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SCANS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        RoomHeatConfig.invalidate();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("raumwaerme").executes(ctx -> report(ctx.getSource())));
    }

    /** Raumwärme in °F für den Spieler; vermisst den Raum neu, wenn er sich bewegt hat oder die Messung alt ist. */
    static double heatFor(ServerPlayer player) {
        if (!RoomHeatConfig.ENABLED.get()) return 0;
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        BlockPos pos = player.blockPosition();
        Scan scan = SCANS.get(player.getUUID());
        if (scan == null || scan.dimension() != level.dimension() || scan.pos() != pos.asLong()
                || now - scan.tick() >= RoomHeatConfig.SCAN_INTERVAL.get()) {
            scan = new Scan(level.dimension(), pos.asLong(), now, measure(level, pos, now));
            SCANS.put(player.getUUID(), scan);
        }
        return evaluate(level, scan.room(), now, null);
    }

    /** Raum vermessen und die gefundenen Wärmespeicher mit Zustand und Speichermasse im HeatStore eintragen. */
    private static RoomScanner.Room measure(ServerLevel level, BlockPos pos, long now) {
        RoomScanner.Room room = RoomScanner.scan(level, pos);
        if (!room.heatable()) return room;
        HeatStore store = HeatStore.get(level);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (long key : room.sources()) {
            p.set(key);
            BlockState state = level.getBlockState(p);
            RoomHeatConfig.Source source = RoomHeatConfig.storageFor(state);
            if (source != null) store.observe(key, source.isActive(state), countMass(level, p), now);
        }
        return room;
    }

    private static int countMass(ServerLevel level, BlockPos center) {
        int mass = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (!p.equals(center) && RoomHeatConfig.isMass(level.getBlockState(p))) mass++;
        }
        return mass;
    }

    /** Beitrag einer Blockart (bei Speichern je Ladestand) für /raumwaerme. */
    private static final class Detail {
        final Block block;
        final String suffix;
        int count;
        double heat;

        Detail(Block block, String suffix) {
            this.block = block;
            this.suffix = suffix;
        }
    }

    /** Raumwärme in °F; mit details werden die Beiträge je Blockart gesammelt. */
    private static double evaluate(ServerLevel level, RoomScanner.Room room, long now, Map<String, Detail> details) {
        if (!room.heatable()) return 0;
        HeatStore store = HeatStore.get(level);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        double divisor = Math.max(room.volume(), RoomHeatConfig.MIN_VOLUME.get());
        double factor = room.leakFactor();
        double sum = 0;
        for (long key : room.sources()) {
            p.set(key);
            BlockState state = level.getBlockState(p);
            for (RoomHeatConfig.Source source : RoomHeatConfig.sourcesFor(state)) {
                double share;
                if (source.stores) {
                    share = store.charge(key, now);
                } else if (source.isActive(state)) {
                    share = 1;
                } else {
                    continue;
                }
                double heat = source.power * source.multiplier(state) * share;
                sum += heat;
                if (details != null) {
                    String suffix = source.stores
                            ? String.format(Locale.GERMANY, " (Speicher %.0f %%, Masse %d)", share * 100, store.mass(key)) : "";
                    Block block = state.getBlock();
                    Detail d = details.computeIfAbsent(block.getDescriptionId() + suffix, k -> new Detail(block, suffix));
                    d.count++;
                    d.heat += heat / divisor * factor;
                }
                break;
            }
        }
        return Math.min(RoomHeatConfig.MAX_HEAT.get(), sum / divisor * factor);
    }

    /** /raumwaerme: Raum an der eigenen Position vermessen und die Wärme aufschlüsseln. */
    private static int report(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        BlockPos pos = BlockPos.containing(src.getPosition());
        long now = level.getGameTime();
        RoomScanner.Room room = measure(level, pos, now);
        if (room.open()) {
            String why = room.leaks() > 0 ? " (" + room.leaks() + " Lecks)" : "";
            src.sendSuccess(() -> Component.literal("Kein geschlossener Raum, hier ist man im Freien" + why + "."), false);
            return 0;
        }
        if (room.tooBig()) {
            src.sendSuccess(() -> Component.literal("Raum zu groß zum Heizen (über "
                    + RoomHeatConfig.MAX_VOLUME.get() + " Luftblöcke)."), false);
            return 0;
        }
        Map<String, Detail> details = new LinkedHashMap<>();
        double heat = evaluate(level, room, now, details);
        // Blocknamen als Übersetzungsschlüssel, damit der Client sie in seiner Sprache zeigt
        MutableComponent msg = Component.literal(String.format(Locale.GERMANY, "Raum: %d Luftblöcke, %d Lecks (Wärme ×%.2f)",
                room.volume(), room.leaks(), room.leakFactor()));
        for (Detail d : details.values()) {
            msg.append("\n  " + d.count + "× ").append(Component.translatable(d.block.getDescriptionId()))
                    .append(String.format(Locale.GERMANY, "%s: +%.1f °F", d.suffix, d.heat));
        }
        if (details.isEmpty()) msg.append("\n  keine Wärmequellen");
        msg.append(String.format(Locale.GERMANY, "\nRaumwärme: +%.1f °F (+%.1f °C), höchstens bis %.0f °F", heat,
                heat / 1.8, RoomHeatConfig.ROOM_TEMP_LIMIT.get()));
        src.sendSuccess(() -> msg, false);
        return (int) Math.round(heat);
    }
}

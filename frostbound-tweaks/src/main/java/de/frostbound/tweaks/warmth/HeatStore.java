package de.frostbound.tweaks.warmth;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Wärmespeicher je Quelle (Ofen, Herd, Lagerfeuer, Hearth …), gespeichert pro Dimension.
 * Es wird nichts getickt: Jeder Eintrag merkt sich Ladung, Zeitpunkt und ob die Quelle brennt;
 * die aktuelle Ladung wird beim Abfragen aus der vergangenen Zeit berechnet (exponentielles Laden/Abkühlen).
 */
final class HeatStore extends SavedData {
    private static final String NAME = "frostbound_room_heat";
    private static final double TICKS_PER_MINUTE = 1200.0;

    private final Long2ObjectOpenHashMap<Entry> entries = new Long2ObjectOpenHashMap<>();

    static final class Entry {
        double charge;
        long time;
        boolean active;
        int mass;
    }

    static HeatStore get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(HeatStore::new, HeatStore::load, null), NAME);
    }

    /** Ladung (0–1) zum Zeitpunkt now, ohne den Eintrag zu verändern; 0 ohne Eintrag. */
    double charge(long pos, long now) {
        Entry e = entries.get(pos);
        return e == null ? 0 : chargeAt(e, now);
    }

    int mass(long pos) {
        Entry e = entries.get(pos);
        return e == null ? 0 : e.mass;
    }

    /** Beim Vermessen gesehen: Eintrag anlegen bzw. Zustand und Speichermasse nachführen. */
    void observe(long pos, boolean active, int mass, long now) {
        Entry e = entries.get(pos);
        if (e != null && e.active == active && e.mass == mass) return;
        if (e == null) {
            e = new Entry();
            e.time = now;
            entries.put(pos, e);
        } else {
            settle(e, now);
        }
        e.active = active;
        e.mass = mass;
        setDirty();
    }

    /** Quelle ist an- oder ausgegangen. */
    void setActive(long pos, boolean active, long now) {
        Entry e = entries.get(pos);
        if (e != null && e.active == active) return;
        if (e == null) {
            e = new Entry();
            e.time = now;
            entries.put(pos, e);
        } else {
            settle(e, now);
        }
        e.active = active;
        setDirty();
    }

    void remove(long pos) {
        if (entries.remove(pos) != null) setDirty();
    }

    /** Ausgekühlte Einträge und solche, deren Block (in geladenen Chunks) keine Wärmequelle mehr ist, entfernen. */
    void prune(ServerLevel level, long now) {
        var it = entries.long2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            Long2ObjectMap.Entry<Entry> le = it.next();
            Entry e = le.getValue();
            BlockPos pos = BlockPos.of(le.getLongKey());
            boolean gone = level.isLoaded(pos) && RoomHeatConfig.storageFor(level.getBlockState(pos)) == null;
            if (gone || (!e.active && chargeAt(e, now) < 0.01)) {
                it.remove();
                setDirty();
            }
        }
    }

    int size() {
        return entries.size();
    }

    private static void settle(Entry e, long now) {
        e.charge = chargeAt(e, now);
        e.time = now;
    }

    private static double chargeAt(Entry e, long now) {
        double minutes = Math.max(0, now - e.time) / TICKS_PER_MINUTE;
        if (e.active) {
            return 1 - (1 - e.charge) * Math.exp(-minutes / RoomHeatConfig.CHARGE_MINUTES.get());
        }
        double tau = RoomHeatConfig.STORAGE_MINUTES.get()
                * (1 + RoomHeatConfig.MASS_BONUS.get() * Math.min(e.mass, RoomHeatConfig.MASS_MAX.get()));
        return e.charge * Math.exp(-minutes / tau);
    }

    private static HeatStore load(CompoundTag tag, HolderLookup.Provider provider) {
        HeatStore store = new HeatStore();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Entry e = new Entry();
            e.charge = t.getDouble("charge");
            e.time = t.getLong("time");
            e.active = t.getBoolean("active");
            e.mass = t.getInt("mass");
            store.entries.put(t.getLong("pos"), e);
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Long2ObjectMap.Entry<Entry> le : entries.long2ObjectEntrySet()) {
            Entry e = le.getValue();
            CompoundTag t = new CompoundTag();
            t.putLong("pos", le.getLongKey());
            t.putDouble("charge", e.charge);
            t.putLong("time", e.time);
            t.putBoolean("active", e.active);
            t.putInt("mass", e.mass);
            list.add(t);
        }
        tag.put("entries", list);
        return tag;
    }
}

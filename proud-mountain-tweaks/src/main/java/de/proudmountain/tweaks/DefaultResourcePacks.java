package de.proudmountain.tweaks;

import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.repository.PackRepository;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.resource.ResourcePackLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Mitgelieferte Resource Packs einmalig einschalten, ohne sie zu erzwingen (nur Client).
 * config/proudmountain_tweaks/default_resourcepacks.txt listet "Schlüssel Pack-ID" von unten nach oben.
 * Jeder Schlüssel wird nur einmal eingeschaltet und danach in applied_resourcepacks.txt vermerkt,
 * wer ein Pack abwählt, behält das so. Neue Datei für denselben Schlüssel (Pack-Update): Die alte
 * Pack-ID wird an ihrer Stelle ersetzt, aber nur, wenn sie noch eingeschaltet ist.
 * Vermerkt wird erst beim nächsten Speichern der options.txt, damit beides zusammenpasst.
 */
public final class DefaultResourcePacks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DEFAULTS = "default_resourcepacks.txt";
    private static final String APPLIED = "applied_resourcepacks.txt";

    private static boolean done;
    private static final Map<String, String> pending = new LinkedHashMap<>();

    private DefaultResourcePacks() {
    }

    /** Vor Options.loadSelectedResourcePacks: fehlende Standard-Packs in die Auswahl eintragen. */
    public static void apply(List<String> selected, PackRepository repository) {
        if (done) {
            return;
        }
        done = true;
        pending.putAll(select(selected, id -> repository.getPack(id) != null, read(dir().resolve(DEFAULTS)), read(dir().resolve(APPLIED))));
    }

    /** Trägt in selected ein bzw. ersetzt und liefert, was danach als eingeschaltet vermerkt werden soll. */
    static Map<String, String> select(List<String> selected, Predicate<String> exists, Map<String, String> defaults, Map<String, String> applied) {
        Map<String, String> changed = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(defaults.keySet());
        for (int k = 0; k < keys.size(); k++) {
            String key = keys.get(k);
            String id = defaults.get(key);
            if (!exists.test(id)) {
                continue;
            }
            String old = applied.get(key);
            if (old == null) {
                if (!selected.contains(id)) {
                    // Ohne Eintrag setzt NeoForge die Mod-Ressourcen ganz nach oben, über die Packs
                    if (!selected.contains(ResourcePackLoader.MOD_RESOURCES_ID)) {
                        selected.add(ResourcePackLoader.MOD_RESOURCES_ID);
                    }
                    selected.add(insertIndex(selected, keys.subList(k + 1, keys.size()), defaults), id);
                    LOGGER.info("Resource Pack {} eingeschaltet", id);
                }
            } else if (!old.equals(id)) {
                int i = selected.indexOf(old);
                if (i >= 0 && !selected.contains(id)) {
                    selected.set(i, id);
                    LOGGER.info("Resource Pack {} ersetzt durch {}", old, id);
                }
            } else {
                continue;
            }
            changed.put(key, id);
        }
        return changed;
    }

    /** Nach Options.save: eingeschaltete Packs vermerken. */
    public static void saved() {
        if (pending.isEmpty()) {
            return;
        }
        Path file = dir().resolve(APPLIED);
        Map<String, String> applied = read(file);
        applied.putAll(pending);
        pending.clear();
        StringBuilder out = new StringBuilder("# Von Proud Mountain Tweaks einmal eingeschaltete Resource Packs (Schlüssel Pack-ID)\n");
        applied.forEach((key, id) -> out.append(key).append(' ').append(id).append('\n'));
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Konnte {} nicht schreiben", file, e);
        }
    }

    // Unter das nächste schon eingeschaltete Standard-Pack, das weiter oben stehen soll, sonst ganz nach oben
    private static int insertIndex(List<String> selected, List<String> laterKeys, Map<String, String> defaults) {
        for (String later : laterKeys) {
            int i = selected.indexOf(defaults.get(later));
            if (i >= 0) {
                return i;
            }
        }
        return selected.size();
    }

    private static Path dir() {
        return FMLPaths.CONFIGDIR.get().resolve(ProudMountainTweaks.MOD_ID);
    }

    // Zeilen "Schlüssel Pack-ID", die Pack-ID darf Leerzeichen enthalten; # leitet Kommentare ein
    static Map<String, String> read(Path file) {
        Map<String, String> map = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return map;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                line = line.strip();
                int space = line.indexOf(' ');
                if (line.isEmpty() || line.startsWith("#") || space < 0) {
                    continue;
                }
                map.put(line.substring(0, space), line.substring(space + 1).strip());
            }
        } catch (IOException e) {
            LOGGER.warn("Konnte {} nicht lesen", file, e);
        }
        return map;
    }
}

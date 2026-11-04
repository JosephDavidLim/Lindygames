package lindefense.sim;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import lindefense.tower.TowerType;

/**
 * The player's permanently unlocked Hero Store heroes. Everyone starts with none; buying a hero in the Hero Store
 * ({@link Game#purchaseHero}) adds it here and, for the {@link #persistent()} collection the windowed game uses, saves
 * it with {@code java.util.prefs} (node {@code lindefense/heroes}) so it survives restarts. Tests and tools use
 * {@link #inMemory()}, which never touches the user's preferences.
 *
 * <p>Persistence is best effort: if the preference store is unavailable the collection simply behaves like an
 * in-memory one for this run.</p>
 */
public final class HeroCollection {
    /** Where unlocks are stored. */
    public interface Storage {
        Set<TowerType> load();
        void save(Set<TowerType> unlocked);
    }

    private static final String PREFS_NODE = "lindefense/heroes";
    private static final String PREFS_KEY = "unlocked";
    private static HeroCollection shared;

    private final EnumSet<TowerType> unlocked = EnumSet.noneOf(TowerType.class);
    private final Storage storage;

    private HeroCollection(Storage storage) {
        this.storage = storage;
        if (storage != null) {
            try {
                for (TowerType t : storage.load()) if (t.isHero()) unlocked.add(t);
            } catch (RuntimeException ignored) {
                // corrupt or unavailable store: start empty
            }
        }
    }

    /** An empty collection that is never saved (tests, tools, bots). */
    public static HeroCollection inMemory() { return new HeroCollection(null); }

    /** A collection backed by a custom store (tests of the persistence path). */
    public static HeroCollection withStorage(Storage storage) { return new HeroCollection(storage); }

    /** The windowed game's collection, loaded once from and saved to the user's preferences. */
    public static synchronized HeroCollection persistent() {
        if (shared == null) shared = new HeroCollection(new PrefsStorage());
        return shared;
    }

    public boolean isUnlocked(TowerType type) { return unlocked.contains(type); }

    /** Read-only view of the unlocked heroes. */
    public Set<TowerType> unlocked() { return Collections.unmodifiableSet(unlocked); }

    /** Adds a hero (no cost handling: see {@link Game#purchaseHero}). Returns false if it was already unlocked. */
    public boolean unlock(TowerType type) {
        if (type == null || !type.isHero() || !unlocked.add(type)) return false;
        save();
        return true;
    }

    private void save() {
        if (storage == null) return;
        try {
            storage.save(EnumSet.copyOf(unlocked));
        } catch (RuntimeException ignored) {
            // best effort
        }
    }

    /** Comma-separated enum names in {@code java.util.prefs}. */
    private static final class PrefsStorage implements Storage {
        @Override public Set<TowerType> load() {
            EnumSet<TowerType> out = EnumSet.noneOf(TowerType.class);
            try {
                String raw = java.util.prefs.Preferences.userRoot().node(PREFS_NODE).get(PREFS_KEY, "");
                for (String s : raw.split(",")) {
                    String k = s.trim();
                    if (k.isEmpty()) continue;
                    try { out.add(TowerType.valueOf(k)); } catch (IllegalArgumentException ignored) { /* removed hero */ }
                }
            } catch (RuntimeException ignored) {
                // no preference backend
            }
            return out;
        }

        @Override public void save(Set<TowerType> set) {
            StringBuilder sb = new StringBuilder();
            for (TowerType t : set) { if (sb.length() > 0) sb.append(','); sb.append(t.name()); }
            try {
                java.util.prefs.Preferences p = java.util.prefs.Preferences.userRoot().node(PREFS_NODE);
                p.put(PREFS_KEY, sb.toString());
                p.flush();
            } catch (Exception ignored) {
                // best effort
            }
        }
    }
}

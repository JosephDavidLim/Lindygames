package lindefense.map;

import java.util.ArrayList;
import java.util.List;

/** Factory for the four campaign maps. Every call returns fresh map instances. */
public final class MapCatalog {
    private MapCatalog() { }

    public static final int MAP_COUNT = 4;

    /** Creates a new instance of map {@code index} (0..3). */
    public static GameMap create(int index) {
        switch (index) {
            case 0: return new SiliconCircuit();
            case 1: return new LegacyMonolith();
            case 2: return new HallucinationCanyon();
            case 3: return new SingularityCore();
            default: throw new IllegalArgumentException("No map " + index);
        }
    }

    /** The catalog index of {@code map} (matched by name), or -1 if it is not a catalog map. */
    public static int indexOf(GameMap map) {
        for (int i = 0; i < MAP_COUNT; i++) if (create(i).getName().equals(map.getName())) return i;
        return -1;
    }

    /** Fresh instances of all maps (for menus and tests). */
    public static List<GameMap> createAll() {
        List<GameMap> all = new ArrayList<>();
        for (int i = 0; i < MAP_COUNT; i++) all.add(create(i));
        return all;
    }
}

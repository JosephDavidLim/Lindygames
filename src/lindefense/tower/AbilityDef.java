package lindefense.tower;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data for a tower's active ability: name, cooldown (game seconds), whether the player must pick a location,
 * and a named-parameter map (duration, damage, radius, ...). The effect itself is implemented by the tower's
 * {@code TowerBehavior.activateAbility}; behaviours read numbers with {@link #p(String)}.
 */
public class AbilityDef {
    public final String name;
    public final String description;
    public final double cooldown;
    /** True when the ability needs a player-chosen map point ({@code Game.activateAbility(tower, point)}). */
    public final boolean needsTarget;
    private final Map<String, Double> params;

    public AbilityDef(String name, String description, double cooldown, boolean needsTarget, Map<String, Double> params) {
        this.name = name; this.description = description; this.cooldown = cooldown; this.needsTarget = needsTarget;
        this.params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }

    /** Named parameter; throws if missing so data typos fail loudly. */
    public double p(String key) {
        Double v = params.get(key);
        if (v == null) throw new IllegalArgumentException("Ability " + name + " has no parameter '" + key + "'");
        return v;
    }
    public double p(String key, double dflt) { Double v = params.get(key); return v == null ? dflt : v; }
    public boolean has(String key) { return params.containsKey(key); }
    public Map<String, Double> params() { return params; }
}

package lindefense.combat;

/**
 * Damage categories (spec 3.0). Only three enemy properties interact with them:
 * <ul>
 *   <li>SHARP cannot damage Lead-type enemies (unless the enemy is ARMOR CRACKED or the hit is flagged
 *       {@code ignoresResistance}).</li>
 *   <li>BLUNT, KINETIC and SONIC deal x2 against Ceramic-type enemies (BLUNT and KINETIC are identical rules;
 *       both names exist only for flavour).</li>
 *   <li>SONIC additionally knocks Lead-type enemies back 20 px per hit.</li>
 *   <li>EXPLOSIVE hits splash radius and cannot damage Camo enemies unless the source has detection.</li>
 *   <li>GRAVITY slows on hit (the hit's slow fraction); MAGIC and ENERGY have no special rules.</li>
 * </ul>
 * {@link #TRUE} is internal (map hazards, burn ticks, test helpers): it ignores every rule above, armour and marks.
 */
public enum DamageType {
    SHARP("Sharp"),
    BLUNT("Blunt"),
    KINETIC("Kinetic"),
    ENERGY("Energy"),
    EXPLOSIVE("Explosive"),
    SONIC("Sonic"),
    MAGIC("Magic"),
    GRAVITY("Gravity"),
    /** Internal: ignores all resistances, immunities, armour and marks. Never used by towers. */
    TRUE("True");

    public final String label;
    DamageType(String label) { this.label = label; }

    /** True for BLUNT and KINETIC (identical rules). */
    public boolean isBluntLike() { return this == BLUNT || this == KINETIC; }
}

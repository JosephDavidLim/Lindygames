package lindefense.enemy;

/**
 * Modifiers are flags applied to existing enemies, never separate species.
 * <ul>
 *   <li>CAMO: cannot be targeted by towers without camo detection (unless revealed).
 *       Inherited by all children.</li>
 *   <li>REGROW: regenerates 1 HP every {@link Enemy#REGROW_INTERVAL} s up to max HP.
 *       Inherited by all children.</li>
 *   <li>FORTIFIED: doubles max HP and shows armour plating. Inherited only by boss
 *       children (boss-class parents), never by ordinary splitter children.</li>
 * </ul>
 */
public enum Modifier {
    CAMO("Camo"), REGROW("Regrow"), FORTIFIED("Fortified");

    public final String label;
    Modifier(String label) { this.label = label; }
}

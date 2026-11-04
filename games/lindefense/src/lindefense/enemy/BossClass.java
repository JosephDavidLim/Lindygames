package lindefense.enemy;

/** BTD6-equivalent boss classes. */
public enum BossClass {
    MOAB("MOAB"), BFB("BFB"), ZOMG("ZOMG"), DDT("DDT"), BAD("BAD");
    public final String label;
    BossClass(String label) { this.label = label; }
}

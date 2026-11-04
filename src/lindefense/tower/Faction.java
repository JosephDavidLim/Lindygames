package lindefense.tower;

/** Tower factions used to organise the shop and for faction-specific synergies. */
public enum Faction {
    HERO("Heroes", 0xffd166),
    PRIMARY("Primary", 0x7fd1ff),
    MILITARY("Military", 0xa0c878),
    MAGIC("Magic", 0xff9ad5),
    SUPPORT("Support", 0xc9b6ff),
    TITAN("Titans", 0xffb27a);

    public final String label;
    public final int rgb;
    Faction(String label, int rgb) { this.label = label; this.rgb = rgb; }
}

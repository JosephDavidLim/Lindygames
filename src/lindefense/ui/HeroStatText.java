package lindefense.ui;

import static lindefense.tower.TowerStats.toUnits;

import java.util.ArrayList;
import java.util.List;
import lindefense.sim.Game;
import lindefense.tower.AbilityDef;
import lindefense.tower.Tower;
import lindefense.tower.TowerCatalog;
import lindefense.tower.TowerDef;
import lindefense.tower.TowerStats;
import lindefense.tower.TowerType;
import lindefense.tower.behavior.BradLindemannBehavior;
import lindefense.tower.behavior.BrandonBehavior;
import lindefense.tower.behavior.OwenBehavior;
import lindefense.tower.behavior.ZethosBehavior;

/**
 * Text for the Hero Store cards and the info panel of the Hero Store heroes and Zethos. Every number is read from the
 * data table (never typed here), and distances are shown in the specification's map units.
 */
public final class HeroStatText {
    private HeroStatText() { }

    /** Whether a type's stats are specified in map units (heroes and Zethos). */
    public static boolean usesUnits(TowerType t) { return t.isHero() || t == TowerType.ZETHOS; }

    /** Range label: "220" px for standard towers, "55u" for heroes and Zethos. */
    public static String range(TowerType t, double px) {
        return usesUnits(t) ? Ui.fmt(toUnits(px)) + "u" : String.valueOf((int) px);
    }

    private static String u(double px) { return Ui.fmt(toUnits(px)); }

    /** Combat role line ("Legendary Area-Damage Commander"). */
    public static String role(TowerType t) { return TowerCatalog.get(t).subtitle; }

    /** Name of the primary attack. */
    public static String attackName(TowerType t) {
        switch (t) {
            case BRAD_LINDEMANN: return "Command Lightning";
            case BRANDON: return "Rapid Volley";
            case ROHAN: return "Cryo Blast";
            case RONIL: return "Meteor Strike";
            case SHASHANK: return "Data Drain";
            case OWEN: return "Drone Squadron";
            case ZETHOS: return "Quantum Pulse";
            default: return "Attack";
        }
    }

    /** Primary attack statistics, two to three short lines. */
    public static List<String> attackLines(TowerType t) {
        TowerDef d = TowerCatalog.get(t);
        TowerStats s = d.baseStats;
        List<String> l = new ArrayList<>();
        String core = Ui.fmt(s.hit.damage) + " dmg  •  every " + Ui.fmt(s.interval()) + "s  •  range " + u(s.range) + "u";
        switch (t) {
            case BRAD_LINDEMANN:
                l.add(core);
                l.add("Splash " + u(s.splashRadius) + "u (100%)  •  speed " + u(s.projectileSpeed) + "u/s");
                l.add("Chains " + s.chainHops + " x " + Ui.fmt(s.p("chainDamage")) + " dmg, " + u(s.chainRange) + "u hops");
                break;
            case BRANDON:
                l.add(core);
                l.add("Pierce " + s.pierce + "  •  speed " + u(s.projectileSpeed) + "u/s  •  strongest first");
                break;
            case ROHAN:
                l.add(core);
                l.add("Splash " + u(s.splashRadius) + "u  •  slow " + pct(s.p("slowPct")) + " for " + Ui.fmt(s.p("slowDuration")) + "s");
                l.add(pct(s.p("freezeChance")) + " freeze " + Ui.fmt(s.p("freezeDuration")) + "s  •  bosses: " + pct(s.p("bossSlowPct")) + " slow, no freeze");
                break;
            case RONIL:
                l.add(core);
                l.add("Splash " + u(s.splashRadius) + "u (100%)  •  speed " + u(s.projectileSpeed) + "u/s");
                break;
            case SHASHANK:
                l.add(core);
                l.add("Pierce " + s.pierce + "  •  +" + pct(s.hit.markBonus) + " damage taken for " + Ui.fmt(s.hit.markDuration) + "s");
                break;
            case OWEN:
                l.add(s.pi("drones") + " drones x " + Ui.fmt(s.hit.damage) + " dmg  •  every " + Ui.fmt(s.interval()) + "s");
                l.add("Range " + u(s.range) + "u  •  drone shots " + u(s.p("droneSpeed")) + "u/s");
                break;
            default:
                l.add(core);
                l.add("Pierce " + s.pierce + "  •  speed " + u(s.projectileSpeed) + "u/s");
                break;
        }
        return l;
    }

    private static String pct(double f) { return Math.round(f * 100) + "%"; }

    /**
     * Live status line for a placed hero / Zethos (active ability effects, drone counts), or null if there is
     * nothing to say right now.
     */
    public static String liveLine(Game game, Tower t) {
        TowerType ty = t.getType();
        AbilityDef a = t.getAbility();
        switch (ty) {
            case BRAD_LINDEMANN:
                if (!BradLindemannBehavior.isSupreme(t)) return null;
                return "SUPREME COMMAND " + Math.round(t.selfBuffRemaining(BradLindemannBehavior.SUPREME)) + "s: x" + Ui.fmt(a.p("damageMult"))
                    + " damage, splash " + u(BradLindemannBehavior.splashRadius(t)) + "u, " + (int) a.p("chainTargets") + " chains x " + Ui.fmt(a.p("chainDamage"));
            case BRANDON:
                if (!t.hasSelfBuff(BrandonBehavior.OVERDRIVE)) return null;
                return "OVERDRIVE " + Math.round(t.selfBuffRemaining(BrandonBehavior.OVERDRIVE)) + "s: interval halved, x" + Ui.fmt(a.p("damageMult")) + " damage, pierce " + (int) a.p("pierce");
            case ZETHOS:
                if (!ZethosBehavior.isCollapsing(t)) return null;
                return "REALITY COLLAPSE " + Math.round(t.selfBuffRemaining(ZethosBehavior.COLLAPSE)) + "s: x" + Ui.fmt(a.p("damageMult")) + " damage, x" + Ui.fmt(a.p("rateMult")) + " speed, +" + u(a.p("rangeBonus")) + "u range, " + (int) a.p("bounces") + " bounces";
            case OWEN: {
                int core = game.getEffects().dronesOf(t, OwenBehavior.CORE).size();
                int swarm = game.getEffects().dronesOf(t, OwenBehavior.SWARM).size();
                return "Drones active: " + core + (swarm > 0 ? " + " + swarm + " swarm" : "");
            }
            default: {
                double buff = t.timedBuffRemaining(TowerType.SHASHANK);
                return buff > 0 ? "RESOURCE OPTIMIZATION: +" + pct(TowerCatalog.get(TowerType.SHASHANK).ability.p("rateBonus")) + " attack speed (" + Math.round(buff) + "s)" : null;
            }
        }
    }
}

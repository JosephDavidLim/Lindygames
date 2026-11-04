package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import acm.graphics.GObject;
import acm.graphics.GRect;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import lindefense.combat.HitSpec;
import lindefense.enemy.Enemy;
import lindefense.enemy.Modifier;
import lindefense.sim.Game;
import lindefense.tower.AbilityDef;
import lindefense.tower.AttackKind;
import lindefense.tower.Faction;
import lindefense.tower.Shop;
import lindefense.tower.Tower;
import lindefense.tower.TowerCatalog;
import lindefense.tower.TowerDef;
import lindefense.tower.TowerStats;
import lindefense.tower.TowerType;
import lindefense.tower.UpgradeDef;

/**
 * Right-hand panel: the tower shop (faction tabs or an ALL view showing every
 * buildable tower grouped by faction) and the context info panel (selected
 * tower with upgrades / sell / targeting / ability, shop tower preview, or
 * enemy inspector). Shop cells show live affordability (dimmed + red price
 * when unaffordable, with the shortfall on hover). The info section is rebuilt
 * when the selection changes and refreshed a few times per second.
 */
public final class SidePanel extends GCompound {
    private static final long serialVersionUID = 1L;
    private static final double INFO_Y = 318;

    /** One rendered shop cell. */
    private static final class Card {
        final ShopLayout.Cell cell; final UIButton button; final GLabel price; final GLabel name; final GObject icon;
        Card(ShopLayout.Cell cell, UIButton button, GLabel price, GLabel name, GObject icon) {
            this.cell = cell; this.button = button; this.price = price; this.name = name; this.icon = icon;
        }
    }

    private final GameScreen screen;
    private final Game game;
    private final List<UIButton> tabButtons = new ArrayList<>();
    private final UIButton allButton;
    private final List<Card> cards = new ArrayList<>();
    private final List<UIButton> infoButtons = new ArrayList<>();
    private final GCompound cardLayer = new GCompound();
    private final GCompound info = new GCompound();
    private final GLabel tooltip;
    private Faction tab = Faction.PRIMARY;
    private boolean allMode;
    private double refresh;
    private String builtKey = "";
    private GRect abilityCd;
    private GLabel dynamicLine;

    public SidePanel(GameScreen screen) {
        this.screen = screen;
        this.game = screen.getGame();
        add(Ui.rect(Ui.PANEL_X, 0, Ui.PANEL_W, Ui.H, Ui.PANEL, Ui.c(0x2a3140)));
        add(Ui.label("SHOP", Ui.PANEL_X + 14, 26, Ui.F_H2, Ui.TEXT));
        add(Ui.label("click a tower, then click the map   (B = all towers)", Ui.PANEL_X + 80, 26, Ui.F_SMALL, Ui.MUTED));
        double x = Ui.PANEL_X + 8;
        for (Faction f : Faction.values()) {
            UIButton b = new UIButton(x, 36, 52, 28, f.label, Ui.F_TINY, () -> setTab(f));
            b.setSelectedColor(Ui.withAlpha(Ui.c(f.rgb), 110));
            tabButtons.add(b);
            add(b);
            x += 54;
        }
        allButton = new UIButton(x + 2, 36, Ui.PANEL_X + Ui.PANEL_W - 8 - (x + 2), 28, "ALL", Ui.F_BOLD, this::toggleAll);
        allButton.setSelectedColor(Ui.c(0x3a5a7a));
        add(allButton);
        add(cardLayer);
        tooltip = Ui.label("", Ui.PANEL_X + 12, INFO_Y - 14, Ui.F_SMALL, Ui.MUTED);
        add(tooltip);
        add(Ui.rect(Ui.PANEL_X + 8, INFO_Y - 8, Ui.PANEL_W - 16, Ui.H - INFO_Y, Ui.c(0x11161f), Ui.c(0x2a3140)));
        add(info);
        setTab(Faction.PRIMARY);
    }

    public Faction getTab() { return tab; }
    public boolean isAllMode() { return allMode; }

    /** Shows one faction (exits ALL mode). */
    public void setTab(Faction f) {
        tab = f;
        allMode = false;
        rebuildCards();
    }

    /** Switches between the single-faction view and the all-towers view. */
    public void toggleAll() {
        allMode = !allMode;
        rebuildCards();
    }

    private void rebuildCards() {
        for (int i = 0; i < tabButtons.size(); i++) tabButtons.get(i).setSelected(!allMode && Faction.values()[i] == tab);
        allButton.setSelected(allMode);
        cardLayer.removeAll();
        cards.clear();
        List<ShopLayout.Cell> cells = allMode ? ShopLayout.allCells() : ShopLayout.factionCells(tab);
        for (ShopLayout.Cell c : cells) {
            TowerType t = c.type;
            TowerDef d = TowerCatalog.get(t);
            UIButton b = new UIButton(c.x, c.y, c.w, c.h, "", () -> screen.beginPlacing(t));
            b.setColors(Ui.withAlpha(Ui.c(t.primaryRgb), 60), Ui.TEXT);
            b.setSelectedColor(Ui.withAlpha(Ui.c(t.primaryRgb), 150));
            cardLayer.add(b);
            GObject icon; GLabel name; GLabel price;
            if (allMode) {
                if (c.firstOfFaction) {
                    cardLayer.add(Ui.label(t.faction.label, ShopLayout.CARDS_X + 2, c.y + 19, Ui.F_TINY, Ui.c(t.faction.rgb)));
                }
                icon = shopIcon(t, c.x + 4, c.y + 5, 18);
                name = Ui.label(fit(shortLabel(t), Ui.F_TINY, c.w - 30), c.x + 25, c.y + 12, Ui.F_TINY, Ui.TEXT);
                price = Ui.label(Ui.money(d.baseCost), c.x + 25, c.y + 24, Ui.F_TINY, Ui.GOLD);
            } else {
                icon = shopIcon(t, c.x + 8, c.y + 14, 28);
                name = Ui.label(t.displayName, c.x + 44, c.y + 22, Ui.F_BOLD, Ui.TEXT);
                cardLayer.add(Ui.label(d.subtitle, c.x + 44, c.y + 37, Ui.F_TINY, Ui.MUTED));
                price = Ui.label(Ui.money(d.baseCost), c.x + 44, c.y + 51, Ui.F_SMALL, Ui.GOLD);
            }
            cardLayer.add(icon); cardLayer.add(name); cardLayer.add(price);
            cards.add(new Card(c, b, price, name, icon));
        }
        refreshAffordability();
        builtKey = "";
    }

    /** Compact label for the dense ALL grid (full name is shown in the tooltip and info panel). */
    private static String shortLabel(TowerType t) {
        switch (t) {
            case BELLARMINE_STUDENT: return "Student";
            case BELLARMINE_STUDENT_AI: return "Student + AI";
            case BRAD_LINDEMANN: return "B. Lindemann";
            default: return t.displayName;
        }
    }

    /** Shop icon: the tower's tier-0 art in a size x size box with top-left (x, y). */
    private static GObject shopIcon(TowerType t, double x, double y, double size) {
        ShopIcon icon = new ShopIcon(t, size);
        icon.setLocation(x + size / 2, y + size / 2);
        return icon;
    }

    /** Tower art icon with a translucent overlay that dims it when the tower is unaffordable. */
    private static final class ShopIcon extends acm.graphics.GCompound {
        private static final long serialVersionUID = 1L;
        private final acm.graphics.GOval dim;

        ShopIcon(TowerType t, double size) {
            add(lindefense.ui.sprites.towers.TowerIcons.create(t, 0, size), 0, 0);
            double r = size * 0.62;
            dim = Ui.oval(-r, -r, 2 * r, 2 * r, Ui.c(0x0b0e14, 165), null);
            dim.setVisible(false);
            add(dim);
        }

        void setDimmed(boolean d) { dim.setVisible(d); }
    }

    /** Truncates text with an ellipsis so it fits maxWidth in the given font. */
    private static String fit(String text, java.awt.Font f, double maxWidth) {
        GLabel probe = new GLabel(text);
        probe.setFont(f);
        if (probe.getWidth() <= maxWidth) return text;
        String s = text;
        while (s.length() > 1) {
            s = s.substring(0, s.length() - 1);
            probe.setLabel(s + "…");
            if (probe.getWidth() <= maxWidth) return s + "…";
        }
        return s;
    }

    public List<UIButton> getButtons() {
        List<UIButton> all = new ArrayList<>(tabButtons);
        all.add(allButton);
        for (Card c : cards) all.add(c.button);
        all.addAll(infoButtons);
        return all;
    }

    public void requestRebuild() { builtKey = ""; }

    private void refreshAffordability() {
        double cash = game.getCash();
        for (Card c : cards) {
            boolean ok = ShopLayout.affordable(cash, c.cell.type);
            c.button.setSelected(screen.getPlacing() == c.cell.type);
            c.button.setEnabled(ok);
            c.price.setColor(ok ? Ui.GOLD : Ui.RED);
            c.name.setColor(ok ? Ui.TEXT : Ui.c(0x6b7585));
            if (c.icon instanceof ShopIcon) ((ShopIcon) c.icon).setDimmed(!ok);
        }
    }

    public void update(double dt) {
        refresh -= dt;
        refreshAffordability();
        // hover tooltip over shop cells
        double[] m = screen.getMouseLogical();
        Card hover = null;
        for (Card c : cards) if (c.button.hit(m[0], m[1])) { hover = c; break; }
        if (hover != null) {
            TowerDef d = TowerCatalog.get(hover.cell.type);
            int need = ShopLayout.shortfall(game.getCash(), hover.cell.type);
            String s = hover.cell.type.displayName + " - " + d.subtitle + " - " + Ui.money(d.baseCost)
                + (need > 0 ? "  (need $" + need + " more)" : "  (click, then place)");
            tooltip.setLabel(fit(s, Ui.F_SMALL, Ui.PANEL_W - 24));
            tooltip.setColor(need > 0 ? Ui.RED : Ui.GREEN);
        } else {
            tooltip.setLabel("");
        }
        String key = selectionKey();
        if (!key.equals(builtKey) || refresh <= 0) {
            builtKey = key;
            refresh = 0.3;
            rebuildInfo();
        }
        if (abilityCd != null && screen.getSelectedTower() != null) {
            Tower t = screen.getSelectedTower();
            abilityCd.setSize(Math.max(0.5, 180 * t.getAbilityCooldownFraction()), 30);
            abilityCd.setVisible(t.getAbilityCooldownRemaining() > 0);
        }
        if (dynamicLine != null && screen.getSelectedEnemy() != null) {
            Enemy e = screen.getSelectedEnemy();
            dynamicLine.setLabel("HP " + (long) Math.ceil(e.getHp()) + " / " + (long) e.getMaxHp() + (e.isAlive() ? "" : "  (destroyed)"));
        }
    }

    private String selectionKey() {
        Tower t = screen.getSelectedTower();
        if (t != null) return "T" + t.getId() + ":" + t.getTier() + ":" + (game.getCash() >= Shop.upgradeCost(t)) + ":" + t.getTargeting() + ":" + t.isAbilityReady();
        if (screen.getPlacing() != null) return "P" + screen.getPlacing();
        if (screen.getSelectedEnemy() != null) return "E" + screen.getSelectedEnemy().getId();
        return "none";
    }

    // ------------------------------------------------------------------ info builders
    private double y;
    private final double x0 = Ui.PANEL_X + 18;
    private final double textW = Ui.PANEL_W - 36;

    private void line(String s, java.awt.Font f, Color c) {
        y += f.getSize() + 6;
        info.add(Ui.label(s, x0, y, f, c));
    }

    private void para(String s, java.awt.Font f, Color c) {
        for (String l : Ui.wrap(s, f, textW)) line(l, f, c);
    }

    private UIButton button(double bx, double by, double w, double h, String text, Runnable r) {
        UIButton b = new UIButton(bx, by, w, h, text, r);
        info.add(b);
        infoButtons.add(b);
        return b;
    }

    private void rebuildInfo() {
        info.removeAll();
        infoButtons.clear();
        abilityCd = null;
        dynamicLine = null;
        y = INFO_Y;
        Tower t = screen.getSelectedTower();
        if (t != null && t.isAlive()) { buildTowerInfo(t); return; }
        if (screen.getPlacing() != null) { buildShopInfo(screen.getPlacing()); return; }
        Enemy e = screen.getSelectedEnemy();
        if (e != null) { buildEnemyInfo(e); return; }
        buildHelp();
    }

    private void buildHelp() {
        line("Lindefense", Ui.F_H2, Ui.TEXT);
        para("Select a tower from the shop and click a free spot beside the track to build it. Click a placed tower to upgrade, retarget, sell or fire its ability.", Ui.F_BODY, Ui.MUTED);
        y += 6;
        line("Map: " + game.getMap().getName(), Ui.F_BOLD, Ui.ACCENT);
        para(game.getMap().getMechanicSummary(), Ui.F_SMALL, Ui.MUTED);
        y += 6;
        line("Controls", Ui.F_BOLD, Ui.ACCENT);
        String[] keys = { "Space - start next wave", "1 / 2 / 3 - game speed", "P - pause", "B - show all towers / faction tabs",
            "Esc / right-click - cancel / deselect", "U - upgrade selected   S - sell selected", "A - ability   T - cycle targeting" };
        for (String k : keys) line(k, Ui.F_SMALL, Ui.MUTED);
        y += 6;
        line("Enemy modifiers", Ui.F_BOLD, Ui.ACCENT);
        para("Camo: needs detection (Perplexity, Bellarmine Student + AI, Brad T3). Regrow: regenerates HP. Fortified: double HP.", Ui.F_SMALL, Ui.MUTED);
    }

    private void statsLines(TowerStats s, Tower t) {
        HitSpec h = s.hit;
        if (s.kind != AttackKind.NONE) {
            String kind = s.kind == AttackKind.PULSE ? "Pulse (all in range)" : s.kind == AttackKind.CHAIN ? "Chain x" + (s.chainHops + 1)
                : s.kind == AttackKind.INSTANT ? "Beam" : (s.splashRadius > 0 ? "Splash " + (int) s.splashRadius + "px" : "Projectile");
            line("Attack: " + kind + "  [" + h.type.label + "]", Ui.F_SMALL, Ui.TEXT);
            double rate = t != null ? t.effectiveRate(game) : s.rate;
            double range = t != null ? t.effectiveRange(game) : s.range;
            double dmg = h.damage * (t != null ? t.damageMultiplier(game) : 1);
            line("Damage " + Ui.fmt(dmg) + "   Rate " + Ui.fmt(rate) + "/s   Range " + (int) range, Ui.F_SMALL, Ui.TEXT);
            String extra = "Pierce " + s.pierce;
            if (s.targets > 1) extra += "   Targets " + s.targets;
            if (s.projectilesPerShot > 1) extra += "   Shots " + s.projectilesPerShot;
            if (s.bounces > 0) extra += "   Bounces " + s.bounces;
            if (s.kind == AttackKind.PROJECTILE) extra += "   Speed " + (int) s.projectileSpeed;
            line(extra, Ui.F_SMALL, Ui.TEXT);
            List<String> fx = new ArrayList<>();
            if (h.slowFraction > 0) fx.add("slow " + (int) (h.slowFraction * 100) + "%");
            if (h.stunDuration > 0) fx.add("stun " + Ui.fmt(h.stunDuration) + "s");
            if (h.knockback > 0) fx.add("knockback " + (int) h.knockback);
            if (h.armorBreak > 0) fx.add("armour break " + (int) (h.armorBreak * 100) + "%");
            if (h.armorPierce > 0) fx.add("armour pierce " + (int) (h.armorPierce * 100) + "%");
            if (h.ignoresResistance) fx.add("ignores immunities");
            if (h.markBonus > 0) fx.add("mark +" + (int) (h.markBonus * 100) + "%");
            if (h.disruptDuration > 0) fx.add("disrupt " + Ui.fmt(h.disruptDuration) + "s");
            if (h.bonusVsBoss > 0) fx.add("+" + (int) (h.bonusVsBoss * 100) + "% vs bosses");
            if (h.bonusVsFortified > 0) fx.add("+" + (int) (h.bonusVsFortified * 100) + "% vs Fortified");
            if (h.bonusVsNonFortified > 0) fx.add("+" + (int) (h.bonusVsNonFortified * 100) + "% vs non-Fortified");
            if (h.bonusVsComplex > 0) fx.add("+" + (int) (h.bonusVsComplex * 100) + "% vs complex");
            if (h.bonusVsFast > 0) fx.add("+" + (int) (h.bonusVsFast * 100) + "% vs fast");
            if (h.bonusVsSplitters > 0) fx.add("+" + (int) (h.bonusVsSplitters * 100) + "% vs splitters");
            if (h.stripsFortified) fx.add("strips Fortified");
            if (h.pctMaxHp > 0) fx.add("+" + (int) (h.pctMaxHp * 100) + "% max HP");
            if (s.stackCap > 0) fx.add("stacks to +" + (int) (s.stackCap * s.stackStep));
            if (!fx.isEmpty()) para("Effects: " + String.join(", ", fx), Ui.F_SMALL, Ui.MUTED);
        } else {
            line("Support tower (no direct attack)", Ui.F_SMALL, Ui.TEXT);
        }
        List<String> aura = new ArrayList<>();
        boolean sees = t != null ? t.canSeeCamo() : s.seesCamo;
        if (sees) aura.add("sees camo");
        if (s.auraDetection) aura.add("shares detection");
        if (s.auraRate > 0) aura.add("aura +" + (int) (s.auraRate * 100) + "% speed" + (s.auraRateMilitary > 0 ? " (Military +" + (int) (s.auraRateMilitary * 100) + "%)" : ""));
        if (s.auraDamage > 0) aura.add("aura +" + (int) (s.auraDamage * 100) + "% damage");
        if (s.auraRangeBonus > 0) aura.add("aura +" + (int) (s.auraRangeBonus * 100) + "% range");
        if (s.auraSurveillanceImmunity) aura.add("blocks Surveillance");
        if (s.auraEnemySlow > 0) aura.add("wind wall slow " + (int) (s.auraEnemySlow * 100) + "%");
        if (s.incomePerWave > 0) aura.add("+$" + (int) s.incomePerWave + " per wave");
        if (s.summonCount > 0) aura.add("summons " + s.summonCount + " sprite(s) / " + (int) s.summonInterval + "s");
        if (!aura.isEmpty()) para("Support: " + String.join(", ", aura), Ui.F_SMALL, Ui.ACCENT);
        if (t != null && (t.getAuraRateBonus() > 0 || t.getAuraDamageBonus() > 0)) {
            line("Buffs: +" + (int) (t.getAuraRateBonus() * 100) + "% speed, +" + (int) (t.getAuraDamageBonus() * 100) + "% damage", Ui.F_SMALL, Ui.GREEN);
        }
    }

    private void buildShopInfo(TowerType type) {
        TowerDef d = TowerCatalog.get(type);
        line(type.displayName, Ui.F_H2, Ui.c(type.primaryRgb));
        line(d.subtitle + "  -  " + type.faction.label + "  -  " + Ui.money(d.baseCost), Ui.F_SMALL, Ui.MUTED);
        para(d.description, Ui.F_BODY, Ui.TEXT);
        y += 4;
        statsLines(d.baseStats, null);
        y += 6;
        line("Upgrades", Ui.F_BOLD, Ui.ACCENT);
        for (int i = 0; i < d.upgrades.length; i++) {
            UpgradeDef u = d.upgrades[i];
            line((i + 1) + ". " + u.name + "  " + Ui.money(u.cost), Ui.F_BOLD, Ui.TEXT);
            para(u.description, Ui.F_SMALL, Ui.MUTED);
        }
        y += 6;
        line("Ability: " + d.ability.name + "  (" + (int) d.ability.cooldown + "s)", Ui.F_BOLD, Ui.GOLD);
        para(d.ability.description, Ui.F_SMALL, Ui.MUTED);
        y += 10;
        para("Click a free spot on the map to place. Right-click or Esc to cancel.", Ui.F_SMALL, Ui.GREEN);
        button(x0, Math.min(y + 12, Ui.H - 50), 120, 32, "Cancel", () -> screen.beginPlacing(null));
    }

    private void buildTowerInfo(Tower t) {
        TowerDef d = t.getDef();
        line(t.getType().displayName, Ui.F_H2, Ui.c(t.getType().primaryRgb));
        line(d.subtitle + "  -  " + t.getTierLabel() + "  (tier " + t.getTier() + "/" + t.getMaxTier() + ")" + (t.isSuperclass() ? "  SUPERCLASS" : ""), Ui.F_SMALL, t.isSuperclass() ? Ui.GOLD : Ui.MUTED);
        statsLines(t.getStats(), t);
        line("Kills " + t.getKills() + "   Damage " + (long) t.getDamageDealt() + "   Invested " + Ui.money(t.getInvestedCost()), Ui.F_SMALL, Ui.MUTED);
        if (t.isSurveilled()) line("SURVEILLED: -50% speed, -30% range (" + String.format("%.1fs", t.getSurveilledRemaining()) + ")", Ui.F_BOLD, Ui.RED);
        y += 8;
        UpgradeDef u = t.nextUpgrade();
        if (u != null) {
            boolean can = game.getCash() >= u.cost;
            UIButton ub = button(x0, y, 364, 34, "Upgrade: " + u.name + "   " + Ui.money(u.cost), () -> screen.upgradeSelected());
            ub.setColors(can ? Ui.c(0x1f6b45) : Ui.PANEL_LIGHT, Ui.TEXT);
            ub.setEnabled(can);
            y += 36;
            para(u.description, Ui.F_SMALL, Ui.MUTED);
            if (t.getTier() + 1 < t.getMaxTier()) {
                UpgradeDef nx = d.upgrades[t.getTier() + 1];
                line("Then: " + nx.name + " (" + Ui.money(nx.cost) + ")", Ui.F_TINY, Ui.MUTED);
            }
        } else {
            line("Fully upgraded - " + t.getTierLabel(), Ui.F_BOLD, Ui.GOLD);
        }
        for (int i = 0; i < t.getTier(); i++) line("Owned: " + d.upgrades[i].name, Ui.F_TINY, Ui.GREEN);
        y += 10;
        if (!t.getStats().isSupportOnly()) {
            info.add(Ui.label("Target", x0, y + 21, Ui.F_BOLD, Ui.TEXT));
            if (t.getType() == TowerType.BELLARMINE_STUDENT_AI) {
                info.add(Ui.label("Auto (camo > bosses > strongest)", x0 + 70, y + 21, Ui.F_SMALL, Ui.ACCENT));
            } else {
                button(x0 + 70, y, 34, 30, "<", () -> screen.cycleTargeting(-1));
                UIButton cur = button(x0 + 108, y, 110, 30, t.getTargeting().label, null);
                cur.setSelected(true);
                button(x0 + 222, y, 34, 30, ">", () -> screen.cycleTargeting(1));
            }
            y += 40;
        }
        AbilityDef a = t.getAbility();
        if (a != null) {
            boolean ready = t.isAbilityReady();
            UIButton ab = button(x0, y, 180, 30, a.name, () -> screen.abilitySelected());
            ab.setColors(ready ? Ui.c(0x7a5a00) : Ui.PANEL_LIGHT, ready ? Ui.GOLD : Ui.MUTED);
            ab.setEnabled(ready);
            abilityCd = Ui.rect(x0, y, 180, 30, Ui.c(0x000000, 120), Ui.c(0x000000, 0));
            abilityCd.setVisible(false);
            info.add(abilityCd);
            info.add(Ui.label(ready ? "READY  (A)" : String.format("%.0fs", t.getAbilityCooldownRemaining()) + " / " + (int) a.cooldown + "s", x0 + 190, y + 20, Ui.F_SMALL, ready ? Ui.GOLD : Ui.MUTED));
            y += 34;
            para(a.description, Ui.F_SMALL, Ui.MUTED);
        }
        y += 8;
        UIButton sell = button(x0, y, 180, 32, "Sell for " + Ui.money(Shop.sellValue(t)), () -> screen.sellSelected());
        sell.setColors(Ui.c(0x6b1f1f), Ui.TEXT);
        button(x0 + 190, y, 120, 32, "Deselect", () -> screen.selectTower(null));
    }

    private void buildEnemyInfo(Enemy e) {
        line(e.getType().displayName, Ui.F_H2, e.isBoss() ? Ui.RED : Ui.TEXT);
        StringBuilder mods = new StringBuilder();
        for (Modifier m : e.getModifiers()) { if (mods.length() > 0) mods.append(", "); mods.append(m.label); }
        line((e.isBoss() ? e.getType().getBossClass().label + "-class boss" : "Enemy") + (mods.length() > 0 ? "  -  " + mods : ""), Ui.F_SMALL, Ui.MUTED);
        dynamicLine = Ui.label("", x0, y + 20, Ui.F_BOLD, Ui.TEXT);
        y += 20;
        info.add(dynamicLine);
        line("Speed " + Ui.fmt(e.getType().speed) + "x   Reward $" + e.getType().reward + "   Lives cost " + e.getType().lives, Ui.F_SMALL, Ui.TEXT);
        if (e.getType().getArmor() > 0) line("Armour " + (int) (e.getType().getArmor() * 100) + "%", Ui.F_SMALL, Ui.TEXT);
        if (e.getType().isPhysicalImmune()) line("Immune to Physical damage", Ui.F_SMALL, Ui.GOLD);
        if (e.getType().hasChildren()) line("Releases " + e.getType().totalChildren() + " " + e.getType().getChildTypes()[0].displayName + (e.getType().getChildTypes().length > 1 ? " + " + e.getType().getChildTypes()[1].displayName : ""), Ui.F_SMALL, Ui.TEXT);
        y += 4;
        para(e.getType().description, Ui.F_BODY, Ui.MUTED);
        if (e.getType().mechanic == lindefense.enemy.Mechanic.RADIX_PHASES) {
            String[] ph = { "plain", "Physical-immune", "Energy/Magic -50%", "Explosive-immune" };
            line("Current digit armour: " + ph[e.radixPhase()], Ui.F_SMALL, Ui.GOLD);
        }
        if (!e.getStoredEnemies().isEmpty()) line("Storing " + e.getStoredEnemies().size() + " enemies", Ui.F_SMALL, Ui.GOLD);
        y += 10;
        button(x0, y, 120, 30, "Deselect", () -> screen.selectEnemy(null));
    }
}

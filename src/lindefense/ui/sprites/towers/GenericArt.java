package lindefense.ui.sprites.towers;

import java.awt.Color;
import lindefense.tower.TowerType;

/** Fallback body for unknown tower types: a round bot in the type's colours with one cannon. */
@SuppressWarnings({ "serial", "this-escape" })
public final class GenericArt extends TowerArt {
    private static final long serialVersionUID = 1L;

    /** @see TowerArt#TowerArt(TowerType, int, double) */
    public GenericArt(TowerType type, int tier, double r) { super(type, tier, r); }

    @Override protected void build() {
        body(0, -r * 0.1, r * 0.7, pri);
        Turret tu = turret();
        double len = r * (0.9 + 0.2 * tier);
        tu.poly(Art.rect(-r * 0.2, -r * 0.12, len + r * 0.2, r * 0.24), sec);
        tu.setFlash(tu.plain(Art.star(len + r * 0.3, 0, r * 0.3, r * 0.12, 5, 0), Art.alpha(Color.WHITE, 230)));
        disc(0, 0, r * 0.2, secDark);
        eye(0, -r * 0.4, r * 0.22, pri);
    }
}

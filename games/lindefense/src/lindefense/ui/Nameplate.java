package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import java.awt.Color;
import java.awt.Font;

/**
 * Small chamfered readout ("Claude · Sonnet 5.5") used for tower hover / selection nameplates and the placement
 * ghost caption. Built once; {@link #set} only changes text when it differs.
 */
public final class Nameplate extends GCompound {
    private static final long serialVersionUID = 1L;
    private static final Font NAME_F = Cyber.display(Font.BOLD, 15);
    private static final Font TIER_F = Cyber.display(Font.PLAIN, 14);
    public static final double HEIGHT = 24;

    private final Cyber.Chamfer frame = new Cyber.Chamfer(0, 0, 100, HEIGHT, 6).fill(new Color(5, 9, 22, 232)).scan(false);
    private final GLabel name = new Cyber.Text("");
    private final GLabel tier = new Cyber.Text("");
    private double width = 100;
    private String lastName = "", lastTier = "";

    public Nameplate() {
        name.setFont(NAME_F);
        tier.setFont(TIER_F);
        add(frame);
        add(name);
        add(tier);
        setVisible(false);
    }

    public double getPlateWidth() { return width; }

    /** Sets the text; tierText may be null/empty for a single-label plate. */
    public void set(String nameText, String tierText, Color accent) {
        String t = tierText == null ? "" : tierText;
        if (!nameText.equals(lastName) || !t.equals(lastTier)) {
            lastName = nameText; lastTier = t;
            name.setLabel(nameText);
            name.setLocation(10, 16.5);
            double w = 10 + name.getWidth() + 10;
            if (!t.isEmpty()) {
                tier.setLabel("·  " + t);
                tier.setLocation(10 + name.getWidth() + 7, 16.5);
                w = tier.getX() + tier.getWidth() + 10;
            } else {
                tier.setLabel("");
            }
            width = w;
            frame.size(w, HEIGHT);
        }
        frame.border(Cyber.alpha(accent, 220)).glow(0.7);
        Cyber.setColor(name, Cyber.TEXT);
        Cyber.setColor(tier, accent);
    }

    /** Positions the plate with top-left corner (x, y). */
    public void place(double x, double y) { setLocation(x, y); }
}

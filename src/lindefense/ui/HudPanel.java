package lindefense.ui;

import acm.graphics.GCompound;
import acm.graphics.GLabel;
import java.util.ArrayList;
import java.util.List;
import lindefense.sim.Game;
import lindefense.wave.WaveData;
import lindefense.wave.WaveDef;

/**
 * Top bar (cash, lives, wave, enemies, speed and wave controls) and bottom bar
 * (banners, map events, wave notes). Positioned in absolute logical coordinates.
 */
public final class HudPanel extends GCompound {
    private static final long serialVersionUID = 1L;

    private final GameScreen screen;
    private final Game game;
    private final List<UIButton> buttons = new ArrayList<>();
    private final GLabel cash, lives, wave, enemies, banner, event, note, mapName;
    private final UIButton pauseBtn, s1, s2, s3, startBtn, autoBtn, menuBtn;
    private String bannerText; private java.awt.Color bannerColor; private double bannerTime;

    public HudPanel(GameScreen screen) {
        this.screen = screen;
        this.game = screen.getGame();
        add(Ui.rect(0, 0, Ui.WORLD_W, Ui.TOP_H, Ui.PANEL, Ui.c(0x2a3140)));
        add(Ui.rect(0, Ui.BOTTOM_Y, Ui.WORLD_W, Ui.H - Ui.BOTTOM_Y, Ui.PANEL, Ui.c(0x2a3140)));
        cash = Ui.label("$650", 16, 31, Ui.F_H2, Ui.GOLD); add(cash);
        lives = Ui.label("Lives 100", 150, 31, Ui.F_H2, Ui.RED); add(lives);
        wave = Ui.label("Wave 0 / 100", 300, 31, Ui.F_H2, Ui.TEXT); add(wave);
        enemies = Ui.label("Enemies 0", 470, 31, Ui.F_BOLD, Ui.MUTED); add(enemies);
        pauseBtn = btn(640, 8, 52, 32, "||", () -> screen.togglePause());
        s1 = btn(696, 8, 44, 32, "1x", () -> screen.setSpeed(1));
        s2 = btn(744, 8, 44, 32, "2x", () -> screen.setSpeed(2));
        s3 = btn(792, 8, 44, 32, "3x", () -> screen.setSpeed(3));
        startBtn = btn(860, 6, 170, 36, "Start Wave 1", () -> screen.startWave());
        startBtn.setColors(Ui.c(0x1f6b45), Ui.TEXT);
        autoBtn = btn(1036, 8, 70, 32, "Auto", () -> screen.toggleAuto());
        menuBtn = btn(1112, 8, 80, 32, "Menu", () -> screen.backToMenu());
        mapName = Ui.label(game.getMap().getName(), 16, Ui.BOTTOM_Y + 20, Ui.F_BOLD, Ui.MUTED); add(mapName);
        banner = Ui.label("", 16, Ui.BOTTOM_Y + 38, Ui.F_BOLD, Ui.TEXT); add(banner);
        event = Ui.label("", 420, Ui.BOTTOM_Y + 20, Ui.F_BOLD, Ui.c(0xff8cf0)); add(event);
        note = Ui.label("", 420, Ui.BOTTOM_Y + 38, Ui.F_SMALL, Ui.MUTED); add(note);
    }

    private UIButton btn(double x, double y, double w, double h, String text, Runnable r) {
        UIButton b = new UIButton(x, y, w, h, text, r);
        buttons.add(b);
        add(b);
        return b;
    }

    public List<UIButton> getButtons() { return buttons; }

    public void flashBanner(String text, java.awt.Color col, double seconds) {
        bannerText = text; bannerColor = col; bannerTime = seconds;
    }

    public void update(double dt) {
        cash.setLabel(Ui.money(game.getCash()));
        lives.setLabel("Lives " + game.getLives());
        int w = game.getWave();
        wave.setLabel("Wave " + w + " / " + WaveData.WAVE_COUNT);
        int alive = game.getEnemyManager().getAliveCount();
        int toSpawn = game.getWaveManager().getRemainingToSpawn();
        enemies.setLabel("Enemies " + alive + (toSpawn > 0 ? " (+" + toSpawn + ")" : ""));
        pauseBtn.setSelected(game.isPaused());
        pauseBtn.setText(game.isPaused() ? ">" : "||");
        s1.setSelected(game.getSpeed() == 1 && !game.isPaused());
        s2.setSelected(game.getSpeed() == 2 && !game.isPaused());
        s3.setSelected(game.getSpeed() == 3 && !game.isPaused());
        boolean canStart = game.getWaveManager().canStartNextWave() && game.getState() == Game.State.PLAYING;
        startBtn.setEnabled(canStart);
        if (canStart) {
            String t = "Start Wave " + (w + 1);
            if (game.getWaveManager().isAutoStart() && w > 0) t += String.format(" (%.0f)", Math.ceil(game.getWaveManager().getPrepTimer()));
            startBtn.setText(t);
        } else {
            startBtn.setText(w >= WaveData.WAVE_COUNT ? "Final Wave" : "Wave " + w + " in progress");
        }
        autoBtn.setSelected(game.getWaveManager().isAutoStart());
        if (bannerTime > 0) bannerTime -= dt;
        String gb = game.getBanner();
        if (bannerTime > 0 && bannerText != null) { banner.setLabel(bannerText); banner.setColor(bannerColor); }
        else if (gb != null) { banner.setLabel(gb); banner.setColor(Ui.c(game.getBannerRgb())); }
        else banner.setLabel("");
        String ev = game.getMap().getEventText();
        event.setLabel(ev == null ? "" : ev);
        WaveDef def = game.getWaveManager().getCurrentDef();
        if (game.getWaveManager().isWaveActive() && def != null && def.note != null) note.setLabel("Wave " + def.number + ": " + def.note);
        else if (w < WaveData.WAVE_COUNT) {
            WaveDef next = WaveData.get(w + 1);
            note.setLabel("Next: " + next.totalSpawns() + " enemies" + (next.note != null ? " - " + next.note : "") + (next.containsBoss() ? "  [BOSS]" : ""));
        } else note.setLabel("");
    }
}

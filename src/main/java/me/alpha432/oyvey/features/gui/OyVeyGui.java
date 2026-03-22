package me.alpha432.oyvey.features.gui;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.ColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OyVeyGui extends Screen {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static OyVeyGui INSTANCE;
    private static Color colorClipboard = null;

    static { INSTANCE = new OyVeyGui(); }

    public static OyVeyGui getInstance() {
        if (INSTANCE == null) INSTANCE = new OyVeyGui();
        return INSTANCE;
    }
    public static OyVeyGui getClickGui() { return getInstance(); }

    // ── Scale ─────────────────────────────────────────────────────────────────
    private float guiScale   = 1.0f;
    private static final float SCALE_MIN = 0.6f;
    private static final float SCALE_MAX = 2.0f;
    private boolean draggingScale = false;

    // ── Base layout (before scale) ────────────────────────────────────────────
    private static final int BASE_W     = 280;
    private static final int BASE_H     = 180;
    private static final int SIDEBAR_W  = 80;
    private static final int CAT_H      = 20;
    private static final int MOD_H      = 13;
    private static final int HEADER_H   = 18;
    private static final int FOOTER_H   = 16;
    private static final int PADDING    = 6;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int C_BG        = 0xF0101214;
    private static final int C_SIDEBAR   = 0xF0141618;
    private static final int C_HEADER    = 0xF00D0F11;
    private static final int C_BORDER    = 0x33FFFFFF;
    private static final int C_CAT_SEL   = 0x22FFFFFF;
    private static final int C_CAT_HOV   = 0x11FFFFFF;
    private static final int C_MOD_ON    = 0x18FFFFFF;
    private static final int C_MOD_HOV   = 0x0FFFFFFF;
    private static final int C_SET_BG    = 0xF0181B1F;
    private static final int C_TEXT_PRI  = 0xFFFFFFFF;
    private static final int C_TEXT_SEC  = 0xFF666666;
    private static final int C_TEXT_DIM  = 0xFF333333;

    // ── State ─────────────────────────────────────────────────────────────────
    private int selectedCat = 0;
    private int winX, winY;
    private boolean draggingWin = false;
    private int dragOffX, dragOffY;
    private long openTime;
    private Module openSettingsFor = null;
    private Setting<?> listeningForBind = null;

    private final ArrayList<Widget> widgets = new ArrayList<>();

    private OyVeyGui() { super(Component.literal("OyVeyGui")); }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        int scaledW = (int)(BASE_W * guiScale);
        int scaledH = (int)((BASE_H + FOOTER_H) * guiScale);
        winX = (width  - scaledW) / 2;
        winY = (height - scaledH) / 2;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int W()  { return (int)(BASE_W * guiScale); }
    private int H()  { return (int)((BASE_H + FOOTER_H) * guiScale); }
    private int sw() { return (int)(SIDEBAR_W * guiScale); }
    private int ch() { return (int)(CAT_H * guiScale); }
    private int mh() { return (int)(MOD_H * guiScale); }
    private int hh() { return (int)(HEADER_H * guiScale); }
    private int fh() { return (int)(FOOTER_H * guiScale); }
    private int pd() { return (int)(PADDING * guiScale); }
    private int fs() { return Math.max(1, (int)(8 * guiScale)); } // font size approx

    private int accentColor(float elapsed, float offset) {
        if (ClickGuiModule.getInstance().rainbow.getValue()) {
            return ColorUtil.rainbow(
                    (int)(offset * ClickGuiModule.getInstance().rainbowHue.getValue())
            ).getRGB() | 0xFF000000;
        }
        Color c = ClickGuiModule.getInstance().color.getValue();
        return 0xFF000000 | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    private static void fill(GuiGraphics ctx, int x1, int y1, int x2, int y2, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        ctx.fill(x1, y1, x2, y2, color);
    }

    private static void outline(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color);
        ctx.fill(x,         y,         x + 1,     y + h,     color);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    private String truncate(String s, int maxW) {
        if (font.width(s) <= maxW) return s;
        while (s.length() > 1 && font.width(s + "..") > maxW) s = s.substring(0, s.length() - 1);
        return s + "..";
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        float elapsed = (System.currentTimeMillis() - openTime) / 1000f;
        int wx = winX, wy = winY;
        int W = W(), H = H(), sw = sw(), ch = ch(), mh = mh(), hh = hh(), fh = fh(), pd = pd();

        // Screen dim
        ctx.fill(0, 0, width, height, 0x88000000);

        // Set Item.context so Button/Item drawString works
        me.alpha432.oyvey.features.gui.items.Item.context = ctx;

        // ── Window ────────────────────────────────────────────────────────────
        fill(ctx, wx, wy, wx + W, wy + H, C_BG);
        fill(ctx, wx, wy, wx + sw, wy + H, C_SIDEBAR);
        fill(ctx, wx, wy, wx + W, wy + hh, C_HEADER);

        // Accent line under header
        int accent = accentColor(elapsed, 0f);
        fill(ctx, wx, wy + hh - 1, wx + W, wy + hh, accent);

        // Header text
        ctx.drawString(font, "Admin Abuse Client", wx + pd, wy + (hh - 8) / 2, accent);

        // Scale label in header right
        String scaleStr = "Scale: " + Math.round(guiScale * 100) + "%";
        ctx.drawString(font, scaleStr, wx + W - font.width(scaleStr) - pd, wy + (hh - 8) / 2, C_TEXT_SEC);

        // Sidebar/content divider
        fill(ctx, wx + sw, wy + hh, wx + sw + 1, wy + H - fh, C_BORDER);

        // Window border
        outline(ctx, wx, wy, W, H, C_BORDER);

        // ── Footer (scale slider) ─────────────────────────────────────────────
        int fy = wy + H - fh;
        fill(ctx, wx, fy, wx + W, wy + H, C_HEADER);
        fill(ctx, wx, fy, wx + W, fy + 1, C_BORDER);

        // Slider track
        int trackX = wx + pd + font.width("Scale ") + 4;
        int trackW = W - pd * 2 - font.width("Scale ") - 8;
        int trackY = fy + fh / 2;
        fill(ctx, trackX, trackY - 1, trackX + trackW, trackY + 1, C_BORDER);

        // Slider fill
        float scalePct = (guiScale - SCALE_MIN) / (SCALE_MAX - SCALE_MIN);
        int fillW = (int)(trackW * scalePct);
        fill(ctx, trackX, trackY - 1, trackX + fillW, trackY + 1, accent);

        // Slider handle
        int handleX = trackX + fillW;
        fill(ctx, handleX - 3, trackY - 4, handleX + 3, trackY + 4, accent);
        fill(ctx, handleX - 2, trackY - 3, handleX + 2, trackY + 3, C_HEADER);

        ctx.drawString(font, "Scale", wx + pd, fy + (fh - 8) / 2, C_TEXT_SEC);

        // ── Categories ────────────────────────────────────────────────────────
        List<Module.Category> cats = OyVey.moduleManager.getCategories();
        for (int i = 0; i < cats.size(); i++) {
            Module.Category cat = cats.get(i);
            if (cat == Module.Category.HUD) continue;

            int cy = wy + hh + i * ch;
            if (cy + ch > wy + H - fh) break;

            boolean sel     = i == selectedCat;
            boolean hovered = !sel && mx >= wx && mx <= wx + sw && my >= cy && my < cy + ch;

            if (sel)    fill(ctx, wx, cy, wx + sw, cy + ch, C_CAT_SEL);
            else if (hovered) fill(ctx, wx, cy, wx + sw, cy + ch, C_CAT_HOV);

            if (sel) fill(ctx, wx, cy, wx + 2, cy + ch, accentColor(elapsed, i * 0.06f));

            int tc = sel ? accentColor(elapsed, i * 0.06f) : (hovered ? C_TEXT_PRI : C_TEXT_SEC);
            ctx.drawString(font, truncate(cat.getName(), sw - pd * 2 - 14), wx + pd, cy + (ch - 8) / 2, tc);

            // Enabled count badge
            long en = OyVey.moduleManager.getModulesByCategory(cat).stream().filter(Module::isEnabled).count();
            if (en > 0) {
                String badge = String.valueOf(en);
                int bw = font.width(badge) + 4;
                int bx = wx + sw - bw - 4;
                int by = cy + (ch - 9) / 2;
                fill(ctx, bx, by, bx + bw, by + 9, 0x33000000);
                ctx.drawString(font, badge, bx + 2, by + 1, accentColor(elapsed, i * 0.06f));
            }
        }

        // ── Module list ───────────────────────────────────────────────────────
        if (selectedCat < cats.size()) {
            Module.Category cat = cats.get(selectedCat);
            List<Module> mods   = OyVey.moduleManager.getModulesByCategory(cat);
            int cx = wx + sw + 1;
            int cw = W - sw - 1;

            // Category title
            ctx.drawString(font, cat.getName().toUpperCase(), cx + pd, wy + hh + 6, C_TEXT_SEC);
            fill(ctx, cx + pd, wy + hh + 17, wx + W - pd, wy + hh + 18, C_TEXT_DIM);

            int startY = wy + hh + 22;

            for (int i = 0; i < mods.size(); i++) {
                Module mod = mods.get(i);
                if (mod.hidden) continue;

                int ry = startY + i * mh;
                if (ry + mh > wy + H - fh - 2) break;

                boolean enabled = mod.isEnabled();
                boolean hovered = mx >= cx && mx <= wx + W && my >= ry && my < ry + mh
                        && (openSettingsFor == null || openSettingsFor == mod);

                if (enabled) fill(ctx, cx, ry, wx + W, ry + mh, C_MOD_ON);
                else if (hovered) fill(ctx, cx, ry, wx + W, ry + mh, C_MOD_HOV);

                // Settings panel open indicator
                if (openSettingsFor == mod) {
                    fill(ctx, cx, ry, cx + 2, ry + mh, accentColor(elapsed, i * 0.04f));
                } else if (enabled) {
                    fill(ctx, cx + 3, ry + mh/2 - 2, cx + 6, ry + mh/2 + 2, accentColor(elapsed, i * 0.04f));
                }

                int nc = enabled ? C_TEXT_PRI : (hovered ? 0xFFAAAAAA : C_TEXT_SEC);
                ctx.drawString(font, truncate(mod.getDisplayName(), cw - pd * 3), cx + pd + 2, ry + (mh - 8) / 2, nc);

                String info = mod.getDisplayInfo();
                if (info != null) {
                    ctx.drawString(font, info, wx + W - font.width(info) - pd, ry + (mh - 8) / 2, C_TEXT_SEC);
                }

                if (i < mods.size() - 1) {
                    fill(ctx, cx + pd, ry + mh - 1, wx + W - pd, ry + mh, 0x0AFFFFFF);
                }
            }

            // ── Settings panel ────────────────────────────────────────────────
            if (openSettingsFor != null) {
                renderSettingsPanel(ctx, mx, my, elapsed, openSettingsFor, cx, cw, wx, wy, W, H, fh, mh, hh, pd, startY, mods);
            }
        }

        super.render(ctx, mx, my, delta);
    }

    private void renderSettingsPanel(GuiGraphics ctx, int mx, int my, float elapsed,
                                     Module mod, int cx, int cw, int wx, int wy,
                                     int W, int H, int fh, int mh, int hh, int pd,
                                     int startY, List<Module> mods) {
        // Find module Y position
        int modIdx = mods.indexOf(mod);
        if (modIdx < 0) return;
        int modY = startY + modIdx * mh;

        // Panel appears to the right of module list, or below if no space
        int panelX = wx + W;
        int panelW = 100;

        // If it would go off screen, put it to the left of the window
        if (panelX + panelW > width) panelX = wx - panelW;

        List<Setting<?>> settings = new ArrayList<>();
        for (Setting<?> s : mod.getSettings()) {
            if (s.getName().equalsIgnoreCase("Enabled")) continue;
            if (s.getName().equalsIgnoreCase("DisplayName")) continue;
            if (!s.isVisible()) continue;
            settings.add(s);
        }

        int maxPanelH = height - 20;
        int panelH = Math.min(settings.size() * 14 + 14, maxPanelH);
        int panelY = Math.max(4, Math.min(modY, height - panelH - 4));

        // Panel background
        fill(ctx, panelX, panelY, panelX + panelW, panelY + panelH, C_SET_BG);
        outline(ctx, panelX, panelY, panelW, panelH, C_BORDER);

        // Panel title
        int accent = accentColor(elapsed, 0f);
        fill(ctx, panelX, panelY, panelX + panelW, panelY + 14, C_HEADER);
        fill(ctx, panelX, panelY + 13, panelX + panelW, panelY + 14, accent);
        ctx.drawString(font, mod.getDisplayName(), panelX + 4, panelY + 3, C_TEXT_PRI);

        // Settings
        for (int i = 0; i < settings.size(); i++) {
            Setting<?> s = settings.get(i);
            int sy = panelY + 14 + i * 14;
            boolean hov = mx >= panelX && mx <= panelX + panelW && my >= sy && my < sy + 12;
            if (hov) fill(ctx, panelX, sy, panelX + panelW, sy + 12, C_MOD_HOV);

            Object val = s.getValue();

            if (val instanceof Boolean) {
                boolean bval = (Boolean) val;
                // Toggle switch look
                int switchX = panelX + panelW - 20;
                fill(ctx, switchX, sy + 4, switchX + 14, sy + 12, bval ? (accent & 0x00FFFFFF | 0x88000000) : 0x44FFFFFF);
                fill(ctx, bval ? switchX + 7 : switchX + 1, sy + 5, bval ? switchX + 13 : switchX + 7, sy + 11, bval ? accent : 0xFF666666);
                ctx.drawString(font, truncate(s.getName(), panelW - 28), panelX + 4, sy + 4, bval ? C_TEXT_PRI : C_TEXT_SEC);

            } else if (s.isNumberSetting() && s.hasRestriction()) {
                // Mini slider
                Number min = (Number) s.getMin(), max = (Number) s.getMax(), cur = (Number) val;
                if (min != null && max != null) {
                    float pct = (cur.floatValue() - min.floatValue()) / (max.floatValue() - min.floatValue());
                    int trackX2 = panelX + 4;
                    int trackW2 = panelW - 8;
                    int trackY2 = sy + 11;
                    fill(ctx, trackX2, trackY2, trackX2 + trackW2, trackY2 + 2, 0x33FFFFFF);
                    fill(ctx, trackX2, trackY2, trackX2 + (int)(trackW2 * pct), trackY2 + 2, accent);

                    String label = s.getName() + " " + ChatFormatting.GRAY + formatNum(cur);
                    ctx.drawString(font, truncate(label, panelW - 8), panelX + 4, sy + 2, C_TEXT_PRI);
                }

            } else if (val instanceof Enum) {
                // Enum cycle
                String label = s.getName() + " " + ChatFormatting.GRAY + s.currentEnumName();
                ctx.drawString(font, truncate(label, panelW - 8), panelX + 4, sy + 4, C_TEXT_PRI);
                // Arrow indicator
                ctx.drawString(font, "»", panelX + panelW - 10, sy + 4, accent);

            } else if (val instanceof java.awt.Color) {
                java.awt.Color c = (java.awt.Color) val;
                int swatchColor = 0xFF000000 | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
                // Name on left, swatch on right
                ctx.drawString(font, truncate(s.getName(), panelW - 22), panelX + 4, sy + 3, C_TEXT_PRI);
                fill(ctx, panelX + panelW - 15, sy + 2, panelX + panelW - 3, sy + 12, swatchColor);
                outline(ctx, panelX + panelW - 15, sy + 2, 12, 10, 0x88FFFFFF);

            } else if (val instanceof me.alpha432.oyvey.features.settings.Bind) {
                // Keybind
                me.alpha432.oyvey.features.settings.Bind bind = (me.alpha432.oyvey.features.settings.Bind) val;
                String keyName = bind.getKey() <= 0 ? "NONE" : GLFW.glfwGetKeyName(bind.getKey(), 0);
                if (keyName == null) keyName = "KEY_" + bind.getKey();
                String label = s.getName() + ": " + keyName.toUpperCase();
                ctx.drawString(font, truncate(label, panelW - 8), panelX + 4, sy + 4,
                        listeningForBind == s ? accent : C_TEXT_PRI);
                if (listeningForBind == s) {
                    ctx.drawString(font, "...", panelX + panelW - 14, sy + 4, accent);
                }

            } else {
                ctx.drawString(font, truncate(s.getName() + ": " + val, panelW - 8), panelX + 4, sy + 4, C_TEXT_SEC);
            }
        }
    }

    private String formatNum(Number n) {
        if (n instanceof Integer) return String.valueOf(n.intValue());
        if (n instanceof Float)   return String.format("%.1f", n.floatValue());
        return String.format("%.1f", n.doubleValue());
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y(), btn = click.button();
        int wx = winX, wy = winY, W = W(), H = H(), sw = sw(), ch = ch();
        int mh = mh(), hh = hh(), fh = fh(), pd = pd();

        // Scale slider click
        int fy = wy + H - fh;
        int trackX = wx + pd + font.width("Scale ") + 4;
        int trackW = W - pd * 2 - font.width("Scale ") - 8;
        if (btn == 0 && mx >= trackX - 4 && mx <= trackX + trackW + 4 && my >= fy && my < wy + H) {
            draggingScale = true;
            updateScale(mx, trackX, trackW);
            return true;
        }

        // Drag window header
        if (btn == 0 && mx >= wx && mx <= wx + W && my >= wy && my < wy + hh) {
            draggingWin = true;
            dragOffX = mx - wx;
            dragOffY = my - wy;
            return true;
        }

        // Settings panel clicks — handle BEFORE module clicks so dragging sliders works
        if (openSettingsFor != null) {
            if (handleSettingsPanelClick(mx, my, btn, wx, wy, W, H, fh, mh, hh, pd)) return true;
            // Don't close here — only close on right-click of module or category switch
        }

        // Category click
        List<Module.Category> cats = OyVey.moduleManager.getCategories();
        for (int i = 0; i < cats.size(); i++) {
            if (cats.get(i) == Module.Category.HUD) continue;
            int cy = wy + hh + i * ch;
            if (mx >= wx && mx <= wx + sw && my >= cy && my < cy + ch) {
                selectedCat = i;
                openSettingsFor = null;
                return true;
            }
        }

        // Module clicks
        if (selectedCat < cats.size()) {
            Module.Category cat = cats.get(selectedCat);
            List<Module> mods   = OyVey.moduleManager.getModulesByCategory(cat);
            int cx       = wx + sw + 1;
            int startY   = wy + hh + 22;

            for (int i = 0; i < mods.size(); i++) {
                Module mod = mods.get(i);
                if (mod.hidden) continue;
                int ry = startY + i * mh;
                if (ry + mh > wy + H - fh - 2) break;

                if (mx >= cx && mx <= wx + W && my >= ry && my < ry + mh) {
                    if (btn == 0) { mod.toggle(); }
                    if (btn == 1) { openSettingsFor = openSettingsFor == mod ? null : mod; listeningForBind = null; }
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @SuppressWarnings("unchecked")
    private boolean handleSettingsPanelClick(int mx, int my, int btn,
                                             int wx, int wy, int W, int H, int fh, int mh, int hh, int pd) {
        Module mod = openSettingsFor;
        List<Module.Category> cats = OyVey.moduleManager.getCategories();
        if (selectedCat >= cats.size()) return false;
        List<Module> mods = OyVey.moduleManager.getModulesByCategory(cats.get(selectedCat));
        int modIdx = mods.indexOf(mod);
        if (modIdx < 0) return false;

        int startY = wy + hh + 22;
        int modY   = startY + modIdx * mh;

        int panelX = wx + W;
        int panelW = 100;
        if (panelX + panelW > width) panelX = wx - panelW;

        List<Setting<?>> settings = new ArrayList<>();
        for (Setting<?> s : mod.getSettings()) {
            if (s.getName().equalsIgnoreCase("Enabled")) continue;
            if (s.getName().equalsIgnoreCase("DisplayName")) continue;
            if (!s.isVisible()) continue;
            settings.add(s);
        }

        int maxPanelH2 = height - 20;
        int panelH = Math.min(settings.size() * 14 + 14, maxPanelH2);
        int panelY = Math.max(4, Math.min(modY, height - panelH - 4));

        if (mx < panelX || mx > panelX + panelW || my < panelY || my > panelY + panelH) return false;

        for (int i = 0; i < settings.size(); i++) {
            Setting<?> s = settings.get(i);
            int sy = panelY + 14 + i * 14;
            if (my < sy || my >= sy + 14) continue;

            Object val = s.getValue();

            if (val instanceof Boolean) {
                ((Setting<Boolean>) s).setValue(!(Boolean) val);
                return true;
            }
            if (val instanceof java.awt.Color) {
                return true;
            }
            if (val instanceof me.alpha432.oyvey.features.settings.Bind) {
                listeningForBind = listeningForBind == s ? null : s;
                return true;
            }
            if (s.isNumberSetting() && s.hasRestriction()) {
                Number min = (Number) s.getMin(), max = (Number) s.getMax();
                if (min != null && max != null) {
                    int trackX2 = panelX + 4;
                    int trackW2 = panelW - 8;
                    float pct = Math.max(0, Math.min(1, (float)(mx - trackX2) / trackW2));
                    setNumericSetting((Setting<Number>) s, pct);
                    return true;
                }
            }
            if (val instanceof Enum) {
                ((Setting<Enum<?>>) s).increaseEnum();
                return true;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void setNumericSetting(Setting<Number> s, float pct) {
        Number min = (Number) s.getMin(), max = (Number) s.getMax();
        if (s.getValue() instanceof Double) {
            double result = min.doubleValue() + (max.doubleValue() - min.doubleValue()) * pct;
            s.setValue((double) Math.round(10.0 * result) / 10.0);
        } else if (s.getValue() instanceof Float) {
            float result = min.floatValue() + (max.floatValue() - min.floatValue()) * pct;
            s.setValue((float) Math.round(10.0f * result) / 10.0f);
        } else if (s.getValue() instanceof Integer) {
            s.setValue((int)(min.intValue() + (max.intValue() - min.intValue()) * pct));
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.button() == 0) { draggingWin = false; draggingScale = false; }
        return super.mouseReleased(click);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (draggingWin) {
            winX = (int) mx - dragOffX;
            winY = (int) my - dragOffY;
        }
        if (draggingScale) {
            int trackX = winX + pd() + font.width("Scale ") + 4;
            int trackW = W() - pd() * 2 - font.width("Scale ") - 8;
            updateScale((int) mx, trackX, trackW);
        }

        // Drag numeric settings
        if (GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), 0) == 1 && openSettingsFor != null) {
            dragNumericSettings((int) mx, (int) my);
        }
    }

    private void dragNumericSettings(int mx, int my) {
        Module mod = openSettingsFor;
        List<Module.Category> cats = OyVey.moduleManager.getCategories();
        if (selectedCat >= cats.size()) return;
        List<Module> mods = OyVey.moduleManager.getModulesByCategory(cats.get(selectedCat));
        int modIdx = mods.indexOf(mod);
        if (modIdx < 0) return;

        int wx = winX, wy = winY, W = W(), H = H(), fh = fh(), mh = mh(), hh = hh();
        int startY = wy + hh + 22;
        int modY   = startY + modIdx * mh;
        int panelX = wx + W;
        int panelW = 100;
        if (panelX + panelW > width) panelX = wx - panelW;

        List<Setting<?>> settings = new ArrayList<>();
        for (Setting<?> s : mod.getSettings()) {
            if (s.getName().equalsIgnoreCase("Enabled")) continue;
            if (s.getName().equalsIgnoreCase("DisplayName")) continue;
            if (!s.isVisible()) continue;
            settings.add(s);
        }

        int panelH = settings.size() * 14 + 14;
        int panelY = Math.min(modY, wy + H - fh - panelH);

        for (int i = 0; i < settings.size(); i++) {
            Setting<?> s = settings.get(i);
            int sy = panelY + 14 + i * 14;
            if (my < sy || my >= sy + 14) continue;
            if (!(s.getValue() instanceof Number)) continue;
            Number min = (Number) s.getMin(), max = (Number) s.getMax();
            if (min == null || max == null) continue;
            int trackX2 = panelX + 4;
            int trackW2 = panelW - 8;
            float pct = Math.max(0, Math.min(1, (float)(mx - trackX2) / trackW2));
            setNumericSetting((Setting<Number>) s, pct);
            break;
        }
    }

    private void updateScale(int mx, int trackX, int trackW) {
        float pct = Math.max(0, Math.min(1, (float)(mx - trackX) / trackW));
        guiScale = SCALE_MIN + (SCALE_MAX - SCALE_MIN) * pct;
        guiScale = Math.round(guiScale * 10) / 10f;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (listeningForBind != null) {
            int key = input.input();
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                ((Setting<me.alpha432.oyvey.features.settings.Bind>) listeningForBind)
                        .setValue(new me.alpha432.oyvey.features.settings.Bind(-1));
            } else {
                ((Setting<me.alpha432.oyvey.features.settings.Bind>) listeningForBind)
                        .setValue(new me.alpha432.oyvey.features.settings.Bind(key));
            }
            listeningForBind = null;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) { return super.charTyped(input); }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        return super.mouseScrolled(mouseX, mouseY, h, v);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics ctx, int mx, int my, float delta) {}

    // ── Compat ────────────────────────────────────────────────────────────────
    public ArrayList<Widget> getComponents() { return widgets; }
    public int getTextOffset() { return -6; }
    public static Color getColorClipboard() { return colorClipboard; }
    public static void setColorClipboard(Color color) { colorClipboard = color; }
}
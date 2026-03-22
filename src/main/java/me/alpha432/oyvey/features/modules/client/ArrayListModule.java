package me.alpha432.oyvey.features.modules.hud;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.Module;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ArrayList HUD — rainbow chroma, sorted by text width (widest on top).
 * Uses getFullArrayString() so displayName + [info] renders correctly.
 *
 * Place in:
 *   src/main/java/me/alpha432/oyvey/features/modules/hud/ArrayListModule.java
 */
public class ArrayListModule extends Module {

    private static final float CHROMA_SPEED = 3.5f; // seconds per full hue cycle
    private static final int   MARGIN_RIGHT = 2;
    private static final int   MARGIN_TOP   = 2;
    private static final int   ENTRY_H      = 10;

    public ArrayListModule() {
        super("ArrayList", "Shows enabled modules on screen", Category.HUD);
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        GuiGraphics ctx     = event.getContext();
        int         screenW = mc.getWindow().getGuiScaledWidth();

        List<Module> enabled = OyVey.moduleManager.getModules()
                .stream()
                .filter(m -> m.isEnabled() && !(m instanceof ArrayListModule))
                .sorted(Comparator.comparingInt(m -> -mc.font.width(m.getFullArrayString())))
                .collect(Collectors.toList());

        long  now = System.currentTimeMillis();
        float t   = (now % (long)(CHROMA_SPEED * 1000)) / (CHROMA_SPEED * 1000f);

        for (int i = 0; i < enabled.size(); i++) {
            Module mod  = enabled.get(i);
            String text = mod.getFullArrayString();
            int    w    = mc.font.width(text);
            int    x    = screenW - MARGIN_RIGHT - w - 2;
            int    y    = MARGIN_TOP + i * ENTRY_H;

            float hue   = (t + i * 0.04f) % 1f;
            int   color = chromaColor(hue);

            // Dark backdrop for legibility
            ctx.fill(x - 1, y - 1, screenW - MARGIN_RIGHT + 1, y + ENTRY_H - 1, 0x55000000);

            // Left chroma accent bar (2px wide)
            ctx.fill(x - 3, y - 1, x - 1, y + ENTRY_H - 1, color);

            // Module text
            ctx.drawString(mc.font, text, x, y, color);
        }
    }

    private static int chromaColor(float hue) {
        if (hue < 0) hue += 1f;
        Color c = Color.getHSBColor(hue, 0.8f, 1.0f);
        return 0xFF000000 | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }
}
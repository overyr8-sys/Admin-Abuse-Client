package me.alpha432.oyvey.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.ColorUtil;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

/**
 * ESP — three modes: Box (3D outline), Shader (filled+outline), Glow (2D screen rect).
 * Place in: src/main/java/me/alpha432/oyvey/features/modules/render/ESP.java
 */
public class ESP extends Module {

    public enum Mode { Box, Shader, Glow }

    private final Setting<Mode>    mode      = mode("Mode",      Mode.Box);
    private final Setting<Boolean> players   = bool("Players",   true);
    private final Setting<Boolean> mobs      = bool("Mobs",      false);
    private final Setting<Float>   lineWidth = num("LineWidth",  1.5f, 0.5f, 4.0f);
    private final Setting<Float>   fillAlpha = num("FillAlpha",  0.15f, 0.0f, 1.0f);

    public ESP() {
        super("ESP", "Renders entities through walls", Category.RENDER);
    }

    // ── 3D render (Box + Shader) ──────────────────────────────────────────────

    @Override
    public void onRender3D(Render3DEvent event) {
        if (mode.getValue() == Mode.Glow) return;

        PoseStack stack = event.getMatrix();
        float elapsed   = (System.currentTimeMillis() % 10000) / 10000f;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!shouldRender(entity)) continue;

            AABB box   = entity.getBoundingBox();
            Color color = getColorObj(elapsed, entity.getId());

            if (mode.getValue() == Mode.Shader) {
                // Filled box
                Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                        (int)(fillAlpha.getValue() * 255));
                RenderUtil.drawBoxFilled(stack, box, fill);
            }
            // Outline for both Box and Shader
            RenderUtil.drawBox(stack, box, color, lineWidth.getValue());
        }
    }

    // ── 2D render (Glow) ─────────────────────────────────────────────────────

    @Override
    public void onRender2D(Render2DEvent event) {
        if (mode.getValue() != Mode.Glow) return;

        var ctx     = event.getContext();
        float elapsed = (System.currentTimeMillis() % 10000) / 10000f;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!shouldRender(entity)) continue;

            AABB box = entity.getBoundingBox();
            int color = getColorInt(elapsed, entity.getId());

            // Project all 8 corners to screen
            float minSX = sw, minSY = sh, maxSX = 0, maxSY = 0;
            boolean anyVisible = false;

            double[][] corners = {
                    {box.minX, box.minY, box.minZ}, {box.maxX, box.minY, box.minZ},
                    {box.minX, box.maxY, box.minZ}, {box.maxX, box.maxY, box.minZ},
                    {box.minX, box.minY, box.maxZ}, {box.maxX, box.minY, box.maxZ},
                    {box.minX, box.maxY, box.maxZ}, {box.maxX, box.maxY, box.maxZ}
            };

            for (double[] c : corners) {
                float[] s = worldToScreen(c[0], c[1], c[2]);
                if (s == null) continue;
                anyVisible = true;
                minSX = Math.min(minSX, s[0]); minSY = Math.min(minSY, s[1]);
                maxSX = Math.max(maxSX, s[0]); maxSY = Math.max(maxSY, s[1]);
            }

            if (!anyVisible) continue;

            int x1 = (int) Math.max(0,  minSX);
            int y1 = (int) Math.max(0,  minSY);
            int x2 = (int) Math.min(sw, maxSX);
            int y2 = (int) Math.min(sh, maxSY);
            int lw = Math.max(1, Math.round(lineWidth.getValue()));

            // 4-layer soft glow
            for (int g = 3; g >= 0; g--) {
                int alpha    = Math.max(0, (int)((1.0f - g * 0.22f) * 255));
                int glowColor = (color & 0x00FFFFFF) | (alpha << 24);
                int pad = g;
                drawRect2D(ctx, x1 - pad, y1 - pad, x2 + pad, y2 + pad, lw, glowColor);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean shouldRender(Entity entity) {
        if (entity == mc.player) return false;
        if (!entity.isAlive()) return false;
        if (entity instanceof Player) return players.getValue();
        return mobs.getValue();
    }

    private Color getColorObj(float elapsed, int id) {
        if (ClickGuiModule.getInstance().rainbow.getValue()) {
            return ColorUtil.rainbow(
                    (int)(id * 0.05f * ClickGuiModule.getInstance().rainbowHue.getValue())
            );
        }
        return ClickGuiModule.getInstance().color.getValue();
    }

    private int getColorInt(float elapsed, int id) {
        Color c = getColorObj(elapsed, id);
        return 0xFF000000 | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    private void drawRect2D(net.minecraft.client.gui.GuiGraphics ctx,
                            int x1, int y1, int x2, int y2, int lw, int color) {
        ctx.fill(x1,      y1,      x2,      y1 + lw, color);
        ctx.fill(x1,      y2 - lw, x2,      y2,      color);
        ctx.fill(x1,      y1,      x1 + lw, y2,      color);
        ctx.fill(x2 - lw, y1,      x2,      y2,      color);
    }

    private float[] worldToScreen(double wx, double wy, double wz) {
        try {
            var camera = mc.gameRenderer.getMainCamera();
            var window = mc.getWindow();
            int sw = window.getGuiScaledWidth();
            int sh = window.getGuiScaledHeight();

            Vec3 camPos = camera.position();
            double dx = wx - camPos.x, dy = wy - camPos.y, dz = wz - camPos.z;

            org.joml.Quaternionf qInv = new org.joml.Quaternionf(camera.rotation()).conjugate();
            org.joml.Vector3f vec = new org.joml.Vector3f((float)dx, (float)dy, (float)dz);
            qInv.transform(vec);

            if (vec.z >= 0) return null;

            double fov    = Math.toRadians(mc.options.fov().get());
            double aspect = (double) window.getWidth() / window.getHeight();
            double f      = 1.0 / Math.tan(fov / 2.0);

            float ndcX = (float)(vec.x / (-vec.z) * f / aspect);
            float ndcY = (float)(vec.y / (-vec.z) * f);
            float sx   = (ndcX + 1f) / 2f * sw;
            float sy   = (1f - ndcY) / 2f * sh;

            if (sx < -200 || sx > sw + 200 || sy < -200 || sy > sh + 200) return null;
            return new float[]{sx, sy};
        } catch (Exception e) {
            return null;
        }
    }
}
package me.alpha432.oyvey.features.modules.render;

import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.ColorUtil;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Nametags — clean modern player nametags with armor, health, ping, shulker preview.
 * Place in: src/main/java/me/alpha432/oyvey/features/modules/render/Nametags.java
 */
public class Nametags extends Module {

    private final Setting<Boolean> health        = bool("Health",        true);
    private final Setting<Boolean> ping          = bool("Ping",          true);
    private final Setting<Boolean> armor         = bool("Armor",         true);
    private final Setting<Boolean> armorDurability = bool("Durability",  true);
    private final Setting<Boolean> heldItem      = bool("HeldItem",      true);
    private final Setting<Boolean> shulkerPreview = bool("ShulkerPreview", true);
    private final Setting<Boolean> invisibles    = bool("Invisibles",    false);
    private final Setting<Float>   scale         = num("Scale",          1.0f, 0.5f, 3.0f);

    // Colors
    private static final int C_BG        = 0xCC0D0F12;
    private static final int C_BORDER    = 0x44FFFFFF;
    private static final int C_NAME      = 0xFFFFFFFF;
    private static final int C_HEALTH_HI = 0xFF55FF55;
    private static final int C_HEALTH_MID= 0xFFFFFF55;
    private static final int C_HEALTH_LO = 0xFFFF5555;
    private static final int C_PING_GOOD = 0xFF55FF55;
    private static final int C_PING_MID  = 0xFFFFFF55;
    private static final int C_PING_BAD  = 0xFFFF5555;
    private static final int C_DIM       = 0xFF888888;

    public Nametags() {
        super("Nametags", "Shows improved player nametags", Category.RENDER);
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        GuiGraphics ctx = event.getContext();

        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            if (!invisibles.getValue() && player.isInvisible()) continue;

            // Project player position to screen
            float[] screen = worldToScreen(
                    player.getX(),
                    player.getY() + player.getBbHeight() + 0.3,
                    player.getZ()
            );
            if (screen == null) continue;

            int sx = (int) screen[0];
            int sy = (int) screen[1];

            renderNameTag(ctx, player, sx, sy);
        }
    }

    private void renderNameTag(GuiGraphics ctx, Player player, int sx, int sy) {
        float elapsed = (System.currentTimeMillis() % 10000) / 10000f;
        int accent = accentColor(elapsed, player.getId() * 0.05f);
        float sc = scale.getValue();

        // ── Gather info ───────────────────────────────────────────────────────
        String name    = player.getName().getString();
        float  hp      = player.getHealth() + player.getAbsorptionAmount();
        int    pingVal = getPing(player);

        // ── Build main tag text ───────────────────────────────────────────────
        String nameStr   = name;
        String healthStr = health.getValue()  ? String.format(" %.0fHP", hp)  : "";
        String pingStr   = ping.getValue()    ? String.format(" %dms", pingVal) : "";

        int nameW  = (int)(mc.font.width(nameStr) * sc);
        int hpW    = (int)(mc.font.width(healthStr) * sc);
        int pingW  = (int)(mc.font.width(pingStr) * sc);
        int totalW = nameW + hpW + pingW + (int)(10 * sc);

        int tagH = (int)(13 * sc);
        int tagX = sx - totalW / 2;
        int tagY = sy - tagH;

        // ── Background ────────────────────────────────────────────────────────
        fill(ctx, tagX - 2, tagY - 2, tagX + totalW + 2, tagY + tagH + 2, C_BG);
        // Accent top bar
        fill(ctx, tagX - 2, tagY - 2, tagX + totalW + 2, tagY - 1, accent);
        // Border
        outline(ctx, tagX - 2, tagY - 2, totalW + 4, tagH + 4, C_BORDER);

        // ── Name ──────────────────────────────────────────────────────────────
        ctx.drawString(mc.font, nameStr, tagX + 4, tagY + 3, accent);

        // ── Health ────────────────────────────────────────────────────────────
        if (health.getValue()) {
            int hpColor = hp > 16 ? C_HEALTH_HI : hp > 8 ? C_HEALTH_MID : C_HEALTH_LO;
            ctx.drawString(mc.font, healthStr, tagX + 4 + nameW, tagY + 3, hpColor);
        }

        // ── Ping ──────────────────────────────────────────────────────────────
        if (ping.getValue()) {
            int pingColor = pingVal < 80 ? C_PING_GOOD : pingVal < 150 ? C_PING_MID : C_PING_BAD;
            ctx.drawString(mc.font, pingStr, tagX + 4 + nameW + hpW, tagY + 3, pingColor);
        }

        // ── Armor row ─────────────────────────────────────────────────────────
        int armorY = tagY - 2;

        if (armor.getValue()) {
            List<ItemStack> armorPieces = new ArrayList<>();
            // Armor slots: 36=boots, 37=leggings, 38=chestplate, 39=helmet
            for (int i = 39; i >= 36; i--) {
                armorPieces.add(player.getInventory().getItem(i));
            }
            armorPieces.add(player.getMainHandItem());

            int armorRowW = 0;
            for (ItemStack s : armorPieces) if (!s.isEmpty()) armorRowW += 18;

            int ax = sx - armorRowW / 2;
            int ay = armorY - 18;

            for (ItemStack stack : armorPieces) {
                if (stack.isEmpty()) continue;
                ctx.renderItem(stack, ax, ay);

                if (armorDurability.getValue() && stack.isDamageableItem()) {
                    int maxDmg = stack.getMaxDamage();
                    int curDmg = stack.getDamageValue();
                    int pct    = (int)(((float)(maxDmg - curDmg) / maxDmg) * 100);
                    int durColor = pct > 60 ? C_HEALTH_HI : pct > 30 ? C_HEALTH_MID : C_HEALTH_LO;
                    String durStr = pct + "%";
                    ctx.drawString(mc.font, durStr, ax + 8 - mc.font.width(durStr) / 2, ay + 18, durColor);
                    ay += 8; // shift up to make room for text
                    ay -= 8; // reset
                }

                // Shulker preview
                if (shulkerPreview.getValue()
                        && stack.getItem() instanceof BlockItem bi
                        && bi.getBlock() instanceof ShulkerBoxBlock) {
                    renderShulkerPreview(ctx, stack, ax, ay - 60);
                }

                ax += 18;
            }

            armorY = ay - 2;
        }

        // ── Held item name ────────────────────────────────────────────────────
        if (heldItem.getValue()) {
            ItemStack held = player.getMainHandItem();
            if (!held.isEmpty() && held.has(DataComponents.CUSTOM_NAME)) {
                String itemName = held.getDisplayName().getString();
                int iw = mc.font.width(itemName);
                int ix = sx - iw / 2;
                int iy = tagY - (armor.getValue() ? 22 : 4) - 10;
                fill(ctx, ix - 2, iy - 1, ix + iw + 2, iy + 9, 0xAA000000);
                ctx.drawString(mc.font, itemName, ix, iy, 0xFFFFFFFF);
            }
        }
    }

    private void renderShulkerPreview(GuiGraphics ctx, ItemStack shulker, int x, int y) {
        ItemContainerContents contents = shulker.get(DataComponents.CONTAINER);
        if (contents == null) return;

        List<ItemStack> items = new ArrayList<>();
        contents.stream().forEach(items::add);
        items.removeIf(ItemStack::isEmpty);
        if (items.isEmpty()) return;

        // Show up to 9 items in a 3x3 grid
        int cols   = 3;
        int rows   = (int) Math.ceil(Math.min(items.size(), 9) / (float) cols);
        int gridW  = cols * 18 + 4;
        int gridH  = rows * 18 + 4;
        int gx     = x - gridW / 2;
        int gy     = y - gridH;

        // Panel background
        fill(ctx, gx, gy, gx + gridW, gy + gridH, 0xEE0D0F12);
        outline(ctx, gx, gy, gridW, gridH, 0x44FFFFFF);

        // Shulker color accent line
        fill(ctx, gx, gy, gx + gridW, gy + 1, getShulkerColor(shulker));

        for (int i = 0; i < Math.min(items.size(), 9); i++) {
            int col = i % cols;
            int row = i / cols;
            int ix  = gx + 2 + col * 18;
            int iy2 = gy + 2 + row * 18;
            ctx.renderItem(items.get(i), ix, iy2);
            ctx.renderItemDecorations(mc.font, items.get(i), ix, iy2);
        }
    }

    private int getShulkerColor(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock shulker) {
            var color = shulker.getColor();
            if (color != null) {
                int c = color.getTextureDiffuseColor();
                return 0xFF000000 | c;
            }
        }
        return 0xFFCC7DEB; // default purple
    }

    // ── Projection ────────────────────────────────────────────────────────────

    private float[] worldToScreen(double wx, double wy, double wz) {
        try {
            var window = mc.getWindow();
            int sw = window.getGuiScaledWidth();
            int sh = window.getGuiScaledHeight();

            var camera = mc.gameRenderer.getMainCamera();
            Vec3 camPos = camera.position();

            // Translate to camera space
            double dx = wx - camPos.x;
            double dy = wy - camPos.y;
            double dz = wz - camPos.z;

            // Rotate by camera orientation
            var rot = camera.rotation();
            org.joml.Quaternionf qInv = new org.joml.Quaternionf(rot).conjugate();

            org.joml.Vector3f vec = new org.joml.Vector3f((float) dx, (float) dy, (float) dz);
            qInv.transform(vec);

            // Behind camera
            if (vec.z >= 0) return null;

            // FOV-based projection
            double fov = Math.toRadians(mc.options.fov().get());
            double aspectRatio = (double) window.getWidth() / window.getHeight();
            double f = 1.0 / Math.tan(fov / 2.0);

            float ndcX = (float) (vec.x / (-vec.z) * f / aspectRatio);
            float ndcY = (float) (vec.y / (-vec.z) * f);

            float screenX = (ndcX + 1.0f) / 2.0f * sw;
            float screenY = (1.0f - ndcY) / 2.0f * sh;

            if (screenX < -200 || screenX > sw + 200 || screenY < -200 || screenY > sh + 200)
                return null;

            return new float[]{screenX, screenY};
        } catch (Exception e) {
            return null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int getPing(Player player) {
        try {
            var info = mc.getConnection().getPlayerInfo(player.getUUID());
            return info != null ? info.getLatency() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private int accentColor(float t, float offset) {
        if (ClickGuiModule.getInstance().rainbow.getValue()) {
            return ColorUtil.rainbow(
                    (int)(offset * ClickGuiModule.getInstance().rainbowHue.getValue())
            ).getRGB() | 0xFF000000;
        }
        Color c = ClickGuiModule.getInstance().color.getValue();
        return 0xFF000000 | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    private static void fill(GuiGraphics ctx, int x1, int y1, int x2, int y2, int color) {
        ctx.fill(x1, y1, x2, y2, color);
    }

    private static void outline(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color);
        ctx.fill(x,         y,         x + 1,     y + h,     color);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     color);
    }
}
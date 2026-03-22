package me.alpha432.oyvey.features.modules.hud;

import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.client.HudModule;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class InventoryHUD extends HudModule {

    private final Setting<Boolean> armor  = bool("Armor",   true);
    private final Setting<Boolean> hotbar = bool("Hotbar",  false);
    private final Setting<Float>   scale  = num("Scale",    1.0f, 0.5f, 2.0f);

    public InventoryHUD() {
        super("InventoryHUD", "Shows your inventory on screen", 5, 100);
    }

    @Override
    protected void render(Render2DEvent event) {
        super.render(event);
        if (nullCheck()) return;

        GuiGraphics ctx = event.getContext();
        int x = (int) getX();
        int y = (int) getY();
        float sc = scale.getValue();
        int curY = y;

        // Armor slots (39=helmet 38=chest 37=legs 36=boots)
        if (armor.getValue()) {
            int curX = x;
            for (int i = 39; i >= 36; i--) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                int rx = (int)(x + (39 - i) * 18 * sc);
                ctx.renderItem(stack, rx, curY);
                ctx.renderItemDecorations(mc.font, stack, rx, curY);
            }
            // Offhand
            int rx = (int)(x + 4 * 18 * sc + 4);
            ItemStack offhand = mc.player.getOffhandItem();
            ctx.renderItem(offhand, rx, curY);
            ctx.renderItemDecorations(mc.font, offhand, rx, curY);
            curY += 20;
        }

        // Main inventory (9-35) — 3 rows of 9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = 9 + row * 9 + col;
                ItemStack stack = mc.player.getInventory().getItem(slot);
                int rx = x + col * 18;
                int ry = curY + row * 18;
                ctx.renderItem(stack, rx, ry);
                ctx.renderItemDecorations(mc.font, stack, rx, ry);
            }
        }

        setWidth(162);
        setHeight((armor.getValue() ? 20 : 0) + 54);
    }
}
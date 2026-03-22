package me.alpha432.oyvey.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * FontManager — draws text using Montserrat via FontDescription.
 * Place in: src/main/java/me/alpha432/oyvey/util/render/FontManager.java
 */
public class FontManager {

    private static final Style REGULAR_STYLE = Style.EMPTY.withFont(
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("oyvey", "montserrat"))
    );

    private static final Style BOLD_STYLE = Style.EMPTY.withFont(
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("oyvey", "montserrat_bold"))
    );

    public static void drawRegular(GuiGraphics ctx, String text, int x, int y, int color) {
        ctx.drawString(Minecraft.getInstance().font,
                Component.literal(text).withStyle(REGULAR_STYLE), x, y, color);
    }

    public static void drawBold(GuiGraphics ctx, String text, int x, int y, int color) {
        ctx.drawString(Minecraft.getInstance().font,
                Component.literal(text).withStyle(BOLD_STYLE), x, y, color);
    }

    public static int width(String text) {
        return Minecraft.getInstance().font.width(
                Component.literal(text).withStyle(REGULAR_STYLE)
        );
    }

    public static int widthBold(String text) {
        return Minecraft.getInstance().font.width(
                Component.literal(text).withStyle(BOLD_STYLE)
        );
    }
}
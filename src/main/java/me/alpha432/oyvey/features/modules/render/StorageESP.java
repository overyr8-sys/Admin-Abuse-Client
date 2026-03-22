package me.alpha432.oyvey.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.WorldUtil;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

public class StorageESP extends Module {

    private final Setting<Boolean> chests       = bool("Chests",       true);
    private final Setting<Color>   chestColor   = color("ChestColor",  255, 165, 0,   200);
    private final Setting<Boolean> shulkers     = bool("Shulkers",     true);
    private final Setting<Color>   shulkerColor = color("ShulkerColor",148, 0,   211, 200);
    private final Setting<Boolean> barrels      = bool("Barrels",      true);
    private final Setting<Color>   barrelColor  = color("BarrelColor", 139, 90,  43,  200);
    private final Setting<Boolean> furnaces     = bool("Furnaces",     false);
    private final Setting<Color>   furnaceColor = color("FurnaceColor",200, 100, 50,  200);
    private final Setting<Boolean> hoppers      = bool("Hoppers",      false);
    private final Setting<Color>   hopperColor  = color("HopperColor", 120, 120, 120, 200);
    private final Setting<Boolean> filled       = bool("Filled",       true);
    private final Setting<Float>   fillAlpha    = num("FillAlpha",     0.25f, 0.0f, 1.0f);
    private final Setting<Float>   lineW        = num("LineWidth",     1.5f,  0.5f, 3.0f);

    public StorageESP() {
        super("StorageESP", "Highlights storage blocks through walls", Category.RENDER);
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        PoseStack stack = event.getMatrix();
        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        for (BlockEntity be : WorldUtil.getBlockEntities()) {
            Color color = getColor(be);
            if (color == null) continue;

            // Offset by camera position same as ESP
            AABB box = new AABB(be.getBlockPos()).move(-cam.x, -cam.y, -cam.z);

            if (filled.getValue()) {
                RenderUtil.drawBoxFilled(stack, box,
                        new Color(color.getRed(), color.getGreen(), color.getBlue(),
                                (int)(fillAlpha.getValue() * 255)));
            }
            RenderUtil.drawBox(stack, box, color, lineW.getValue());
        }
    }

    private Color getColor(BlockEntity be) {
        if ((be instanceof ChestBlockEntity || be instanceof TrappedChestBlockEntity) && chests.getValue()) return chestColor.getValue();
        if (be instanceof ShulkerBoxBlockEntity && shulkers.getValue()) return shulkerColor.getValue();
        if (be instanceof BarrelBlockEntity && barrels.getValue()) return barrelColor.getValue();
        if (be instanceof AbstractFurnaceBlockEntity && furnaces.getValue()) return furnaceColor.getValue();
        if (be instanceof HopperBlockEntity && hoppers.getValue()) return hopperColor.getValue();
        return null;
    }
}
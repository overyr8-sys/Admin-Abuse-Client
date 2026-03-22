package me.alpha432.oyvey.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StorageESP extends Module {

    private final Setting<Boolean> chests       = bool("Chests",       true);
    private final Setting<Color>   chestColor   = color("ChestColor",  255, 165, 0,   200);
    private final Setting<Boolean> eChests      = bool("EnderChests",  true);
    private final Setting<Color>   eChestColor  = color("EChestColor", 0,   255, 255, 200);
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

    private final List<BlockEntity> found = new ArrayList<>();
    private int tick = 0;

    public StorageESP() {
        super("StorageESP", "Highlights storage blocks through walls", Category.RENDER);
    }

    @Override
    public void onEnable()  { found.clear(); tick = 20; }
    @Override
    public void onDisable() { found.clear(); }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        if (++tick < 20) return;
        tick = 0;
        found.clear();

        int cx = mc.player.blockPosition().getX() >> 4;
        int cz = mc.player.blockPosition().getZ() >> 4;
        int r = Math.min(mc.options.renderDistance().get(), 8);

        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                if (!mc.level.hasChunk(x, z)) continue;
                LevelChunk chunk = mc.level.getChunk(x, z);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be.isRemoved()) continue;
                    if (getColor(be) != null) found.add(be);
                }
            }
        }
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (found.isEmpty()) return;
        PoseStack stack = event.getMatrix();

        for (BlockEntity be : new ArrayList<>(found)) {
            Color color = getColor(be);
            if (color == null) continue;

            // Use world coordinates directly - same as ESP does with entity.getBoundingBox()
            AABB box = new AABB(be.getBlockPos());

            if (filled.getValue()) {
                RenderUtil.drawBoxFilled(stack, box, new Color(
                        color.getRed(), color.getGreen(), color.getBlue(),
                        (int)(fillAlpha.getValue() * 255)));
            }
            RenderUtil.drawBox(stack, box, color, lineW.getValue());
        }
    }

    private Color getColor(BlockEntity be) {
        if ((be instanceof ChestBlockEntity || be instanceof TrappedChestBlockEntity) && chests.getValue()) return chestColor.getValue();
        if (be instanceof EnderChestBlockEntity && eChests.getValue()) return eChestColor.getValue();
        if (be instanceof ShulkerBoxBlockEntity && shulkers.getValue()) return shulkerColor.getValue();
        if (be instanceof BarrelBlockEntity && barrels.getValue()) return barrelColor.getValue();
        if (be instanceof AbstractFurnaceBlockEntity && furnaces.getValue()) return furnaceColor.getValue();
        if (be instanceof HopperBlockEntity && hoppers.getValue()) return hopperColor.getValue();
        return null;
    }
}
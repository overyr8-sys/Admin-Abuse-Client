package me.alpha432.oyvey.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    // Cache the field via reflection once
    private static Field beField = null;

    static {
        for (Field f : ClientLevel.class.getDeclaredFields()) {
            f.setAccessible(true);
            try {
                // Look for a field that holds a collection/list and has "entity" in the name
                if ((Collection.class.isAssignableFrom(f.getType()) || List.class.isAssignableFrom(f.getType()))
                        && f.getName().toLowerCase().contains("entity")) {
                    beField = f;
                }
            } catch (Exception ignored) {}
        }
        // Also check superclasses
        if (beField == null) {
            Class<?> cls = ClientLevel.class.getSuperclass();
            while (cls != null) {
                for (Field f : cls.getDeclaredFields()) {
                    f.setAccessible(true);
                    if ((Collection.class.isAssignableFrom(f.getType()) || List.class.isAssignableFrom(f.getType()))
                            && f.getName().toLowerCase().contains("blockentity")) {
                        beField = f;
                        break;
                    }
                }
                if (beField != null) break;
                cls = cls.getSuperclass();
            }
        }
    }

    public StorageESP() {
        super("StorageESP", "Highlights storage blocks through walls", Category.RENDER);
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        PoseStack stack = event.getMatrix();
        Vec3 cam = mc.gameRenderer.getMainCamera().position();

        for (BlockEntity be : getBlockEntities()) {
            Color color = getColor(be);
            if (color == null) continue;
            AABB box = new AABB(be.getBlockPos()).move(-cam.x, -cam.y, -cam.z);
            if (filled.getValue()) {
                RenderUtil.drawBoxFilled(stack, box, new Color(
                        color.getRed(), color.getGreen(), color.getBlue(),
                        (int)(fillAlpha.getValue() * 255)));
            }
            RenderUtil.drawBox(stack, box, color, lineW.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private List<BlockEntity> getBlockEntities() {
        // Try reflection on all fields to find block entity list
        if (beField != null) {
            try {
                Object val = beField.get(mc.level);
                if (val instanceof List) {
                    List<?> raw = (List<?>) val;
                    List<BlockEntity> result = new ArrayList<>();
                    for (Object o : new ArrayList<>(raw)) {
                        if (o instanceof BlockEntity) result.add((BlockEntity) o);
                    }
                    if (!result.isEmpty()) return result;
                }
            } catch (Exception ignored) {}
        }

        // Nuclear option: scan ALL fields of ClientLevel for a list containing BlockEntity
        for (Field f : ClientLevel.class.getDeclaredFields()) {
            try {
                f.setAccessible(true);
                Object val = f.get(mc.level);
                if (val instanceof List) {
                    List<?> list = (List<?>) val;
                    if (!list.isEmpty() && list.get(0) instanceof BlockEntity) {
                        List<BlockEntity> result = new ArrayList<>();
                        for (Object o : new ArrayList<>(list)) {
                            if (o instanceof BlockEntity) result.add((BlockEntity) o);
                        }
                        beField = f; // cache it
                        return result;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Final fallback: chunk scan
        List<BlockEntity> result = new ArrayList<>();
        int cx = (int)(mc.player.getX()) >> 4;
        int cz = (int)(mc.player.getZ()) >> 4;
        for (int x = cx - 4; x <= cx + 4; x++) {
            for (int z = cz - 4; z <= cz + 4; z++) {
                try {
                    var chunk = mc.level.getChunkSource().getChunk(x, z, false);
                    if (chunk != null) result.addAll(chunk.getBlockEntities().values());
                } catch (Exception ignored) {}
            }
        }
        return result;
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
package me.alpha432.oyvey.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NewChunks extends Module {

    private final Setting<Color>   color    = color("Color",    255, 50, 50, 80);
    private final Setting<Boolean> filled   = bool("Filled",    true);
    private final Setting<Float>   lineW    = num("LineWidth",  1.0f, 0.5f, 3.0f);

    // Chunks that were generated fresh (not loaded from disk)
    private final Set<ChunkPos> newChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // Previously seen chunks (loaded from disk = old)
    private final Set<ChunkPos> oldChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public NewChunks() {
        super("NewChunks", "Highlights newly generated chunks", Category.RENDER);
    }

    @Override
    public void onEnable()  { newChunks.clear(); oldChunks.clear(); }
    @Override
    public void onDisable() { newChunks.clear(); oldChunks.clear(); }

    @Override
    public void onTick() {
        if (nullCheck()) return;

        int cx = (int)(mc.player.getX()) >> 4;
        int cz = (int)(mc.player.getZ()) >> 4;
        int range = Math.min(mc.options.renderDistance().get(), 8);

        for (int x = cx - range; x <= cx + range; x++) {
            for (int z = cz - range; z <= cz + range; z++) {
                ChunkPos pos = new ChunkPos(x, z);
                if (newChunks.contains(pos) || oldChunks.contains(pos)) continue;

                if (!mc.level.hasChunk(x, z)) continue;
                LevelChunk chunk = mc.level.getChunk(x, z);

                // A new chunk has no inhabited time (player has never been there)
                // InhabitedTime is how many ticks players have been near this chunk
                if (chunk.getInhabitedTime() == 0) {
                    newChunks.add(pos);
                } else {
                    oldChunks.add(pos);
                }
            }
        }
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (newChunks.isEmpty()) return;
        PoseStack stack = event.getMatrix();
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        Color c = color.getValue();

        for (ChunkPos pos : newChunks) {
            double x1 = pos.getMinBlockX() - cam.x;
            double z1 = pos.getMinBlockZ() - cam.z;
            double x2 = pos.getMaxBlockX() + 1 - cam.x;
            double z2 = pos.getMaxBlockZ() + 1 - cam.z;
            double y  = mc.player.getY() - cam.y;

            AABB box = new AABB(x1, y - 1, z1, x2, y, z2);

            if (filled.getValue()) {
                RenderUtil.drawBoxFilled(stack, box, new Color(c.getRed(), c.getGreen(), c.getBlue(), 40));
            }
            RenderUtil.drawBox(stack, box, c, lineW.getValue());
        }
    }
}
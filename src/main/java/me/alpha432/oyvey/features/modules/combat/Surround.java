package me.alpha432.oyvey.features.modules.combat;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Surround extends Module {

    private final Setting<Boolean> obsidian  = bool("ObsidianOnly", true);
    private final Setting<Boolean> rotate    = bool("Rotate",        false);
    private final Setting<Boolean> center    = bool("Center",        true);

    // The 4 cardinal positions around the player's feet
    private static final BlockPos[] OFFSETS = {
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 0, -1)
    };

    public Surround() {
        super("Surround", "Places blocks around your feet", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        if (center.getValue()) {
            // Snap player to block center
            double cx = Math.floor(mc.player.getX()) + 0.5;
            double cz = Math.floor(mc.player.getZ()) + 0.5;
            mc.player.setPos(cx, mc.player.getY(), cz);
        }
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;

        int slot = findBlock();
        if (slot == -1) return;

        int prevSlot = mc.player.getInventory().selected;
        mc.player.getInventory().selected = slot;

        BlockPos feet = mc.player.blockPosition();

        for (BlockPos offset : OFFSETS) {
            BlockPos target = feet.offset(offset);
            if (!mc.level.getBlockState(target).isAir()) continue;

            // Find a solid neighbor to place against
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = target.relative(dir);
                if (mc.level.getBlockState(neighbor).isSolid()) {
                    Direction placeDir = dir.getOpposite();
                    Vec3 hitVec = Vec3.atCenterOf(target);

                    mc.player.connection.send(new ServerboundUseItemOnPacket(
                            InteractionHand.MAIN_HAND,
                            new BlockHitResult(hitVec, placeDir, neighbor, false),
                            0
                    ));
                    break;
                }
            }
        }

        mc.player.getInventory().selected = prevSlot;
    }

    private int findBlock() {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof BlockItem)) continue;
            if (obsidian.getValue() && stack.getItem() != Items.OBSIDIAN) continue;
            return i;
        }
        return -1;
    }
}
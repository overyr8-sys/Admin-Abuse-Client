package me.alpha432.oyvey.util;

import me.alpha432.oyvey.mixin.world.MixinClientLevel;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class WorldUtil implements Util {
    public static List<BlockEntity> getBlockEntities() {
        try {
            List<BlockEntity> list = ((MixinClientLevel)(Object) mc.level).getBlockEntityList();
            return new ArrayList<>(list);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
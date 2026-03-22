package me.alpha432.oyvey.features.modules.render;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class Fullbright extends Module {

    public enum Mode { Gamma, NightVision }

    private final Setting<Mode> mode = mode("Mode", Mode.Gamma);

    private double prevGamma = 0;

    public Fullbright() {
        super("Fullbright", "Makes the world fully bright", Category.RENDER);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        prevGamma = mc.options.gamma().get();
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        if (mode.getValue() == Mode.Gamma) {
            mc.options.gamma().set(prevGamma);
        } else {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        if (mode.getValue() == Mode.Gamma) {
            mc.options.gamma().set(10000.0);
        } else {
            mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 9999, 0, false, false));
        }
    }

    @Override
    public String getDisplayInfo() { return mode.getValue().name(); }
}
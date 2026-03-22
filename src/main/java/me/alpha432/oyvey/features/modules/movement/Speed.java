package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;

/**
 * Speed — boosts horizontal movement.
 *
 * Place in:
 *   src/main/java/me/alpha432/oyvey/features/modules/movement/Speed.java
 */
public class Speed extends Module {

    public enum Mode { STRAFE, VANILLA }

    private final Setting<Mode>   mode  = mode("Mode",  Mode.STRAFE);
    private final Setting<Double> speed = num("Speed", 1.4, 1.0, 5.0);

    public Speed() {
        super("Speed", "Makes you move faster", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;

        if (mode.getValue() == Mode.VANILLA) {
            mc.player.getAbilities().setWalkingSpeed((float)(speed.getValue() / 20.0));
            return;
        }

        // STRAFE: only boost when on ground and actually moving
        if (!mc.player.onGround()) return;

        double vx  = mc.player.getDeltaMovement().x;
        double vz  = mc.player.getDeltaMovement().z;
        double len = Math.sqrt(vx * vx + vz * vz);

        if (len > 0.001) {
            double factor = speed.getValue() / 20.0;
            mc.player.setDeltaMovement(
                    vx / len * factor,
                    mc.player.getDeltaMovement().y,
                    vz / len * factor
            );
        }
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        mc.player.getAbilities().setWalkingSpeed(0.1f);
    }

    @Override
    public String getDisplayInfo() {
        return mode.getValue().name();
    }
}
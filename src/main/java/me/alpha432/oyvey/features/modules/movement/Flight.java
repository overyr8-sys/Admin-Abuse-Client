package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;

/**
 * Flight — VANILLA, VELOCITY, ELYTRA, ANTIKICK modes.
 * Place in: src/main/java/me/alpha432/oyvey/features/modules/movement/Flight.java
 */
public class Flight extends Module {

    public enum Mode { VANILLA, VELOCITY, ANTIKICK }

    private final Setting<Mode>    mode     = mode("Mode",    Mode.VANILLA);
    private final Setting<Double>  speed    = num("Speed",    2.5, 0.1, 10.0);
    private final Setting<Integer> akDelay  = num("AK-Delay", 20, 0, 60);
    private final Setting<Integer> akOffTime= num("AK-OffTime",3, 0, 20);

    private int antiKickTick = 0;
    private int akDelayLeft  = 0;
    private int akOffLeft    = 0;

    public Flight() {
        super("Flight", "Allows you to fly", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        antiKickTick = 0;
        akDelayLeft  = akDelay.getValue();
        akOffLeft    = akOffTime.getValue();
        if (mode.getValue() == Mode.VANILLA) {
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().mayfly = true;
            mc.player.onUpdateAbilities();
        }
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().mayfly = false;
        mc.player.onUpdateAbilities();
        mc.player.setDeltaMovement(0, 0, 0);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;

        switch (mode.getValue()) {
            case VANILLA  -> {
                mc.player.getAbilities().flying = true;
                mc.player.getAbilities().mayfly = true;
                mc.player.getAbilities().setFlyingSpeed((float)(speed.getValue() / 20.0));
            }
            case VELOCITY -> velocityFlight();
            case ANTIKICK -> antiKickFlight();
        }
    }

    @Override
    public String getDisplayInfo() { return mode.getValue().name(); }

    // ── Flight modes ──────────────────────────────────────────────────────────

    private void velocityFlight() {
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().mayfly = false;

        double spd = speed.getValue() / 20.0;
        double vx = 0, vy = 0, vz = 0;
        float yaw = mc.player.getYRot();

        if (mc.options.keyUp.isDown())    { vx -= Math.sin(Math.toRadians(yaw)) * spd; vz += Math.cos(Math.toRadians(yaw)) * spd; }
        if (mc.options.keyDown.isDown())  { vx += Math.sin(Math.toRadians(yaw)) * spd; vz -= Math.cos(Math.toRadians(yaw)) * spd; }
        if (mc.options.keyLeft.isDown())  { vx -= Math.sin(Math.toRadians(yaw - 90)) * spd; vz += Math.cos(Math.toRadians(yaw - 90)) * spd; }
        if (mc.options.keyRight.isDown()) { vx -= Math.sin(Math.toRadians(yaw + 90)) * spd; vz += Math.cos(Math.toRadians(yaw + 90)) * spd; }
        if (mc.options.keyJump.isDown())  vy =  spd;
        if (mc.options.keyShift.isDown()) vy = -spd;

        mc.player.setDeltaMovement(vx, vy, vz);
        mc.player.fallDistance = 0;
    }

    private void antiKickFlight() {
        // Vanilla flight
        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().mayfly = true;
        mc.player.getAbilities().setFlyingSpeed((float)(speed.getValue() / 20.0));
        mc.player.fallDistance = 0;

        // Antikick logic — move down slightly every akDelay ticks for akOffTime ticks
        if (akDelayLeft > 0) {
            akDelayLeft--;
        } else if (akOffLeft > 0) {
            akOffLeft--;
            // Only move down if there's air below or we're above the block
            net.minecraft.core.BlockPos playerPos = mc.player.blockPosition();
            net.minecraft.core.BlockPos below = playerPos.below();
            boolean airBelow = mc.level.getBlockState(below).isAir();
            boolean aboveBlock = mc.player.getY() >= below.getY() + 1.11;
            if (airBelow || aboveBlock) {
                mc.player.move(net.minecraft.world.entity.MoverType.SELF,
                        new net.minecraft.world.phys.Vec3(0, -0.11, 0));
            }
        } else {
            akDelayLeft = akDelay.getValue();
            akOffLeft   = akOffTime.getValue();
        }
    }
}
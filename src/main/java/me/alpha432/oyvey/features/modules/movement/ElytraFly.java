package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

/**
 * ElytraFly — 2b2t-style elytra flight.
 * Requires elytra equipped in chest slot.
 * Place in: src/main/java/me/alpha432/oyvey/features/modules/movement/ElytraFly.java
 */
public class ElytraFly extends Module {

    public enum Mode { Normal, Control }

    private final Setting<Mode>    mode        = mode("Mode",        Mode.Normal);
    private final Setting<Float>   speed       = num("Speed",        1.82f, 0.0f, 10.0f);
    private final Setting<Float>   downSpeed   = num("DownSpeed",    1.82f, 0.0f, 10.0f);
    private final Setting<Float>   glideSpeed  = num("GlideSpeed",   1.0f,  0.0f, 10.0f);
    private final Setting<Boolean> instantFly  = bool("InstantFly",  true);

    private int instantFlyTick = 0;

    public ElytraFly() {
        super("ElytraFly", "2b2t-style elytra flight", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        instantFlyTick = 0;
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        mc.player.setDeltaMovement(0, 0, 0);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;

        // Must be wearing elytra
        if (mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) return;

        // InstantFly — send start gliding packet when off ground
        if (!mc.player.isFallFlying()) {
            if (instantFly.getValue() && !mc.player.onGround()) {
                instantFlyTick++;
                if (instantFlyTick >= 20) {
                    instantFlyTick = 0;
                    mc.player.connection.send(new ServerboundPlayerCommandPacket(
                            mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
                    ));
                }
            }
            return;
        }

        // Flying — apply motion
        switch (mode.getValue()) {
            case Normal  -> handleNormal();
            case Control -> handleControl();
        }

        mc.player.fallDistance = 0;
    }

    @Override
    public String getDisplayInfo() { return mode.getValue().name(); }

    // ── Flight handlers ───────────────────────────────────────────────────────

    private void handleNormal() {
        float glide = -(glideSpeed.getValue() / 10000f);

        if (mc.options.keyJump.isDown()) {
            // Accelerate upward
            double[] dir = directionSpeed(speed.getValue());
            mc.player.setDeltaMovement(dir[0], -glide + 0.1, dir[1]);
            return;
        }

        if (mc.options.keyShift.isDown()) {
            mc.player.setDeltaMovement(
                    mc.player.getDeltaMovement().x,
                    -downSpeed.getValue(),
                    mc.player.getDeltaMovement().z
            );
            return;
        }

        // Normal horizontal flight
        if (mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()) {
            double[] dir = directionSpeed(speed.getValue());
            mc.player.setDeltaMovement(dir[0], glide, dir[1]);
        } else {
            mc.player.setDeltaMovement(
                    mc.player.getDeltaMovement().x * 0.9,
                    glide,
                    mc.player.getDeltaMovement().z * 0.9
            );
        }
    }

    private void handleControl() {
        float glide = -(glideSpeed.getValue() / 10000f);
        float pitch = mc.player.getXRot();

        double[] dir = directionSpeed(speed.getValue());

        if (mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()) {
            // Scale horizontal speed by pitch
            double pitchFactor = Math.cos(Math.toRadians(Math.abs(pitch)));
            mc.player.setDeltaMovement(
                    dir[0] * pitchFactor,
                    (float)(-Math.toRadians(pitch) * speed.getValue() * 0.1),
                    dir[1] * pitchFactor
            );
        } else {
            mc.player.setDeltaMovement(0, glide, 0);
        }

        if (mc.options.keyShift.isDown()) {
            mc.player.setDeltaMovement(
                    mc.player.getDeltaMovement().x,
                    -downSpeed.getValue(),
                    mc.player.getDeltaMovement().z
            );
        }
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private double[] directionSpeed(float spd) {
        float yaw = mc.player.getYRot();
        double forward = 0, strafe = 0;

        if (mc.options.keyUp.isDown())    forward =  1;
        if (mc.options.keyDown.isDown())  forward = -1;
        if (mc.options.keyLeft.isDown())  strafe  =  1;
        if (mc.options.keyRight.isDown()) strafe  = -1;

        double angle = Math.toRadians(yaw);
        double vx = (-Math.sin(angle) * forward + Math.cos(angle) * strafe) * spd / 20.0;
        double vz = ( Math.cos(angle) * forward + Math.sin(angle) * strafe) * spd / 20.0;
        return new double[]{vx, vz};
    }
}
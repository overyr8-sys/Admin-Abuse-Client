package me.alpha432.oyvey.manager;

import me.alpha432.oyvey.util.MathUtil;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RotationManager implements Util {

    // Real client rotations (saved before spoofing)
    private float yaw;
    private float pitch;

    // Spoofed rotations to send to server
    private float targetYaw   = Float.NaN;
    private float targetPitch = Float.NaN;
    private boolean hasSpoofed = false;

    /** Called PRE UpdateWalkingPlayerEvent — saves real rotation, applies spoof */
    public void updateRotations() {
        this.yaw   = mc.player.getYRot();
        this.pitch = mc.player.getXRot();

        if (!Float.isNaN(targetYaw) && !Float.isNaN(targetPitch)) {
            mc.player.setYRot(targetYaw);
            mc.player.yHeadRot = targetYaw;
            mc.player.setXRot(targetPitch);
            hasSpoofed = true;
        }
    }

    /** Called POST UpdateWalkingPlayerEvent — restores real camera rotation */
    public void restoreRotations() {
        if (hasSpoofed) {
            mc.player.setYRot(yaw);
            mc.player.yHeadRot = yaw;
            mc.player.setXRot(pitch);
            hasSpoofed = false;
        }
        // Clear target so we don't keep spoofing next tick unless set again
        targetYaw   = Float.NaN;
        targetPitch = Float.NaN;
    }

    /** Set a silent server-side rotation toward an entity */
    public void lookAtEntity(Entity entity) {
        float[] angle = MathUtil.calcAngle(mc.player.getEyePosition(), entity.getEyePosition());
        this.targetYaw   = angle[0];
        this.targetPitch = angle[1];
    }

    /** Set a silent server-side rotation toward a position */
    public void lookAtVec3d(Vec3 vec3d) {
        float[] angle = MathUtil.calcAngle(mc.player.getEyePosition(), new Vec3(vec3d.x, vec3d.y, vec3d.z));
        this.targetYaw   = angle[0];
        this.targetPitch = angle[1];
    }

    public void lookAtVec3d(double x, double y, double z) {
        lookAtVec3d(new Vec3(x, y, z));
    }

    public void lookAtPos(BlockPos pos) {
        float[] angle = MathUtil.calcAngle(mc.player.getEyePosition(),
                new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        this.targetYaw   = angle[0];
        this.targetPitch = angle[1];
    }

    // ── Direct setters (moves camera — use only when silent rotation not needed) ──

    public void setPlayerRotations(float yaw, float pitch) {
        mc.player.setYRot(yaw);
        mc.player.yHeadRot = yaw;
        mc.player.setXRot(pitch);
    }

    public void setPlayerYaw(float yaw) {
        mc.player.setYRot(yaw);
        mc.player.yHeadRot = yaw;
    }

    public void setPlayerPitch(float pitch) {
        mc.player.setXRot(pitch);
    }

    public float getYaw()          { return this.yaw; }
    public void  setYaw(float y)   { this.yaw = y; }
    public float getPitch()        { return this.pitch; }
    public void  setPitch(float p) { this.pitch = p; }
}
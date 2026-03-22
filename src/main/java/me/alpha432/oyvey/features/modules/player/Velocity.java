package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class Velocity extends Module {

    public enum Mode { Cancel, Reduce, Packet, Vanilla }

    private final Setting<Mode>   mode    = mode("Mode",       Mode.Reduce);
    private final Setting<Double> hReduce = num("Horizontal",  0.0,   0.0, 100.0);
    private final Setting<Double> vReduce = num("Vertical",    100.0, 0.0, 100.0);

    private int prevHurtTime = 0;

    public Velocity() {
        super("Velocity", "Reduces knockback", Category.PLAYER);
    }

    @Override
    public void onEnable() { prevHurtTime = 0; }

    @Override
    public void onTick() {
        if (nullCheck()) return;

        int hurtTime = mc.player.hurtTime;

        // Detect the moment we get hit (hurtTime resets to max ~10)
        boolean justHit = hurtTime > prevHurtTime && hurtTime >= 8;
        prevHurtTime = hurtTime;

        if (!justHit) return;

        switch (mode.getValue()) {
            case Cancel -> {
                mc.player.setDeltaMovement(
                        0,
                        mc.player.getDeltaMovement().y,
                        0
                );
            }
            case Reduce -> {
                double hMult = 1.0 - (hReduce.getValue() / 100.0);
                double vMult = 1.0 - (vReduce.getValue() / 100.0);
                mc.player.setDeltaMovement(
                        mc.player.getDeltaMovement().x * hMult,
                        mc.player.getDeltaMovement().y * vMult,
                        mc.player.getDeltaMovement().z * hMult
                );
            }
            case Packet -> {
                mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, false));
                mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(false, false));
                mc.player.setDeltaMovement(
                        mc.player.getDeltaMovement().x * (1.0 - hReduce.getValue() / 100.0),
                        mc.player.getDeltaMovement().y,
                        mc.player.getDeltaMovement().z * (1.0 - hReduce.getValue() / 100.0)
                );
            }
            case Vanilla -> {
                mc.player.setDeltaMovement(
                        mc.player.getDeltaMovement().x * 0.8,
                        mc.player.getDeltaMovement().y,
                        mc.player.getDeltaMovement().z * 0.8
                );
            }
        }
    }

    @Override
    public String getDisplayInfo() { return mode.getValue().name(); }
}
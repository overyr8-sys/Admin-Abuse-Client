package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/**
 * NoFall — prevents fall damage with multiple bypass modes.
 * Place in: src/main/java/me/alpha432/oyvey/features/modules/player/NoFall.java
 */
public class NoFall extends Module {

    public enum Mode {
        Packet,   // Sends onGround=true packet — bypasses most servers
        NoGround, // Resets fall distance — works on vanilla/no-AC servers
        SpoofGround // Sends ground packet only when about to take damage
    }

    private final Setting<Mode> mode = mode("Mode", Mode.Packet);

    public NoFall() {
        super("NoFall", "Prevents fall damage", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        if (mc.player.onGround()) return;

        switch (mode.getValue()) {
            case Packet -> {
                // Send onGround=true packet when falling — most effective bypass
                if (mc.player.getDeltaMovement().y < 0 && mc.player.fallDistance > 2.0f) {
                    mc.player.connection.send(
                            new ServerboundMovePlayerPacket.StatusOnly(true, false)
                    );
                }
            }
            case NoGround -> {
                // Reset fall distance — works on vanilla and some servers
                mc.player.fallDistance = 0;
            }
            case SpoofGround -> {
                // Only send when about to take damage (fallDistance > 3)
                if (mc.player.fallDistance > 3.0f) {
                    mc.player.connection.send(
                            new ServerboundMovePlayerPacket.StatusOnly(true, false)
                    );
                    mc.player.fallDistance = 0;
                }
            }
        }
    }

    @Override
    public String getDisplayInfo() { return mode.getValue().name(); }
}
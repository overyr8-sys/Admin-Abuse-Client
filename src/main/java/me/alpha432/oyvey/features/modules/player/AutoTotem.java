package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/**
 * AutoTotem — keeps a totem in offhand automatically.
 * Place in: src/main/java/me/alpha432/oyvey/features/modules/player/AutoTotem.java
 */
public class AutoTotem extends Module {

    private final Setting<Integer> health = num("Health", 10, 1, 20);

    public AutoTotem() {
        super("AutoTotem", "Automatically puts totems in offhand", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;

        ItemStack offhand = mc.player.getOffhandItem();

        // Already has totem in offhand
        if (offhand.getItem() == Items.TOTEM_OF_UNDYING) return;

        // Only equip if health is below threshold
        if (mc.player.getHealth() > health.getValue() && offhand.getItem() != Items.AIR) return;

        // Find totem in hotbar/inventory
        Inventory inv = mc.player.getInventory();
        int totemSlot = -1;

        // Check hotbar first (0-8), then inventory (9-35)
        for (int i = 0; i < 36; i++) {
            if (inv.getItem(i).getItem() == Items.TOTEM_OF_UNDYING) {
                totemSlot = i;
                break;
            }
        }

        if (totemSlot == -1) return;

        // Move totem to offhand via inventory click
        // First pick up the totem
        mc.gameMode.handleInventoryMouseClick(
                mc.player.containerMenu.containerId,
                totemSlot < 9 ? totemSlot + 36 : totemSlot, // convert hotbar slot to container slot
                0,
                net.minecraft.world.inventory.ClickType.PICKUP,
                mc.player
        );

        // Then place in offhand slot (slot 45 in the container)
        mc.gameMode.handleInventoryMouseClick(
                mc.player.containerMenu.containerId,
                45,
                0,
                net.minecraft.world.inventory.ClickType.PICKUP,
                mc.player
        );

        // If there was something in offhand, pick it back up
        if (!offhand.isEmpty()) {
            mc.gameMode.handleInventoryMouseClick(
                    mc.player.containerMenu.containerId,
                    totemSlot < 9 ? totemSlot + 36 : totemSlot,
                    0,
                    net.minecraft.world.inventory.ClickType.PICKUP,
                    mc.player
            );
        }
    }

    @Override
    public String getDisplayInfo() {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            if (mc.player != null && mc.player.getInventory().getItem(i).getItem() == Items.TOTEM_OF_UNDYING)
                count++;
        }
        return count + " totems";
    }
}
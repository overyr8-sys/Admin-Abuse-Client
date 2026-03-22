package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.features.modules.Module;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;

public class AutoArmor extends Module {

    private static final int[] ARMOR_CONTAINER_SLOTS = {8, 7, 6, 5}; // boots, legs, chest, helmet
    private static final EquipmentSlot[] EQUIP_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private int tick = 0;

    public AutoArmor() {
        super("AutoArmor", "Automatically equips best armor", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        if (++tick < 5) return;
        tick = 0;

        for (int ai = 0; ai < 4; ai++) {
            int armorContainerSlot = ARMOR_CONTAINER_SLOTS[ai];
            EquipmentSlot equipSlot = EQUIP_SLOTS[ai];

            ItemStack current = mc.player.getItemBySlot(equipSlot);
            int bestSlot = -1;
            int bestProt = getDefense(current);

            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;
                Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
                if (equippable == null) continue;
                if (equippable.slot() != equipSlot) continue;
                int prot = getDefense(stack);
                if (prot > bestProt) {
                    bestProt = prot;
                    bestSlot = i;
                }
            }

            if (bestSlot == -1) continue;

            int containerSlot = bestSlot < 9 ? bestSlot + 36 : bestSlot;

            if (!current.isEmpty()) {
                mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, armorContainerSlot, 0, ClickType.PICKUP, mc.player);
                mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, mc.player);
                mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, armorContainerSlot, 0, ClickType.PICKUP, mc.player);
            } else {
                mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, mc.player);
                mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, armorContainerSlot, 0, ClickType.PICKUP, mc.player);
                mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, mc.player);
            }
            return;
        }
    }

    private int getDefense(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) return 0;
        // Get defense from attribute modifiers
        var attrs = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (attrs == null) return 0;
        return (int) attrs.modifiers().stream()
                .filter(e -> e.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR.unwrapKey().orElse(null)))
                .mapToDouble(e -> e.modifier().amount())
                .sum();
    }
}
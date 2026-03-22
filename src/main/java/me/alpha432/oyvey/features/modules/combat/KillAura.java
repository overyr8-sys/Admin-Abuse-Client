package me.alpha432.oyvey.features.modules.combat;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Comparator;
import java.util.List;

public class KillAura extends Module {

    public enum Modes { Closest, Priority, Switch }

    private final Setting<Modes>   mode           = mode("Mode",          Modes.Closest);
    private final Setting<Double>  distance       = num("Range",          4.5, 1.0, 10.0);
    private final Setting<Boolean> hitDelay       = bool("HitDelay",      true);
    private final Setting<Boolean> players        = bool("Players",       true);
    private final Setting<Boolean> monsters       = bool("Monsters",      true);
    private final Setting<Boolean> neutrals       = bool("Neutrals",      false);
    private final Setting<Boolean> animals        = bool("Animals",       false);
    private final Setting<Boolean> projectiles    = bool("Projectiles",   false);
    private final Setting<Boolean> swordOnly      = bool("SwordOnly",     false);
    private final Setting<Boolean> pauseIfCrystal = bool("PauseIfCrystal",false);
    private final Setting<Boolean> pauseIfEating  = bool("PauseIfEating", false);
    private final Setting<Boolean> autoSwitch     = bool("AutoSwitch",    false);
    private final Setting<Integer> ticks          = num("Ticks",          10, 0, 40);
    private final Setting<Integer> iterations     = num("Iterations",     1, 1, 10);

    private Entity currentTarget  = null;
    private int    remainingTicks = 0;

    public KillAura() {
        super("KillAura", "Automatically attacks nearby entities", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        remainingTicks = 0;
        currentTarget  = null;
    }

    @Override
    public void onDisable() {
        currentTarget = null;
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;

        // ── Sword / item checks ───────────────────────────────────────────────
        boolean holdingSword = isSword(mc.player.getMainHandItem());

        if (!holdingSword) {
            // Pause if crystal in hand
            if (pauseIfCrystal.getValue() && mc.player.getMainHandItem().getItem() == Items.END_CRYSTAL) return;
            // Pause if eating
            if (pauseIfEating.getValue() && mc.player.isUsingItem()) return;

            // AutoSwitch
            if (autoSwitch.getValue()) {
                int slot = -1;
                for (int i = 0; i < 9; i++) {
                    if (isSword(mc.player.getInventory().getItem(i))) {
                        slot = i;
                        mc.player.getInventory().selected = slot;
                        break;
                    }
                }
                if (swordOnly.getValue() && slot == -1) return;
            } else if (swordOnly.getValue()) return;
        }

        // ── Tick cooldown ─────────────────────────────────────────────────────
        if (remainingTicks > 0) {
            remainingTicks--;
        }

        // ── Target selection ──────────────────────────────────────────────────
        Entity targetToHit = currentTarget;

        switch (mode.getValue()) {
            case Closest:
                targetToHit = mc.level.getEntities(mc.player,
                                mc.player.getBoundingBox().inflate(distance.getValue()))
                        .stream()
                        .filter(this::isValidTarget)
                        .min(Comparator.comparingDouble(e -> e.distanceToSqr(mc.player)))
                        .orElse(null);
                break;

            case Priority:
                if (targetToHit == null || !isValidTarget(targetToHit)) {
                    targetToHit = mc.level.getEntities(mc.player,
                                    mc.player.getBoundingBox().inflate(distance.getValue()))
                            .stream()
                            .filter(this::isValidTarget)
                            .min(Comparator.comparingDouble(e -> e.distanceToSqr(mc.player)))
                            .orElse(null);
                }
                break;

            case Switch:
                Entity closest = mc.level.getEntities(mc.player,
                                mc.player.getBoundingBox().inflate(distance.getValue()))
                        .stream()
                        .filter(this::isValidTarget)
                        .min(Comparator.comparingDouble(e -> e.distanceToSqr(mc.player)))
                        .orElse(null);
                targetToHit = closest != null ? closest : currentTarget;
                break;
        }

        if (targetToHit == null || targetToHit.distanceTo(mc.player) > distance.getValue()) {
            currentTarget = null;
            return;
        }

        currentTarget = targetToHit;

        // ── Silent rotation ───────────────────────────────────────────────────
        // Sets targetYaw/targetPitch in RotationManager — applied to server
        // packet in UpdateWalkingPlayerEvent PRE, restored to camera in POST.
        OyVey.rotationManager.lookAtEntity(targetToHit);

        // ── Attack cooldown check ─────────────────────────────────────────────
        boolean attackReady = !hitDelay.getValue() || mc.player.getAttackStrengthScale(0f) >= 1.0f;
        if (!attackReady) return;

        if (!hitDelay.getValue() && remainingTicks > 0) return;

        remainingTicks = ticks.getValue();

        // ── Attack ────────────────────────────────────────────────────────────
        for (int i = 0; i < iterations.getValue(); i++) {
            mc.gameMode.attack(mc.player, targetToHit);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    /** Swords have no dedicated class in 1.21.11 — detect by attack damage >= 2 */
    private boolean isSword(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemAttributeModifiers mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return mods.modifiers().stream().anyMatch(e ->
                e.attribute().is(Attributes.ATTACK_DAMAGE.unwrapKey().orElse(null))
                        && e.modifier().amount() >= 2.0
        );
    }

    @Override
    public String getDisplayInfo() {
        return mode.getValue().name();
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == mc.player) return false;
        if (!entity.isAlive()) return false;
        if (entity.distanceTo(mc.player) > distance.getValue()) return false;

        // Projectiles
        if (entity instanceof Projectile) {
            return projectiles.getValue();
        }

        // Must be living
        if (!(entity instanceof LivingEntity)) return false;
        LivingEntity living = (LivingEntity) entity;
        if (living.isDeadOrDying()) return false;

        // Players
        if (entity instanceof Player) {
            return players.getValue();
        }

        // Monsters (hostile)
        if (entity instanceof Monster) {
            return monsters.getValue();
        }

        // Animals (passive)
        if (entity instanceof Animal) {
            return animals.getValue();
        }

        // Everything else (neutral mobs etc)
        return neutrals.getValue();
    }
}
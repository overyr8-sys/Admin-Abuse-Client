package me.alpha432.oyvey.features.modules.combat;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.CrystalUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AutoCrystal — places and breaks end crystals to damage players.
 * Port of SalHack AutoCrystalRewrite to 1.21.11 oyvey-ported.
 *
 * Place in: src/main/java/me/alpha432/oyvey/features/modules/combat/AutoCrystal.java
 */
public class AutoCrystal extends Module {

    public enum BreakMode { Always, Smart }
    public enum PlaceMode { Most, Lethal }

    private final Setting<BreakMode> breakMode       = mode("BreakMode",       BreakMode.Always);
    private final Setting<PlaceMode> placeMode       = mode("PlaceMode",       PlaceMode.Most);
    private final Setting<Double>    placeRadius     = num("PlaceRadius",      4.0, 0.0, 6.0);
    private final Setting<Double>    breakRadius     = num("BreakRadius",      4.0, 0.0, 6.0);
    private final Setting<Double>    wallsRange      = num("WallsRange",       3.5, 0.0, 6.0);
    private final Setting<Float>     minDmg          = num("MinDMG",           4.0f, 0.0f, 20.0f);
    private final Setting<Float>     maxSelfDmg      = num("MaxSelfDMG",       4.0f, 0.0f, 20.0f);
    private final Setting<Float>     facePlace       = num("FacePlace",        8.0f, 0.0f, 20.0f);
    private final Setting<Boolean>   autoSwitch      = bool("AutoSwitch",      true);
    private final Setting<Boolean>   antiWeakness    = bool("AntiWeakness",    true);
    private final Setting<Boolean>   noSuicide       = bool("NoSuicide",       true);
    private final Setting<Boolean>   multiPlace      = bool("MultiPlace",      false);
    private final Setting<Boolean>   pauseIfEating   = bool("PauseIfEating",   false);
    private final Setting<Integer>   ticks           = num("Ticks",            2, 0, 20);

    private final ConcurrentHashMap<EndCrystal, Integer> attackedCrystals = new ConcurrentHashMap<>();
    private BlockPos lastPlacePos   = null;
    private String   lastTarget     = null;
    private int      remainingTicks = 0;

    public AutoCrystal() {
        super("AutoCrystal", "Places and breaks end crystals", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        attackedCrystals.clear();
        lastPlacePos   = null;
        remainingTicks = 0;
    }

    @Override
    public void onDisable() {
        attackedCrystals.clear();
        lastTarget = null;
    }

    @Override
    public void onTick() {
        if (nullCheck()) return;
        if (pauseIfEating.getValue() && mc.player.isUsingItem()) return;

        // Clear stale attacked crystals every 20 ticks
        attackedCrystals.entrySet().removeIf(e -> e.getValue() > 5 || !e.getKey().isAlive());

        if (remainingTicks > 0) {
            remainingTicks--;
        }

        // ── Find best place position ──────────────────────────────────────────
        List<BlockPos> placeLocations = new ArrayList<>();
        Player playerTarget = null;

        if (remainingTicks <= 0) {
            remainingTicks = ticks.getValue();

            List<BlockPos> crystalBlocks = CrystalUtils.findCrystalBlocks(mc.player, placeRadius.getValue().floatValue());

            float bestDamage = 0f;

            for (Player player : mc.level.players()) {
                if (player == mc.player) continue;
                if (!player.isAlive()) continue;

                float minDamageNeeded = minDmg.getValue();
                if (player.getHealth() + player.getAbsorptionAmount() <= facePlace.getValue()) {
                    minDamageNeeded = 1f;
                }

                for (BlockPos pos : crystalBlocks) {
                    double cx = pos.getX() + 0.5, cy = pos.getY() + 1.0, cz = pos.getZ() + 0.5;
                    float dmg = CrystalUtils.calculateDamage(mc.level, cx, cy, cz, player, 0);

                    if (dmg >= minDamageNeeded && dmg > bestDamage) {
                        bestDamage = dmg;
                        if (!placeLocations.contains(pos)) placeLocations.add(pos);
                        lastTarget   = player.getName().getString();
                        playerTarget = player;
                    }
                }
            }

            if (playerTarget != null && !placeLocations.isEmpty()) {
                final Player ft  = playerTarget;
                final float fmd  = playerTarget.getHealth() + playerTarget.getAbsorptionAmount() <= facePlace.getValue() ? 1f : minDmg.getValue();
                placeLocations.removeIf(pos -> CrystalUtils.calculateDamage(mc.level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, ft, 0) < fmd);
                Collections.reverse(placeLocations);
            }
        }

        // ── Break nearest crystal ─────────────────────────────────────────────
        EndCrystal crystal = getNearestCrystal();
        boolean validCrystal = crystal != null && mc.player.distanceTo(crystal) < breakRadius.getValue();

        if (validCrystal) {
            // AntiWeakness — switch to sword/tool
            if (antiWeakness.getValue()) {
                boolean hasWeakness = mc.player.getActiveEffectsMap().containsKey(
                        net.minecraft.world.effect.MobEffects.WEAKNESS
                );
                if (hasWeakness && !isSword(mc.player.getMainHandItem())) {
                    for (int i = 0; i < 9; i++) {
                        if (isSword(mc.player.getInventory().getItem(i))) {
                            mc.player.getInventory().selected = i;
                            break;
                        }
                    }
                }
            }

            OyVey.rotationManager.lookAtEntity(crystal);
            mc.gameMode.attack(mc.player, crystal);
            mc.player.swing(InteractionHand.MAIN_HAND);
            addAttacked(crystal);

            if (!multiPlace.getValue()) return;
        }

        // ── Place crystal ─────────────────────────────────────────────────────
        if (placeLocations.isEmpty() && lastPlacePos == null) return;

        // AutoSwitch to crystal
        if (autoSwitch.getValue()) {
            if (mc.player.getMainHandItem().getItem() != Items.END_CRYSTAL
                    && mc.player.getOffhandItem().getItem() != Items.END_CRYSTAL) {
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getItem(i).getItem() == Items.END_CRYSTAL) {
                        mc.player.getInventory().selected = i;
                        break;
                    }
                }
            }
        }

        if (mc.player.getMainHandItem().getItem() != Items.END_CRYSTAL
                && mc.player.getOffhandItem().getItem() != Items.END_CRYSTAL) return;

        // Find best place pos
        BlockPos selectedPos = null;
        for (BlockPos pos : placeLocations) {
            if (CrystalUtils.canPlaceCrystal(pos)) {
                selectedPos = pos;
                break;
            }
        }

        if (selectedPos == null) selectedPos = lastPlacePos;
        if (selectedPos == null) return;

        // Check self damage
        float selfDmg = CrystalUtils.calculateDamage(mc.level,
                selectedPos.getX() + 0.5, selectedPos.getY() + 1.0, selectedPos.getZ() + 0.5,
                mc.player, 0);

        if (selfDmg > maxSelfDmg.getValue()) return;
        if (noSuicide.getValue() && selfDmg >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) return;

        // Rotate toward placement
        OyVey.rotationManager.lookAtVec3d(selectedPos.getX() + 0.5, selectedPos.getY() + 0.5, selectedPos.getZ() + 0.5);

        // Determine hand
        InteractionHand hand = mc.player.getOffhandItem().getItem() == Items.END_CRYSTAL
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

        // Send place packet
        BlockHitResult hitResult = new BlockHitResult(
                new Vec3(selectedPos.getX() + 0.5, selectedPos.getY() + 1.0, selectedPos.getZ() + 0.5),
                Direction.UP,
                selectedPos,
                false
        );

        mc.player.connection.send(new ServerboundUseItemOnPacket(hand, hitResult, 0));
        mc.player.swing(hand);

        lastPlacePos = selectedPos;
    }

    @Override
    public String getDisplayInfo() {
        return lastTarget != null ? lastTarget : null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EndCrystal getNearestCrystal() {
        return mc.level.getEntitiesOfClass(EndCrystal.class,
                mc.player.getBoundingBox().inflate(breakRadius.getValue()),
                e -> isValidCrystal(e)
        ).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(mc.player))).orElse(null);
    }

    private boolean isValidCrystal(EndCrystal crystal) {
        if (!crystal.isAlive()) return false;
        if (attackedCrystals.getOrDefault(crystal, 0) > 5) return false;

        double dist = mc.player.distanceTo(crystal);
        boolean canSee = mc.player.hasLineOfSight(crystal);
        double maxDist = canSee ? breakRadius.getValue() : wallsRange.getValue();

        if (dist > maxDist) return false;

        if (breakMode.getValue() == BreakMode.Smart) {
            float selfDmg = CrystalUtils.calculateDamage(mc.level, crystal.getX(), crystal.getY(), crystal.getZ(), mc.player, 0);
            if (selfDmg > maxSelfDmg.getValue()) return false;
            if (noSuicide.getValue() && selfDmg >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) return false;

            for (Player p : mc.level.players()) {
                if (p == mc.player || !p.isAlive()) continue;
                float dmg = CrystalUtils.calculateDamage(mc.level, crystal.getX(), crystal.getY(), crystal.getZ(), p, 0);
                if (dmg > minDmg.getValue()) return true;
            }
            return false;
        }

        return true;
    }

    private boolean isSword(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemAttributeModifiers mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return mods.modifiers().stream().anyMatch(e ->
                e.attribute().is(Attributes.ATTACK_DAMAGE.unwrapKey().orElse(null))
                        && e.modifier().amount() >= 2.0
        );
    }

    private void addAttacked(EndCrystal crystal) {
        attackedCrystals.merge(crystal, 1, Integer::sum);
    }
}
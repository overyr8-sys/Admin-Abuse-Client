package me.alpha432.oyvey.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.render.HandChams;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void preRenderHands(float f, PoseStack poseStack,
                                SubmitNodeCollector submitNodeCollector,
                                LocalPlayer localPlayer, int i, CallbackInfo ci) {
        HandChams hc = OyVey.moduleManager.getModuleByClass(HandChams.class);
        if (hc == null || !hc.isEnabled()) return;

        float r = hc.red.getValue()   / 255f;
        float g = hc.green.getValue() / 255f;
        float b = hc.blue.getValue()  / 255f;
        float a = hc.alpha.getValue() / 255f;

        GL11.glColor4f(r, g, b, a);

        if (hc.mode.getValue() == HandChams.RenderMode.Wireframe) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        }
    }

    @Inject(method = "renderHandsWithItems", at = @At("RETURN"))
    private void postRenderHands(float f, PoseStack poseStack,
                                 SubmitNodeCollector submitNodeCollector,
                                 LocalPlayer localPlayer, int i, CallbackInfo ci) {
        HandChams hc = OyVey.moduleManager.getModuleByClass(HandChams.class);
        if (hc == null || !hc.isEnabled()) return;

        GL11.glColor4f(1f, 1f, 1f, 1f);

        if (hc.mode.getValue() == HandChams.RenderMode.Wireframe) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        }
    }
}
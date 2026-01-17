package net.nerdorg.minehop.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.nerdorg.minehop.MinehopClient;
import net.nerdorg.minehop.config.ConfigWrapper;
import net.nerdorg.minehop.config.MinehopConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(at = @At("TAIL"), method = "render")
    private void renderSpeedometerHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo info) {
        MinehopConfig config = ConfigWrapper.config;
        MinecraftClient client = MinecraftClient.getInstance();
        float tickDelta = client != null ? client.getRenderTickCounter().tickDelta : 1.0f;

        if (config.jHud.speedHud.show_current_speed && config.enabled) {
            MinehopClient.speedometerHud.drawMain(context, tickDelta, config);
        }
        MinehopClient.speedometerHud.drawJHUD(context, config);
    }
}
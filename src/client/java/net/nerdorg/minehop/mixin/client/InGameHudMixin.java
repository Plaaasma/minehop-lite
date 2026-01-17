package net.nerdorg.minehop.mixin.client;

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

        if (config.jHud.speedHud.show_current_speed && config.enabled) {
            MinehopClient.speedometerHud.drawMain(context, tickCounter.getTickDelta(false), config);
        }
        MinehopClient.speedometerHud.drawJHUD(context, config);
    }
}
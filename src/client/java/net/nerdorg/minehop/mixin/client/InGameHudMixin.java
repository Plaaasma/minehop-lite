package net.nerdorg.minehop.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.nerdorg.minehop.MinehopClient;
import net.nerdorg.minehop.config.ConfigWrapper;
import net.nerdorg.minehop.config.MinehopConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

@Mixin(Gui.class)
public abstract class InGameHudMixin {
    // Yarn InGameHud#render(DrawContext, RenderTickCounter) -> Mojang Gui#render(GuiGraphics, DeltaTracker)
    @Inject(at = @At("TAIL"), method = "render")
    private void renderSpeedometerHud(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo info) {
        MinehopConfig config = ConfigWrapper.config;

        if (config.jHud.speedHud.show_current_speed && config.enabled) {
            MinehopClient.speedometerHud.drawMain(context, tickCounter.getGameTimeDeltaPartialTick(true), config);
        }
        MinehopClient.speedometerHud.drawJHUD(context, config);
    }
}

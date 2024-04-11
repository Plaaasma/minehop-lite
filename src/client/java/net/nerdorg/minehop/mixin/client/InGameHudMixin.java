package net.nerdorg.minehop.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.nerdorg.minehop.Minehop;
import net.nerdorg.minehop.MinehopClient;
import net.nerdorg.minehop.config.ConfigWrapper;
import net.nerdorg.minehop.config.MinehopConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(at = @At("TAIL"), method = "render(Lnet/minecraft/client/gui/DrawContext;F)V")
    private void renderSqueedometerHud(DrawContext context, float tickDelta, CallbackInfo info) {
        MinehopConfig config = ConfigWrapper.config;

        if (config.show_current_speed && config.enabled) {
            MinehopClient.squeedometerHud.drawMain(context, tickDelta);
        }
        MinehopClient.squeedometerHud.drawSSJ(context, config);
    }
}
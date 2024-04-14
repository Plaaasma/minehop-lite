package net.nerdorg.minehop;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.render.RenderLayer;
import net.nerdorg.minehop.client.SqueedometerHud;
import net.nerdorg.minehop.config.ConfigWrapper;

public class MinehopClient implements ClientModInitializer {
	public static SqueedometerHud squeedometerHud;

	public static int jump_count = 0;
	public static boolean jumping = false;
	public static double last_jump_speed = 0;
	public static double start_jump_speed = 0;
	public static double old_jump_speed = 0;
	public static long last_jump_time = 0;
	public static long old_jump_time = 0;
	public static double last_efficiency;
	public static boolean wasOnGround = false;

    @Override
	public void onInitializeClient() {
		ConfigWrapper.loadConfig();
		squeedometerHud = new SqueedometerHud();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null) {
				if (client.options.jumpKey.isPressed()) {
					jumping = true;
				}
				else {
					jumping = false;
				}
			}
		});
	}
}
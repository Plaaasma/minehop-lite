package net.nerdorg.minehop;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.nerdorg.minehop.client.SpeedometerHud;
import net.nerdorg.minehop.config.ConfigWrapper;
import net.nerdorg.minehop.config.MinehopConfig;
import org.lwjgl.glfw.GLFW;

public class MinehopClient implements ClientModInitializer {
	public static SpeedometerHud speedometerHud;

	public static int jump_count = 0;
	public static boolean jumping = false;
	public static double last_jump_speed = 0;
	public static double start_jump_speed = 0;
	public static double old_jump_speed = 0;
	public static long last_jump_time = 0;
	public static long old_jump_time = 0;
	public static double last_efficiency;
	public static double gauge;
	public static boolean wasOnGround = false;
	private static KeyBinding toggle_movement;
    @Override
	public void onInitializeClient() {
		ConfigWrapper.loadConfig();
		speedometerHud = new SpeedometerHud();
		toggle_movement = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.minehop.toggle", // The translation key of the keybinding's name
				InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
				GLFW.GLFW_KEY_H, // The keycode of the key
				KeyBinding.Category.create(Identifier.of("minehop", "category")) // The keybinding's category.
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null) {
				if (client.options.jumpKey.isPressed()) {
					jumping = true;
				}
				else {
					jumping = false;
				}
				while (toggle_movement.wasPressed()) {
					MinehopConfig.enabled = !MinehopConfig.enabled;
				}
			}
		});
	}
}
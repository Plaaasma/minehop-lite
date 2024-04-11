package net.nerdorg.minehop;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import net.nerdorg.minehop.config.MinehopConfig;
import net.nerdorg.minehop.config.ConfigWrapper;

import java.util.HashMap;
import java.util.List;

public class Minehop implements ModInitializer {
	public static HashMap<String, List<Double>> efficiencyListMap = new HashMap<>();
	public static HashMap<String, Double> efficiencyUpdateMap = new HashMap<>();

	@Override
	public void onInitialize() {
		AutoConfig.register(MinehopConfig.class, JanksonConfigSerializer::new);
		ConfigWrapper.loadConfig();
	}
}
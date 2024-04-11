package net.nerdorg.minehop;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import net.nerdorg.minehop.config.MinehopConfig;
import net.nerdorg.minehop.config.ConfigWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Minehop implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("minehop");
    public static final String MOD_ID = "minehop";

	public static HashMap<String, List<Double>> efficiencyListMap = new HashMap<>();
	public static HashMap<String, Double> efficiencyUpdateMap = new HashMap<>();

	public static List<String> adminList = List.of(
			"lolrow",
			"Plaaasma",
			"_Moriz_"
	);

	@Override
	public void onInitialize() {
		AutoConfig.register(MinehopConfig.class, JanksonConfigSerializer::new);
		ConfigWrapper.loadConfig();
	}
}
package net.nerdorg.minehop.config;

import me.shedaniel.autoconfig.AutoConfig;

public class ConfigWrapper {
    public static MinehopConfig config;

    public static void loadConfig() {
        config = AutoConfig.getConfigHolder(MinehopConfig.class).getConfig();
    }

    public static void saveConfig(MinehopConfig minehopConfig) {
        AutoConfig.getConfigHolder(MinehopConfig.class).setConfig(minehopConfig);
        AutoConfig.getConfigHolder(MinehopConfig.class).save();
    }
}

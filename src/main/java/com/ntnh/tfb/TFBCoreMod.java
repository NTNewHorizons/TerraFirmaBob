// src/main/java/com/ntnh/tfb/TFBCoreMod.java
package com.ntnh.tfb;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.Name("TFBCoreMod")
public class TFBCoreMod implements IFMLLoadingPlugin {

    public static final Logger LOGGER = LogManager.getLogger("TFBCoreMod");

    public TFBCoreMod() {
        LOGGER.info("TFBCoreMod loading!");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0]; // Не используем ASM трансформеры
    }

    @Override
    public String getModContainerClass() {
        return null; // Используем стандартный @Mod для контейнера
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        // Здесь можно получить данные от FML, если нужно
    }

    @Override
    public String getAccessTransformerClass() {
        return null; // Не используем Access Transformers
    }

    @Override
    public boolean isRequiredMod() {
        return false; // Это не обязательный мод
    }

    @Override
    public boolean hasMinumumVersion() {
        return false;
    }
}

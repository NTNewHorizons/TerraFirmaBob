// src/main/java/com/ntnh/tfb/mixins/MixinGenLayerBiomeEdge.java
package com.ntnh.tfb.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.dunk.tfc.WorldGen.DataLayer;
import com.dunk.tfc.WorldGen.GenLayers.Biome.GenLayerBiomeEdge;

// Указываем, к какому классу применяем миксин
@Mixin(GenLayerBiomeEdge.class)
public class MixinGenLayerBiomeEdge {

    // Используем @Redirect для перехвата вызова getPHLayerAt
    @Redirect(
        method = "getInts",
        at = @At(
            value = "INVOKE",
            target = "Lcom/dunk/tfc/WorldGen/CacheManager;getPHLayerAt(II)Lcom/dunk/tfc/WorldGen/DataLayer;"))
    private DataLayer redirectGetPHLayerAt(com.dunk.tfc.WorldGen.CacheManager instance, int x, int z) {
        // Проверяем, является ли instance null (что может произойти, если TFC_Climate.getCacheManager(world) вернул
        // null)
        if (instance == null) {
            System.out.println(
                "[TFB Fix] CacheManager is null when getting pH layer at (" + x
                    + ", "
                    + z
                    + "). Returning default DataLayer.");
            // Создаем "безопасный" DataLayer. ВАЖНО: data1 != 0, чтобы избежать acidic = true.
            // --- ПРЕДПОЛОЖЕНИЕ: DataLayer(int d1, int d2, int d3) существует ---
            // --- ЗАМЕНИТЬ НА РЕАЛЬНЫЙ СПОСОБ СОЗДАНИЯ И БЕЗОПАСНОЕ ЗНАЧЕНИЕ data1 ---
            return new com.dunk.tfc.WorldGen.DataLayer(1, 0, 0); // <-- ПРЕДПОЛОЖЕНИЕ: d1=1 -> не кислый
            // --- КОНЕЦ ПРЕДПОЛОЖЕНИЯ ---
        }

        // Если instance не null, выполняем оригинальный вызов и оборачиваем в try-catch на всякий случай
        try {
            return instance.getPHLayerAt(x, z);
        } catch (NullPointerException | RuntimeException e) {
            System.out.println(
                "[TFB Fix] Failed to get pH layer at (" + x
                    + ", "
                    + z
                    + ") due to "
                    + e.getClass()
                        .getSimpleName()
                    + ". Returning default DataLayer. Error: "
                    + e.getMessage());
            // Создаем "безопасный" DataLayer. ВАЖНО: data1 != 0, чтобы избежать acidic = true.
            // --- ПРЕДПОЛОЖЕНИЕ: DataLayer(int d1, int d2, int d3) существует ---
            // --- ЗАМЕНИТЬ НА РЕАЛЬНЫЙ СПОСОБ СОЗДАНИЯ И БЕЗОПАСНОЕ ЗНАЧЕНИЕ data1 ---
            return new com.dunk.tfc.WorldGen.DataLayer(1, 0, 0); // <-- ПРЕДПОЛОЖЕНИЕ: d1=1 -> не кислый
            // --- КОНЕЦ ПРЕДПОЛОЖЕНИЯ ---
        }
    }
}

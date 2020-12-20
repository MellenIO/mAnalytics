package io.mellen.manalytics.util;

import org.bukkit.inventory.ItemStack;

public class ItemUtil {
    public static String getItemName(ItemStack stack) {
        if (stack == null) return "AIR";

        return stack.getType().name();
    }
}

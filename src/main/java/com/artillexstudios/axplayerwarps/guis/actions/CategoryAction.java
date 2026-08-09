package com.artillexstudios.axplayerwarps.guis.actions;

import com.artillexstudios.axguiframework.GuiFrame;
import com.artillexstudios.axguiframework.actions.Action;
import com.artillexstudios.axplayerwarps.category.Category;
import com.artillexstudios.axplayerwarps.category.CategoryManager;
import com.artillexstudios.axplayerwarps.guis.WarpsGui;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class CategoryAction extends Action {

    public CategoryAction() {
        super("category");
    }

    @Override
    public void run(Player player, GuiFrame gui, InventoryClickEvent event, String arguments) {
        Category category = CategoryManager.getCategories().get(arguments);
        if (category == null) {
            new WarpsGui(player).open();
        } else {
            new WarpsGui(player, category, null).open();
        }
    }
}

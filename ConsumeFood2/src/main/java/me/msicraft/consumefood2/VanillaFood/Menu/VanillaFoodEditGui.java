package me.msicraft.consumefood2.VanillaFood.Menu;

import me.msicraft.API.Data.CustomGui;
import me.msicraft.API.Food.Food;
import me.msicraft.API.Food.VanillaFood;
import me.msicraft.consumefood2.ConsumeFood2;
import me.msicraft.consumefood2.PlayerData.Data.PlayerData;
import me.msicraft.consumefood2.Utils.GuiUtil;
import me.msicraft.consumefood2.VanillaFood.VanillaFoodManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

public class VanillaFoodEditGui extends CustomGui {

    public enum Type {
        SELECT, EDIT
    }

    private final Inventory gui;
    private final ConsumeFood2 plugin;

    private final NamespacedKey selectKey;
    private final NamespacedKey editKey;

    public VanillaFoodEditGui(ConsumeFood2 plugin) {
        this.plugin = plugin;
        this.gui = Bukkit.createInventory(this, 54, "VanillaFood");

        this.selectKey = new NamespacedKey(plugin, "VanillaFood_Select");
        this.editKey = new NamespacedKey(plugin, "VanillaFood_Edit");
    }

    public void setGui(Type type, Player player) {
        gui.clear();
        switch (type) {
            case SELECT -> {
                setSelectGui(player);
            }
            case EDIT -> {
                setEditGui(player);
            }
        }
    }

    private void setSelectGui(Player player) {
        ItemStack itemStack;
        itemStack = GuiUtil.createItemStack(Material.ARROW, "Next", GuiUtil.EMPTY_LORE, -1, selectKey, "Next");
        gui.setItem(50, itemStack);
        itemStack = GuiUtil.createItemStack(Material.ARROW, "Previous", GuiUtil.EMPTY_LORE, -1, selectKey, "Previous");
        gui.setItem(48, itemStack);

        VanillaFoodManager vanillaFoodManager = plugin.getVanillaFoodManager();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);

        List<Material> materials = vanillaFoodManager.getVanillaFoodMaterials();
        int maxSize = materials.size();
        int page = (int) playerData.getTempData("VanillaFood_Select_Page", 0);
        int guiCount = 0;
        int lastCount = page * 45;

        String pageS = "Page: " + (page + 1) + "/" + ((maxSize / 45) + 1);
        itemStack = GuiUtil.createItemStack(Material.BOOK, pageS, GuiUtil.EMPTY_LORE, -1, selectKey, "Page");
        gui.setItem(49, itemStack);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Left Click: edit", NamedTextColor.YELLOW));
        lore.add(Component.text("Right Click: get item", NamedTextColor.YELLOW));
        for (int i = lastCount; i < maxSize && i < lastCount + 45; i++) {
            Material material = materials.get(i);
            VanillaFood vanillaFood = vanillaFoodManager.getVanillaFood(material);
            if (vanillaFood != null) {
                itemStack = new ItemStack(vanillaFood.getMaterial());
                ItemMeta itemMeta = itemStack.getItemMeta();
                PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();

                itemMeta.lore(lore);

                dataContainer.set(selectKey, PersistentDataType.STRING, material.name());

                itemStack.setItemMeta(itemMeta);
                gui.setItem(guiCount, itemStack);
                guiCount++;
                if (guiCount >= 45) {
                    break;
                }
            }
        }
    }

    private final int[] editSlots = new int[]{10,11,12,13,14,15,16, 19,20,21,22,23,24,25,
            28,29,30,31,32,33,34, 37,38,39,40,41,42,43, 46,47,48,49,50,51,52};

    private void setEditGui(Player player) {
        VanillaFoodManager vanillaFoodManager = plugin.getVanillaFoodManager();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (!playerData.hasTempData("VanillaFood_Edit_Key")) {
            player.sendMessage(ConsumeFood2.PREFIX.append(Component.text("material does not exist", NamedTextColor.RED)));
            player.closeInventory();
            return;
        }
        String materialS = (String) playerData.getTempData("VanillaFood_Edit_Key");
        Material material = Material.getMaterial(materialS);
        VanillaFood vanillaFood = vanillaFoodManager.getVanillaFood(material);
        ItemStack itemStack;
        itemStack = GuiUtil.createItemStack(Material.BARRIER, "&fBack", GuiUtil.EMPTY_LORE, -1,
                editKey, "Back");
        gui.setItem(0, itemStack);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Right Click: get item", NamedTextColor.YELLOW));
        lore.add(Component.empty());
        itemStack = new ItemStack(vanillaFood.getMaterial());
        ItemMeta tItemMeta = itemStack.getItemMeta();
        PersistentDataContainer tDataContainer = tItemMeta.getPersistentDataContainer();
        tDataContainer.set(editKey, PersistentDataType.STRING, "Edit_Item");

        tItemMeta.lore(lore);

        itemStack.setItemMeta(tItemMeta);
        gui.setItem(4, itemStack);

        int count = 0;
        Food.Options[] foodOptions = Food.Options.values();
        for (Food.Options options : foodOptions) {
            if (options.isCustomFoodOption() || options == Food.Options.UUID
                    || options == Food.Options.MATERIAL) {
                continue;
            }
            lore.clear();
            ItemMeta itemMeta;
            lore.add(Component.text("Left Click: edit value (change value)", NamedTextColor.YELLOW));
            lore.add(Component.text("Right Click: reset", NamedTextColor.YELLOW));
            lore.add(Component.empty());
            lore.add(Component.text("Set " + options.getDisplayName(), NamedTextColor.GRAY));
            for (String s : options.getDescription()) {
                lore.add(Component.text(s, NamedTextColor.WHITE));
            }
            lore.add(Component.empty());
            switch (options) {
                case FOOD_LEVEL -> {
                    itemStack = new ItemStack(Material.PORKCHOP);
                    lore.add(Component.text("Current Food Level: " + vanillaFood.getOptionValue(Food.Options.FOOD_LEVEL), NamedTextColor.GRAY));
                }
                case SATURATION -> {
                    itemStack = new ItemStack(Material.COOKED_PORKCHOP);
                    lore.add(Component.text("Current Saturation: " + vanillaFood.getOptionValue(Food.Options.SATURATION), NamedTextColor.GRAY));
                }
                case COOLDOWN -> {
                    itemStack = new ItemStack(Material.COMPASS);
                    lore.add(Component.text("Current Cooldown: " + vanillaFood.getOptionValue(Food.Options.COOLDOWN), NamedTextColor.GRAY));
                }
                case INSTANT_EAT -> {
                    itemStack = new ItemStack(Material.CAKE);
                    lore.add(Component.text("Current Instant Eat: " + vanillaFood.getOptionValue(Food.Options.INSTANT_EAT), NamedTextColor.GRAY));
                }
                case POTION_EFFECT -> {
                    itemStack = new ItemStack(Material.POTION);
                    lore.add(Component.text("Format: <potionType>:<level>:<duration>:<chance>", NamedTextColor.GRAY));
                    lore.add(Component.text("Current Potion Effect: ", NamedTextColor.GRAY));
                    vanillaFood.getPotionEffects().forEach(foodPotionEffect -> {
                        lore.add(Component.text(foodPotionEffect.toFormat(), NamedTextColor.GRAY));
                    });
                }
                case COMMAND -> {
                    itemStack = new ItemStack(Material.COMMAND_BLOCK);
                    lore.add(Component.text("Format: <executeType>:<command>", NamedTextColor.GRAY));
                    lore.add(Component.text("Current Command: ", NamedTextColor.GRAY));
                    vanillaFood.getCommands().forEach(foodCommand -> {
                        lore.add(Component.text(foodCommand.toFormat(), NamedTextColor.GRAY));
                    });
                }
            }
            itemMeta = itemStack.getItemMeta();
            PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
            itemMeta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(options.getDisplayName()));
            itemMeta.lore(lore);

            dataContainer.set(editKey,  PersistentDataType.STRING, options.name().toUpperCase());
            itemStack.setItemMeta(itemMeta);

            gui.setItem(editSlots[count], itemStack);
            count++;
        }
    }

    public NamespacedKey getSelectKey() {
        return selectKey;
    }

    public NamespacedKey getEditKey() {
        return editKey;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return gui;
    }

}
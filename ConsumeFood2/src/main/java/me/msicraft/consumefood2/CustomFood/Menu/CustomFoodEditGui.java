package me.msicraft.consumefood2.CustomFood.Menu;

import me.msicraft.API.Common;
import me.msicraft.API.Data.CustomGui;
import me.msicraft.API.Food.CustomFood;
import me.msicraft.API.Food.Food;
import me.msicraft.consumefood2.ConsumeFood2;
import me.msicraft.consumefood2.CustomFood.CustomFoodManager;
import me.msicraft.consumefood2.PlayerData.Data.PlayerData;
import me.msicraft.consumefood2.Utils.GuiUtil;
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

public class CustomFoodEditGui extends CustomGui {

    public enum Type {
        SELECT, EDIT, DELETE
    }

    private final Inventory gui;
    private final ConsumeFood2 plugin;

    private final NamespacedKey selectKey;
    private final NamespacedKey editKey;
    private final NamespacedKey deleteKey;

    public CustomFoodEditGui(ConsumeFood2 plugin) {
        this.plugin = plugin;
        this.gui = Bukkit.createInventory(this, 54, "CustomFood Edit");

        this.selectKey = new NamespacedKey(plugin, "CustomFood_Select");
        this.editKey = new NamespacedKey(plugin, "CustomFood_Edit");
        this.deleteKey = new NamespacedKey(plugin, "CustomFood_Delete");
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
            case DELETE -> {
                setDeleteGui(player);
            }
        }
    }

    private void setSelectGui(Player player) {
        ItemStack itemStack;
        itemStack = GuiUtil.createItemStack(Material.ARROW, "Next", GuiUtil.EMPTY_LORE, -1, selectKey, "Next");
        gui.setItem(50, itemStack);
        itemStack = GuiUtil.createItemStack(Material.ARROW, "Previous", GuiUtil.EMPTY_LORE, -1, selectKey, "Previous");
        gui.setItem(48, itemStack);
        itemStack = GuiUtil.createItemStack(Material.CAULDRON, "Delete", GuiUtil.EMPTY_LORE, -1, selectKey, "Delete");
        gui.setItem(45, itemStack);
        itemStack = GuiUtil.createItemStack(Material.WRITABLE_BOOK, "Create", GuiUtil.EMPTY_LORE, -1, selectKey, "Create");
        gui.setItem(53, itemStack);

        CustomFoodManager customFoodManager = plugin.getCustomFoodManager();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);

        List<String> internalNames = customFoodManager.getAllInternalNames();
        int maxSize = internalNames.size();
        int page = (int) playerData.getTempData("CustomFood_Select_Page", 0);
        int guiCount = 0;
        int lastCount = page * 45;

        String pageS = "Page: " + (page + 1) + "/" + ((maxSize / 45) + 1);
        itemStack = GuiUtil.createItemStack(Material.BOOK, pageS, GuiUtil.EMPTY_LORE, -1, selectKey, "Page");
        gui.setItem(49, itemStack);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Left Click: edit", NamedTextColor.YELLOW));
        lore.add(Component.text("Right Click: get item", NamedTextColor.YELLOW));
        for (int a = lastCount; a < maxSize; a++) {
            String internalName = internalNames.get(a);
            CustomFood customFood = customFoodManager.getCustomFood(internalName);
            if (customFood != null) {
                itemStack = customFood.getGuiItemStack(plugin.isUseFoodComponent());
                ItemMeta itemMeta = itemStack.getItemMeta();
                PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();

                itemMeta.lore(lore);

                dataContainer.set(selectKey, PersistentDataType.STRING, internalName);

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
        CustomFoodManager customFoodManager = plugin.getCustomFoodManager();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (!playerData.hasTempData("CustomFood_Edit_Key")) {
            player.sendMessage(ConsumeFood2.PREFIX.append(Component.text("internalName does not exist", NamedTextColor.RED)));
            player.closeInventory();
            return;
        }
        String internalName = (String) playerData.getTempData("CustomFood_Edit_Key");
        CustomFood customFood = customFoodManager.getCustomFood(internalName);
        ItemStack itemStack;
        itemStack = GuiUtil.createItemStack(Material.BARRIER, "&fBack", GuiUtil.EMPTY_LORE, -1,
                editKey, "Back");
        gui.setItem(0, itemStack);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Right Click: get item", NamedTextColor.YELLOW));
        lore.add(Component.empty());
        for (String s : customFood.getLore()) {
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(s));
        }
        itemStack = customFoodManager.createItemStack(customFood);
        ItemMeta tItemMeta = itemStack.getItemMeta();
        PersistentDataContainer tDataContainer = tItemMeta.getPersistentDataContainer();
        tDataContainer.set(editKey, PersistentDataType.STRING, "Edit_Item");
        tItemMeta.lore(lore);

        itemStack.setItemMeta(tItemMeta);
        gui.setItem(4, itemStack);

        boolean upper_1_20_5 = plugin.isUseFoodComponent();
        int count = 0;
        Food.Options[] foodOptions = Food.Options.values();
        for (Food.Options options : foodOptions) {
            if (options == Food.Options.UUID) {
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
                case MATERIAL -> {
                    itemStack = new ItemStack(customFood.getMaterial());
                    lore.add(Component.text("Current Material: " + customFood.getMaterial().name(), NamedTextColor.GRAY));
                }
                case TEXTURE_VALUE -> {
                    itemStack = new ItemStack(Material.PLAYER_HEAD);
                    itemMeta = itemStack.getItemMeta();
                    itemMeta.displayName(Component.text("TextureValue"));
                    lore.add(Component.text("Current Texture Value: " + customFood.getOptionValue(Food.Options.TEXTURE_VALUE), NamedTextColor.GRAY));
                }
                case DISPLAYNAME -> {
                    itemStack = new ItemStack(Material.OAK_SIGN);
                    lore.add(Component.text("Current DisplayName: " + customFood.getOptionValue(Food.Options.DISPLAYNAME), NamedTextColor.GRAY));
                }
                case CUSTOM_MODEL_DATA -> {
                    itemStack = new ItemStack(Material.NAME_TAG);
                    lore.add(Component.text("Current Custom Model Data: " + customFood.getOptionValue(Food.Options.CUSTOM_MODEL_DATA), NamedTextColor.GRAY));
                }
                case LORE -> {
                    itemStack = new ItemStack(Material.PAPER);
                    lore.add(Component.text("Current Lore: ", NamedTextColor.GRAY));
                    for (String s : customFood.getLore()) {
                        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(s));
                    }
                }
                case POTION_EFFECT -> {
                    itemStack = new ItemStack(Material.POTION);
                    lore.add(Component.text("Format: <potionType>:<level>:<duration>:<chance>", NamedTextColor.GRAY));
                    lore.add(Component.text("Current Potion Effect: ", NamedTextColor.GRAY));
                    customFood.getPotionEffects().forEach(foodPotionEffect -> {
                        lore.add(Component.text(foodPotionEffect.toFormat(), NamedTextColor.GRAY));
                    });
                }
                case COMMAND -> {
                    itemStack = new ItemStack(Material.COMMAND_BLOCK);
                    lore.add(Component.text("Format: <executeType>:<command>", NamedTextColor.GRAY));
                    lore.add(Component.text("Current Command: ", NamedTextColor.GRAY));
                    customFood.getCommands().forEach(foodCommand -> {
                        lore.add(Component.text(foodCommand.toFormat(), NamedTextColor.GRAY));
                    });
                }
                case FOOD_LEVEL -> {
                    itemStack = new ItemStack(Material.PORKCHOP);
                    lore.add(Component.text("Current Food Level: " + customFood.getOptionValue(Food.Options.FOOD_LEVEL), NamedTextColor.GRAY));
                }
                case SATURATION -> {
                    itemStack = new ItemStack(Material.COOKED_PORKCHOP);
                    lore.add(Component.text("Current Saturation: " + customFood.getOptionValue(Food.Options.SATURATION), NamedTextColor.GRAY));
                }
                case COOLDOWN -> {
                    itemStack = new ItemStack(Material.COMPASS);
                    lore.add(Component.text("Current Cooldown: " + customFood.getOptionValue(Food.Options.COOLDOWN), NamedTextColor.GRAY));
                }
                case ENCHANT -> {
                    itemStack = new ItemStack(Material.ENCHANTED_BOOK);
                    lore.add(Component.text("Format: <enchant>:<level>", NamedTextColor.GRAY));
                    lore.add(Component.text("Current Enchant: ", NamedTextColor.GRAY));
                    customFood.getEnchantFormatList().forEach(s -> {
                        lore.add(Component.text(s, NamedTextColor.GRAY));
                    });
                }
                case HIDE_ENCHANT -> {
                    itemStack = new ItemStack(Material.ENCHANTING_TABLE);
                    lore.add(Component.text("Current HideEnchant: " + customFood.getOptionValue(Food.Options.HIDE_ENCHANT), NamedTextColor.GRAY));
                }
                case DISABLE_CRAFTING -> {
                    itemStack = new ItemStack(Material.CRAFTING_TABLE);
                    lore.add(Component.text("Current Disable Crafting: " + customFood.getOptionValue(Food.Options.DISABLE_CRAFTING), NamedTextColor.GRAY));
                }
                case DISABLE_SMELTING -> {
                    itemStack = new ItemStack(Material.FURNACE);
                    lore.add(Component.text("Current Disable Smelting: " + customFood.getOptionValue(Food.Options.DISABLE_SMELTING), NamedTextColor.GRAY));
                }
                case DISABLE_ANVIL -> {
                    itemStack = new ItemStack(Material.ANVIL);
                    lore.add(Component.text("Current Disable Anvil: " + customFood.getOptionValue(Food.Options.DISABLE_ANVIL), NamedTextColor.GRAY));
                }
                case DISABLE_ENCHANT -> {
                    itemStack = new ItemStack(Material.ENCHANTING_TABLE);
                    lore.add(Component.text("Current Disable Enchant: " + customFood.getOptionValue(Food.Options.DISABLE_ENCHANT), NamedTextColor.GRAY));
                }
                case SOUND -> {
                    itemStack = new ItemStack(Material.JUKEBOX);
                    lore.add(Component.text("Format: <sound>:<volume>:<pitch>", NamedTextColor.GRAY));
                    lore.add(Component.text("Current Sound: " + customFood.getOptionValue(Food.Options.SOUND), NamedTextColor.GRAY));
                }
                case POTION_COLOR -> {
                    itemStack = new ItemStack(Material.SPLASH_POTION);
                    lore.add(Component.text("Current Potion Color: " + customFood.getOptionValue(Food.Options.POTION_COLOR), NamedTextColor.GRAY));
                }
                case HIDE_POTION_EFFECT -> {
                    itemStack = new ItemStack(Material.LINGERING_POTION);
                    lore.add(Component.text("Current HidePotionEffect: " + customFood.getOptionValue(Food.Options.HIDE_POTION_EFFECT), NamedTextColor.GRAY));
                }
                case UNSTACKABLE -> {
                    itemStack = new ItemStack(Material.CHEST);
                    lore.add(Component.text("Current Unstackable: " + customFood.getOptionValue(Food.Options.UNSTACKABLE), NamedTextColor.GRAY));
                }
                case INSTANT_EAT -> {
                    itemStack = new ItemStack(Material.CAKE);
                    lore.add(Component.text("Current Instant Eat: " + customFood.getOptionValue(Food.Options.INSTANT_EAT), NamedTextColor.GRAY));
                }
                case ALWAYS_EAT -> {
                    if (!upper_1_20_5) {
                        continue;
                    }
                    itemStack = new ItemStack(Material.CAKE);
                    lore.add(Component.text("Current Always Eat: " + customFood.getOptionValue(Food.Options.ALWAYS_EAT), NamedTextColor.GRAY));
                }
                case EAT_SECONDS -> {
                    if (!upper_1_20_5) {
                        continue;
                    }
                    itemStack = new ItemStack(Material.KELP);
                    lore.add(Component.text("Current Eat Seconds: " + customFood.getOptionValue(Food.Options.EAT_SECONDS), NamedTextColor.GRAY));
                }
                case MAX_STACK_SIZE -> {
                    if (!upper_1_20_5) {
                        continue;
                    }
                    itemStack = new ItemStack(Material.SHULKER_BOX);
                    lore.add(Component.text("Current Max Stack Size: " + customFood.getOptionValue(Food.Options.MAX_STACK_SIZE), NamedTextColor.GRAY));
                }
                case HIDE_ADDITIONAL_TOOLTIP -> {
                    if (!upper_1_20_5) {
                        continue;
                    }
                    itemStack = new ItemStack(Material.FIREWORK_ROCKET);
                    lore.add(Component.text("Current Hide Additional Tooltip: " + customFood.getOptionValue(Food.Options.HIDE_ADDITIONAL_TOOLTIP), NamedTextColor.GRAY));
                }
                case MAX_CONSUME_COUNT -> {
                    itemStack = new ItemStack(Material.COOKED_BEEF);
                    lore.add(Component.text("Current Max Consume Count: " + customFood.getOptionValue(Food.Options.MAX_CONSUME_COUNT), NamedTextColor.GRAY));
                }
                case DISPLAY_MAX_CONSUME_COUNT -> {
                    itemStack = new ItemStack(Material.COOKED_BEEF);
                    lore.add(Component.text("Current DisplayMaxConsumeCount: " + customFood.getOptionValue(Food.Options.DISPLAY_MAX_CONSUME_COUNT), NamedTextColor.GRAY));
                }
                case OTHER_PLUGIN_COMPATIBILITY -> {
                    itemStack = new ItemStack(Material.BEDROCK);
                    lore.add(Component.text("Current OtherPluginCompatibility: " + customFood.getOptionValue(Food.Options.OTHER_PLUGIN_COMPATIBILITY), NamedTextColor.GREEN));
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

    private void setDeleteGui(Player player) {
        ItemStack itemStack;
        itemStack = GuiUtil.createItemStack(Material.ARROW, "Next", GuiUtil.EMPTY_LORE, -1, deleteKey, "Next");
        gui.setItem(50, itemStack);
        itemStack = GuiUtil.createItemStack(Material.ARROW, "Previous", GuiUtil.EMPTY_LORE, -1, deleteKey, "Previous");
        gui.setItem(48, itemStack);
        itemStack = GuiUtil.createItemStack(Material.BARRIER, "&fBack", GuiUtil.EMPTY_LORE,
                -1, deleteKey, "Back");
        gui.setItem(45, itemStack);

        CustomFoodManager customFoodManager = plugin.getCustomFoodManager();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);

        List<String> internalNames = customFoodManager.getAllInternalNames();
        int maxSize = internalNames.size();
        int page = (int) playerData.getTempData("CustomFood_Delete_Select_Page", 0);
        int guiCount = 0;
        int lastCount = page * 45;

        String pageS = "Page: " + (page + 1) + "/" + ((maxSize / 45) + 1);
        itemStack = GuiUtil.createItemStack(Material.BOOK, pageS, GuiUtil.EMPTY_LORE, -1, selectKey, "Page");
        gui.setItem(49, itemStack);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Left Click: delete", NamedTextColor.YELLOW));
        for (int a = lastCount; a < maxSize; a++) {
            String internalName = internalNames.get(a);
            CustomFood customFood = customFoodManager.getCustomFood(internalName);
            if (customFood != null) {
                itemStack = customFood.getGuiItemStack(plugin.isUseFoodComponent());
                ItemMeta itemMeta = itemStack.getItemMeta();
                PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();

                itemMeta.lore(lore);
                dataContainer.set(deleteKey, PersistentDataType.STRING, internalName);

                itemStack.setItemMeta(itemMeta);
                gui.setItem(guiCount, itemStack);
                guiCount++;
                if (guiCount >= 45) {
                    break;
                }
            }
        }
    }

    public NamespacedKey getSelectKey() {
        return selectKey;
    }

    public NamespacedKey getEditKey() {
        return editKey;
    }

    public NamespacedKey getDeleteKey() {
        return deleteKey;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return gui;
    }

}
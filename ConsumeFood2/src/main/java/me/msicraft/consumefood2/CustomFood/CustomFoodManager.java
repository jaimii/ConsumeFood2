package me.msicraft.consumefood2.CustomFood;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import me.msicraft.API.CoolDownType;
import me.msicraft.API.Common;
import me.msicraft.API.Data.CustomGui;
import me.msicraft.API.Food.CustomFood;
import me.msicraft.API.Food.Food;
import me.msicraft.API.Food.FoodPotionEffect;
import me.msicraft.API.Wrapper;
import me.msicraft.consumefood2.ConsumeFood2;
import me.msicraft.consumefood2.CustomFood.File.CustomFoodData;
import me.msicraft.consumefood2.CustomFood.Menu.CustomFoodEditGui;
import me.msicraft.consumefood2.PlayerData.Data.PlayerData;
import me.msicraft.consumefood2.Utils.MessageUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.*;

public class CustomFoodManager implements Wrapper {

    private final ConsumeFood2 plugin;
    private final CustomFoodData customFoodData;
    private CoolDownType coolDownType = CoolDownType.DISABLE;
    private double globalCoolDown = 0;
    private boolean disablePlayerHeadPlace = false;

    private final Map<String, CustomFood> customFoodMap = new HashMap<>();
    private final List<String> customFoodNames = new ArrayList<>();

    private Wrapper wrapperFallback;

    public CustomFoodManager(ConsumeFood2 plugin) {
        this.plugin = plugin;
        this.customFoodData = new CustomFoodData(plugin);
    }

    public enum CompatibilityPlugin {
        ITEMSADDER, ORAXEN
    }

    private Wrapper getWrapper() {
        if (wrapperFallback == null) {
            if (plugin.getBukkitVersion() >= 12005) {
                try {
                    Class<?> clazz = Class.forName("me.msicraft.upper_1_21_11.Upper_1_21_11");
                    java.lang.reflect.Method method = clazz.getMethod("getInstance");
                    wrapperFallback = (Wrapper) method.invoke(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (wrapperFallback == null) {
                wrapperFallback = this;
            }
        }
        return wrapperFallback;
    }

    public void reloadVariables() {
        customFoodData.reloadConfig();

        String cooldownTypeS = plugin.getConfig().getString("CustomFood-Settings.Cooldown.Type");
        if (cooldownTypeS != null) {
            try {
                this.coolDownType = CoolDownType.valueOf(cooldownTypeS.toUpperCase());
            } catch (IllegalArgumentException e) {
                this.coolDownType = CoolDownType.DISABLE;
                Bukkit.getConsoleSender().sendMessage(ConsumeFood2.PREFIX.append(Component.text("=====Invalid CustomFood CoolDown Type=====", NamedTextColor.YELLOW)));
                Bukkit.getConsoleSender().sendMessage(ConsumeFood2.PREFIX.append(Component.text("Invalid: " + cooldownTypeS, NamedTextColor.YELLOW)));
                Bukkit.getConsoleSender().sendMessage(ConsumeFood2.PREFIX.append(Component.text("Default value of 'disable' is used", NamedTextColor.YELLOW)));
            }
        }
        this.globalCoolDown = plugin.getConfig().getDouble("CustomFood-Settings.Cooldown.Global-Cooldown", 0);
        this.disablePlayerHeadPlace = plugin.getConfig().getBoolean("CustomFood-Settings.Disable-PlayerHead-Place", false);

        loadCustomFood();
    }

    public void openCustomFoodEditGui(CustomFoodEditGui.Type type, Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        CustomFoodEditGui customFoodEditGui = (CustomFoodEditGui) playerData.getCustomGui(CustomGui.GuiType.CUSTOM_FOOD);
        player.openInventory(customFoodEditGui.getInventory());
        customFoodEditGui.setGui(type, player);
    }

    public void saveOptionToConfig(CustomFood customFood, Food.Options options) {
        FileConfiguration config = customFoodData.getConfig();
        String path = "Food." + customFood.getInternalName() + "." + options.getPath();
        config.set(path, customFood.getOptionValue(options));
        customFoodData.saveConfig();
    }

    public void createCustomFood(String internalName) {
        CustomFood customFood = new CustomFood(Material.APPLE, internalName);
        String path = "Food." + internalName;
        customFoodData.getConfig().set(path + ".Material", "APPLE");
        customFoodData.saveConfig();
        registerCustomFood(customFood);
    }

    public void deleteCustomFood(String internalName) {
        String path = "Food." + internalName;
        customFoodData.getConfig().set(path, null);
        customFoodData.saveConfig();
        unregisterCustomFood(internalName);
    }

    public boolean hasCustomFood(String internalName) {
        return customFoodMap.containsKey(internalName);
    }

    public void loadCustomFood() {
        FileConfiguration config = customFoodData.getConfig();
        ConfigurationSection section = config.getConfigurationSection("Food");
        int count = 0;
        if (section != null) {
            customFoodNames.clear();
            Set<String> keys = section.getKeys(false);
            Food.Options[] foodOptions = Food.Options.values();
            for (String key : keys) {
                Material material = Material.APPLE;
                if (config.contains("Food." + key + ".Material")) {
                    String matName = config.getString("Food." + key + ".Material");
                    material = Material.getMaterial(matName.toUpperCase());
                    if (material == null) {
                        MessageUtil.sendErrorMessage(MessageUtil.FoodType.CUSTOMFOOD, "Invalid Material", key);
                        continue;
                    }
                }

                CustomFood customFood;
                if (customFoodMap.containsKey(key)) {
                    customFood = customFoodMap.get(key);
                } else {
                    customFood = new CustomFood(material, key);
                }
                String path = "Food." + key;
                for (Food.Options option : foodOptions) {
                    if (option == Food.Options.MATERIAL) {
                        continue;
                    }
                    String p = path + "." + option.getPath();
                    if (config.contains(p)) {
                        Food.ValueType valueType = option.getValueType();
                        switch (valueType) {
                            case STRING -> customFood.setOption(option, config.getString(p, (String) option.getBaseValue()));
                            case INTEGER -> customFood.setOption(option, config.getInt(p, (int) option.getBaseValue()));
                            case DOUBLE -> customFood.setOption(option, config.getDouble(p, (double) option.getBaseValue()));
                            case BOOLEAN -> customFood.setOption(option, config.getBoolean(p, (boolean) option.getBaseValue()));
                        }
                    }
                }

                // Load Lore
                customFood.getLore().clear();
                List<String> loreList = config.getStringList(path + ".Lore");
                loreList.forEach(customFood::addLore);

                // Load Enchantments
                customFood.getEnchantments().clear();
                List<String> enchantList = config.getStringList(path + ".Enchant");
                enchantList.forEach(format -> {
                    try {
                        String[] a = format.split(":");
                        Enchantment enchantment = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft(a[0]));
                        if (enchantment != null) {
                            int level = Integer.parseInt(a[1]);
                            customFood.addEnchantment(enchantment, level);
                        }
                    } catch (Exception ignored) {}
                });

                // Load Potion Effects
                customFood.getPotionEffects().clear();
                List<String> potionEffectList = config.getStringList(path + ".PotionEffect");
                potionEffectList.forEach(format -> {
                    try {
                        FoodPotionEffect foodPotionEffect = Common.getInstance().formatToFoodPotionEffect(format);
                        customFood.addPotionEffect(foodPotionEffect);
                    } catch (Exception ignored) {}
                });

                // Load Commands
                customFood.getCommands().clear();
                List<String> commandList = config.getStringList(path + ".Command");
                commandList.forEach(format -> {
                    try {
                        me.msicraft.API.Food.FoodCommand foodCommand = Common.getInstance().formatToFoodCommand(format);
                        customFood.addCommand(foodCommand);
                    } catch (Exception ignored) {}
                });

                customFoodMap.put(key, customFood);
                customFoodNames.add(key);
                count++;
            }
        }
        Bukkit.getConsoleSender().sendMessage(ConsumeFood2.PREFIX.append(Component.text(count + " CustomFood loaded")));
    }

    public void saveCustomFoodToConfig(CustomFood customFood, boolean saveConfig) {
        FileConfiguration config = customFoodData.getConfig();
        Set<Food.Options> optionsSet = customFood.getOptions();
        String basePath = "Food." + customFood.getInternalName();
        config.set(basePath + ".Material", customFood.getMaterial().name());
        for (Food.Options options : optionsSet) {
            if (options == Food.Options.MATERIAL || options == Food.Options.POTION_EFFECT || options == Food.Options.COMMAND || options == Food.Options.LORE || options == Food.Options.ENCHANT) {
                continue;
            }
            String path = basePath + "." + options.getPath();
            config.set(path, customFood.getOptionValue(options));
        }

        // Save Lore
        config.set(basePath + ".Lore", customFood.getLore().isEmpty() ? null : customFood.getLore());

        // Save Enchantments
        List<String> enchantList = new ArrayList<>();
        customFood.getEnchantFormatList().forEach(enchantList::add);
        config.set(basePath + ".Enchant", enchantList.isEmpty() ? null : enchantList);

        // Save Potion Effects
        List<String> potionEffectList = new ArrayList<>();
        customFood.getPotionEffects().forEach(potionEffect -> potionEffectList.add(potionEffect.toFormat()));
        config.set(basePath + ".PotionEffect", potionEffectList.isEmpty() ? null : potionEffectList);

        // Save Commands
        List<String> commandList = new ArrayList<>();
        customFood.getCommands().forEach(command -> commandList.add(command.toFormat()));
        config.set(basePath + ".Command", commandList.isEmpty() ? null : commandList);

        if (saveConfig) {
            customFoodData.saveConfig();
        }
    }

    public void saveCustomFood() {
        int count = 0;
        for (CustomFood customFood : customFoodMap.values()) {
            saveCustomFoodToConfig(customFood, false);
            count++;
        }
        customFoodData.saveConfig();
        Bukkit.getConsoleSender().sendMessage(ConsumeFood2.PREFIX.append(Component.text(count + " CustomFood saved")));
    }

    public void registerCustomFood(CustomFood customFood) {
        customFoodMap.put(customFood.getInternalName(), customFood);
        if (!customFoodNames.contains(customFood.getInternalName())) {
            customFoodNames.add(customFood.getInternalName());
        }
    }

    public void unregisterCustomFood(String internalName) {
        customFoodMap.remove(internalName);
        customFoodNames.remove(internalName);
    }

    public CustomFood getCustomFood(String internalName) {
        return customFoodMap.getOrDefault(internalName, null);
    }

    public List<String> getAllInternalNames() {
        return customFoodNames;
    }

    public CustomFoodData getCustomFoodData() {
        return customFoodData;
    }

    public CoolDownType getCoolDownType() {
        return coolDownType;
    }

    public double getGlobalCoolDown() {
        return globalCoolDown;
    }

    public boolean isDisablePlayerHeadPlace() {
        return disablePlayerHeadPlace;
    }

    public CompatibilityPlugin getCompatibilityPlugin(ItemStack itemStack) {
        if (itemStack == null) return null;
        if (Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            if (dev.lone.itemsadder.api.CustomStack.byItemStack(itemStack) != null) {
                return CompatibilityPlugin.ITEMSADDER;
            }
        }
        return null;
    }

    public String getCompatibilityItemId(CompatibilityPlugin compatibilityPlugin, ItemStack itemStack) {
        if (compatibilityPlugin == CompatibilityPlugin.ITEMSADDER) {
            dev.lone.itemsadder.api.CustomStack customStack = dev.lone.itemsadder.api.CustomStack.byItemStack(itemStack);
            if (customStack != null) {
                return customStack.getNamespacedID();
            }
        }
        return null;
    }

    public String getInternalNameByCompatibilityPlugin(CompatibilityPlugin compatibilityPlugin, String compatibilityItemId) {
        if (compatibilityItemId == null) return null;
        for (CustomFood customFood : customFoodMap.values()) {
            if (customFood.hasOption(Food.Options.OTHER_PLUGIN_COMPATIBILITY)) {
                String comp = (String) customFood.getOptionValue(Food.Options.OTHER_PLUGIN_COMPATIBILITY);
                if (comp != null && comp.equalsIgnoreCase(compatibilityPlugin.name() + ":" + compatibilityItemId)) {
                    return customFood.getInternalName();
                }
            }
        }
        return null;
    }

    public String getInternalName(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return null;
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "CustomFood");
        if (dataContainer.has(key, PersistentDataType.STRING)) {
            return dataContainer.get(key, PersistentDataType.STRING);
        }
        return null;
    }

    public boolean isCustomFood(ItemStack itemStack) {
        return getInternalName(itemStack) != null || getCompatibilityPlugin(itemStack) != null;
    }

    public void updateInventory(Player player) {
        if (player == null) return;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack itemStack = contents[i];
            if (itemStack != null) {
                String internalName = getInternalName(itemStack);
                if (internalName != null) {
                    CustomFood customFood = getCustomFood(internalName);
                    if (customFood != null) {
                        int amount = itemStack.getAmount();
                        ItemStack newStack = createItemStack(customFood);
                        newStack.setAmount(amount);
                        player.getInventory().setItem(i, newStack);
                    }
                }
            }
        }
    }

    public void consumeCustomFood(Player player, CustomFood customFood, EquipmentSlot hand, boolean useFoodComponent) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getPluginManager().callEvent(new me.msicraft.API.CustomEvent.CustomFoodConsumeEvent(true, -1,
                    player, hand, customFood));
        });

        if (!useFoodComponent) {
            int foodLevel = (int) customFood.getOptionValue(Food.Options.FOOD_LEVEL);
            double saturationD = (double) customFood.getOptionValue(Food.Options.SATURATION);
            float saturation = (float) saturationD;

            int calFoodLevel = player.getFoodLevel() + foodLevel;
            if (calFoodLevel > 20) {
                calFoodLevel = 20;
            }
            player.setFoodLevel(calFoodLevel);

            float calSaturation = player.getSaturation() + saturation;
            if (calSaturation > calFoodLevel) {
                calSaturation = calFoodLevel;
            }
            player.setSaturation(calSaturation);

            customFood.getPotionEffects().forEach(foodPotionEffect -> {
                if (Math.random() <= foodPotionEffect.getChance()) {
                    player.addPotionEffect(foodPotionEffect.getPotionEffect());
                }
            });
        }

        final boolean usePlaceHolderAPI = plugin.isUsePlaceHolderAPI();
        customFood.getCommands().forEach(foodCommand -> {
            String command = foodCommand.getCommand();
            if (usePlaceHolderAPI) {
                command = PlaceholderAPI.setPlaceholders(player, command);
            } else {
                command = command.replaceAll("%player_name%", player.getName());
            }
            me.msicraft.API.Food.FoodCommand.ExecuteType executeType = foodCommand.getExecuteType();
            switch (executeType) {
                case PLAYER -> Bukkit.dispatchCommand(player, command);
                case CONSOLE -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        });

        if (hand == EquipmentSlot.HAND) {
            ItemStack handStack = player.getInventory().getItemInMainHand();
            handStack.setAmount(handStack.getAmount() - 1);
        } else if (hand == EquipmentSlot.OFF_HAND) {
            ItemStack handStack = player.getInventory().getItemInOffHand();
            handStack.setAmount(handStack.getAmount() - 1);
        }
    }

    public ItemStack createItemStack(CustomFood customFood) {
        int bukkitVersion = plugin.getBukkitVersion();
        Map<String, NamespacedKey> namespacedKeyMap = new HashMap<>();
        namespacedKeyMap.put("CustomFood", new NamespacedKey(plugin, "CustomFood"));
        namespacedKeyMap.put("UnStackable", new NamespacedKey(plugin, "UnStackable"));

        return getWrapper().createCustomFoodItemStack(bukkitVersion, customFood, namespacedKeyMap);
    }

    // --- FALLBACK INTERFACE METHODS FOR Wrapper ---

    @Override
    public String translateColorCodes(String message) {
        if (message == null) return null;
        return LegacyComponentSerializer.legacySection().serialize(
                LegacyComponentSerializer.legacyAmpersand().deserialize(message)
        );
    }

    @Override
    public ItemStack createCustomFoodItemStack(int bukkitVersion, CustomFood customFood, Map<String, NamespacedKey> namespacedKeyMap) {
        ItemStack itemStack = new ItemStack(customFood.getMaterial());
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
        FoodComponent foodComponent = itemMeta.getFood();

        if (customFood.hasOption(Food.Options.DISPLAYNAME)) {
            String displayName = (String) customFood.getOptionValue(Food.Options.DISPLAYNAME);
            if (displayName != null) {
                Component displayNameComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(displayName);
                itemMeta.displayName(displayNameComponent);
            }
        }
        if (customFood.hasOption(Food.Options.CUSTOM_MODEL_DATA)) {
            itemMeta.setCustomModelData((int) customFood.getOptionValue(Food.Options.CUSTOM_MODEL_DATA));
        }

        List<Component> lore = new ArrayList<>(customFood.getLore().size());
        for (String s : customFood.getLore()) {
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(s));
        }
        itemMeta.lore(lore);

        if ((boolean) customFood.getOptionValue(Food.Options.HIDE_ENCHANT)) {
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if ((boolean) customFood.getOptionValue(Food.Options.HIDE_ADDITIONAL_TOOLTIP)) {
            itemMeta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        }
        if ((boolean) customFood.getOptionValue(Food.Options.UNSTACKABLE)) {
            dataContainer.set(namespacedKeyMap.get("UnStackable"), PersistentDataType.STRING, UUID.randomUUID().toString());
        }

        if (customFood.hasOption(Food.Options.FOOD_LEVEL)) {
            foodComponent.setNutrition((int) customFood.getOptionValue(Food.Options.FOOD_LEVEL));
        }
        if (customFood.hasOption(Food.Options.SATURATION)) {
            double saturationD = (double) customFood.getOptionValue(Food.Options.SATURATION);
            foodComponent.setSaturation((float) saturationD);
        }

        foodComponent.setCanAlwaysEat((boolean) customFood.getOptionValue(Food.Options.ALWAYS_EAT));

        dataContainer.set(namespacedKeyMap.get("CustomFood"), PersistentDataType.STRING, customFood.getInternalName());

        itemMeta.setFood(foodComponent);
        if (customFood.hasOption(Food.Options.MAX_STACK_SIZE)) {
            int maxStackSize = (int) customFood.getOptionValue(Food.Options.MAX_STACK_SIZE);
            if (maxStackSize != -1) {
                itemMeta.setMaxStackSize((int) customFood.getOptionValue(Food.Options.MAX_STACK_SIZE));
            }
        }
        itemStack.setItemMeta(itemMeta);

        for (Enchantment enchantment : customFood.getEnchantments()) {
            int level = customFood.getEnchantmentLevel(enchantment);
            itemStack.addUnsafeEnchantment(enchantment, level);
        }

        if (itemStack.getType() == Material.PLAYER_HEAD) {
            NBT.modifyComponents(itemStack, nbt -> {
                ReadWriteNBT profileNbt = nbt.getOrCreateCompound("minecraft:profile");
                profileNbt.setUUID("id", (UUID) customFood.getOptionValue(Food.Options.UUID));
                ReadWriteNBT propertiesNbt = profileNbt.getCompoundList("properties").addCompound();
                propertiesNbt.setString("name", "textures");
                propertiesNbt.setString("value", (String) customFood.getOptionValue(Food.Options.TEXTURE_VALUE));
            });
        } else if (itemStack.getType() == Material.POTION || itemStack.getType() == Material.LINGERING_POTION || itemStack.getType() == Material.SPLASH_POTION) {
            PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
            String colorCode = (String) customFood.getOptionValue(Food.Options.POTION_COLOR);
            Color color;
            java.awt.Color awtColor;
            try {
                awtColor = java.awt.Color.decode(colorCode);
                color = Color.fromBGR(awtColor.getBlue(), awtColor.getGreen(), awtColor.getRed());
            } catch (IllegalArgumentException | NullPointerException e) {
                color = Color.WHITE;
            }
            potionMeta.setColor(color);
            itemStack.setItemMeta(potionMeta);
        }

        if (!itemStack.getType().isEdible()) {
            if (bukkitVersion >= 12102) {
                NBT.modifyComponents(itemStack, nbt -> {
                    ReadWriteNBT consumableNbt = nbt.getOrCreateCompound("minecraft:consumable");
                    consumableNbt.setString("animation", "eat");
                    if (customFood.hasOption(Food.Options.EAT_SECONDS)) {
                        double eatSeconds = (double) customFood.getOptionValue(Food.Options.EAT_SECONDS);
                        if (eatSeconds < 0) {
                            eatSeconds = 0.0;
                        }
                        consumableNbt.setFloat("consume_seconds", (float) eatSeconds);
                    }
                    ReadWriteNBT soundNbt = consumableNbt.getOrCreateCompound("sound");
                    soundNbt.setString("sound_id", "entity.generic.eat");
                });
            }
        }

        if (bukkitVersion >= 12102) {
            Consumable.Builder consumableBuilder;
            if (itemStack.hasData(DataComponentTypes.CONSUMABLE)) {
                consumableBuilder = itemStack.getData(DataComponentTypes.CONSUMABLE).toBuilder();
            } else {
                consumableBuilder = Consumable.consumable();
            }

            if (customFood.hasOption(Food.Options.EAT_SECONDS)) {
                double eatSecondsD = (double) customFood.getOptionValue(Food.Options.EAT_SECONDS);
                if (eatSecondsD > -1) {
                    consumableBuilder.consumeSeconds((float) eatSecondsD);
                }
            }

            List<ConsumeEffect> paperEffects = new ArrayList<>();
            for (FoodPotionEffect foodPotionEffect : customFood.getPotionEffects()) {
                ConsumeEffect.ApplyStatusEffects effect = ConsumeEffect.applyStatusEffects(
                        List.of(foodPotionEffect.getPotionEffect()),
                        Float.parseFloat(String.valueOf(foodPotionEffect.getChance()))
                );
                paperEffects.add(effect);
            }
            if (!paperEffects.isEmpty()) {
                consumableBuilder.addEffects(paperEffects);
            }

            itemStack.setData(DataComponentTypes.CONSUMABLE, consumableBuilder.build());
        }

        return itemStack;
    }

}
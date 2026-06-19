package me.msicraft.upper_1_21_11;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import me.msicraft.API.Food.CustomFood;
import me.msicraft.API.Food.Food;
import me.msicraft.API.Food.FoodPotionEffect;
import me.msicraft.API.Wrapper;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Upper_1_21_11 implements Wrapper {

    private static Upper_1_21_11 instance;

    private Upper_1_21_11() {
    }

    public static Upper_1_21_11 getInstance() {
        if (instance == null) {
            instance = new Upper_1_21_11();
        }
        return instance;
    }

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
            if (bukkitVersion >= 12102) { // Matches robust semantic checks
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

        if (bukkitVersion >= 12102) { // Matches robust semantic checks
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
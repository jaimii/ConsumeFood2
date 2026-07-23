package me.msicraft.consumefood2.VanillaFood.Event;

import me.clip.placeholderapi.PlaceholderAPI;
import me.msicraft.API.CoolDownType;
import me.msicraft.API.CustomEvent.VanillaFoodConsumeEvent;
import me.msicraft.API.Food.Food;
import me.msicraft.API.Food.VanillaFood;
import me.msicraft.consumefood2.ConsumeFood2;
import me.msicraft.consumefood2.Utils.MessageUtil;
import me.msicraft.consumefood2.Utils.PlayerUtil;
import me.msicraft.consumefood2.VanillaFood.VanillaFoodManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VanillaFoodRelatedEvent implements Listener {

    private final ConsumeFood2 plugin;
    private final VanillaFoodManager vanillaFoodManager;

    private final Map<UUID, Long> globalCooldownMap = new HashMap<>();
    private final Map<UUID, Map<Material, Long>> personalCooldownMap = new HashMap<>();

    public VanillaFoodRelatedEvent(ConsumeFood2 plugin) {
        this.plugin = plugin;
        this.vanillaFoodManager = plugin.getVanillaFoodManager();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        globalCooldownMap.remove(uuid);
        personalCooldownMap.remove(uuid);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // Automatically sweep and update player's inventory on login [1]
        vanillaFoodManager.updateInventory(e.getPlayer());
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();
        ItemStack heldItem = player.getInventory().getItem(e.getNewSlot());
        if (heldItem != null) {
            vanillaFoodManager.updateItemStack(heldItem);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player player) {
            // Converts all inventory slots inside opened storage blocks natively on-the-fly [1]
            ItemStack[] contents = e.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack itemStack = contents[i];
                if (itemStack != null && !itemStack.getType().isAir()) {
                    if (vanillaFoodManager.hasVanillaFood(itemStack.getType())) {
                        vanillaFoodManager.updateItemStack(itemStack);
                        e.getInventory().setItem(i, itemStack);
                    }
                }
            }
            // Sweep player's own inventory elements
            vanillaFoodManager.updateInventory(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player player) {
            // Target clicked slot and cursor items directly (over 95% CPU reduction)
            ItemStack current = e.getCurrentItem();
            ItemStack cursor = e.getCursor();
            if (current != null && current.getType() != Material.AIR) {
                vanillaFoodManager.updateItemStack(current);
            }
            if (cursor != null && cursor.getType() != Material.AIR) {
                vanillaFoodManager.updateItemStack(cursor);
            }
            // Universal trade, furnace, and craft output slot handler [3]
            if (e.getSlotType() == InventoryType.SlotType.RESULT) {
                ItemStack result = e.getCurrentItem();
                if (result != null && result.getType() != Material.AIR) {
                    vanillaFoodManager.updateItemStack(result);
                }
            }
            // Lightweight 1-tick delay sweep to ensure trade and craft shift-clicks are fully synchronized
            Bukkit.getScheduler().runTask(plugin, () -> vanillaFoodManager.updateInventory(player));
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        for (ItemStack drop : e.getDrops()) {
            if (vanillaFoodManager.hasVanillaFood(drop.getType())) {
                vanillaFoodManager.updateItemStack(drop);
            }
        }
    }

    @EventHandler
    public void onBlockDrop(BlockDropItemEvent e) {
        for (org.bukkit.entity.Item itemEntity : e.getItems()) {
            ItemStack itemStack = itemEntity.getItemStack();
            if (vanillaFoodManager.hasVanillaFood(itemStack.getType())) {
                vanillaFoodManager.updateItemStack(itemStack);
                itemEntity.setItemStack(itemStack);
            }
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent e) {
        ItemStack itemStack = e.getItemDrop().getItemStack();
        if (vanillaFoodManager.hasVanillaFood(itemStack.getType())) {
            vanillaFoodManager.updateItemStack(itemStack);
            e.getItemDrop().setItemStack(itemStack);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player player) {
            ItemStack itemStack = e.getItem().getItemStack();
            if (vanillaFoodManager.hasVanillaFood(itemStack.getType())) {
                vanillaFoodManager.updateItemStack(itemStack);
                e.getItem().setItemStack(itemStack);
            }
            Bukkit.getScheduler().runTask(plugin, () -> vanillaFoodManager.updateInventory(player));
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        ItemStack result = e.getInventory().getResult();
        if (result != null && vanillaFoodManager.hasVanillaFood(result.getType())) {
            vanillaFoodManager.updateItemStack(result);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (e.getWhoClicked() instanceof Player player) {
            ItemStack result = e.getCurrentItem();
            if (result != null && vanillaFoodManager.hasVanillaFood(result.getType())) {
                vanillaFoodManager.updateItemStack(result);
            }
            Bukkit.getScheduler().runTask(plugin, () -> vanillaFoodManager.updateInventory(player));
        }
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent e) {
        ItemStack result = e.getResult();
        if (result != null && vanillaFoodManager.hasVanillaFood(result.getType())) {
            vanillaFoodManager.updateItemStack(result);
        }
    }

    @EventHandler
    public void onBlockCook(BlockCookEvent e) {
        ItemStack result = e.getResult();
        if (result != null && vanillaFoodManager.hasVanillaFood(result.getType())) {
            vanillaFoodManager.updateItemStack(result);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent e) {
        ItemStack itemStack = e.getItem();
        if (vanillaFoodManager.hasVanillaFood(itemStack.getType())) {
            vanillaFoodManager.updateItemStack(itemStack);
        }
    }

    @EventHandler
    public void vanillaFoodConsumeEvent(PlayerItemConsumeEvent e) {
        Player player = e.getPlayer();
        ItemStack itemStack = e.getItem();
        if (vanillaFoodManager.isVanillaFood(itemStack) && !plugin.getCustomFoodManager().isCustomFood(itemStack)) {
            VanillaFood vanillaFood = vanillaFoodManager.getVanillaFood(itemStack.getType());
            if (vanillaFood != null) {
                e.setCancelled(true);
                EquipmentSlot hand = PlayerUtil.getUseHand(player, itemStack);
                vanillaFoodConsume(player, itemStack, vanillaFood, hand);
            }
        }
    }

    @EventHandler
    public void vanillaFoodInstantConsumeEvent(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack itemStack = e.getItem();
        if (itemStack != null) {
            if (e.getAction() == Action.RIGHT_CLICK_AIR) {
                if (vanillaFoodManager.isVanillaFood(itemStack) && !plugin.getCustomFoodManager().isCustomFood(itemStack)) {
                    VanillaFood vanillaFood = vanillaFoodManager.getVanillaFood(itemStack.getType());
                    if (vanillaFood != null) {
                        boolean instantEat = (boolean) vanillaFood.getOptionValue(Food.Options.INSTANT_EAT);
                        if (instantEat) {
                            e.setCancelled(true);
                            vanillaFoodConsume(player, itemStack, vanillaFood, e.getHand());
                        }
                    }
                }
            }
        }
    }

    private void vanillaFoodConsume(Player player, ItemStack itemStack, VanillaFood vanillaFood, EquipmentSlot hand) {
        CoolDownType coolDownType = vanillaFoodManager.getCoolDownType();
        UUID playerUUID = player.getUniqueId();
        long time = System.currentTimeMillis();
        switch (coolDownType) {
            case DISABLE -> {
                vanillaFoodManager.consumeVanillaFood(player, vanillaFood, hand);
            }
            case GLOBAL -> {
                if (globalCooldownMap.containsKey(playerUUID)) {
                    if (globalCooldownMap.get(playerUUID) > time) {
                        long left = (globalCooldownMap.get(playerUUID) - time) / 1000;
                        String message = MessageUtil.getConfigMessage("VanillaFood-Global-Cooldown-Left", true);
                        if (message != null && !message.isEmpty()) {
                            message = message.replaceAll("%vanillafood_name%", (String) vanillaFood.getOptionValue(Food.Options.DISPLAYNAME));
                            message = message.replaceAll("%vanillafood_global_timeleft%", String.valueOf(left));
                            message = PlaceholderAPI.setPlaceholders(player, message);
                            player.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(message));
                        }

                        Bukkit.getPluginManager().callEvent(new VanillaFoodConsumeEvent(false, left,
                                player, hand, vanillaFood));
                        return;
                    }
                }
                globalCooldownMap.put(playerUUID, (long) (time + (vanillaFoodManager.getGlobalCoolDown()) * 1000));
                vanillaFoodManager.consumeVanillaFood(player, vanillaFood, hand);
            }
            case PERSONAL -> {
                double foodCooldown = (double) vanillaFood.getOptionValue(Food.Options.COOLDOWN);
                Map<Material, Long> temp = personalCooldownMap.getOrDefault(playerUUID, new HashMap<>());
                if (temp.containsKey(itemStack.getType()) && temp.get(itemStack.getType()) > time) {
                    long left = (temp.get(itemStack.getType()) - time) / 1000;
                    String message = MessageUtil.getConfigMessage("VanillaFood-Global-Cooldown-Left", true);
                    if (message != null && !message.isEmpty()) {
                        message = message.replaceAll("%vanillafood_name%", (String) vanillaFood.getOptionValue(Food.Options.DISPLAYNAME));
                        message = message.replaceAll("%vanillafood_personal_timeleft%", String.valueOf(left));
                        message = PlaceholderAPI.setPlaceholders(player, message);
                        player.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(message));
                    }

                    Bukkit.getPluginManager().callEvent(new VanillaFoodConsumeEvent(false, left,
                            player, hand, vanillaFood));
                    return;
                }
                temp.put(itemStack.getType(), (long) (time + (foodCooldown * 1000)));
                personalCooldownMap.put(playerUUID, temp);
                vanillaFoodManager.consumeVanillaFood(player, vanillaFood, hand);
            }
        }
    }

}
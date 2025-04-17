package net.bram.customitems;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class CustomItemsPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("CustomItems Plugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomItems Plugin disabled.");
    }

    private boolean isOnCooldown(Player player, String ability, int cooldownSeconds) {
        cooldowns.putIfAbsent(player.getUniqueId(), new HashMap<>());
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        long currentTime = System.currentTimeMillis();
        if (playerCooldowns.containsKey(ability) && currentTime < playerCooldowns.get(ability)) {
            long secondsLeft = (playerCooldowns.get(ability) - currentTime) / 1000;
            player.sendMessage(ChatColor.RED + "Ability on cooldown: " + secondsLeft + "s remaining");
            return true;
        }
        playerCooldowns.put(ability, currentTime + cooldownSeconds * 1000L);
        return false;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return;

        String name = ChatColor.stripColor(meta.getDisplayName());

        switch (name) {
            case "Stormwalker’s Blade":
                if (isOnCooldown(player, "tidal_uplift", 120)) return;
                player.getWorld().spawnParticle(Particle.DRIPPING_WATER, player.getLocation(), 150, 2, 2, 2, 0.2);
                for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        entity.setVelocity(new Vector(0, 2, 0));
                    }
                }
                break;

            case "Celestial Fang":
                if (isOnCooldown(player, "starfall", 90)) return;
                Location target = player.getTargetBlockExact(50) != null ?
                        player.getTargetBlockExact(50).getLocation().add(0, 10, 0) :
                        player.getLocation().add(0, 10, 0);
                Fireball fireball = player.getWorld().spawn(target, Fireball.class);
                fireball.setIsIncendiary(true);
                fireball.setYield(4f);
                break;

            case "Voided Scythe":
                if (isOnCooldown(player, "abyssal_grasp", 90)) return;
                LivingEntity closest = null;
                double minDist = 20;
                for (Entity e : player.getNearbyEntities(20, 20, 20)) {
                    if (e instanceof LivingEntity && e != player) {
                        double dist = player.getLocation().distance(e.getLocation());
                        if (dist < minDist) {
                            minDist = dist;
                            closest = (LivingEntity) e;
                        }
                    }
                }
                if (closest != null) {
                    Vector pull = player.getLocation().toVector().subtract(closest.getLocation().toVector()).normalize().multiply(2);
                    closest.setVelocity(pull);
                    closest.getWorld().spawnParticle(Particle.PORTAL, closest.getLocation(), 50);
                }
                break;

            case "Bloodthorn Blade":
                if (isOnCooldown(player, "crimson_dance", 60)) return;
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.HASTE, 20 * 5, 1));
                break;

            case "Sunforged Aegis":
                if (isOnCooldown(player, "radiant_barrier", 120)) return;
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 100, 2, 2, 2, 0.1);
                for (Entity e : player.getNearbyEntities(4, 4, 4)) {
                    if (e instanceof LivingEntity && e != player) {
                        e.setVelocity(e.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5));
                    }
                }
                break;

            case "Serpentfang Bow":
                if (isOnCooldown(player, "coiled_strike", 40)) return;
                break;
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getHealth() - event.getFinalDamage() <= 8) {
            ItemStack chestplate = player.getInventory().getChestplate();
            if (chestplate != null && chestplate.hasItemMeta()) {
                String name = ChatColor.stripColor(chestplate.getItemMeta().getDisplayName());
                if (name.equals("Infernoheart Chestplate") && !isOnCooldown(player, "blazing_rebirth", 180)) {
                    for (Entity e : player.getNearbyEntities(3, 3, 3)) {
                        if (e instanceof LivingEntity && e != player) {
                            e.setFireTicks(60);
                        }
                    }
                    player.setFireTicks(0);
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 20 * 10, 1));
                    player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 100);
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1, 1);
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;

        if (command.getName().equalsIgnoreCase("weaponsgui")) {
            Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "Weapon Selector");

            gui.addItem(createWeapon(Material.DIAMOND_SWORD, ChatColor.AQUA + "Stormwalker’s Blade"));
            gui.addItem(createWeapon(Material.FIRE_CHARGE, ChatColor.RED + "Infernoheart Chestplate"));
            gui.addItem(createWeapon(Material.FEATHER, ChatColor.GRAY + "Phantomstep Boots"));
            gui.addItem(createWeapon(Material.BLAZE_ROD, ChatColor.LIGHT_PURPLE + "Celestial Fang"));
            gui.addItem(createWeapon(Material.ICE, ChatColor.BLUE + "Glacierbound Greaves"));
            gui.addItem(createWeapon(Material.NETHERITE_HOE, ChatColor.DARK_PURPLE + "Voided Scythe"));
            gui.addItem(createWeapon(Material.CHAINMAIL_HELMET, ChatColor.GREEN + "Tempest Helm"));
            gui.addItem(createWeapon(Material.IRON_SWORD, ChatColor.DARK_RED + "Bloodthorn Blade"));
            gui.addItem(createWeapon(Material.SHIELD, ChatColor.GOLD + "Sunforged Aegis"));
            gui.addItem(createWeapon(Material.BOW, ChatColor.DARK_GREEN + "Serpentfang Bow"));

            player.openInventory(gui);
            return true;
        }

        return false;
    }

    private ItemStack createWeapon(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.DARK_AQUA + "Weapon Selector")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            Player player = (Player) event.getWhoClicked();
            player.getInventory().addItem(event.getCurrentItem());
            player.sendMessage(ChatColor.GREEN + "You received: " + event.getCurrentItem().getItemMeta().getDisplayName());
        }
    }
}

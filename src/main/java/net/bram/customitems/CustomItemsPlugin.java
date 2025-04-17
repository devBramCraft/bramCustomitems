package net.bram.customitems;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class CustomItemsPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private boolean isWearing(Player player, String armorName) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.hasItemMeta()) {
                String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                if (name != null && name.equals(armorName)) return true;
            }
        }
        return false;
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
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        double finalHealth = player.getHealth() - event.getFinalDamage();
        if (isWearing(player, "Infernoheart Chestplate") && finalHealth <= 8 && !isOnCooldown(player, "blazing_rebirth", 3)) {
            event.setCancelled(true);
            player.setHealth(Math.max(player.getHealth(), 1));
            for (Entity e : player.getNearbyEntities(3, 3, 3)) {
                if (e instanceof LivingEntity && e != player) {
                    e.setFireTicks(100);
                }
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 60, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 60, 1));
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 100, 2, 2, 2, 0.2);
            player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 50, 1, 1, 1);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isWearing(player, "Glacierbound Greaves")) {
            if (!player.hasPotionEffect(PotionEffectType.SPEED)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1));
            }
        }
        if (isWearing(player, "Phantomstep Boots")) {
            player.setAllowFlight(true);
        } else {
            player.setAllowFlight(false);
        }
    }

    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (isWearing(player, "Phantomstep Boots") && !isOnCooldown(player, "ghost_dash", 3)) {
            Vector dash = player.getLocation().getDirection().normalize().multiply(2);
            dash.setY(0.3);
            player.setVelocity(dash);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setFallDistance(0);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20);
            Bukkit.getScheduler().runTaskLater(this, () -> player.setAllowFlight(true), 40L);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return;
        String name = ChatColor.stripColor(meta.getDisplayName());
        Action action = event.getAction();
        boolean shiftRightClick = (player.isSneaking() && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK));
        if (!shiftRightClick) return;

        switch (name) {
            case "Tempest Helm":
                if (!isWearing(player, "Tempest Helm") || isOnCooldown(player, "wind_jump", 5)) return;
                player.setVelocity(player.getLocation().getDirection().multiply(0.6).setY(1.5));
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 40);
                break;
            case "Stormwalker’s Blade":
                if (isOnCooldown(player, "tidal_uplift", 3)) return;
                player.getWorld().spawnParticle(Particle.DRIPPING_WATER, player.getLocation(), 4000, 2, 2, 2, 0.2);
                for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        entity.setVelocity(new Vector(0, 2, 0));
                    }
                }
                break;
            case "Celestial Fang":
                if (isOnCooldown(player, "starfall", 3)) return;
                RayTraceResult result = player.rayTraceBlocks(50);
                if (result != null && result.getHitPosition() != null) {
                    Location fireballTarget = result.getHitPosition().toLocation(player.getWorld()).add(0, 10, 0);
                    Fireball fireball = player.getWorld().spawn(fireballTarget, Fireball.class);
                    fireball.setIsIncendiary(true);
                    fireball.setYield(4f);
                    fireball.setDirection(new Vector(0, -1, 0));
                }
                break;
            case "Voided Scythe":
                if (isOnCooldown(player, "abyssal_grasp", 3)) return;
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
                if (isOnCooldown(player, "crimson_dance", 10)) return;
                AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
                if (attackSpeed != null) {
                    double originalSpeed = attackSpeed.getBaseValue();
                    attackSpeed.setBaseValue(originalSpeed * 100);
                    Bukkit.getScheduler().runTaskLater(this, () -> attackSpeed.setBaseValue(originalSpeed), 20 * 5);
                }
                player.sendMessage(ChatColor.RED + "You entered Blood Frenzy!");
                break;
            case "Sunforged Aegis":
                if (isOnCooldown(player, "radiant_barrier", 3)) return;
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 100, 2, 2, 2, 0.1);
                for (Entity e : player.getNearbyEntities(4, 4, 4)) {
                    if (e instanceof LivingEntity && e != player) {
                        Vector knockback = e.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5);
                        e.setVelocity(knockback);
                    }
                }
                break;
        }
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null || !bow.hasItemMeta()) return;
        String name = ChatColor.stripColor(bow.getItemMeta().getDisplayName());

        if (name.equals("Serpentfang Bow") && !isOnCooldown(player, "serpent_bow", 3)) {
            Arrow arrow = (Arrow) event.getProjectile();
            Bukkit.getScheduler().runTaskLater(this, () -> {
                TNTPrimed tnt = player.getWorld().spawn(arrow.getLocation(), TNTPrimed.class);
                tnt.setVelocity(arrow.getVelocity());
                tnt.setFuseTicks(40);
            }, 2L);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;

        if (command.getName().equalsIgnoreCase("weaponsgui")) {
            Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "Weapon Selector");

            gui.addItem(createWeapon(Material.DIAMOND_SWORD, ChatColor.AQUA + "Stormwalker’s Blade", "§bShift + Right Click to launch enemies upward with water."));
            gui.addItem(createWeapon(Material.NETHERITE_CHESTPLATE, ChatColor.RED + "Infernoheart Chestplate", "§cUnder 4 hearts: ignite enemies and gain regen/fire resist."));
            gui.addItem(createWeapon(Material.NETHERITE_BOOTS, ChatColor.GRAY + "Phantomstep Boots", "§7Double jump to dash where you're looking."));
            gui.addItem(createWeapon(Material.BLAZE_ROD, ChatColor.LIGHT_PURPLE + "Celestial Fang", "§dShift + Right Click to call a meteor strike."));
            gui.addItem(createWeapon(Material.NETHERITE_LEGGINGS, ChatColor.AQUA + "Glacierbound Greaves", "§bWear to gain Speed II."));
            gui.addItem(createWeapon(Material.NETHERITE_HELMET, ChatColor.LIGHT_PURPLE + "Tempest Helm", "§dShift + Right Click to boost upward."));
            gui.addItem(createWeapon(Material.NETHERITE_HOE, ChatColor.DARK_PURPLE + "Voided Scythe", "§5Shift + Right Click to pull the nearest enemy."));
            gui.addItem(createWeapon(Material.IRON_SWORD, ChatColor.DARK_RED + "Bloodthorn Blade", "§cShift + Right Click for frenzy (100x attack speed)."));
            gui.addItem(createWeapon(Material.SHIELD, ChatColor.GOLD + "Sunforged Aegis", "§6Shift + Right Click to knock back and block projectiles."));
            gui.addItem(createWeapon(Material.BOW, ChatColor.DARK_GREEN + "Serpentfang Bow", "§2Shoot to launch explosive TNT."));

            player.openInventory(gui);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 2, 0), 50, 0.5, 0.5, 0.5);
            return true;
        }
        return false;
    }

    private ItemStack createWeapon(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(loreLines));
        if (mat.name().contains("SWORD")) {
            meta.addEnchant(Enchantment.SHARPNESS, 4, true);
        } else if (mat == Material.BOW) {
            meta.addEnchant(Enchantment.POWER, 3, true);
        } else if (mat == Material.SHIELD) {
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        } else if (mat.name().contains("HELMET")) {
            meta.addEnchant(Enchantment.PROTECTION, 5, true);
        }
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
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

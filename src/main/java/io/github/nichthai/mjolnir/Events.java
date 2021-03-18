package io.github.nichthai.mjolnir;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

final class Events implements Listener {
    private final Mjolnir plugin;
    private final Cooldown
        throwCooldown = new Cooldown(),
        lightningCooldown = new Cooldown(),
        superchargeCooldown = new Cooldown(),
        superThrowCooldown = new Cooldown(),
        superLightningCooldown = new Cooldown();
    private final Duration supercharge;
    private final Set<ArmorStand> mjolnirThrown = new HashSet<>();

    Events(final Mjolnir plugin) {
        this.plugin = plugin;
        supercharge = new Duration(plugin);
    }

    void schedule() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (final UUID uuid : supercharge.getAll()) {
                final Player player = Bukkit.getPlayer(uuid);
                if (player == null) continue;
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 1, 1, 1, 1, 0);
            }
        }, 0, 2);
    }

    @EventHandler
    void cancelArmorStandManipulate(final PlayerArmorStandManipulateEvent e) {
        for (final MetadataValue m : e.getRightClicked().getMetadata("mjolnir")) {
            if (m.getOwningPlugin() != null && m.getOwningPlugin().equals(plugin)) e.setCancelled(true);
        }
    }

    @EventHandler
    void onPlayerQuit(final PlayerQuitEvent e) {
        final Player player = e.getPlayer();
        outer:
        for (final ArmorStand a : mjolnirThrown)
            for (final MetadataValue m : a.getMetadata("mjolnir")) {
                if (m.getOwningPlugin().equals(plugin) && m.asString().equals(player.getUniqueId().toString())) {
                    final ItemStack i = a.getEquipment().getItemInMainHand();
                    if (player.getInventory().firstEmpty() != -1) {
                        if (player.getInventory().getItemInMainHand().getType() == Material.AIR)
                            player.getInventory().setItemInMainHand(i);
                        else player.getInventory().addItem(i);
                    } else player.getWorld().dropItem(player.getLocation(), i);
                    a.remove();
                    mjolnirThrown.remove(a);
                    throwCooldown.put(player, sectionn().getDouble("throw.cooldown"));
                    break outer;
                }
            }
    }

    @EventHandler
    void onRightClick(final PlayerInteractEvent e) {
        final Player player = e.getPlayer();
        if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && e.hasItem() && e.getItem().getItemMeta().getPersistentDataContainer().has(plugin.key, PersistentDataType.INTEGER)) {
            if (!player.isSneaking()) {
                if (player.hasPermission("mjolnir.use.throw") || !plugin.getConfig().getBoolean("require_permissions_to_use")) {
                    if (!supercharge.has(player)) {
                        if (throwCooldown.ready(player)) throww(player, false);
                        else
                            player.sendMessage(ChatColor.RED + "On Cooldown! (" + throwCooldown.getSecondsRemaining(player, true) + " seconds)");
                    } else {
                        if (superThrowCooldown.ready(player)) throww(player, true);
                        else
                            player.sendMessage(ChatColor.RED + "On Cooldown! (" + superThrowCooldown.getSecondsRemaining(player, true) + " seconds)");
                    }
                } else player.sendMessage(ChatColor.RED + "You don't have permission to use this.");
            } else if (player.getLocation().getPitch() > -70) {
                if (player.hasPermission("mjolnir.use.lightning") || !plugin.getConfig().getBoolean("require_permissions_to_use")) {
                    if (!supercharge.has(player)) {
                        if (lightningCooldown.ready(player)) lightning(player, false);
                        else
                            player.sendMessage(ChatColor.RED + "On Cooldown! (" + lightningCooldown.getSecondsRemaining(player, true) + " seconds)");
                    } else {
                        if (superLightningCooldown.ready(player)) lightning(player, true);
                        else
                            player.sendMessage(ChatColor.RED + "On Cooldown! (" + superLightningCooldown.getSecondsRemaining(player, true) + " seconds)");
                    }
                } else player.sendMessage(ChatColor.RED + "You don't have permission to use this.");
            } else {
                if (player.hasPermission("mjolnir.use.supercharge") || !plugin.getConfig().getBoolean("require_permissions_to_use")) {
                    if (superchargeCooldown.ready(player)) supercharge(player);
                    else
                        player.sendMessage(ChatColor.RED + "On Cooldown! (" + superchargeCooldown.getSecondsRemaining(player, true) + " seconds)");
                } else player.sendMessage(ChatColor.RED + "You don't have permission to use this.");
            }
        }
    }

    @EventHandler
    void onDamage(final EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Damageable)) return;
        final Damageable victim = ((Damageable) e.getEntity());
        final Entity damager = e.getDamager();
        if (e.getCause() != EntityDamageEvent.DamageCause.LIGHTNING) return;

        final Map<String, String> map = new HashMap<>();
        map.put("strike", "lightning.damage");
        map.put("superstrike", "supercharge.super_abilities.lightning.damage");
        map.put("super", "supercharge.damage");
        for (final Map.Entry<String, String> entry : map.entrySet()) {
            for (final MetadataValue m : damager.getMetadata(entry.getKey())) {
                if (plugin.equals(m.getOwningPlugin())) {
                    final Player caster = Bukkit.getPlayer(UUID.fromString(m.asString()));
                    if (victim.equals(caster)) {
                        e.setCancelled(true);
                        return;
                    }
                    final Vector v = victim.getVelocity();
                    damager.remove();
                    e.setDamage(0);
                    victim.damage(sectionn().getDouble(entry.getValue()), caster);
                    victim.setVelocity(v);
                    return;
                }
            }
        }
    }

    private void throww(final Player player, final boolean superd) {
        for (final ArmorStand as : mjolnirThrown)
            for (final MetadataValue m : as.getMetadata("mjolnir"))
                if (m.getOwningPlugin().equals(plugin) && m.asString().equals(player.getUniqueId().toString())) return;
        final Location loc = player.getLocation().add(0, player.getHeight() / 2, 0);
        final ItemStack item = player.getInventory().getItemInMainHand();
        final Vector v = loc.getDirection();
        final ArmorStand stand = player.getWorld().spawn(loc.add(0, 0.25, 0), ArmorStand.class, a ->
        {
            a.setVisible(false);
            a.setSmall(true);
            a.setInvulnerable(true);
            a.getEquipment().setItemInMainHand(item);
            a.setMetadata("mjolnir", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        });
        item.setAmount(0);
        throwMjolnir(stand, player, v, 0, superd);
        mjolnirThrown.add(stand);
    }

    private void lightning(final Player player, final boolean superd) {
        if (superd)
            superLightningCooldown.put(player, sectionn().getDouble("supercharge.super_abilities.lightning.cooldown"));
        else lightningCooldown.put(player, sectionn().getDouble("lightning.cooldown"));
        final BlockIterator iterator = new BlockIterator(player, 40);
        Location loc = null;
        blocks:
        while (iterator.hasNext()) {
            final Block block = iterator.next();
            outer:
            for (final Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 1, 1, 1)) {
                if (!(entity instanceof LivingEntity) || entity.equals(player)) continue;
                for (final MetadataValue m : entity.getMetadata("strike"))
                    if (m.getOwningPlugin().equals(plugin)) continue outer;
                //	lightning = entity.getWorld().strikeLightning(entity.getLocation());
                loc = entity.getLocation();
                break blocks;
            }
            if (!block.isPassable() || !iterator.hasNext()) {
                //	lightning = block.getWorld().strikeLightning(block.getLocation());
                loc = block.getLocation();
                break;
            }
        }
        if (superd) superLightningRecurse(loc, 0, player.getUniqueId());
        else {
            final LightningStrike lightning = loc.getWorld().strikeLightning(loc);
            lightning.setMetadata("strike", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        }
    }

    private void supercharge(final Player player) {
        superchargeCooldown.put(player, sectionn().getDouble("supercharge.cooldown"));
        final Location loc = player.getLocation();
        strikeEffect(loc, 50, 10);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
        {
            strikeEffect(loc, 50, 10);
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> superChargeRecurse(player, 0), 10);
        }, 10);
    }

    private void throwMjolnir(final ArmorStand stand, final Player thrower, final Vector v, final int recurse, final boolean superd) {
        if (stand.isDead()) return;
        if (recurse < 30) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
            {
                outer:
                for (final Entity e : stand.getNearbyEntities(1.0, 1.0, 1.0)) {
                    for (final MetadataValue m : e.getMetadata("mjolnir"))
                        if (m.getOwningPlugin().equals(plugin)) continue outer;
                    if (e != thrower && e != stand && e instanceof LivingEntity) {
                        ((LivingEntity) e).damage(sectionn().getDouble(superd ? "supercharge.super_abilities.throw.damage" : "throw.damage"), thrower);
                        throwMjolnir(stand, thrower, v, superd ? recurse + 1 : 30, superd);
                        return;
                    }
                }
                stand.setRightArmPose(stand.getRightArmPose().add(0.3, 0.0, 0.01));
                final Vector v2 = v.subtract(new Vector(0, 0.03, 0));
                stand.setVelocity(v2);
                if (!stand.isDead() && stand.getWorld().getBlockAt(stand.getLocation().add(v)).getType().isSolid())
                    throwMjolnir(stand, thrower, v2, 30, superd);
                else if (!stand.isDead()) throwMjolnir(stand, thrower, v2, recurse + 1, superd);
            }, 1);
        } else {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
            {
                final Location loc = thrower.getLocation().add(0, 0.25, 0);
                final Location standLoc = stand.getLocation();

                if (loc.distance(standLoc) < 0.7 && thrower.getInventory().firstEmpty() != -1) {
                    final ItemStack i = stand.getEquipment().getItemInMainHand();
                    if (thrower.getInventory().getItemInMainHand().getType() == Material.AIR)
                        thrower.getInventory().setItemInMainHand(i);
                    else thrower.getInventory().addItem(i);
                    stand.remove();
                    if (superd)
                        superThrowCooldown.put(thrower, sectionn().getDouble("supercharge.super_abilities.throw.cooldown"));
                    else throwCooldown.put(thrower, sectionn().getDouble("throw.cooldown"));
                    mjolnirThrown.remove(stand);
                } else {
                    if (recurse % 100 == 0) stand.teleport(loc);
                    stand.setRightArmPose(stand.getRightArmPose().add(0.3, 0.0, 0.01));
                    final Vector v2 = loc.subtract(standLoc).toVector().normalize().multiply(1.5);
                    stand.setVelocity(v2);
                    if (!stand.isDead()) throwMjolnir(stand, thrower, v2, recurse + 1, superd);
                }
            }, 1);
        }
    }

    private void superLightningRecurse(final Location loc, final int recurse, final UUID uuid) {
        if (recurse > 4) {
            final LightningStrike lightning = loc.getWorld().strikeLightning(loc);
            lightning.setMetadata("superstrike", new FixedMetadataValue(plugin, uuid.toString()));
            loc.getWorld().createExplosion(loc, (float) sectionn().getDouble("supercharge.super_abilities.lightning.explosion_power"), sectionn().getBoolean("supercharge.super_abilities.lightning.set_fire"), sectionn().getBoolean("supercharge.super_abilities.lightning.break_blocks"));
            //mythicalgear generics enum
            return;
        }
        strikeEffect(loc.clone(), 20 - recurse * 4, 5 - recurse);
        strikeEffect(loc.clone(), 20 - recurse * 4, 5 - recurse);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> superLightningRecurse(loc, recurse + 1, uuid), 2);
    }

    private void superChargeRecurse(final Player player, final int recurse) {
        if (recurse > 9) {
            final LightningStrike lightning = player.getWorld().strikeLightning(player.getLocation());
            lightning.setMetadata("super", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
            supercharge.put(player, sectionn().getDouble("supercharge.duration"));
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
            return;
        }
        strikeEffect(player.getLocation(), 50 - recurse * 5, 5 - recurse / 2);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> superChargeRecurse(player, recurse + 1), 2);
    }

    void cleanUp() {
        for (final ArmorStand a : mjolnirThrown)
            for (final MetadataValue m : a.getMetadata("mjolnir"))
                if (m.getOwningPlugin().equals(plugin)) {
                    final Player player = Bukkit.getPlayer(UUID.fromString(m.asString()));
                    final ItemStack i = a.getEquipment().getItemInMainHand();
                    if (player.getInventory().firstEmpty() != -1) {
                        if (player.getInventory().getItemInMainHand().getType() == Material.AIR)
                            player.getInventory().setItemInMainHand(i);
                        else player.getInventory().addItem(i);
                    } else player.getWorld().dropItem(player.getLocation(), i);
                    a.remove();
                }
    }

    private ConfigurationSection sectionn() {
        return plugin.getConfig().getConfigurationSection("abilities");
    }

    private void strikeEffect(final Location loc, final int yOffset, final int bound) {
        loc.getWorld().strikeLightningEffect(loc.add(ThreadLocalRandom.current().nextDouble(-bound, bound), yOffset, ThreadLocalRandom.current().nextDouble(-bound, bound)));
    }
}

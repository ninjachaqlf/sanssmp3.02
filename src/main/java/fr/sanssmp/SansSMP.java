package fr.sanssmp;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;

public class SansSMP extends JavaPlugin implements Listener {

    private final String PREFIX = ChatColor.AQUA + "" + ChatColor.BOLD + "[Sans SMP] " + ChatColor.RESET;

    private NamespacedKey swordKey;
    private NamespacedKey ritualKey;
    private Enchantment enchDensity;
    private Enchantment enchBreach;

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> helmetHits = new HashMap<>();
    private final Set<Location> protectedBlocks = new HashSet<>();
    private final Set<UUID> interdimUsers = new HashSet<>();
    private World arenaWorld;
    private int arenaOffset = 0;
    private World eventWorld;
    private Location eventSpawn;
    private boolean eventBuildAllowed = false;

    private boolean ritualActive = false;
    private Item ritualItem;
    private BossBar ritualBar;
    private BukkitTask ritualParticleTask;
    private final List<BukkitTask> ritualTasks = new ArrayList<>();
    private double ritualAngle = 0;

    @Override
    public void onEnable() {
        swordKey = new NamespacedKey(this, "sans_sword");
        ritualKey = new NamespacedKey(this, "ritual_item");
        enchDensity = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("density"));
        enchBreach = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("breach"));
        getServer().getPluginManager().registerEvents(this, this);
        try { Bukkit.removeRecipe(NamespacedKey.minecraft("mace")); } catch (Throwable ignored) {}
        for (Player p : Bukkit.getOnlinePlayers()) setTab(p);
        Bukkit.getScheduler().runTaskTimer(this, this::capAllMaces, 40L, 40L);
        arenaWorld = createArena();
        setupEventWorld();
        // Retour auto sur la plateforme si on tombe dans le vide du monde event
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (eventWorld == null || eventSpawn == null) return;
            for (Player p : eventWorld.getPlayers())
                if (p.getLocation().getY() < eventSpawn.getY() - 25) {
                    p.teleport(eventSpawn);
                    p.setFallDistance(0);
                }
        }, 20L, 10L);
        getLogger().info("Sans SMP active !");
    }

    private void setupEventWorld() {
        try {
            World w = Bukkit.getWorld("sans_event");
            if (w == null) w = new WorldCreator("sans_event")
                    .generator(new VoidGenerator()).environment(World.Environment.NORMAL).createWorld();
            if (w == null) return;
            eventWorld = w;
            w.setGameRule(GameRule.KEEP_INVENTORY, true);
            w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            int y = 100;
            for (int x = 0; x < 10; x++)
                for (int z = 0; z < 10; z++)
                    w.getBlockAt(x, y, z).setType(Material.SMOOTH_STONE);
            eventSpawn = new Location(w, 5.5, y + 1, 5.5);
            w.setSpawnLocation(5, y + 1, 5);
        } catch (Throwable t) {
            getLogger().warning("Impossible de creer le monde event : " + t.getMessage());
        }
    }

    private World createArena() {
        try {
            World w = Bukkit.getWorld("sans_arene");
            if (w == null) w = new WorldCreator("sans_arene")
                    .generator(new VoidGenerator())
                    .environment(World.Environment.NORMAL)
                    .createWorld();
            return w;
        } catch (Throwable t) {
            getLogger().warning("Impossible de creer l'arene Inter-Dimension : " + t.getMessage());
            return null;
        }
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        if ("sans_arene".equals(worldName) || "sans_event".equals(worldName)) return new VoidGenerator();
        return null;
    }

    @Override
    public void onDisable() { if (ritualActive) endRitual(false); }

    // ===================== TAB =====================
    private void setTab(Player p) {
        TextColor violet = TextColor.color(0xB14CFF);
        Component header = Component.text("")
                .append(Component.newline())
                .append(Component.text("\u2726  SANS SMP  \u2726", violet, TextDecoration.BOLD))
                .append(Component.newline());
        Component footer = Component.text("")
                .append(Component.newline())
                .append(Component.text("Owners : ", NamedTextColor.GRAY))
                .append(Component.text("NinjachaQLF", NamedTextColor.GOLD))
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text("_0sans", NamedTextColor.GOLD))
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text("_5sans", NamedTextColor.GOLD))
                .append(Component.newline());
        p.sendPlayerListHeaderAndFooter(header, footer);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        setTab(p);
        if (ritualActive && ritualBar != null) ritualBar.addPlayer(p);
    }

    // ===================== COMMANDES =====================
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("epee")) {
            if (args.length < 1) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Usage : /epee <dash|foudre|glace|vampire|vide|explosive|ocean|grappin|gravite|racine|catch|masse|casque> [joueur]");
                return true;
            }
            SwordType type = SwordType.fromId(args[0]);
            if (type == null) { sender.sendMessage(PREFIX + ChatColor.RED + "Objet inconnu : " + args[0]); return true; }
            Player target;
            if (args.length >= 2) {
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(PREFIX + ChatColor.RED + "Joueur introuvable."); return true; }
            } else if (sender instanceof Player) target = (Player) sender;
            else { sender.sendMessage(PREFIX + ChatColor.RED + "Precise un joueur."); return true; }
            target.getInventory().addItem(type.createItem(swordKey));
            target.sendMessage(PREFIX + ChatColor.GREEN + "Tu as recu : " + type.getDisplayName());
            return true;
        }
        if (command.getName().equalsIgnoreCase("rituel")) {
            if (args.length >= 1 && args[0].equalsIgnoreCase("stop")) {
                if (!ritualActive) { sender.sendMessage(PREFIX + ChatColor.RED + "Aucun rituel en cours."); return true; }
                endRitual(true); return true;
            }
            if (!(sender instanceof Player player)) { sender.sendMessage(PREFIX + ChatColor.RED + "Commande en jeu uniquement."); return true; }
            if (ritualActive) { sender.sendMessage(PREFIX + ChatColor.RED + "Un rituel est deja en cours ! (/rituel stop)"); return true; }
            SwordType type;
            if (args.length >= 1) {
                type = SwordType.fromId(args[0]);
                if (type == null) { sender.sendMessage(PREFIX + ChatColor.RED + "Objet inconnu : " + args[0]); return true; }
            } else {
                List<SwordType> pool = new ArrayList<>(Arrays.asList(SwordType.values()));
                pool.remove(SwordType.TOKEN);
                type = pool.get(new Random().nextInt(pool.size()));
            }
            startRitual(type, player.getLocation());
            return true;
        }

        if (command.getName().equalsIgnoreCase("warp")) {
            if (!(sender instanceof Player player)) { sender.sendMessage(PREFIX + ChatColor.RED + "Commande en jeu uniquement."); return true; }
            if (args.length >= 1 && args[0].equalsIgnoreCase("event")) {
                if (eventWorld == null || eventSpawn == null) { player.sendMessage(PREFIX + ChatColor.RED + "Le monde event n'est pas disponible."); return true; }
                player.teleport(eventSpawn);
                player.sendMessage(PREFIX + ChatColor.AQUA + "Bienvenue dans le monde event !");
                return true;
            }
            sender.sendMessage(PREFIX + ChatColor.RED + "Usage : /warp event");
            return true;
        }

        if (command.getName().equalsIgnoreCase("event")) {
            if (args.length >= 2 && args[0].equalsIgnoreCase("build")) {
                if (args[1].equalsIgnoreCase("on")) { eventBuildAllowed = true; sender.sendMessage(PREFIX + ChatColor.GREEN + "Casse/pose de blocs AUTORISEE dans le monde event."); }
                else if (args[1].equalsIgnoreCase("off")) { eventBuildAllowed = false; sender.sendMessage(PREFIX + ChatColor.RED + "Casse/pose de blocs BLOQUEE dans le monde event."); }
                else sender.sendMessage(PREFIX + ChatColor.RED + "Usage : /event build <on|off>");
                return true;
            }
            sender.sendMessage(PREFIX + ChatColor.RED + "Usage : /event build <on|off>");
            return true;
        }
        return false;
    }

    // ===================== RITUEL =====================
    private void startRitual(SwordType type, Location center) {
        World world = center.getWorld();
        if (world == null) return;
        ritualActive = true;
        Location loc = center.clone().add(0, 3, 0);
        Item item = world.dropItem(loc, type.createItem(swordKey));
        item.setGravity(false); item.setVelocity(new Vector(0, 0, 0));
        item.setPickupDelay(Integer.MAX_VALUE); item.setInvulnerable(true);
        item.setGlowing(true); item.setCustomNameVisible(true);
        item.setCustomName(type.getDisplayName()); item.setCanMobPickup(false); item.setWillAge(false);
        item.getPersistentDataContainer().set(ritualKey, PersistentDataType.BYTE, (byte) 1);
        ritualItem = item;

        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        ritualBar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "RITUEL "
                + ChatColor.WHITE + type.getDisplayName() + ChatColor.GRAY + "  |  "
                + ChatColor.AQUA + "X: " + x + "  Y: " + y + "  Z: " + z, BarColor.PURPLE, BarStyle.SOLID);
        for (Player p : Bukkit.getOnlinePlayers()) ritualBar.addPlayer(p);

        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
        Bukkit.broadcastMessage(PREFIX + ChatColor.LIGHT_PURPLE + "Un RITUEL a commence ! " + type.getDisplayName()
                + ChatColor.LIGHT_PURPLE + " flotte en X:" + x + " Y:" + y + " Z:" + z + ". Elle tombe dans 2 minutes !");

        ritualAngle = 0;
        ritualParticleTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (ritualItem == null || !ritualItem.isValid()) return;
            Location l = ritualItem.getLocation();
            ritualAngle += 0.20;
            double r = 1.1;
            for (int i = 0; i < 3; i++) {
                double a = ritualAngle + (i * (2 * Math.PI / 3));
                Location pt = l.clone().add(Math.cos(a) * r, Math.sin(ritualAngle * 2 + i) * 0.4, Math.sin(a) * r);
                world.spawnParticle(Particle.END_ROD, pt, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.WITCH, pt, 1, 0, 0, 0, 0);
            }
            world.spawnParticle(Particle.ENCHANT, l.clone().add(0, 0.4, 0), 8, 0.5, 0.5, 0.5, 0.6);
            world.spawnParticle(Particle.PORTAL, l, 6, 0.3, 0.3, 0.3, 0.2);
        }, 0L, 2L);

        ritualTasks.add(Bukkit.getScheduler().runTaskLater(this, () -> Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW + "L'objet tombe dans 60 secondes !"), 60 * 20L));
        ritualTasks.add(Bukkit.getScheduler().runTaskLater(this, () -> Bukkit.broadcastMessage(PREFIX + ChatColor.GOLD + "L'objet tombe dans 30 secondes !"), 90 * 20L));
        ritualTasks.add(Bukkit.getScheduler().runTaskLater(this, () -> Bukkit.broadcastMessage(PREFIX + ChatColor.RED + "L'objet tombe dans 10 secondes !"), 110 * 20L));
        ritualTasks.add(Bukkit.getScheduler().runTaskLater(this, () -> {
            if (ritualItem == null || !ritualItem.isValid()) { endRitual(false); return; }
            ritualItem.setGravity(true); ritualItem.setInvulnerable(false); ritualItem.setPickupDelay(0);
            world.strikeLightningEffect(ritualItem.getLocation());
            world.playSound(ritualItem.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1.2f);
            if (ritualParticleTask != null) ritualParticleTask.cancel();
            if (ritualBar != null) ritualBar.setTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "L'OBJET EST TOMBE ! Premier arrive, premier servi !");
            Bukkit.broadcastMessage(PREFIX + ChatColor.GREEN + "L'objet est TOMBE ! " + ChatColor.BOLD + "Premier arrive, premier servi !");
        }, 120 * 20L));
    }

    private void endRitual(boolean announce) {
        ritualActive = false;
        for (BukkitTask t : ritualTasks) if (t != null) t.cancel();
        ritualTasks.clear();
        if (ritualParticleTask != null) { ritualParticleTask.cancel(); ritualParticleTask = null; }
        if (ritualBar != null) { ritualBar.removeAll(); ritualBar = null; }
        if (ritualItem != null && ritualItem.isValid()) ritualItem.remove();
        ritualItem = null;
        if (announce) Bukkit.broadcastMessage(PREFIX + ChatColor.RED + "Le rituel a ete annule.");
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        Item item = event.getItem();
        if (!item.getPersistentDataContainer().has(ritualKey, PersistentDataType.BYTE)) return;
        if (!(event.getEntity() instanceof Player player)) { event.setCancelled(true); return; }
        SwordType type = SwordType.fromItem(item.getItemStack(), swordKey);
        String name = (type != null) ? type.getDisplayName() : (ChatColor.AQUA + "l'objet du rituel");
        Bukkit.broadcastMessage(PREFIX + ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " a remporte le rituel et obtenu " + name + ChatColor.YELLOW + " !");
        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        if (ritualParticleTask != null) { ritualParticleTask.cancel(); ritualParticleTask = null; }
        if (ritualBar != null) { ritualBar.removeAll(); ritualBar = null; }
        for (BukkitTask t : ritualTasks) if (t != null) t.cancel();
        ritualTasks.clear(); ritualItem = null; ritualActive = false;
    }

    // ===================== CAPACITES (s'accroupir + clic droit) =====================
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        // Ender pearls : utilisables au craft mais pas pour se teleporter
        if (event.getItem() != null && event.getItem().getType() == Material.ENDER_PEARL) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("Les ender pearls ne servent qu'au craft ici", NamedTextColor.RED));
            return;
        }
        Player player = event.getPlayer();
        SwordType type = SwordType.fromItem(player.getInventory().getItemInMainHand(), swordKey);
        if (type == null || type == SwordType.VAMPIRE || type == SwordType.CASQUE || type == SwordType.TOKEN) return;
        if (!player.isSneaking()) return; // sans accroupissement -> bouclier/clic normal
        if (isOnCooldown(player, type)) return;
        event.setCancelled(true);
        customSound(player.getLocation(), "sanssmp:" + type.getId());
        switch (type) {
            case DASH -> abilityDash(player);
            case FOUDRE -> abilityLightning(player);
            case GLACE -> abilityIce(player);
            case VIDE -> abilityVoid(player);
            case EXPLOSIVE -> abilityExplosive(player);
            case OCEAN -> abilityOcean(player);
            case GRAPPIN -> abilityGrapple(player);
            case GRAVITE -> abilityGravity(player);
            case RACINE -> abilityRoot(player);
            case INTERDIM -> abilityInterdim(player);
            case CATCH -> abilityCatch(player);
            case MASSE -> abilityStun(player);
            default -> { }
        }
    }

    private void abilityDash(Player player) {
        Vector dir = player.getLocation().getDirection().normalize().multiply(2.4);
        dir.setY(Math.max(dir.getY(), 0.35) + 0.25);
        player.setVelocity(dir);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.6f);
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 0.3, 0), 20, 0.3, 0.2, 0.3, 0.02);
    }

    private void abilityLightning(Player player) {
        Block target = player.getTargetBlockExact(30);
        Location loc = (target != null) ? target.getLocation().add(0.5, 1, 0.5)
                : player.getEyeLocation().add(player.getLocation().getDirection().multiply(15));
        World world = player.getWorld();
        world.strikeLightning(loc);
        for (Entity e : world.getNearbyEntities(loc, 4, 4, 4))
            if (e instanceof LivingEntity le && !e.equals(player)) le.damage(8.0, player);
    }

    private void abilityIce(Player player) {
        World world = player.getWorld();
        RayTraceResult res = world.rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 25, 1.0, e -> e != player && e instanceof LivingEntity);
        if (res != null && res.getHitEntity() instanceof LivingEntity le) {
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 4));
            le.setFreezeTicks(160);
            le.getWorld().spawnParticle(Particle.SNOWFLAKE, le.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.05);
            le.getWorld().playSound(le.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.4f);
        } else player.sendMessage(PREFIX + ChatColor.GRAY + "Aucune cible a geler.");
    }

    private void abilityVoid(Player player) {
        Block target = player.getTargetBlockExact(40);
        if (target == null) { player.sendMessage(PREFIX + ChatColor.GRAY + "Aucun bloc en vue."); return; }
        Location from = player.getLocation();
        Location dest = target.getLocation().add(0.5, 1, 0.5);
        dest.setYaw(from.getYaw()); dest.setPitch(from.getPitch());
        from.getWorld().spawnParticle(Particle.PORTAL, from.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.6);
        player.teleport(dest);
        dest.getWorld().spawnParticle(Particle.PORTAL, dest.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.6);
        player.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }

    private void abilityExplosive(Player player) {
        Block target = player.getTargetBlockExact(40);
        Location loc = (target != null) ? target.getLocation().add(0.5, 1, 0.5)
                : player.getEyeLocation().add(player.getLocation().getDirection().multiply(12));
        player.getWorld().createExplosion(loc, 3.0f, false, false, player);
    }

    // Ocean : bulle + vraie eau autour + nage rapide pendant 8s
    private void abilityOcean(Player player) {
        final World world = player.getWorld();
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_DOLPHIN_SPLASH, 1f, 1f);
        player.sendMessage(PREFIX + ChatColor.AQUA + "Bulle d'ocean activee !");
        // vraie eau temporaire autour du joueur (remplace seulement l'air)
        placeTempWater(player.getLocation(), 3, 160);
        final double radius = 4.0;
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks >= 160 || !player.isOnline()) { holder[0].cancel(); return; }
                ticks += 4;
                Location c = player.getLocation().add(0, 1, 0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 60, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 60, 1));
                for (int i = 0; i < 25; i++) {
                    double theta = Math.random() * Math.PI * 2, phi = Math.acos(2 * Math.random() - 1);
                    world.spawnParticle(Particle.BUBBLE_COLUMN_UP, c.clone().add(
                            radius * Math.sin(phi) * Math.cos(theta), radius * Math.cos(phi), radius * Math.sin(phi) * Math.sin(theta)), 1, 0, 0, 0, 0);
                }
                world.spawnParticle(Particle.SPLASH, c, 10, radius / 2, radius / 2, radius / 2, 0.1);
                for (Entity e : world.getNearbyEntities(c, radius, radius, radius))
                    if (e instanceof Player other && !other.equals(player)) {
                        other.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 0));
                        other.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0));
                    }
            }
        }, 0L, 4L);
    }

    // Grappin : projection vers le bloc vise
    private void abilityGrapple(Player player) {
        Block target = player.getTargetBlockExact(45);
        Location dest = (target != null) ? target.getLocation().add(0.5, 1, 0.5)
                : player.getEyeLocation().add(player.getLocation().getDirection().multiply(20));
        Vector pull = dest.toVector().subtract(player.getLocation().toVector());
        double dist = pull.length();
        if (dist < 0.5) return;
        pull.normalize().multiply(Math.min(0.5 + dist * 0.22, 2.6));
        pull.setY(pull.getY() + 0.35);
        player.setVelocity(pull);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 1.2f);
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
    }

    // Gravite : levitation aux ennemis proches
    private void abilityGravity(Player player) {
        World world = player.getWorld();
        Location c = player.getLocation();
        boolean hit = false;
        for (Entity e : world.getNearbyEntities(c, 6, 4, 6))
            if (e instanceof LivingEntity le && !e.equals(player)) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 1));
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));
                le.getWorld().spawnParticle(Particle.PORTAL, le.getLocation().add(0, 1, 0), 25, 0.4, 0.8, 0.4, 0.3);
                hit = true;
            }
        world.playSound(c, Sound.BLOCK_CONDUIT_ACTIVATE, 1f, 1f);
        if (!hit) player.sendMessage(PREFIX + ChatColor.GRAY + "Aucun ennemi a portee.");
    }

    // Racine : toiles d'araignee autour de la cible (3s)
    private void abilityRoot(Player player) {
        World world = player.getWorld();
        RayTraceResult res = world.rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 30, 1.0, e -> e != player && e instanceof LivingEntity);
        if (res == null || !(res.getHitEntity() instanceof LivingEntity le)) { player.sendMessage(PREFIX + ChatColor.GRAY + "Aucune cible visee."); return; }
        Location base = le.getLocation();
        final Map<Block, BlockData> saved = new HashMap<>();
        for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
            Block b = base.clone().add(dx, dy, dz).getBlock();
            if (b.getType().isAir()) { saved.put(b, b.getBlockData()); b.setType(Material.COBWEB); }
        }
        le.getWorld().playSound(base, Sound.ENTITY_SPIDER_AMBIENT, 1f, 0.8f);
        Bukkit.getScheduler().runTaskLater(this, () -> saved.forEach((b, d) -> b.setBlockData(d)), 60L);
    }

    // Epee Inter-Dimension : duel dans une salle d'un autre monde (2 min)
    private void abilityInterdim(Player player) {
        if (arenaWorld == null) { player.sendMessage(PREFIX + ChatColor.RED + "L'arene n'est pas disponible."); return; }
        World w = player.getWorld();
        RayTraceResult res = w.rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(),
                30, 1.0, e -> e != player && e instanceof Player);
        if (res == null || !(res.getHitEntity() instanceof Player target)) {
            player.sendMessage(PREFIX + ChatColor.GRAY + "Aucun joueur vise.");
            return;
        }
        final Location userBack = player.getLocation().clone();
        final Location targetBack = target.getLocation().clone();

        int ox = arenaOffset;
        arenaOffset += 64;
        final Location center = new Location(arenaWorld, ox + 0.5, 101, 0.5);
        final List<Location> placed = buildArena(center);

        Location p1 = center.clone().add(-1.5, 0, 1.5); p1.setYaw(135);
        Location p2 = center.clone().add(1.5, 0, -1.5); p2.setYaw(-45);
        player.teleport(p1);
        target.teleport(p2);

        applyMaxHealth(player, 20.0);
        if (player.getAttribute(maxHealthAttr()) != null) player.setHealth(player.getAttribute(maxHealthAttr()).getValue());
        interdimUsers.add(player.getUniqueId());

        arenaWorld.playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 1f, 1f);
        player.sendMessage(PREFIX + ChatColor.DARK_PURPLE + "Faille dimensionnelle ! 20 coeurs + degats renforces pendant 2 minutes.");
        target.sendMessage(PREFIX + ChatColor.DARK_PURPLE + "Tu as ete aspire dans une autre dimension par " + player.getName() + " !");
        Bukkit.broadcastMessage(PREFIX + ChatColor.LIGHT_PURPLE + player.getName() + " a defie " + target.getName() + " dans l'Inter-Dimension !");

        Bukkit.getScheduler().runTaskLater(this, () -> {
            interdimUsers.remove(player.getUniqueId());
            removeMaxHealth(player);
            if (player.isOnline()) player.teleport(userBack);
            if (target.isOnline()) target.teleport(targetBack);
            for (Location l : placed) { l.getBlock().setType(Material.AIR); protectedBlocks.remove(l); }
            player.sendMessage(PREFIX + ChatColor.GRAY + "Retour a la realite.");
        }, 120 * 20L);
    }

    private List<Location> buildArena(Location center) {
        World w = center.getWorld();
        List<Location> placed = new ArrayList<>();
        if (w == null) return placed;
        int bx = center.getBlockX(), by = center.getBlockY(), bz = center.getBlockZ();
        int half = 3; // interieur 5x5, un peu petite
        for (int dx = -half; dx <= half; dx++)
            for (int dy = -1; dy <= 4; dy++)
                for (int dz = -half; dz <= half; dz++) {
                    boolean shell = dx == -half || dx == half || dz == -half || dz == half || dy == -1 || dy == 4;
                    if (!shell) continue;
                    Block b = w.getBlockAt(bx + dx, by + dy, bz + dz);
                    b.setType(Material.OBSIDIAN);
                    Location loc = b.getLocation();
                    protectedBlocks.add(loc);
                    placed.add(loc);
                }
        return placed;
    }

    private Attribute maxHealthAttr() {
        return Registry.ATTRIBUTE.get(NamespacedKey.minecraft("max_health"));
    }

    private void applyMaxHealth(Player p, double amount) {
        Attribute hp = maxHealthAttr();
        if (hp == null) return;
        AttributeInstance inst = p.getAttribute(hp);
        if (inst == null) return;
        NamespacedKey key = new NamespacedKey(this, "interdim_hp");
        inst.getModifiers().stream().filter(m -> m.getKey().equals(key)).toList().forEach(inst::removeModifier);
        inst.addModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
    }

    private void removeMaxHealth(Player p) {
        Attribute hp = maxHealthAttr();
        if (hp == null) return;
        AttributeInstance inst = p.getAttribute(hp);
        if (inst == null) return;
        NamespacedKey key = new NamespacedKey(this, "interdim_hp");
        inst.getModifiers().stream().filter(m -> m.getKey().equals(key)).toList().forEach(inst::removeModifier);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    // Catch Axe : prison d'obsidienne
    private void abilityCatch(Player player) {
        World world = player.getWorld();
        RayTraceResult res = world.rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 30, 1.0, e -> e != player && e instanceof Player);
        if (res == null || !(res.getHitEntity() instanceof Player target)) { player.sendMessage(PREFIX + ChatColor.GRAY + "Aucun joueur vise."); return; }
        Location center = target.getLocation();
        player.teleport(center.clone().add(2, 0, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 2));
        buildPrison(center);
        player.getWorld().playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 0.6f);
        Bukkit.broadcastMessage(PREFIX + ChatColor.DARK_PURPLE + player.getName() + " a emprisonne " + target.getName() + " (20s) !");
    }

    private void buildPrison(Location center) {
        World w = center.getWorld();
        if (w == null) return;
        int bx = center.getBlockX(), by = center.getBlockY(), bz = center.getBlockZ();
        final Map<Block, BlockData> saved = new HashMap<>();
        final List<Location> placed = new ArrayList<>();
        int half = 5;
        for (int dx = -half; dx <= half; dx++) for (int dy = -1; dy <= 9; dy++) for (int dz = -half; dz <= half; dz++) {
            boolean shell = dx == -half || dx == half || dz == -half || dz == half || dy == -1 || dy == 9;
            if (!shell) continue;
            Block b = w.getBlockAt(bx + dx, by + dy, bz + dz);
            saved.put(b, b.getBlockData());
            b.setType(Material.OBSIDIAN);
            Location loc = b.getLocation();
            protectedBlocks.add(loc); placed.add(loc);
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Map.Entry<Block, BlockData> e : saved.entrySet()) e.getKey().setBlockData(e.getValue());
            placed.forEach(protectedBlocks::remove);
        }, 20 * 20L);
    }

    private void placeTempWater(Location center, int radius, long durationTicks) {
        World w = center.getWorld();
        if (w == null) return;
        final Map<Block, BlockData> saved = new HashMap<>();
        for (int dx = -radius; dx <= radius; dx++) for (int dy = -1; dy <= radius; dy++) for (int dz = -radius; dz <= radius; dz++) {
            if (dx * dx + dy * dy + dz * dz > radius * radius) continue;
            Block b = center.clone().add(dx, dy, dz).getBlock();
            if (b.getType().isAir()) { saved.put(b, b.getBlockData()); b.setType(Material.WATER); }
        }
        Bukkit.getScheduler().runTaskLater(this, () -> saved.forEach((b, d) -> b.setBlockData(d)), durationTicks);
    }

    // Masse : etourdissement
    private void abilityStun(Player player) {
        World world = player.getWorld();
        Location c = player.getLocation();
        boolean hit = false;
        for (Entity e : world.getNearbyEntities(c, 6, 4, 6))
            if (e instanceof Player other && !other.equals(player)) {
                other.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 6));
                other.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 128));
                other.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 3));
                other.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                other.getWorld().spawnParticle(Particle.CRIT, other.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.2);
                other.sendMessage(PREFIX + ChatColor.RED + "Tu es etourdi !");
                hit = true;
            }
        world.playSound(c, Sound.BLOCK_ANVIL_LAND, 1f, 0.8f);
        if (!hit) player.sendMessage(PREFIX + ChatColor.GRAY + "Aucun joueur a portee.");
    }

    // ===================== PASSIFS (degats) =====================
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        SwordType weapon = SwordType.fromItem(player.getInventory().getItemInMainHand(), swordKey);

        // Cooldown de 15s sur l'UTILISATION de la masse (le coup lui-meme)
        if (weapon == SwordType.MASSE) {
            long now = System.currentTimeMillis();
            Map<String, Long> m = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
            long readyAt = m.getOrDefault("masse_use", 0L) + 15000L;
            if (now < readyAt) {
                event.setCancelled(true);
                player.sendActionBar(Component.text(String.format("Masse : %.1fs", (readyAt - now) / 1000.0), NamedTextColor.RED));
                return;
            }
            m.put("masse_use", now);
        }

        // Bonus +0.5 coeur/coup pendant le duel Inter-Dimension
        if (interdimUsers.contains(player.getUniqueId()))
            event.setDamage(event.getDamage() + 1.0);

        // Son de hit custom pour toute arme du Sans SMP
        if (weapon != null && weapon != SwordType.CASQUE && weapon != SwordType.TOKEN && event.getEntity() instanceof LivingEntity)
            customSound(event.getEntity().getLocation(), "sanssmp:hit");

        // Vampire (en main)
        if (weapon == SwordType.VAMPIRE) {
            double heal = event.getFinalDamage() * 0.35;
            player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 3, 0.3, 0.3, 0.3, 0);
        }

        // Casque du Piglin : tous les 40 coups -> +4 degats
        if (event.getEntity() instanceof LivingEntity victim
                && SwordType.fromItem(player.getInventory().getHelmet(), swordKey) == SwordType.CASQUE) {
            int hits = helmetHits.merge(player.getUniqueId(), 1, Integer::sum);
            if (hits % 40 == 0) {
                victim.damage(4.0, player);
                victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.3);
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PIGLIN_ANGRY, 1f, 1f);
                player.sendMessage(PREFIX + ChatColor.GOLD + "Furie du Piglin ! (+2 coeurs de degats)");
            }
        }
    }

    // ===================== MASSE : non-craft + cap enchant =====================
    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getRecipe() != null && event.getRecipe().getResult().getType() == Material.MACE) event.setCancelled(true);
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack r = event.getResult();
        if (r == null || r.getType() != Material.MACE) return;
        boolean changed = false;
        if (enchDensity != null && r.getEnchantmentLevel(enchDensity) > 2) { r.addUnsafeEnchantment(enchDensity, 2); changed = true; }
        if (enchBreach != null && r.getEnchantmentLevel(enchBreach) > 2) { r.addUnsafeEnchantment(enchBreach, 2); changed = true; }
        if (changed) event.setResult(r);
    }

    private void capAllMaces() {
        for (Player p : Bukkit.getOnlinePlayers())
            for (ItemStack it : p.getInventory().getContents()) {
                if (it == null || it.getType() != Material.MACE) continue;
                if (enchDensity != null && it.getEnchantmentLevel(enchDensity) > 2) it.addUnsafeEnchantment(enchDensity, 2);
                if (enchBreach != null && it.getEnchantmentLevel(enchBreach) > 2) it.addUnsafeEnchantment(enchBreach, 2);
            }
    }

    // ===================== BLOCS PROTEGES =====================
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (protectedBlocks.contains(event.getBlock().getLocation())) { event.setCancelled(true); return; }
        if (eventWorld != null && event.getBlock().getWorld().equals(eventWorld) && !eventBuildAllowed
                && !event.getPlayer().isOp()) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("Casse de blocs desactivee ici", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (eventWorld != null && event.getBlock().getWorld().equals(eventWorld) && !eventBuildAllowed
                && !event.getPlayer().isOp()) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("Pose de blocs desactivee ici", NamedTextColor.RED));
        }
    }

    // Token SANS's : 50% de drop quand on tue un joueur
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer != null && Math.random() < 0.5) {
            victim.getWorld().dropItemNaturally(victim.getLocation(), SwordType.TOKEN.createItem(swordKey));
            killer.sendMessage(PREFIX + ChatColor.GOLD + "Un Token SANS's a ete laisse par " + victim.getName() + " !");
        }
    }

    // Empeche le tp a l'ender pearl (craft autorise, deplacement non)
    @EventHandler
    public void onPearlLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof EnderPearl pearl && pearl.getShooter() instanceof Player)
            event.setCancelled(true);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> protectedBlocks.contains(b.getLocation()));
    }

    // ===================== COOLDOWNS =====================
    private void customSound(Location loc, String key) {
        if (loc.getWorld() != null) loc.getWorld().playSound(loc, key, SoundCategory.PLAYERS, 1.3f, 1.0f);
    }

    private boolean isOnCooldown(Player player, SwordType type) {
        if (type.getCooldown() <= 0) return false;
        long now = System.currentTimeMillis();
        Map<String, Long> map = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        long readyAt = map.getOrDefault(type.getId(), 0L) + type.getCooldown() * 1000L;
        if (now < readyAt) {
            player.sendActionBar(Component.text(String.format("Recharge : %.1fs", (readyAt - now) / 1000.0), NamedTextColor.RED));
            return true;
        }
        map.put(type.getId(), now);
        return false;
    }
}

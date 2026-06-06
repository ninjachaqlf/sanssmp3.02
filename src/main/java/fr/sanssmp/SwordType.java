package fr.sanssmp;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tous les objets custom du Sans SMP.
 * id, materiau, nom, capacite, cooldown (s), customModelData (texture pack), lore.
 */
public enum SwordType {

    DASH("dash", Material.DIAMOND_SWORD, ChatColor.AQUA + "" + ChatColor.BOLD + "Dash Sword",
            ChatColor.AQUA + "\u26A1 Dash fulgurant", 20, 1,
            Arrays.asList(ChatColor.GRAY + "Propulse-toi en avant.", ChatColor.DARK_GRAY + "S'accroupir + clic droit")),

    FOUDRE("foudre", Material.DIAMOND_SWORD, ChatColor.YELLOW + "" + ChatColor.BOLD + "Lame de Foudre",
            ChatColor.YELLOW + "\uD83C\uDF29 Frappe celeste", 20, 2,
            Arrays.asList(ChatColor.GRAY + "Eclair + degats de zone.", ChatColor.DARK_GRAY + "S'accroupir + clic droit")),

    GLACE("glace", Material.DIAMOND_SWORD, ChatColor.AQUA + "" + ChatColor.BOLD + "Lame de Glace",
            ChatColor.AQUA + "\u2744 Gel instantane", 20, 3,
            Arrays.asList(ChatColor.GRAY + "Fige la cible visee.", ChatColor.DARK_GRAY + "S'accroupir + clic droit")),

    VAMPIRE("vampire", Material.DIAMOND_SWORD, ChatColor.DARK_RED + "" + ChatColor.BOLD + "Lame Vampirique",
            ChatColor.RED + "\uD83E\uDE78 Vol de vie", 0, 4,
            Arrays.asList(ChatColor.GRAY + "Chaque coup te rend de la vie.", ChatColor.DARK_GRAY + "Passif")),

    VIDE("vide", Material.DIAMOND_SWORD, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Lame du Vide",
            ChatColor.LIGHT_PURPLE + "\uD83C\uDF00 Teleportation", 20, 5,
            Arrays.asList(ChatColor.GRAY + "Teleporte-toi au bloc vise.", ChatColor.DARK_GRAY + "S'accroupir + clic droit")),

    EXPLOSIVE("explosive", Material.DIAMOND_SWORD, ChatColor.GOLD + "" + ChatColor.BOLD + "Lame Explosive",
            ChatColor.GOLD + "\uD83D\uDCA5 Onde de choc", 20, 6,
            Arrays.asList(ChatColor.GRAY + "Explosion qui repousse (sans casser).", ChatColor.DARK_GRAY + "S'accroupir + clic droit")),

    OCEAN("ocean", Material.DIAMOND_SWORD, ChatColor.BLUE + "" + ChatColor.BOLD + "Ocean Sword",
            ChatColor.AQUA + "\uD83C\uDF0A Bulle d'ocean", 60, 7,
            Arrays.asList(ChatColor.GRAY + "Bulle d'eau (8s), tu nages vite + Regen II.",
                    ChatColor.GRAY + "Les intrus : Poison + Cecite.", ChatColor.DARK_GRAY + "S'accroupir + clic droit")),

    GRAPPIN("grappin", Material.DIAMOND_SWORD, ChatColor.GREEN + "" + ChatColor.BOLD + "Lame du Grappin",
            ChatColor.GREEN + "\uD83E\uDE9D Grappin", 20, 8,
            Arrays.asList(ChatColor.GRAY + "Te projette vers le bloc vise.", ChatColor.DARK_GRAY + "S'accroupir + clic droit")),

    GRAVITE("gravite", Material.DIAMOND_SWORD, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Lame de Gravite",
            ChatColor.DARK_AQUA + "\uD83C\uDF0C Zone d'apesanteur", 25, 9,
            Arrays.asList(ChatColor.GRAY + "Les ennemis proches flottent (Levitation 3s).",
                    ChatColor.GRAY + "Ils deviennent vulnerables.", ChatColor.DARK_GRAY + "S'accroupir + clic droit")),

    RACINE("racine", Material.DIAMOND_SWORD, ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Lame Racine",
            ChatColor.DARK_GREEN + "\uD83C\uDF3F Emprisonnement", 25, 10,
            Arrays.asList(ChatColor.GRAY + "Piege la cible dans des toiles (3s).", ChatColor.DARK_GRAY + "S'accroupir + clic droit")),

    INTERDIM("interdim", Material.DIAMOND_SWORD, ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Epee Inter-Dimension",
            ChatColor.DARK_PURPLE + "\uD83C\uDF11 Faille dimensionnelle", 420, 13,
            Arrays.asList(ChatColor.GRAY + "Te teleporte avec ta cible dans une salle",
                    ChatColor.GRAY + "d'un autre monde, enfermes 2 minutes.",
                    ChatColor.GRAY + "Tu obtiens 20 coeurs + 0.5 coeur de degats/coup.",
                    ChatColor.DARK_GRAY + "S'accroupir + clic droit sur un joueur")),

    CATCH("catch", Material.DIAMOND_AXE, ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Catch Axe",
            ChatColor.DARK_PURPLE + "\u26D3 Prison d'obsidienne", 90, 11,
            Arrays.asList(ChatColor.GRAY + "Salle d'obsidienne 10x10 (20s) + Vitesse III.", ChatColor.DARK_GRAY + "S'accroupir + clic droit sur un joueur")),

    MASSE("masse", Material.MACE, ChatColor.RED + "" + ChatColor.BOLD + "Masse du Sans SMP",
            ChatColor.RED + "\uD83D\uDCA2 Etourdissement", 30, 12,
            Arrays.asList(ChatColor.GRAY + "Seule masse du serveur. Etourdit les proches.",
                    ChatColor.DARK_GRAY + "Densite & Breach max 2  -  S'accroupir + clic droit")),

    CASQUE("casque", Material.PIGLIN_HEAD, ChatColor.GOLD + "" + ChatColor.BOLD + "Casque du Piglin",
            ChatColor.GOLD + "\uD83D\uDC37 Furie du Piglin", 0, 0,
            Arrays.asList(ChatColor.GRAY + "+2 coeurs de vie.",
                    ChatColor.GRAY + "Tous les 40 coups : +2 coeurs de degats.",
                    ChatColor.GRAY + "Protection IV + Solidite III (comme un casque diamant).",
                    ChatColor.DARK_GRAY + "A porter sur la tete")),

    TOKEN("token", Material.GOLD_INGOT, ChatColor.GOLD + "" + ChatColor.BOLD + "Token SANS's",
            ChatColor.YELLOW + "\uD83E\uDE99 Jeton", 0, 0,
            Arrays.asList(ChatColor.GRAY + "Recompense de kill du Sans SMP.",
                    ChatColor.DARK_GRAY + "Monnaie d'evenement"));

    private final String id;
    private final Material material;
    private final String displayName;
    private final String ability;
    private final int cooldown;
    private final int cmd;
    private final List<String> loreLines;

    SwordType(String id, Material material, String displayName, String ability,
              int cooldown, int cmd, List<String> loreLines) {
        this.id = id; this.material = material; this.displayName = displayName;
        this.ability = ability; this.cooldown = cooldown; this.cmd = cmd; this.loreLines = loreLines;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getCooldown() { return cooldown; }

    public ItemStack createItem(NamespacedKey key) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            List<String> lore = new ArrayList<>();
            lore.add(ability); lore.add("");
            lore.addAll(loreLines); lore.add("");
            lore.add(ChatColor.DARK_AQUA + "" + ChatColor.ITALIC + "Objet legendaire du Sans SMP");
            meta.setLore(lore);
            meta.setEnchantmentGlintOverride(true);
            meta.setUnbreakable(true);
            if (cmd > 0) meta.setCustomModelData(cmd);

            if (this == CASQUE) {
                addEnch(meta, "protection", 4);
                addEnch(meta, "unbreaking", 3);
                addAttr(meta, "max_health", 4.0, "casque_hp");
                addAttr(meta, "armor", 3.0, "casque_armor");
                addAttr(meta, "armor_toughness", 2.0, "casque_tough");
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            } else {
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
                if (this == MASSE) { addEnch(meta, "density", 2); addEnch(meta, "breach", 2); }
            }
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void addEnch(ItemMeta meta, String key, int level) {
        Enchantment e = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
        if (e != null) meta.addEnchant(e, level, true);
    }

    private static void addAttr(ItemMeta meta, String attrKey, double amount, String modName) {
        Attribute a = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(attrKey));
        if (a != null) meta.addAttributeModifier(a, new AttributeModifier(
                new NamespacedKey("sanssmp", modName), amount,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD));
    }

    public static SwordType fromItem(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return fromId(meta.getPersistentDataContainer().get(key, PersistentDataType.STRING));
    }

    public static SwordType fromId(String id) {
        if (id == null) return null;
        for (SwordType t : values()) if (t.id.equalsIgnoreCase(id)) return t;
        return null;
    }
}

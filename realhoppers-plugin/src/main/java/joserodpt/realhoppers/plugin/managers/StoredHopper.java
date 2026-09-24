package joserodpt.realhoppers.plugin.managers;

/*
 *   ____            _ _   _
 *  |  _ \ ___  __ _| | | | | ___  _ __  _ __   ___ _ __ ___
 *  | |_) / _ \/ _` | | |_| |/ _ \| '_ \| '_ \ / _ \ '__/ __|
 *  |  _ <  __/ (_| | |  _  | (_) | |_) | |_) |  __/ |  \__ \
 *  |_| \_\___|\__,_|_|_| |_|\___/| .__/| .__/ \___|_|  |___/
 *                                |_|   |_|
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2023-2026
 * @link https://github.com/joserodpt/RealHoppers
 */

import joserodpt.realhoppers.api.config.RHLanguage;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.utils.Items;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A private hopper packed into the item it drops as: whose it is and what was inside, so placing it
 * again puts back the same hopper rather than an empty one anybody could open.
 */
public final class StoredHopper {

    private final UUID owner;
    private final String ownerName;
    private final ItemStack[] contents;

    private StoredHopper(final UUID owner, final String ownerName, final ItemStack[] contents) {
        this.owner = owner;
        this.ownerName = ownerName;
        this.contents = contents;
    }

    public UUID getOwner() {
        return this.owner;
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    public ItemStack[] getContents() {
        return this.contents;
    }

    private static NamespacedKey key(final Plugin plugin, final String name) {
        return new NamespacedKey(plugin, name);
    }

    /** The item a private hopper drops as, carrying its owner and a copy of its contents. */
    public static ItemStack toItem(final Plugin plugin, final RHopper hopper, final ItemStack[] contents) {
        final long stored = Arrays.stream(contents).filter(Objects::nonNull).mapToLong(ItemStack::getAmount).sum();
        final List<String> lore = new ArrayList<>();
        for (final String line : RHLanguage.file().getStringList("Hoppers.Stored-Item.Description")) {
            lore.add(line.replace("%player%", hopper.getOwnerDisplayName()).replace("%value%", String.valueOf(stored)));
        }
        final ItemStack item = Items.createItem(Material.HOPPER, 1,
                RHLanguage.file().getString("Hoppers.Stored-Item.Name", "&dPrivate Hopper")
                        .replace("%player%", hopper.getOwnerDisplayName()), lore);

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        final PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(key(plugin, "owner"), PersistentDataType.STRING, hopper.getOwner().toString());
        if (hopper.getOwnerName() != null) {
            data.set(key(plugin, "owner_name"), PersistentDataType.STRING, hopper.getOwnerName());
        }
        data.set(key(plugin, "contents"), PersistentDataType.STRING, serialize(contents));
        //two of these with the same owner and contents would otherwise stack, and placing each of
        //the pair would put the same items back twice
        data.set(key(plugin, "id"), PersistentDataType.STRING, UUID.randomUUID().toString());
        item.setItemMeta(meta);
        return item;
    }

    /** What an item carries, or null for a plain hopper or one whose data can't be read. */
    public static StoredHopper fromItem(final Plugin plugin, final ItemStack item) {
        if (item == null || item.getType() != Material.HOPPER || !item.hasItemMeta()) {
            return null;
        }
        final PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        final String owner = data.get(key(plugin, "owner"), PersistentDataType.STRING);
        if (owner == null) {
            return null;
        }
        try {
            final String contents = data.get(key(plugin, "contents"), PersistentDataType.STRING);
            return new StoredHopper(UUID.fromString(owner),
                    data.get(key(plugin, "owner_name"), PersistentDataType.STRING),
                    contents == null ? new ItemStack[0] : deserialize(contents));
        } catch (final IllegalArgumentException | IOException | ClassNotFoundException e) {
            plugin.getLogger().warning("Could not read a stored hopper item: " + e.getMessage());
            return null;
        }
    }

    private static String serialize(final ItemStack[] contents) {
        try (final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             final BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            out.writeObject(contents);
            out.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (final IOException e) {
            throw new IllegalStateException("Could not store a hopper's contents", e);
        }
    }

    private static ItemStack[] deserialize(final String data) throws IOException, ClassNotFoundException {
        try (final BukkitObjectInputStream in = new BukkitObjectInputStream(
                new ByteArrayInputStream(Base64.getDecoder().decode(data)))) {
            return (ItemStack[]) in.readObject();
        }
    }
}

package joserodpt.realhoppers.plugin.gui;

/*
 *   ____            _ _   _
 *  |  _ \ ___  __ _| | | | | ___  _ __  _ __   ___ _ __ ___
 *  | |_) / _ \/ _` | | |_| |/ _ \| '_ \| '_ \ / _ \ '__/ __|
 *  |  _ <  __/ (_| | |  _  | (_) | |_) | |_) |  __/ |  \__ \
 *  |_| \_\___|\__,_|_|_| |_|\___/| .__/| .__/ \___|_|  |___/
 *                                |_|   |_|
 *
 * Licensed under the MIT License
 * @author José Rodrigues
 * @link https://github.com/joserodpt/RealHoppers
 */

import joserodpt.realhoppers.api.RealHoppersAPI;
import joserodpt.realhoppers.api.config.RHLanguage;
import joserodpt.realhoppers.api.config.TranslatableLine;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.utils.Itens;
import joserodpt.realhoppers.api.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;


public class HopperGUI {

    public static Map<UUID, HopperGUI> inventories = new HashMap<>();
    private Inventory inv;

    private final ItemStack close = Itens.createItem(Material.OAK_DOOR, 1, TranslatableLine.GUI_CLOSE_NAME.get(),
            RHLanguage.file().getStringList("GUI.Items.Close.Description"));

    private final UUID uuid;
    private RHopper h;
    private RealHoppersAPI rh;

    public HopperGUI(Player as, RHopper h, RealHoppersAPI rh) {
        this.rh = rh;
        this.uuid = as.getUniqueId();
        this.h = h;
        this.inv = Bukkit.getServer().createInventory(null, 27, TranslatableLine.GUI_TITLE.get());

        load();

        this.register();
    }

    public void load() {
        this.inv.setItem(22, close);

        this.inv.setItem(11, Itens.createItem(Material.CHEST, 1, TranslatableLine.GUI_HOPPER_INVENTORY_NAME.get()));

        this.inv.setItem(13, Itens.createItem(Material.HOPPER, 1, TranslatableLine.GUI_HOPPER_NAME.get(), h.getHopperDescription()));
    }

    public void openInventory(Player target) {
        Inventory inv = getInventory();
        InventoryView openInv = target.getOpenInventory();
        if (openInv != null) {
            Inventory openTop = target.getOpenInventory().getTopInventory();
            if (openTop != null && openTop.getType().name().equalsIgnoreCase(inv.getType().name())) {
                openTop.setContents(inv.getContents());
            } else {
                target.openInventory(inv);
            }
        }
    }

    public RHopper getHopper() {
        return h;
    }

    public static Listener getListener() {
        return new Listener() {
            @EventHandler
            public void onClick(InventoryClickEvent e) {
                HumanEntity clicker = e.getWhoClicked();
                if (clicker instanceof Player) {
                    Player p = (Player) clicker;
                    if (e.getCurrentItem() == null) {
                        return;
                    }
                    UUID uuid = clicker.getUniqueId();
                    if (inventories.containsKey(uuid)) {
                        HopperGUI current = inventories.get(uuid);
                        if (e.getInventory().getHolder() != current.getInventory().getHolder()) {
                            return;
                        }

                        e.setCancelled(true);

                        switch (e.getRawSlot()) {
                            case 22:
                                p.closeInventory();
                                break;
                            case 11:
                                p.closeInventory();
                                current.h.openInventory(p);
                                break;
                            case 13:
                                if (current.h.getBalance() > 0) {
                                    if (current.rh.getVault() == null) {
                                        TranslatableLine.SYSTEM_VAULT_MISSING.send(p);
                                        p.closeInventory();
                                        return;
                                    }

                                    if (Objects.requireNonNull(e.getClick()) == ClickType.SHIFT_LEFT) {
                                        current.rh.getVault().depositPlayer(p, current.h.getBalance());
                                        TranslatableLine.HOPPER_BALANCE_COLLECTED
                                                .setV1(TranslatableLine.ReplacableVar.MONEY.eq(Text.formatNumber(current.h.getBalance()))).send(p);
                                        current.h.setBalance(0);
                                    } else {
                                        current.rh.getVault().depositPlayer(p, current.h.getBalance() / 2);
                                        TranslatableLine.HOPPER_BALANCE_COLLECTED
                                                .setV1(TranslatableLine.ReplacableVar.MONEY.eq(Text.formatNumber(current.h.getBalance() / 2))).send(p);
                                        current.h.setBalance(current.h.getBalance() / 2);
                                    }
                                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                                    current.load();
                                }
                                break;
                        }
                    }
                }
            }
            @EventHandler
            public void onClose(InventoryCloseEvent e) {
                if (e.getPlayer() instanceof Player) {
                    if (e.getInventory() == null) {
                        return;
                    }
                    Player p = (Player) e.getPlayer();
                    UUID uuid = p.getUniqueId();
                    if (inventories.containsKey(uuid)) {
                        inventories.get(uuid).unregister();
                    }
                }
            }
        };
    }

    public Inventory getInventory() {
        return inv;
    }

    private void register() {
        inventories.put(this.uuid, this);
    }

    private void unregister() {
        inventories.remove(this.uuid);
    }
}
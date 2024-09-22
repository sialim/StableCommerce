package me.sialim.stablecommerce;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PaginatedGUI implements Listener, CommandExecutor {
    private final int ITEMS_PER_PAGE = 28;
    private List<ItemStack> items;
    private Map<UUID, Inventory> playerInventories;
    private Map<UUID, Integer> playerPages;
    int[] slotsToFill = {
            11, 12, 13, 14, 15, 16, 17,
            20, 21, 22, 23, 24, 25, 26,
            29, 30, 31, 32, 33, 34, 35,
            38, 39, 40, 41, 42, 43, 44
    };

    public PaginatedGUI(List<ItemStack> items) {
        this.playerInventories = new HashMap<>();
        this.playerPages = new HashMap<>();
        this.items = new ArrayList<>(items);
    }

    public void openInventory(Player p) {
        playerPages.putIfAbsent(p.getUniqueId(), 0);
        Inventory inventory = Bukkit.createInventory(null, 54, "Items - Page " + (playerPages.get(p.getUniqueId()) + 1));
        playerInventories.put(p.getUniqueId(), inventory);

        updateInventory(p);
    }

    private void updateInventory(Player p) {
        UUID pUUID = p.getUniqueId();
        Inventory inventory = playerInventories.get(pUUID);
        int page = playerPages.get(pUUID);

        inventory.clear();

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, items.size());
        for (int i = 0; i < slotsToFill.length && start + i < items.size(); i++) {
            inventory.setItem(slotsToFill[i], items.get(start + i));
        }

        if (page > 0) {
            inventory.setItem(49, createItem(Material.ARROW, "Previous Page"));
        }
        if ((page + 1) * ITEMS_PER_PAGE < items.size()) {
            inventory.setItem(51, createItem(Material.ARROW, "Next Page"));
        }

        inventory.setItem(9, createItem(Material.BARRIER, "Close Window"));
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void handleInventoryClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        UUID pUUID = p.getUniqueId();

        if (e.getInventory().equals(playerInventories.get(pUUID))) {
            e.setCancelled(true);

            ItemStack clicked = e.getCurrentItem();
            if (clicked != null && clicked.getItemMeta() != null) {
                String itemName = clicked.getItemMeta().getDisplayName();
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (commandSender instanceof Player p) {

        }
        return false;
    }
}

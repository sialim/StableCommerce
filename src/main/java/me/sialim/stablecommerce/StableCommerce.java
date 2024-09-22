package me.sialim.stablecommerce;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;

public final class StableCommerce extends JavaPlugin implements CommandExecutor {
    private File file;
    private static Economy vaultEconomy = null;
    private static Permission perms = null;
    private static Chat chat = null;
    private double baseBuyPrice = 10.0; // Initial base buy price
    private double baseSellPrice = 8.0; // Initial base sell price
    private double buyPrice = baseBuyPrice; // Current buy price
    private double sellPrice = baseSellPrice; // Current sell price
    private static double totalGoldBought = 0; // Total gold bought by all players
    private static double totalGoldSold = 0; // Total gold sold by all players
    private final double adjustmentFactor = 0.25; // Factor for price adjustments
    private final double minBuyPrice = 1.0; // Minimum buy price
    private final double minSellPrice = 1.0; // Minimum sell price

    @Override
    public void onEnable() {
        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        setupPermissions();
        setupChat();
        getCommand("buygold").setExecutor(this);
        getCommand("sellgold").setExecutor(this);
        file = new File(getDataFolder(), "gold_totals.txt");

        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        loadTransactionTotals();
    }

    @Override
    public void onDisable() {
        getLogger().info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
        saveTransactionTotals();
    }

    private void loadTransactionTotals() {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    if (parts[0].trim().equals("totalGoldBought")) {
                        totalGoldBought = Double.parseDouble(parts[1].trim());
                    } else if (parts[0].trim().equals("totalGoldSold")) {
                        totalGoldSold = Double.parseDouble(parts[1].trim());
                    }
                }
            }
        } catch (IOException e) {
            getLogger().warning("Could not load transaction totals: " + e.getMessage());
        }
    }

    private void saveTransactionTotals() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("totalGoldBought: " + totalGoldBought);
            writer.newLine();
            writer.write("totalGoldSold: " + totalGoldSold);
        } catch (IOException e) {
            getLogger().warning("Could not save transaction totals: " + e.getMessage());
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        vaultEconomy = rsp.getProvider();
        return vaultEconomy != null;
    }

    private boolean setupChat() {
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        chat = rsp.getProvider();
        return chat != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    private void adjustPrices(double amount, int transactionType) {
        if (transactionType == 0) {
            totalGoldBought += amount;
        } else {
            totalGoldSold += amount;
        }

        buyPrice = baseBuyPrice + (totalGoldBought / 100) * adjustmentFactor;
        sellPrice = baseSellPrice - (totalGoldSold / 100) * adjustmentFactor;

        buyPrice = Math.max(minBuyPrice, buyPrice);
        sellPrice = Math.max(minSellPrice, sellPrice);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use these commands.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("buygold")) {
            if (args.length != 1) {
                player.sendMessage("Usage: /buygold <amount>");
                return true;
            }

            double amountToBuy = Double.parseDouble(args[0]);
            double totalCost = buyPrice * amountToBuy;

            if (vaultEconomy.has(player, totalCost)) {
                vaultEconomy.withdrawPlayer(player, totalCost);
                player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, (int) amountToBuy));
                adjustPrices(amountToBuy, 0);
                player.sendMessage("You bought " + amountToBuy + " gold for " + totalCost);
            } else {
                player.sendMessage("You don't have enough balance!");
            }
            return true;

        } else if (command.getName().equalsIgnoreCase("sellgold")) {
            if (args.length != 1) {
                player.sendMessage("Usage: /sellgold <amount>");
                return true;
            }

            double amountToSell = Double.parseDouble(args[0]);
            ItemStack goldStack = new ItemStack(Material.GOLD_INGOT, (int) amountToSell);

            if (player.getInventory().contains(goldStack.getType(), (int) amountToSell)) {
                double totalRevenue = sellPrice * amountToSell;
                vaultEconomy.depositPlayer(player, totalRevenue);
                player.getInventory().removeItem(goldStack);
                adjustPrices(amountToSell, 1);
                player.sendMessage("You sold " + amountToSell + " gold for " + totalRevenue);
            } else {
                player.sendMessage("You don't have enough gold to sell!");
            }
            return true;
        }
        return false;
    }
}

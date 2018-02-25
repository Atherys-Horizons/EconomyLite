/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.github.flibio.economylite.api.CurrencyEconService;
import io.github.flibio.economylite.api.PlayerEconService;
import io.github.flibio.economylite.api.VirtualEconService;
import io.github.flibio.economylite.bstats.Metrics;
import io.github.flibio.economylite.commands.MigrateCommand;
import io.github.flibio.economylite.commands.PayCommand;
import io.github.flibio.economylite.commands.admin.AddCommand;
import io.github.flibio.economylite.commands.admin.EconCommand;
import io.github.flibio.economylite.commands.admin.RemoveCommand;
import io.github.flibio.economylite.commands.admin.SetAllCommand;
import io.github.flibio.economylite.commands.admin.SetCommand;
import io.github.flibio.economylite.commands.balance.BalTopCommand;
import io.github.flibio.economylite.commands.balance.BalanceCommand;
import io.github.flibio.economylite.commands.currency.CurrencyCommand;
import io.github.flibio.economylite.commands.currency.CurrencyCreateCommand;
import io.github.flibio.economylite.commands.currency.CurrencyDeleteCommand;
import io.github.flibio.economylite.commands.currency.CurrencySetCommand;
import io.github.flibio.economylite.commands.virtual.PayVirtualCommand;
import io.github.flibio.economylite.commands.virtual.VirtualAddCommand;
import io.github.flibio.economylite.commands.virtual.VirtualBalanceCommand;
import io.github.flibio.economylite.commands.virtual.VirtualEconCommand;
import io.github.flibio.economylite.commands.virtual.VirtualPayCommand;
import io.github.flibio.economylite.commands.virtual.VirtualRemoveCommand;
import io.github.flibio.economylite.commands.virtual.VirtualSetCommand;
import io.github.flibio.economylite.impl.CurrencyService;
import io.github.flibio.economylite.impl.PlayerDataService;
import io.github.flibio.economylite.impl.VirtualDataService;
import io.github.flibio.economylite.impl.economy.LiteCurrency;
import io.github.flibio.economylite.impl.economy.LiteEconomyService;
import io.github.flibio.economylite.impl.economy.registry.CurrencyRegistryModule;
import io.github.flibio.economylite.modules.Module;
import io.github.flibio.economylite.modules.loan.LoanModule;
import io.github.flibio.economylite.modules.sql.SqlModule;
import io.github.flibio.utils.commands.CommandLoader;
import io.github.flibio.utils.file.FileManager;
import io.github.flibio.utils.message.MessageStorage;
import ninja.leaping.configurate.ConfigurationNode;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Plugin(id = PluginInfo.ID, name = PluginInfo.NAME, version = PluginInfo.VERSION, description = PluginInfo.DESCRIPTION)
public class EconomyLite {

    @Inject @ConfigDir(sharedRoot = false) private Path configDir;

    @Inject @ConfigDir(sharedRoot = true) private Path mainDir;

    @SuppressWarnings("unused") @Inject private Metrics metrics;

    @Inject private Logger logger;

    @Inject private Game game;

    @Inject private PluginContainer container;

    private static FileManager fileManager;
    private static MessageStorage messageStorage;
    private static EconomyLite instance;

    public static LiteEconomyService economyService;
    private static VirtualEconService virtualEconService;
    private static PlayerEconService playerEconService;
    public static CurrencyEconService currencyEconService;

    @Listener
    public void onConstruct(GameConstructionEvent event) {
        System.getProperties().setProperty("h2.bindAddress", "127.0.0.1");
    }

    @Listener
    public void onServerInitialize(GamePreInitializationEvent event) {
        logger.info("EconomyLite " + PluginInfo.VERSION + " is initializing!");
        instance = this;
        // File setup
        if (new File(configDir.toString()).isAbsolute()) {
            fileManager = FileManager.createInstance(this, configDir.toString());
        } else {
            fileManager = FileManager.createInstance(this, "./" + configDir.toString());
        }
        initializeFiles();
        initializeCurrencies();
        // Load Message Storage
        messageStorage = MessageStorage.createInstance(this, configDir.toString());
        initializeMessage();
        // Load modules
        List<Module> postInitModules = new ArrayList<>();
        getModules().forEach(m -> {
            m.initializeConfig();
            if (m.isEnabled()) {
                if (m.initialize(logger, instance)) {
                    logger.info("Loaded the " + m.getName() + " module!");
                    postInitModules.add(m);
                } else {
                    logger.error("Failed to load the " + m.getName() + " module!");
                }
            } else {
                logger.info("The " + m.getName() + " module is disabled!");
            }
        });
        // If the services have not been set, set them to default.
        if (playerEconService == null || virtualEconService == null) {
            playerEconService = new PlayerDataService();
            virtualEconService = new VirtualDataService();
        }
        // Load the economy service
        economyService = new LiteEconomyService();
        // Register the Economy Service
        game.getServiceManager().setProvider(this, EconomyService.class, economyService);
        // Post-initialize modules
        postInitModules.forEach(module -> {
            module.postInitialization(logger, instance);
        });
        // Register commands
        CommandLoader.registerCommands(this, TextSerializers.FORMATTING_CODE.serialize(messageStorage.getMessage("command.invalidsource")),
                new CurrencyCommand(),
                new CurrencySetCommand(),
                new CurrencyDeleteCommand(),
                new CurrencyCreateCommand(),
                new BalanceCommand(),
                new EconCommand(),
                new SetCommand(),
                new SetAllCommand(),
                new RemoveCommand(),
                new AddCommand(),
                new PayCommand(),
                new BalTopCommand(),
                new VirtualBalanceCommand(),
                new VirtualEconCommand(),
                new VirtualAddCommand(),
                new VirtualSetCommand(),
                new VirtualRemoveCommand(),
                new VirtualPayCommand(),
                new PayVirtualCommand(),
                new MigrateCommand()
        );
        // Register currency registry
        game.getRegistry().registerModule(Currency.class, new CurrencyRegistryModule());
    }

    private void initializeFiles() {
        fileManager.setDefault("config.conf", "default-balance", Double.class, 0.0);
        fileManager.setDefault("config.conf", "virt-default-balance", Double.class, 0.0);
        fileManager.setDefault("config.conf", "debug-logging", Boolean.class, true);
        fileManager.setDefault("config.conf", "notify-on-admin-commands", Boolean.class, false);
        fileManager.setDefault("config.conf", "confirm-offline-payments", Boolean.class, false);
    }

    public static boolean isEnabled(String path) {
        if (fileManager.getValue("config.conf", path, Boolean.class).isPresent()) {
            return fileManager.getValue("config.conf", path, Boolean.class).get();
        } else {
            getInstance().getLogger().error("An error has occurred loading config value " + path + "! Defaulting to false.");
            return false;
        }
    }

    private void initializeCurrencies() {
        // Initialize the default currency into file
        fileManager.setDefault("currencies.conf", "current", String.class, "coin");
        fileManager.setDefault("currencies.conf", "coin.singular", String.class, "Coin");
        fileManager.setDefault("currencies.conf", "coin.plural", String.class, "Coins");
        fileManager.setDefault("currencies.conf", "coin.symbol", String.class, "C");
        Currency defaultCurrency =
                new LiteCurrency(fileManager.getValue("currencies.conf", "coin.singular", String.class).get(), fileManager.getValue(
                        "currencies.conf", "coin.plural", String.class).get(), fileManager.getValue("currencies.conf", "coin.symbol", String.class)
                        .get(), true, 2);
        currencyEconService = new CurrencyService(defaultCurrency);
        // Load all of the currencies
        Optional<ConfigurationNode> fOpt = fileManager.getFile("currencies.conf");
        if (fOpt.isPresent()) {
            ConfigurationNode root = fOpt.get();
            for (Object raw : root.getChildrenMap().keySet()) {
                if (raw instanceof String) {
                    String currencyId = (String) raw;
                    Optional<String> sOpt = fileManager.getValue("currencies.conf", currencyId + ".singular", String.class);
                    Optional<String> pOpt = fileManager.getValue("currencies.conf", currencyId + ".plural", String.class);
                    Optional<String> syOpt = fileManager.getValue("currencies.conf", currencyId + ".symbol", String.class);
                    if (sOpt.isPresent() && pOpt.isPresent() && syOpt.isPresent() && !currencyId.equals("coin")) {
                        Currency currency = new LiteCurrency(sOpt.get(), pOpt.get(), syOpt.get(), false, 2);
                        currencyEconService.addCurrency(currency);
                    }
                }
            }
        }
        // Attempt to load the current currency
        Optional<String> cOpt = fileManager.getValue("currencies.conf", "current", String.class);
        if (cOpt.isPresent()) {
            String currentCur = cOpt.get();
            currencyEconService.getCurrencies().forEach(c -> {
                if (("economylite:" + currentCur).equalsIgnoreCase(c.getId())) {
                    // This is the current currency
                    currencyEconService.setCurrentCurrency(c);
                }
            });
        }
        // If the current currency string failed to load set it to default
        if (currencyEconService.getCurrentCurrency().equals(defaultCurrency)) {
            fileManager.setValue("currencies.conf", "current", String.class, "coin");
        }
        logger.info("Using currency: " + currencyEconService.getCurrentCurrency().getName());
    }

    private void initializeMessage() {
        messageStorage.defaultMessages("messages");
    }

    public Logger getLogger() {
        return logger;
    }

    public Game getGame() {
        return game;
    }

    public PluginContainer getPluginContainer() {
        return container;
    }

    public static FileManager getFileManager() {
        return fileManager;
    }

    public static MessageStorage getMessageStorage() {
        return messageStorage;
    }

    public static PlayerEconService getPlayerService() {
        return playerEconService;
    }

    public static VirtualEconService getVirtualService() {
        return virtualEconService;
    }

    public static CurrencyEconService getCurrencyService() {
        return currencyEconService;
    }

    public static EconomyService getEconomyService() {
        return economyService;
    }

    public static EconomyLite getInstance() {
        return instance;
    }

    public static List<Module> getModules() {
        return ImmutableList.of(new SqlModule(), new LoanModule());
    }

    public String getConfigDir() {
        return configDir.toString();
    }

    public String getMainDir() {
        return mainDir.toString();
    }

    // Setters

    public static void setPlayerService(PlayerEconService serv) {
        playerEconService = serv;
    }

    public static void setVirtualService(VirtualEconService serv) {
        virtualEconService = serv;
    }

}

package com.gambitsmp;

import com.gambitsmp.cards.ActiveCardService;
import com.gambitsmp.cards.CardManager;
import com.gambitsmp.dimension.CardDimensionManager;
import com.gambitsmp.dimension.FirstJoinListener;
import com.gambitsmp.dimension.PortalProtectionListener;
import com.gambitsmp.dimension.PortalTravelListener;
import com.gambitsmp.dimension.SpecialMatterListener;
import com.gambitsmp.dimension.SpecialMatterManager;
import com.gambitsmp.gui.CardGuiListener;
import com.gambitsmp.legendary.LegendaryManager;
import com.gambitsmp.listeners.*;
import com.gambitsmp.player.PlayerDataManager;
import com.gambitsmp.player.PlayerDataPersistenceListener;
import com.gambitsmp.shrine.ShrineManager;
import com.gambitsmp.shrine.ShrineListener;
import com.gambitsmp.util.PeriodicTaskManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class GambitSMP extends JavaPlugin {

    private static GambitSMP instance;

    private NamespacedKey cardKey;
    private NamespacedKey legendaryKey;
    private NamespacedKey mysteryDropKey;
    private NamespacedKey specialMatterKey;

    private CardManager cardManager;
    private LegendaryManager legendaryManager;
    private PlayerDataManager playerDataManager;
    private ShrineManager shrineManager;
    private PeriodicTaskManager periodicTaskManager;
    private ActiveCardService activeCardService;
    private CardGuiListener cardGuiListener;
    private CardDimensionManager cardDimensionManager;
    private SpecialMatterManager specialMatterManager;
    private GambitCommand gambitCommand;
    private AbilityCommand abilityCommand;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        cardKey = new NamespacedKey(this, "gambit_card");
        legendaryKey = new NamespacedKey(this, "gambit_legendary");
        mysteryDropKey = new NamespacedKey(this, "gambit_mystery_drop");
        specialMatterKey = new NamespacedKey(this, "gambit_special_matter");

        cardManager = new CardManager(this);
        legendaryManager = new LegendaryManager(this);
        playerDataManager = new PlayerDataManager(this);
        playerDataManager.loadAll();
        shrineManager = new ShrineManager(this);
        periodicTaskManager = new PeriodicTaskManager(this);
        activeCardService = new ActiveCardService(this);
        cardGuiListener = new CardGuiListener(this);
        specialMatterManager = new SpecialMatterManager(this);

        // CardDimension setup happens early: it needs to extract the datapack and
        // create/load the world before anything else references it.
        cardDimensionManager = new CardDimensionManager(this);
        cardDimensionManager.setup();

        getServer().getPluginManager().registerEvents(new CardEquipListener(this), this);
        getServer().getPluginManager().registerEvents(new ActiveCardListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new MovementListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new FusionListener(this), this);
        getServer().getPluginManager().registerEvents(new ShrineListener(this), this);
        getServer().getPluginManager().registerEvents(new MysteryDropListener(this), this);
        getServer().getPluginManager().registerEvents(cardGuiListener, this);
        getServer().getPluginManager().registerEvents(new PortalProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new SpecialMatterListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalTravelListener(this), this);
        getServer().getPluginManager().registerEvents(new FirstJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDataPersistenceListener(this), this);

        gambitCommand = new GambitCommand(this);
        getCommand("gambit").setExecutor(gambitCommand);
        getCommand("gambit").setTabCompleter(gambitCommand);

        abilityCommand = new AbilityCommand(this);
        getCommand("ability").setExecutor(abilityCommand);
        getCommand("ability").setTabCompleter(abilityCommand);

        periodicTaskManager.start();

        // Crash-safety net: onDisable already saves on a clean shutdown, and quitting
        // saves per-player, but a hard crash skips both - autosave everyone every 5
        // minutes so a crash never loses more than a few minutes of card changes.
        getServer().getScheduler().runTaskTimer(this, () -> playerDataManager.saveAll(), 6000L, 6000L);

        getLogger().info("GambitSMP enabled.");
    }

    @Override
    public void onDisable() {
        if (shrineManager != null) shrineManager.save();
        if (playerDataManager != null) playerDataManager.saveAll();
        getLogger().info("GambitSMP disabled.");
    }

    public static GambitSMP get() {
        return instance;
    }

    public NamespacedKey cardKey() {
        return cardKey;
    }

    public NamespacedKey legendaryKey() {
        return legendaryKey;
    }

    public NamespacedKey mysteryDropKey() {
        return mysteryDropKey;
    }

    public NamespacedKey specialMatterKey() {
        return specialMatterKey;
    }

    public CardManager cards() {
        return cardManager;
    }

    public LegendaryManager legendaries() {
        return legendaryManager;
    }

    public PlayerDataManager data() {
        return playerDataManager;
    }

    public ShrineManager shrines() {
        return shrineManager;
    }

    public ActiveCardService activeCards() {
        return activeCardService;
    }

    public CardGuiListener cardGui() {
        return cardGuiListener;
    }

    public CardDimensionManager cardDimension() {
        return cardDimensionManager;
    }

    public SpecialMatterManager specialMatter() {
        return specialMatterManager;
    }
}

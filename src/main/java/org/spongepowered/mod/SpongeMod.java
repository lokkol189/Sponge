/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.mod;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.MetadataCollection;
import net.minecraftforge.fml.common.ModContainerFactory;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import org.spongepowered.api.Game;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.ProviderExistsException;
import org.spongepowered.api.service.command.CommandService;
import org.spongepowered.api.service.event.EventManager;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.persistence.SerializationService;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.command.CommandMapping;
import org.spongepowered.api.world.Dimension;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.common.Sponge;
import org.spongepowered.common.command.MinecraftCommandWrapper;
import org.spongepowered.common.interfaces.IMixinServerCommandManager;
import org.spongepowered.common.service.permission.SpongeContextCalculator;
import org.spongepowered.common.service.permission.SpongePermissionService;
import org.spongepowered.common.service.persistence.SpongeSerializationService;
import org.spongepowered.common.service.scheduler.SyncScheduler;
import org.spongepowered.common.service.sql.SqlServiceImpl;
import org.spongepowered.common.util.SpongeHooks;
import org.spongepowered.common.world.SpongeDimensionType;
import org.spongepowered.mod.command.CommandSponge;
import org.spongepowered.mod.event.SpongeEventHooks;
import org.spongepowered.mod.guice.SpongeGuiceModule;
import org.spongepowered.mod.plugin.SpongeModPluginContainer;
import org.spongepowered.mod.registry.SpongeModGameRegistry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class SpongeMod extends DummyModContainer implements PluginContainer {

    public static final String MODID = "Sponge";
    
    private static final Logger logger = LogManager.getLogger(SpongeMod.MODID);
    public static SpongeMod instance;
    private final Game game;
    private LoadController controller;
    private SpongeModGameRegistry registry;

    // This is a special Mod, provided by the IFMLLoadingPlugin. It will be
    // instantiated before FML scans the system for mods (or plugins)
    public SpongeMod() {
        super(SpongeMod.createMetadata(ImmutableMap.<String, Object>of("name", SpongeMod.MODID, "version", "DEV")));
        // Register our special instance creator with FML
        ModContainerFactory.instance().registerContainerType(Type.getType(Plugin.class), SpongeModPluginContainer.class);

        SpongeMod.instance = this;

        // Initialize Sponge
        Sponge common = Guice.createInjector(new SpongeGuiceModule()).getInstance(Sponge.class);

        this.game = Sponge.getGame();
        this.registry = (SpongeModGameRegistry) this.game.getRegistry();
        common.registerServices();

        this.game.getCommandDispatcher().register(this, CommandSponge.getCommand(this), "sponge", "sp");
    }

    @Override
    public Object getMod() {
        return this;
    }

    public Game getGame() {
        return this.game;
    }

    public EventManager getEventManager() {
        return this.game.getEventManager();
    }

    public Injector getInjector() {
        return Sponge.getInjector();
    }

    public LoadController getController() {
        return this.controller;
    }

    public Logger getLogger() {
        return SpongeMod.logger;
    }

    public File getConfigDir() {
        return Sponge.getConfigDirectory();
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        this.controller = controller;
        return true;
    }

    @Subscribe
    public void onPreInit(FMLPreInitializationEvent e) {
        try {
            MinecraftForge.EVENT_BUS.register(new SpongeEventHooks());

            this.game.getServiceManager().potentiallyProvide(PermissionService.class).executeWhenPresent(new Predicate<PermissionService>() {

                @Override
                public boolean apply(PermissionService input) {
                    input.registerContextCalculator(new SpongeContextCalculator());
                    return true;
                }
            });

            // Add the SyncScheduler as a listener for ServerTickEvents
            FMLCommonHandler.instance().bus().register(this);

            if (e.getSide() == Side.SERVER) {
                SpongeHooks.enableThreadContentionMonitoring();
            }
            this.registry.preInit();
        } catch (Throwable t) {
            this.controller.errorOccurred(this, t);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ((SyncScheduler) SyncScheduler.getInstance()).tick();
        }
    }

    @Subscribe
    public void onInitialization(FMLInitializationEvent e) {
        try {
            this.registry.init();
            if (!this.game.getServiceManager().provide(PermissionService.class).isPresent()) {
                try {
                    SpongePermissionService service = new SpongePermissionService();
                    // Setup default permissions
                    service.getGroupForOpLevel(1).getSubjectData().setPermission(SubjectData.GLOBAL_CONTEXT, "minecraft.selector", Tristate.TRUE);
                    service.getGroupForOpLevel(2).getSubjectData().setPermission(SubjectData.GLOBAL_CONTEXT, "minecraft.commandblock", Tristate.TRUE);
                    this.game.getServiceManager().setProvider(this, PermissionService.class, service);
                } catch (ProviderExistsException e1) {
                    // It's a fallback, ignore
                }
            }
        } catch (Throwable t) {
            this.controller.errorOccurred(this, t);
        }
    }

    @Subscribe
    public void onPostInitialization(FMLPostInitializationEvent e) {
        try {
            this.registry.postInit();
            SerializationService service = this.game.getServiceManager().provide(SerializationService.class).get();
            ((SpongeSerializationService) service).completeRegistration();
        } catch (Throwable t) {
            this.controller.errorOccurred(this, t);
        }
    }

    @Subscribe
    public void onServerAboutToStart(FMLServerAboutToStartEvent e) {
        registerAllEnabledWorlds();
    }

    @Subscribe
    public void onServerStarting(FMLServerStartingEvent e) {
        try {
            // Register vanilla-style commands (if necessary -- not necessary on client)
            ((IMixinServerCommandManager) MinecraftServer.getServer().getCommandManager()).registerEarlyCommands(this.game);
        } catch (Throwable t) {
            this.controller.errorOccurred(this, t);
        }
    }

    @Subscribe
    public void onServerStarted(FMLServerStartedEvent e) {
        try {
            ((IMixinServerCommandManager) MinecraftServer.getServer().getCommandManager()).registerLowPriorityCommands(this.game);
        } catch (Throwable t) {
            this.controller.errorOccurred(this, t);
        }

    }

    @Subscribe
    public void onServerStopped(FMLServerStoppedEvent e) throws IOException {
        try {
            CommandService service = getGame().getCommandDispatcher();
            for (CommandMapping mapping : service.getCommands()) {
                if (mapping.getCallable() instanceof MinecraftCommandWrapper) {
                    service.removeMapping(mapping);
                }
            }
            ((SqlServiceImpl) getGame().getServiceManager().provideUnchecked(SqlService.class)).close();
        } catch (Throwable t) {
            this.controller.errorOccurred(this, t);
        }
    }

    @Override
    public String getId() {
        return getModId();
    }

    @Override
    public Object getInstance() {
        return getMod();
    }

    public SpongeModGameRegistry getSpongeRegistry() {
        return this.registry;
    }

    public void registerAllEnabledWorlds() {
        File[] directoryListing = DimensionManager.getCurrentSaveRootDirectory().listFiles();
        if (directoryListing == null) {
            return;
        }

        for (File child : directoryListing) {
            File levelData = new File(child, "level_sponge.dat");
            if (!child.isDirectory() || !levelData.exists()) {
                continue;
            }

            try {
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(new FileInputStream(levelData));
                if (nbt.hasKey(getModId())) {
                    NBTTagCompound spongeData = nbt.getCompoundTag(getModId());
                    boolean enabled = spongeData.getBoolean("enabled");
                    boolean loadOnStartup = spongeData.getBoolean("loadOnStartup");
                    int dimensionId = spongeData.getInteger("dimensionId");
                    if (!(dimensionId == -1) && !(dimensionId == 0) && !(dimensionId == 1)) {
                        if (!enabled) {
                            getLogger().info("World {} is currently disabled. Skipping world load...", child.getName());
                            continue;
                        }
                        if (!loadOnStartup) {
                            getLogger().info("World {} 'loadOnStartup' is disabled.. Skipping world load...", child.getName());
                            continue;
                        }
                    } else if (dimensionId == -1) {
                        if (!MinecraftServer.getServer().getAllowNether()) {
                            continue;
                        }
                    }
                    if (spongeData.hasKey("uuid_most") && spongeData.hasKey("uuid_least")) {
                        UUID uuid = new UUID(spongeData.getLong("uuid_most"), spongeData.getLong("uuid_least"));
                        this.registry.registerWorldUniqueId(uuid, child.getName());
                    }
                    if (spongeData.hasKey("dimensionId") && spongeData.getBoolean("enabled")) {
                        int dimension = spongeData.getInteger("dimensionId");
                        for (Map.Entry<Class<? extends Dimension>, DimensionType> mapEntry : getSpongeRegistry().dimensionClassMappings
                                .entrySet()) {
                            if (mapEntry.getKey().getCanonicalName().equalsIgnoreCase(spongeData.getString("dimensionType"))) {
                                this.registry.registerWorldDimensionId(dimension, child.getName());
                                if (!DimensionManager.isDimensionRegistered(dimension)) {
                                    DimensionManager.registerDimension(dimension,
                                            ((SpongeDimensionType) mapEntry.getValue()).getDimensionTypeId());
                                }
                            }
                        }
                    } else {
                        getLogger().info("World {} is disabled! Skipping world registration...", child.getName());
                    }
                }
            } catch (Throwable t) {
                getLogger().error("Error during world registration.", t);
            }
        }
    }

    private static ModMetadata createMetadata(Map<String, Object> defaults) {
        try {
            return MetadataCollection.from(SpongeMod.class.getResourceAsStream("/mcmod.info"), SpongeMod.MODID).getMetadataForId(SpongeMod.MODID, defaults);
        } catch (Exception ex) {
            return new ModMetadata();
        }
    }

}

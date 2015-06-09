package org.spongepowered.mod.mixin.core.common.world;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.network.ForgeMessage;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.spongepowered.api.world.Dimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.common.world.SpongeDimensionType;

import java.io.File;

/**
 * Proxies all calls to the dimension manager to Forge's
 */
@Mixin(value = org.spongepowered.common.world.DimensionManager.class, remap = false)
public abstract class MixinDimensionManager {

    @Overwrite
    public static void init() {
        DimensionManager.init();
    }

    @Overwrite
    public static boolean registerProviderType(int id, Class<? extends WorldProvider> provider, boolean keepLoaded) {
        return DimensionManager.registerProviderType(id, provider, keepLoaded);
    }

    @Overwrite
    public static void registerDimension(int id, int providerType) {
        DimensionManager.registerDimension(id, providerType);
    }

    @Overwrite
    public static boolean isDimensionRegistered(int dim) {
        return DimensionManager.isDimensionRegistered(dim);
    }

    @Overwrite
    public static int getProviderType(int dim) {
        return DimensionManager.getProviderType(dim);
    }

    @Overwrite
    public static Integer[] getIDs(boolean check) {
        return DimensionManager.getIDs(check);
    }

    @Overwrite
    public static Integer[] getIDs() {
        return DimensionManager.getIDs();
    }

    @Overwrite
    public static void setWorld(int id, WorldServer world) {
        DimensionManager.setWorld(id, world);
    }

    @Overwrite
    public static void initDimension(int dim) {
        DimensionManager.initDimension(dim);
    }

    @Overwrite
    public static WorldServer getWorldFromDimId(int id) {
        return DimensionManager.getWorld(id);
    }

    @Overwrite
    public static WorldServer[] getWorlds() {
        return DimensionManager.getWorlds();
    }

    @Overwrite
    public static boolean shouldLoadSpawn(int dim) {
        return DimensionManager.shouldLoadSpawn(dim);
    }

    @Overwrite
    public static Integer[] getStaticDimensionIDs() {
        return DimensionManager.getStaticDimensionIDs();
    }

    @Overwrite
    public static WorldProvider createProviderFor(int dim) {
        return DimensionManager.createProviderFor(dim);
    }

    @Overwrite
    public static void unloadWorldFromDimId(int id) {
        DimensionManager.unloadWorld(id);
    }

    @Overwrite
    public static int getNextFreeDimId() {
        return DimensionManager.getNextFreeDimId();
    }

    @Overwrite
    public static NBTTagCompound saveDimensionDataMap() {
        return DimensionManager.saveDimensionDataMap();
    }

    @Overwrite
    public static void loadDimensionDataMap(NBTTagCompound compoundTag) {
        DimensionManager.loadDimensionDataMap(compoundTag);
    }

    @Overwrite
    public static File getCurrentSaveRootDirectory() {
        return DimensionManager.getCurrentSaveRootDirectory();
    }

    // TODO World changes. Remove this when common handles this
    @Overwrite
    public static void sendDimensionRegistration(WorldServer worldserver, EntityPlayerMP playerIn, int dim) {
        // register dimension on client-side
        FMLEmbeddedChannel serverChannel = NetworkRegistry.INSTANCE.getChannel("FORGE", Side.SERVER);
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(playerIn);
        serverChannel.writeOutbound(new ForgeMessage.DimensionRegisterMessage(dim,
                ((SpongeDimensionType) ((Dimension) worldserver.provider).getType()).getDimensionTypeId()));
    }
}

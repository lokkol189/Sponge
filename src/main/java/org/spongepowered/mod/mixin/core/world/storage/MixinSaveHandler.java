package org.spongepowered.mod.mixin.core.world.storage;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SaveHandler.class)
public abstract class MixinSaveHandler {
    private void loadDimensionAndOtherData(SaveHandler handler, WorldInfo info, NBTTagCompound compound) {
        //NOOP cause Forge does this in the SaveHandler
    }
}

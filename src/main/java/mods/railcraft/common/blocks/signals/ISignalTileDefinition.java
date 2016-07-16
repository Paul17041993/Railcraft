/*
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.blocks.signals;

import net.minecraft.block.Block;

import javax.annotation.Nullable;

/**
 * @author CovertJaguar <http://www.railcraft.info/>
 */
public interface ISignalTileDefinition {

    String getTag();

    float getHardness();

    boolean needsSupport();

    boolean isEnabled();

    @Nullable
    Block getBlock();

    int getMeta();

    Class<? extends TileSignalFoundation> getTileClass();

}

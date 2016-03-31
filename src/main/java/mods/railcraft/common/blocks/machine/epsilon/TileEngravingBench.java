/* 
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.blocks.machine.epsilon;

import buildcraft.api.statements.IActionExternal;
import cofh.api.energy.EnergyStorage;
import cofh.api.energy.IEnergyReceiver;

import mods.railcraft.common.blocks.machine.TileMachineItem;
import mods.railcraft.common.core.RailcraftConfig;
import mods.railcraft.common.emblems.EmblemToolsServer;
import mods.railcraft.common.gui.EnumGui;
import mods.railcraft.common.gui.GuiHandler;
import mods.railcraft.common.plugins.buildcraft.actions.Actions;
import mods.railcraft.common.plugins.buildcraft.triggers.IHasWork;
import mods.railcraft.common.plugins.forge.OreDictPlugin;
import mods.railcraft.common.util.inventory.InvTools;
import mods.railcraft.common.util.inventory.wrappers.InventoryMapper;
import mods.railcraft.common.util.misc.Game;
import mods.railcraft.common.util.network.IGuiReturnHandler;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class TileEngravingBench extends TileMachineItem<EnumMachineEpsilon> implements IEnergyReceiver, ISidedInventory, IHasWork, IGuiReturnHandler {

    public enum GuiPacketType {

        START_CRAFTING, NORMAL_RETURN, OPEN_UNLOCK, OPEN_NORMAL, UNLOCK_EMBLEM
    }

    private final static int PROCESS_TIME = 100;
    private final static int ACTIVATION_POWER = 50;
    private final static int MAX_RECEIVE = 1000;
    private final static int MAX_ENERGY = ACTIVATION_POWER * (PROCESS_TIME + (PROCESS_TIME / 2));
    private final static int SLOT_INPUT = 0;
    private final static int SLOT_RESULT = 1;
    private static final int[] SLOTS = InvTools.buildSlotArray(0, 2);
    private final IInventory invResult = new InventoryMapper(this, SLOT_RESULT, 1, false);
    private EnergyStorage energyStorage;
    private int progress;
    public boolean paused, startCrafting, isCrafting, flippedAxis;
    public String currentEmblem = "";
    private final Set<IActionExternal> actions = new HashSet<IActionExternal>();

    public TileEngravingBench() {
        super(2);
        if (RailcraftConfig.machinesRequirePower())
            energyStorage = new EnergyStorage(MAX_ENERGY, MAX_RECEIVE);
    }

    @Override
    public EnumMachineEpsilon getMachineType() {
        return EnumMachineEpsilon.ENGRAVING_BENCH;
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);

        data.setBoolean("flippedAxis", flippedAxis);
        data.setBoolean("isCrafting", isCrafting);
        data.setInteger("progress", progress);
        data.setString("currentEmblem", currentEmblem);

        if (energyStorage != null)
            energyStorage.writeToNBT(data);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);

        flippedAxis = data.getBoolean("flippedAxis");
        isCrafting = data.getBoolean("isCrafting");
        progress = data.getInteger("progress");
        currentEmblem = data.getString("currentEmblem");

        if (energyStorage != null)
            energyStorage.readFromNBT(data);
    }

    @Override
    public void writePacketData(DataOutputStream data) throws IOException {
        super.writePacketData(data);
        data.writeBoolean(flippedAxis);
    }

    @Override
    public void readPacketData(DataInputStream data) throws IOException {
        super.readPacketData(data);
        flippedAxis = data.readBoolean();
        markBlockForUpdate();
    }

    @Override
    public void writeGuiData(DataOutputStream data) throws IOException {
    }

    @Override
    public void readGuiData(DataInputStream data, EntityPlayer sender) throws IOException {
        GuiPacketType type = GuiPacketType.values()[data.readByte()];
        switch (type) {
            case START_CRAFTING:
                startCrafting = true;
            case NORMAL_RETURN:
                currentEmblem = data.readUTF();
                break;
            case OPEN_UNLOCK:
                GuiHandler.openGui(EnumGui.ENGRAVING_BENCH_UNLOCK, sender, worldObj, getPos());
                break;
            case OPEN_NORMAL:
                GuiHandler.openGui(EnumGui.ENGRAVING_BENCH, sender, worldObj, getPos());
                break;
            case UNLOCK_EMBLEM:
                if (EmblemToolsServer.manager != null) {
                    int windowId = data.readByte();
                    String code = data.readUTF();
                    EmblemToolsServer.manager.unlockEmblem((EntityPlayerMP) sender, code, windowId);
                }
                break;
        }
    }

    @Override
    public boolean openGui(EntityPlayer player) {
        BlockPos offsetPos = getPos().add(0.5, 0.5, 0.5);
        if (player.getDistanceSq(offsetPos) > 64D)
            return false;
        GuiHandler.openGui(EnumGui.ENGRAVING_BENCH, player, worldObj, getPos());
        return true;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    public int getProgressScaled(int i) {
        return (progress * i) / PROCESS_TIME;
    }

    @Override
    public void update() {
        super.update();

        if (Game.isNotHost(worldObj))
            return;

        if (clock % 16 == 0)
            processActions();

        if (paused)
            return;

        if (startCrafting) {
            startCrafting = false;
            isCrafting = true;
        }

        if (getStackInSlot(SLOT_RESULT) != null)
            isCrafting = false;

        if (!isCrafting) {
            progress = 0;
            return;
        }

        ItemStack emblem = makeEmblem();
        if (emblem == null)
            return;

        if (!isItemValidForSlot(SLOT_INPUT, getStackInSlot(SLOT_INPUT))) {
            progress = 0;
            return;
        }

        if (progress >= PROCESS_TIME) {
            isCrafting = false;
            if (InvTools.isRoomForStack(emblem, invResult)) {
                decrStackSize(SLOT_INPUT, 1);
                InvTools.moveItemStack(emblem, invResult);
                progress = 0;
            }
        } else if (energyStorage != null) {
            int energy = energyStorage.extractEnergy(ACTIVATION_POWER, true);
            if (energy >= ACTIVATION_POWER) {
                progress++;
                energyStorage.extractEnergy(ACTIVATION_POWER, false);
            }
        } else
            progress++;
    }

    private ItemStack makeEmblem() {
        if (currentEmblem == null || currentEmblem.isEmpty() || EmblemToolsServer.manager == null)
            return null;
        return EmblemToolsServer.manager.getEmblemItemStack(currentEmblem);
    }

    @Override
    public boolean hasWork() {
        return isCrafting;
    }

    public void setPaused(boolean p) {
        paused = p;
    }

    private void processActions() {
        paused = false;
        for (IActionExternal action : actions) {
            if (action == Actions.PAUSE)
                paused = true;
        }
        actions.clear();
    }

    @Override
    public void actionActivated(IActionExternal action) {
        actions.add(action);
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return SLOTS;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        return isItemValidForSlot(index, itemStackIn);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return index == SLOT_RESULT;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_RESULT)
            return false;
        if (stack == null)
            return false;
        if (stack.stackSize <= 0)
            return false;
        if (OreDictPlugin.isOreType("ingotSteel", stack))
            return true;
        if (OreDictPlugin.isOreType("ingotBronze", stack))
            return true;
        return Items.gold_ingot == stack.getItem();
    }

    @Override
    public void onBlockPlacedBy(IBlockState state, EntityLivingBase entityliving, ItemStack stack) {
        super.onBlockPlacedBy(state, entityliving, stack);
        EnumFacing facing = entityliving.getHorizontalFacing();
        if (facing == EnumFacing.EAST || facing == EnumFacing.WEST)
            flippedAxis = true;
    }

    @Override
    public boolean rotateBlock(EnumFacing axis) {
        flippedAxis = !flippedAxis;
        sendUpdateToClient();
        return true;
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    @Override
    public boolean canConnectEnergy(EnumFacing side) {
        return energyStorage != null;
    }

    @Override
    public int receiveEnergy(EnumFacing from, int maxReceive, boolean simulate) {
        if (energyStorage == null)
            return 0;
        return energyStorage.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int getEnergyStored(EnumFacing from) {
        if (energyStorage == null)
            return 0;
        return energyStorage.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored(EnumFacing from) {
        if (energyStorage == null)
            return 0;
        return energyStorage.getMaxEnergyStored();
    }
}

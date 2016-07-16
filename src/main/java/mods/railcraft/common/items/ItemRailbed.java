/* 
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.items;

import mods.railcraft.common.core.IRailcraftObjectContainer;
import mods.railcraft.common.core.IVariantEnum;
import mods.railcraft.common.items.ItemTie.EnumTie;
import mods.railcraft.common.plugins.forge.CraftingPlugin;
import mods.railcraft.common.plugins.forge.RailcraftRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

public class ItemRailbed extends ItemRailcraft {

    public ItemRailbed() {
        setHasSubtypes(true);
        setMaxDamage(0);
        setUnlocalizedName("railcraft.part.railbed");
    }

    @Nullable
    @Override
    public Class<? extends IVariantEnum> getVariantEnum() {
        return EnumRailbed.class;
    }

    @Override
    public void initializeDefinintion() {
        for (EnumRailbed railbed : EnumRailbed.VALUES) {
            RailcraftRegistry.register(new ItemStack(this, 1, railbed.ordinal()));
        }
    }

    @Override
    public void getSubItems(Item id, CreativeTabs tab, List<ItemStack> list) {
        for (int i = 0; i < 2; i++) {
            list.add(new ItemStack(this, 1, i));
        }
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        switch (stack.getItemDamage()) {
            case 1:
                return "item.railcraft.part.railbed.stone";
            default:
                return "item.railcraft.part.railbed.wood";
        }
    }

    @Override
    public void defineRecipes() {
        RailcraftItems item = RailcraftItems.railbed;

        Object tieWood = RailcraftItems.tie.getRecipeObject(EnumTie.WOOD);
        CraftingPlugin.addShapelessRecipe(item.getStack(1, EnumRailbed.WOOD),
                tieWood, tieWood, tieWood, tieWood);

        Object tieStone = RailcraftItems.tie.getRecipeObject(EnumTie.STONE);
        CraftingPlugin.addShapelessRecipe(item.getStack(1, EnumRailbed.STONE),
                tieStone, tieStone, tieStone, tieStone);
    }

    public enum EnumRailbed implements IVariantEnum {
        WOOD("stickWood"),
        STONE(Blocks.STONE_SLAB);
        public static final EnumRailbed[] VALUES = values();
        private Object alternate;

        EnumRailbed(Object alt) {
            this.alternate = alt;
        }

        @Override
        public Object getAlternate(IRailcraftObjectContainer container) {
            return alternate;
        }

        @Override
        public String getName() {
            return name().toLowerCase(Locale.ENGLISH);
        }
    }

}

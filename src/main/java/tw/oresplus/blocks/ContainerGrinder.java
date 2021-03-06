package tw.oresplus.blocks;

import tw.oresplus.api.Ores;
import tw.oresplus.core.FuelHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotFurnace;
import net.minecraft.item.ItemStack;

public class ContainerGrinder extends ContainerMachine {
	public ContainerGrinder(InventoryPlayer inventory, TileEntityGrinder te) {
		this.tileEntity = te;
		this.addSlotToContainer(new Slot(te, te.startingSourceSlot, 50, 20));
		this.addSlotToContainer(new Slot(te, te.furnaceSlot, 79, 64));
		this.addSlotToContainer(new Slot(te, te.interfaceSlot, 150, 10));
		this.addSlotToContainer(new SlotFurnace(inventory.player, te, te.startingOutputSlot, 115, 21));
		this.addPlayerInventory(inventory);
	}

	@Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotNum) {
		TileEntityGrinder te = (TileEntityGrinder)this.tileEntity;
		ItemStack itemStack = null;
		Slot slot = (Slot)this.inventorySlots.get(slotNum);
		
		if (slot != null && slot.getHasStack()) {
			ItemStack slotStack = slot.getStack();
			itemStack = slotStack.copy();
			
			if (slotNum == te.startingOutputSlot) {
            	if (!this.mergeItemStack(slotStack, 3, 39, false))
            		return null;
            	slot.onSlotChange(slotStack, itemStack);
			}
			else if (slotNum >= 3) {
				if (FuelHelper.isItemFuel(slotStack)) {
					if (!this.mergeItemStack(slotStack, te.furnaceSlot, te.furnaceSlot + 1, false))
						return null;
				}
				else if (Ores.grinderRecipes.getResult(slotStack) != null) {
					if (!this.mergeItemStack(slotStack, te.startingSourceSlot, te.startingSourceSlot + 1, false))
						return null;
				}
				else if (slotNum >= 3 && slotNum < 30) {
					if (!this.mergeItemStack(slotStack, 30, 39, false))
						return null;
				}
				else if (slotNum >= 31 && slotNum < 39) {
					if (!this.mergeItemStack(slotStack, 3, 30, false))
						return null;
				}
			}
			else if (!this.mergeItemStack(slotStack, 3, 39, false)) {
				return null;
			}
			
            if (slotStack.stackSize == 0) {
                slot.putStack((ItemStack)null);
            }
            else {
                slot.onSlotChanged();
            }
            if (slotStack.stackSize == itemStack.stackSize) 
            	return null;
            slot.onPickupFromSlot(player, slotStack);
		}
		
		return itemStack;
	}

}

package com.example.examplemod.gui;

import com.example.examplemod.CheapBlocksTraderMod;
import com.example.examplemod.util.CartographerOffersStorage;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.IntReferenceHolder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CartographerContainer extends Container {
    private final VillagerEntity villager;
    private final PlayerEntity player;

    // 54 слота для товаров (6 рядов по 9)
    public final ItemStackHandler offerSlots = new ItemStackHandler(54) {
        @Override
        protected void onContentsChanged(int slot) {
            // Сохранение происходит в removed() и refreshOffers(), здесь не нужно
        }
    };

    // Счётчик палок у игрока
    private final IntReferenceHolder stickCountHolder = new IntReferenceHolder() {
        @Override
        public int get() {
            return countSticksInInventory(player.inventory);
        }

        @Override
        public void set(int value) {
            // Не требуется
        }
    };

    public CartographerContainer(int windowId, PlayerInventory playerInventory, VillagerEntity villager) {
        super(CheapBlocksTraderMod.CARTOGRAPHER_CONTAINER.get(), windowId);
        this.villager = villager;
        this.player = playerInventory.player;

        // Слоты инвентаря игрока (сдвинуты вниз)
        int playerInvY = 140;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, playerInvY + 58));
        }

        // 54 слота для товаров (6 рядов по 9)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                final int index = row * 9 + col;
                this.addSlot(new SlotItemHandler(offerSlots, index, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(@Nonnull ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(@Nonnull PlayerEntity playerIn) {
                        return false;
                    }
                });
            }
        }

        this.addDataSlot(stickCountHolder);

        // Загрузка сохранённых предложений (или генерация новых)
        if (!player.level.isClientSide && villager != null) {
            // ИСПРАВЛЕНО: передаём ожидаемое количество слотов (54)
            List<ItemStack> savedOffers = CartographerOffersStorage.getOffers(villager, 54);
            for (int i = 0; i < savedOffers.size() && i < 54; i++) {
                offerSlots.setStackInSlot(i, savedOffers.get(i));
            }
            this.broadcastChanges();
        }
    }

    public CartographerContainer(int windowId, PlayerInventory playerInventory) {
        this(windowId, playerInventory, null);
    }

    public void refreshOffers() {
        if (player.level.isClientSide || villager == null) return;

        List<ItemStack> newOffers = CartographerOffersStorage.generateRandomOffers(player.getRandom(), 54);
        for (int i = 0; i < newOffers.size(); i++) {
            offerSlots.setStackInSlot(i, newOffers.get(i));
        }
        CartographerOffersStorage.saveOffers(villager, newOffers);
        this.broadcastChanges();
    }

    public void purchaseItem(int slotIndex) {
        if (player.level.isClientSide) return;
        if (slotIndex < 0 || slotIndex >= 54) return;

        if (!removeStickFromInventory()) {
            player.sendMessage(new StringTextComponent("У вас нет палок!"), player.getUUID());
            return;
        }

        ItemStack boughtStack = offerSlots.getStackInSlot(slotIndex).copy();
        if (boughtStack.isEmpty()) return;

        if (!player.inventory.add(boughtStack)) {
            player.drop(boughtStack, false);
        }

        stickCountHolder.set(countSticksInInventory(player.inventory));
    }

    private boolean removeStickFromInventory() {
        PlayerInventory inv = player.inventory;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == Items.STICK) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inv.setItem(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    private int countSticksInInventory(PlayerInventory inv) {
        int count = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == Items.STICK) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public int getStickCount() {
        return stickCountHolder.get();
    }

    @Override
    public boolean stillValid(@Nonnull PlayerEntity player) {
        return villager != null && villager.isAlive();
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull PlayerEntity player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 36) {
                if (!this.moveItemStackTo(itemstack1, 36, 90, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(itemstack1, 0, 36, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public void removed(@Nonnull PlayerEntity player) {
        super.removed(player);
        if (!player.level.isClientSide && villager != null) {
            List<ItemStack> currentOffers = new ArrayList<>();
            for (int i = 0; i < 54; i++) {
                currentOffers.add(offerSlots.getStackInSlot(i).copy());
            }
            CartographerOffersStorage.saveOffers(villager, currentOffers);
        }
    }

    public static class CartographerMenuProvider implements INamedContainerProvider {
        private final VillagerEntity villager;

        public CartographerMenuProvider(VillagerEntity villager) {
            this.villager = villager;
        }

        @Nonnull
        @Override
        public ITextComponent getDisplayName() {
            return new StringTextComponent("Торговля с Картографом");
        }

        @Nullable
        @Override
        public Container createMenu(int windowId, @Nonnull PlayerInventory playerInventory, @Nonnull PlayerEntity player) {
            return new CartographerContainer(windowId, playerInventory, villager);
        }
    }
}
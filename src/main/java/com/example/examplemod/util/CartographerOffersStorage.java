package com.example.examplemod.util;

import com.example.examplemod.CheapBlocksTraderMod;
import net.minecraft.block.Block;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CartographerOffersStorage {
    private static final String OFFERS_KEY = CheapBlocksTraderMod.MOD_ID + ":offers";

    // Кеш всех доступных блоков (заполняется лениво)
    private static List<Block> cachedAllBlocks = null;

    private static List<Block> getAllBlocks() {
        if (cachedAllBlocks == null) {
            cachedAllBlocks = new ArrayList<>();
            for (Block block : ForgeRegistries.BLOCKS.getValues()) {
                if (block.asItem() != Items.AIR) {
                    cachedAllBlocks.add(block);
                }
            }
        }
        return cachedAllBlocks;
    }

    /**
     * Загружает сохранённые предложения из NBT жителя.
     * Если данных нет или их меньше expectedCount, дополняет случайными и сохраняет.
     *
     * @param villager      житель-картограф
     * @param expectedCount ожидаемое количество слотов (например, 27 или 54)
     */
    public static List<ItemStack> getOffers(VillagerEntity villager, int expectedCount) {
        CompoundNBT persistentData = villager.getPersistentData();
        List<ItemStack> offers = new ArrayList<>();

        if (persistentData.contains(OFFERS_KEY, Constants.NBT.TAG_LIST)) {
            ListNBT list = persistentData.getList(OFFERS_KEY, Constants.NBT.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String blockId = list.getString(i);
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockId));
                if (block != null && block.asItem() != Items.AIR) {
                    offers.add(new ItemStack(block, 64));
                } else {
                    offers.add(new ItemStack(Items.DIRT, 64));
                }
            }
        }

        // Если список меньше ожидаемого, дополняем случайными блоками
        if (offers.size() < expectedCount) {
            Random random = villager.getRandom();
            if (random == null) random = new Random();
            List<ItemStack> extra = generateRandomOffers(random, expectedCount - offers.size());
            offers.addAll(extra);
            saveOffers(villager, offers);
        }

        return offers;
    }

    /**
     * Сохраняет список предложений в NBT жителя.
     */
    public static void saveOffers(VillagerEntity villager, List<ItemStack> offers) {
        CompoundNBT persistentData = villager.getPersistentData();
        ListNBT list = new ListNBT();
        for (ItemStack stack : offers) {
            ResourceLocation id = stack.getItem().getRegistryName();
            if (id != null) {
                list.add(StringNBT.valueOf(id.toString()));
            }
        }
        persistentData.put(OFFERS_KEY, list);
    }

    /**
     * Генерирует count случайных блоков (стаками по 64).
     */
    public static List<ItemStack> generateRandomOffers(Random random, int count) {
        List<ItemStack> offers = new ArrayList<>();
        List<Block> allBlocks = getAllBlocks();
        if (allBlocks.isEmpty()) {
            // На всякий случай, если реестр пуст – вернём землю
            for (int i = 0; i < count; i++) {
                offers.add(new ItemStack(Items.DIRT, 64));
            }
            return offers;
        }

        for (int i = 0; i < count; i++) {
            Block randomBlock = allBlocks.get(random.nextInt(allBlocks.size()));
            offers.add(new ItemStack(randomBlock, 64));
        }
        return offers;
    }

    // Устаревший метод для обратной совместимости (вызывает новый с 27)
    @Deprecated
    public static List<ItemStack> generateRandomOffers(Random random) {
        return generateRandomOffers(random, 27);
    }
}
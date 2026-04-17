package com.example.examplemod;

import com.example.examplemod.gui.CartographerContainer;
import com.example.examplemod.gui.CartographerScreen;
import com.example.examplemod.network.PacketBuyItem;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CheapBlocksTraderMod.MOD_ID)
public class CheapBlocksTraderMod {
    public static final String MOD_ID = "cheap_blocks_trader";
    private static final Logger LOGGER = LogManager.getLogger();

    private static final DeferredRegister<ContainerType<?>> CONTAINERS =
            DeferredRegister.create(ForgeRegistries.CONTAINERS, MOD_ID);

    public static final RegistryObject<ContainerType<CartographerContainer>> CARTOGRAPHER_CONTAINER =
            CONTAINERS.register("cartographer_container",
                    () -> IForgeContainerType.create((windowId, inv, data) -> new CartographerContainer(windowId, inv)));

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public CheapBlocksTraderMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        CONTAINERS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        NETWORK.registerMessage(0, PacketBuyItem.class,
                PacketBuyItem::encode,
                PacketBuyItem::decode,
                PacketBuyItem::handle);
        LOGGER.info("Сетевой канал зарегистрирован.");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        // Безопасно выполняем клиентский код только на физическом клиенте
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ScreenManager.register(CARTOGRAPHER_CONTAINER.get(), CartographerScreen::new);
            LOGGER.info("Экран CartographerScreen привязан к контейнеру.");
        });
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof VillagerEntity) {
            VillagerEntity villager = (VillagerEntity) event.getTarget();
            if (villager.getVillagerData().getProfession() == VillagerProfession.CARTOGRAPHER) {
                event.setCanceled(true);
                PlayerEntity player = event.getPlayer();
                if (!player.level.isClientSide) {
                    ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                    serverPlayer.openMenu(new CartographerContainer.CartographerMenuProvider(villager));
                }
            }
        }
    }
}
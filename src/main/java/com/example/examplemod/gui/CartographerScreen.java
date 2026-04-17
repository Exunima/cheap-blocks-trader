package com.example.examplemod.gui;

import com.example.examplemod.CheapBlocksTraderMod;
import com.example.examplemod.network.PacketBuyItem;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class CartographerScreen extends ContainerScreen<CartographerContainer> {
    // Используем ванильную текстуру двойного сундука (поддерживает 54 слота)
    private static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation("textures/gui/container/generic_54.png");

    public CartographerScreen(CartographerContainer container, PlayerInventory playerInventory, ITextComponent title) {
        super(container, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256; // Достаточно для 6 рядов товаров + инвентарь
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // Кнопка "Обновить" слева от интерфейса
        this.addButton(new Button(this.leftPos - 50, this.topPos + 20, 40, 20,
                new StringTextComponent("⟳"), button -> {
            CheapBlocksTraderMod.NETWORK.sendToServer(new PacketBuyItem(-1));
        }));
    }

    @Override
    protected void renderBg(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bind(GUI_TEXTURE);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);

        // Счётчик палок справа от интерфейса
        int stickCount = this.menu.getStickCount();
        String stickText = "Палок: " + stickCount;
        int textX = this.leftPos + this.imageWidth + 10;
        int textY = this.topPos + 20;
        this.font.draw(matrixStack, stickText, textX, textY, 0xFFFFFF); // Белый цвет для читаемости
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        this.font.draw(matrixStack, this.title, (float)this.titleLabelX, (float)this.titleLabelY, 0x404040);
        // Надпись "Инвентарь" не рисуется
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Слоты товаров: теперь индексы 36..89 (36 + 54 = 90)
        if (this.hoveredSlot != null && this.hoveredSlot.index >= 36 && this.hoveredSlot.index < 90) {
            int offerIndex = this.hoveredSlot.index - 36;
            CheapBlocksTraderMod.NETWORK.sendToServer(new PacketBuyItem(offerIndex));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
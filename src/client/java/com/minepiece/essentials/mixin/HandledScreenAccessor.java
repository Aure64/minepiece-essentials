package com.minepiece.essentials.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Expose le coin haut-gauche du fond ({@code x}/{@code y}) de tout HandledScreen. */
@Mixin(AbstractContainerScreen.class)
public interface HandledScreenAccessor {
    @Accessor("leftPos") int minepiece$getBgX();
    @Accessor("topPos") int minepiece$getBgY();
}

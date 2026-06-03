package com.minepiece.essentials.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Expose le coin haut-gauche du fond ({@code x}/{@code y}) de tout HandledScreen. */
@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("x") int minepiece$getBgX();
    @Accessor("y") int minepiece$getBgY();
}

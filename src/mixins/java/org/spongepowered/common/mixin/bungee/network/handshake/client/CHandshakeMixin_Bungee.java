/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.bungee.network.handshake.client;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.handshake.client.CHandshakePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.applaunch.config.core.SpongeConfigs;

@Mixin(CHandshakePacket.class)
public abstract class CHandshakeMixin_Bungee {

    @Redirect(method = "read",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketBuffer;readUtf(I)Ljava/lang/String;"))
    private String bungee$patchReadStringForPortForwarding(final PacketBuffer buf, final int value) {
        if (!SpongeConfigs.getCommon().get().getModules().usePluginBungeeCord()
                || !SpongeConfigs.getCommon().get().getBungeeCord().getIpForwarding()) {
            return buf.readUtf(255);
        }
        return buf.readUtf(Short.MAX_VALUE);
    }
}

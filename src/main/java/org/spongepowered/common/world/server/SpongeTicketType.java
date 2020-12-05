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
package org.spongepowered.common.world.server;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.TicketType;
import org.spongepowered.common.util.SpongeTicks;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3i;

import java.util.function.BiFunction;

public final class SpongeTicketType<S, T> implements TicketType<T> {

    private final net.minecraft.server.level.TicketType<S> wrappedType;
    private final BiFunction<T, ServerWorld, S> spongeToNative;
    private final BiFunction<S, ServerWorld, T> nativeToSponge;
    private final Ticks lifetime;

    public static SpongeTicketType<ChunkPos, Vector3i> createForChunkPos(final net.minecraft.server.level.TicketType<ChunkPos> ticketType) {
        return new SpongeTicketType<>(ticketType, (in, world) -> VecHelper.toChunkPos(in), (in, world) -> VecHelper.toVector3i(in));
    }

    public static SpongeTicketType<BlockPos, Vector3i> createForBlockPos(final net.minecraft.server.level.TicketType<BlockPos> ticketType) {
        return new SpongeTicketType<>(ticketType, (in, world) -> VecHelper.toBlockPos(in), (in, world) -> VecHelper.toVector3i(in));
    }

    @SuppressWarnings("unchecked")
    public static <S, T> SpongeTicketType<S, T> createForCastedType(final net.minecraft.server.level.TicketType<S> ticketType) {
        return new SpongeTicketType<>(ticketType, (tIn, world) -> (S) tIn, (sIn, world) -> (T) sIn);
    }

    public SpongeTicketType(final net.minecraft.server.level.TicketType<S> wrappedType, final BiFunction<T, ServerWorld, S> spongeToNative,
            final BiFunction<S, ServerWorld, T> nativeToSponge) {
        this.wrappedType = wrappedType;
        this.spongeToNative = spongeToNative;
        this.nativeToSponge = nativeToSponge;
        this.lifetime = new SpongeTicks(this.wrappedType.timeout());
    }

    public net.minecraft.server.level.TicketType<S> getWrappedType() {
        return this.wrappedType;
    }

    public S getNativeType(final T spongeType, final ServerWorld world) {
        return this.spongeToNative.apply(spongeType, world);
    }

    public T getSpongeType(final S nativeType, final ServerWorld world) {
        return this.nativeToSponge.apply(nativeType, world);
    }

    @Override
    public Ticks lifetime() {
        return this.lifetime;
    }

}

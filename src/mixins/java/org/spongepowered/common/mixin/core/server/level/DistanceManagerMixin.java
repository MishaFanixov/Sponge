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
package org.spongepowered.common.mixin.core.server.level;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.Ticket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.accessor.server.level.TicketAccessor;
import org.spongepowered.common.bridge.world.DistanceManagerBridge;
import org.spongepowered.common.bridge.world.server.TicketBridge;
import org.spongepowered.common.bridge.world.server.TicketTypeBridge;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.SpongeTicks;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.math.vector.Vector3i;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin implements DistanceManagerBridge {

    // @formatter:off
    @Shadow private void shadow$addTicket(final long chunkpos, final net.minecraft.server.level.Ticket<?> ticket) { }
    @Shadow protected abstract void shadow$removeTicket(long chunkPosIn, net.minecraft.server.level.Ticket<?> ticketIn);
    @Shadow @Final private Long2ObjectOpenHashMap<SortedArraySet<net.minecraft.server.level.Ticket<?>>> tickets;
    @Shadow private long ticketTickCounter;
    // @formatter:on

    @Override
    @SuppressWarnings({ "unchecked" })
    public boolean bridge$checkTicketValid(final Ticket<?> ticket) {
        // is the ticket in our manager?
        final net.minecraft.server.level.Ticket<?> nativeTicket = ((net.minecraft.server.level.Ticket<?>) (Object) ticket);
        if (nativeTicket.getType() == TicketType.FORCED) {
            final TicketAccessor<ChunkPos> ticketAccessor = ((TicketAccessor<ChunkPos>) ticket);
            final ChunkPos chunkPos = ticketAccessor.accessor$key();
            if (this.tickets.computeIfAbsent(chunkPos.toLong(), x -> SortedArraySet.create(4)).contains(nativeTicket)) {
                // check to see if it's expired.
                return ticketAccessor.invoker$timedOut(this.ticketTickCounter);
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Ticks bridge$getTimeLeft(final Ticket<?> ticket) {
        if (this.bridge$checkTicketValid(ticket)) {
            return new SpongeTicks(((TicketAccessor<ChunkPos>) ticket).accessor$createdTick() - this.ticketTickCounter);
        }
        return Ticks.zero();
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public boolean bridge$renewTicket(final Ticket<?> ticket) {
        if (this.bridge$checkTicketValid(ticket)) {
            final net.minecraft.server.level.Ticket<?> nativeTicket = (net.minecraft.server.level.Ticket<?>) (Object) ticket;
            ((TicketAccessor<ChunkPos>) ticket).invoker$setCreatedTick(this.ticketTickCounter + nativeTicket.getType().timeout());
            return true;
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S, T> Optional<Ticket<T>> bridge$registerTicket(
            final ServerWorld world, final org.spongepowered.api.world.server.TicketType<T> ticketType, final Vector3i pos, final T value, final int distanceLimit) {
        final TicketType<S> type = (net.minecraft.server.level.TicketType<S>) ticketType;
        final net.minecraft.server.level.Ticket<?> ticketToRequest =
                TicketAccessor.accessor$createInstance(
                        type,
                        Constants.ChunkTicket.MAXIMUM_CHUNK_TICKET_LEVEL - distanceLimit,
                        ((TicketTypeBridge<S, T>) ticketType).bridge$convertToNativeType(value));
        this.shadow$addTicket(VecHelper.toChunkPos(pos).toLong(), ticketToRequest);
        return Optional.of((Ticket<T>) (Object) ticketToRequest);
    }

    @Override
    @SuppressWarnings({"ConstantConditions"})
    public boolean bridge$releaseTicket(final Ticket<?> ticket) {
        if (this.bridge$checkTicketValid(ticket)) {
            this.shadow$removeTicket(((TicketBridge) ticket).bridge$getChunkPosition(),
                    (net.minecraft.server.level.Ticket<?>) (Object) ticket);
            return true;
        }
        return false;
    }

    @SuppressWarnings({"ConstantConditions", "unchecked", "EqualsBetweenInconvertibleTypes"})
    @Override
    public <T> Collection<Ticket<T>> bridge$getTickets(final org.spongepowered.api.world.server.TicketType<T> ticketType) {
        return this.tickets.values().stream()
                .flatMap(x -> x.stream().filter(ticket -> ticket.getType().equals(ticketType)))
                .map(x -> (Ticket<T>) (Object) x)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "addTicket(JLnet/minecraft/server/level/Ticket;)V", at = @At("HEAD"))
    private void impl$addChunkPosToTicket(final long chunkPos, final net.minecraft.server.level.Ticket<?> ticket, final CallbackInfo ci) {
        ((TicketBridge) (Object) ticket).bridge$setChunkPosition(chunkPos);
    }

}

/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blockentities.rotation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blockentities.TickableBlockEntity;
import net.dries007.tfc.common.blocks.rotation.CreativeRotationBlock;
import net.dries007.tfc.util.rotation.NetworkAction;
import net.dries007.tfc.util.rotation.Node;
import net.dries007.tfc.util.rotation.Rotation;
import net.dries007.tfc.util.rotation.SourceNode;

public class CreativeRotationBlockEntity extends TickableBlockEntity implements RotatingBlockEntity
{
    // 30 RPM
    public static final float MAX_SPEED = Mth.TWO_PI / (2 * 20);
    public static final float LERP_SPEED = MAX_SPEED / 8;

    public static void serverTick(Level level, BlockPos pos, BlockState state, CreativeRotationBlockEntity be)
    {
        be.checkForLastTickSync();

        clientTick(level, pos, state, be);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, CreativeRotationBlockEntity be)
    {
        final Rotation.Tickable rotation = be.node.rotation();
        rotation.tick();
        if (rotation.speed() != be.targetSpeed)
        {
            rotation.setSpeed(be.targetSpeed);
        }
    }

    private final SourceNode node;
    private float targetSpeed;

    public CreativeRotationBlockEntity(BlockPos pos, BlockState state)
    {
        this(TFCBlockEntities.CREATIVE_MOTOR.get(), pos, state);
    }

    protected CreativeRotationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
        Direction.Axis axis = state.getValue(CreativeRotationBlock.AXIS);
        this.node = new SourceNode(pos, Node.ofAxis(axis), Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE), 0f)
        {
            @Override
            public String toString()
            {
                return "CreativeRotator[pos=%s, axis=%s]".formatted(pos(), axis);
            }
        };
    }

    @Override
    public void markAsInvalidInNetwork()
    {

    }

    @Override
    public boolean isInvalidInNetwork()
    {
        return false;
    }

    @Override
    public Node getRotationNode()
    {
        return node;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.saveAdditional(tag, provider);
        tag.putFloat("targetSpeed", targetSpeed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.loadAdditional(tag, provider);
        targetSpeed = tag.getFloat("targetSpeed");
    }

    @Override
    protected void onLoadAdditional()
    {
        performNetworkAction(NetworkAction.ADD_SOURCE);
    }

    @Override
    protected void onUnloadAdditional()
    {
        performNetworkAction(NetworkAction.REMOVE);
    }

    public void incrementSpeed()
    {
        targetSpeed = Mth.clamp(targetSpeed + LERP_SPEED, -MAX_SPEED, MAX_SPEED);
    }

    public void decrementSpeed()
    {
        targetSpeed = Mth.clamp(targetSpeed - LERP_SPEED, -MAX_SPEED, MAX_SPEED);
    }
}

package com.otherworldinn.block;

import com.otherworldinn.world.commission.CommissionService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;

public class CommissionBoardBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // 基准模型朝向: SOUTH
    private static final VoxelShape SHAPE_SOUTH =
            Shapes.or(
                    Block.box(-3, 0, 0, 19, 20, 1),
                    Block.box(-2, 0, 1, 18, 1, 2),
                    Block.box(-2, 19, 1, 18, 20, 2),
                    Block.box(-3, 0, 1, -2, 20, 2),
                    Block.box(18, 0, 1, 19, 20, 2));
    private static final VoxelShape SHAPE_WEST = rotateShapeY(SHAPE_SOUTH, 1);
    private static final VoxelShape SHAPE_NORTH = rotateShapeY(SHAPE_SOUTH, 2);
    private static final VoxelShape SHAPE_EAST = rotateShapeY(SHAPE_SOUTH, 3);

    public CommissionBoardBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 放置时牌面朝向玩家
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShapeByFacing(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShapeByFacing(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult) {
        return handleUse(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            net.minecraft.world.item.ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        handleUse(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private InteractionResult handleUse(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) {
            player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8F, 1.0F);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            CommissionService.openBoard(serverPlayer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static VoxelShape getShapeByFacing(Direction facing) {
        return switch (facing) {
            case WEST -> SHAPE_WEST;
            case NORTH -> SHAPE_NORTH;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_SOUTH;
        };
    }

    private static VoxelShape rotateShapeY(VoxelShape shape, int times) {
        VoxelShape current = shape;
        for (int i = 0; i < times; i++) {
            final VoxelShape[] rotated = new VoxelShape[] {Shapes.empty()};
            VoxelShape source = current;
            source.forAllBoxes(
                    (minX, minY, minZ, maxX, maxY, maxZ) -> {
                        // y 轴顺时针旋转 90 度 (与 blockstate y:90 保持一致)
                        double newMinX = 1.0D - maxZ;
                        double newMinZ = minX;
                        double newMaxX = 1.0D - minZ;
                        double newMaxZ = maxX;
                        rotated[0] =
                                Shapes.or(
                                        rotated[0],
                                        Shapes.box(newMinX, minY, newMinZ, newMaxX, maxY, newMaxZ));
                    });
            current = rotated[0];
        }
        return current;
    }
}

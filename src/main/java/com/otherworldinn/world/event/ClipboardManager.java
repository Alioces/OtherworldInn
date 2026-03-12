package com.otherworldinn.world.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.clipboard.ClipboardBlockEntity;
import com.simibubi.create.content.equipment.clipboard.ClipboardContent;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides.ClipboardType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

import net.minecraft.world.level.chunk.LevelChunk;

public class ClipboardManager {

    /**
     * 在指定范围内所有的墙面剪贴板上添加一条待办事项
     * @param level 世界
     * @param area 扫描范围
     * @param text 待办事项文本
     * @return 是否成功添加（至少修改了一个剪贴板）
     */
    public static boolean addTodo(Level level, AABB area, String text) {
        return modifyClipboards(level, area, pages -> {
            // 寻找第一个未满的页面（每页最多12条）
            List<ClipboardEntry> targetPage = null;
            for (List<ClipboardEntry> page : pages) {
                if (page.size() < 12) {
                    targetPage = page;
                    break;
                }
            }
            
            // 如果所有页面都满了，创建新页面
            if (targetPage == null) {
                targetPage = new ArrayList<>();
                pages.add(targetPage);
            }
            
            // 添加新的未勾选条目
            targetPage.add(new ClipboardEntry(false, Component.literal(text)));
        });
    }

    /**
     * 在指定范围内删除所有匹配文本的待办事项
     */
    public static void removeTodo(Level level, AABB area, String text) {
        modifyClipboards(level, area, pages -> {
            for (List<ClipboardEntry> page : pages) {
                page.removeIf(entry -> entry.text.getString().equals(text));
            }
            // 移除空页面
            pages.removeIf(List::isEmpty);
        });
    }

    /**
     * 修改指定待办事项的勾选状态
     * @param checked true为勾选，false为取消勾选
     */
    public static void setTodoStatus(Level level, AABB area, String text, boolean checked) {
        modifyClipboards(level, area, pages -> {
            for (List<ClipboardEntry> page : pages) {
                for (ClipboardEntry entry : page) {
                    if (entry.text.getString().equals(text)) {
                        entry.checked = checked;
                    }
                }
            }
        });
    }
    
    /**
     * 清空范围内所有剪贴板的内容
     */
    public static void clear(Level level, AABB area) {
        modifyClipboards(level, area, List::clear);
    }

    /**
     * 核心修改方法
     */
    public static boolean modifyClipboards(Level level, AABB area, Consumer<List<List<ClipboardEntry>>> modifier) {
        if (level.isClientSide)
            return false;

        boolean modified = false;
        BlockPos min = BlockPos.containing(area.minX, area.minY, area.minZ);
        BlockPos max = BlockPos.containing(area.maxX, area.maxY, area.maxZ);

        int minChunkX = min.getX() >> 4;
        int maxChunkX = max.getX() >> 4;
        int minChunkZ = min.getZ() >> 4;
        int maxChunkZ = max.getZ() >> 4;

        // 遍历范围内的所有区块
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, false);
                if (chunk == null) continue;

                // 遍历区块内的所有 BlockEntity
                for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                    // 检查是否在范围内
                    if (!area.contains(pos.getX(), pos.getY(), pos.getZ())) continue;

                    if (level.getBlockEntity(pos) instanceof ClipboardBlockEntity cbe) {
                        BlockState state = cbe.getBlockState();
                        
                        // 检查是否为墙面剪贴板
                        if (!AllBlocks.CLIPBOARD.has(state)) continue;
                        if (state.getValue(BlockStateProperties.ATTACH_FACE) != AttachFace.WALL) continue;

                        // 读取当前内容
                        ClipboardContent content = cbe.components().getOrDefault(AllDataComponents.CLIPBOARD_CONTENT, ClipboardContent.EMPTY);
                        
                        // readAll 返回的是页面列表的可变副本，可以直接修改
                        List<List<ClipboardEntry>> pages = ClipboardEntry.readAll(content);
                        
                        // 应用修改逻辑
                        modifier.accept(pages);
                        modified = true;
                        
                        // 构建新的内容对象
                        ClipboardContent newContent = content.setPages(pages)
                            .setType(pages.isEmpty() ? ClipboardType.EMPTY : ClipboardType.WRITTEN);
                        
                        // 写回 BlockEntity 的组件数据中
                        PatchedDataComponentMap map = new PatchedDataComponentMap(cbe.components());
                        map.set(AllDataComponents.CLIPBOARD_CONTENT, newContent);
                        cbe.setComponents(map);
                        
                        // 通知更新（确保客户端同步和方块状态更新）
                        cbe.notifyUpdate();
                        cbe.updateWrittenState();
                    }
                }
            }
        }
        return modified;
    }
}
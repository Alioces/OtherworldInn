package com.otherworldinn.datagen;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.foundation.BlockDataGenInfo;
import com.otherworldinn.foundation.ItemDataGenInfo;
import com.otherworldinn.init.ModBlocks;
import com.otherworldinn.init.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;
import java.util.Map;

/**
 * 语言文件生成器
 * <p>
 * 负责生成 en_us.json 和 zh_cn.json 语言文件。
 * 根据传入的 locale 参数决定生成哪种语言。
 */
public class ModLanguageProvider extends LanguageProvider {
    private final String locale;

    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, OtherworldInn.MODID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        boolean isZh = "zh_cn".equals(locale);
        
        if (isZh) {
            add("itemGroup.otherworldinn", "旅社物语");
            add("key.categories.otherworldinn", "旅社物语");
            add("key.otherworldinn.map_mode", "地图视图");
            
            // 地图点名称
            add("map_point.otherworldinn.inn", "旅社");
            add("map_point.otherworldinn.blacksmith", "铁匠铺");
            add("map_point.otherworldinn.town_gate", "城镇大门");
            add("map_point.otherworldinn.locked", "未解锁");
            add("map_point.otherworldinn.cant_teleport", "无法传送至");
            
            // 队伍命令
            add("command.otherworldinn.team.already_in_team", "你已经在一个队伍中了！");
            add("command.otherworldinn.team.created", "已创建队伍：%s");
            add("command.otherworldinn.team.target_no_team", "目标玩家不在队伍中！");
            add("command.otherworldinn.team.joined", "已加入队伍：%s");
            add("command.otherworldinn.team.not_in_team", "你不在一个队伍中！");
            add("command.otherworldinn.team.left", "已离开队伍。");
            add("command.otherworldinn.team.not_leader", "只有队长可以执行此操作！");
            add("command.otherworldinn.team.target_not_in_team", "目标玩家不在你的队伍中！");
            add("command.otherworldinn.team.kick_self", "你不能踢出你自己！请使用离开命令。");
            add("command.otherworldinn.team.kicked", "已将 %s 踢出队伍。");
            add("command.otherworldinn.team.you_were_kicked", "你已被踢出队伍。");
            add("command.otherworldinn.team.transferred", "队长职位已移交给 %s");
            add("command.otherworldinn.team.renamed", "队伍已重命名为：%s");
            add("command.otherworldinn.team.teleport_set", "队伍传送功能已设置为：%s");
            add("command.otherworldinn.team.edit_mode_enabled", "已开启编辑模式。");
            add("command.otherworldinn.team.edit_mode_disabled", "已关闭编辑模式。");
            add("command.otherworldinn.team.edit_mode_fail", "无法开启编辑模式！请确保旅社未营业且无客人。");
            add("command.otherworldinn.team.open_status_set", "旅社营业状态已设置为：%s");
            add("command.otherworldinn.team.point_unlocked", "已解锁地图点：%s (队伍：%s)");
            add("command.otherworldinn.team.point_locked", "已锁定地图点：%s (队伍：%s)");
            
            // 保护提示
            add("message.otherworldinn.protection.deny", "你不能修改城镇内的方块");
            add("message.otherworldinn.protection.banned_item", "此物品在城镇维度被禁用！");
            
            // 物品提示
            add("item.otherworldinn.recall_scroll.fail_in_town", "回程卷轴无法在城镇中使用！");
            add("tooltip.otherworldinn.banned_in_town", "§c当前维度内禁用");

            // 房间登记册
            add("message.otherworldinn.room_register.not_edit_mode", "旅社未处于编辑模式！");
            add("message.otherworldinn.room_register.pos1_set", "位置1已设置：%s");
            add("message.otherworldinn.room_register.create_success", "房间创建成功！ID：%s");
            add("message.otherworldinn.room_register.remove_success", "ID为%s的房间已被移除");
            add("message.otherworldinn.room_register.invalid_room", "无效的房间结构！");
            add("message.otherworldinn.room_register.validation.too_small", "房间空间太小！");
            add("message.otherworldinn.room_register.validation.out_of_bounds", "房间超出旅社范围！");
            add("message.otherworldinn.room_register.validation.hole_in_floor", "房间的地板有漏洞！");
            add("message.otherworldinn.room_register.validation.hole_in_ceiling", "房间的天花板有漏洞！");
            add("message.otherworldinn.room_register.validation.hole_in_wall", "房间的墙壁有漏洞！");
            add("message.otherworldinn.room_register.validation.missing_door", "房间缺少门！");
            add("message.otherworldinn.room_register.validation.missing_bed", "房间缺少床！");
            add("message.otherworldinn.room_register.validation.overlap", "房间与已有房间重叠！");
            add("message.otherworldinn.room_register.remove_success_with_reason", "ID为%s的房间已被移除。原因：%s");
            add("message.otherworldinn.room_register.manual_removal", "手动移除");
            add("message.otherworldinn.room_register.validation.too_crowded", "房间过于拥挤！");
            
            // 生物群系
            add("biome.otherworldinn.town", "城镇");

            // 家具属性
            add("tooltip.otherworldinn.furniture.comfort", "舒适度: %s");
            add("tooltip.otherworldinn.furniture.light", "光照度: %s");
            add("tooltip.otherworldinn.furniture.humidity", "湿度: %s");
        } else {
            add("itemGroup.otherworldinn", "Otherworld Inn");
            add("key.categories.otherworldinn", "Otherworld Inn");
            add("key.otherworldinn.map_mode", "Map View");
            
            // 地图点名称 (Map Point Names)
            add("map_point.otherworldinn.inn", "Otherworld Inn");
            add("map_point.otherworldinn.blacksmith", "Blacksmith");
            add("map_point.otherworldinn.town_gate", "Town Gate");
            add("map_point.otherworldinn.locked", "Locked");
            add("map_point.otherworldinn.cant_teleport", "Cannot teleport to");
            
            // 队伍命令 (Team Commands)
            add("command.otherworldinn.team.already_in_team", "You are already in a team!");
            add("command.otherworldinn.team.created", "Created team: %s");
            add("command.otherworldinn.team.target_no_team", "Target player is not in a team!");
            add("command.otherworldinn.team.joined", "Joined team: %s");
            add("command.otherworldinn.team.not_in_team", "You are not in a team!");
            add("command.otherworldinn.team.left", "Left the team.");
            add("command.otherworldinn.team.not_leader", "Only the leader can perform this action!");
            add("command.otherworldinn.team.target_not_in_team", "Target player is not in your team!");
            add("command.otherworldinn.team.kick_self", "You cannot kick yourself! Use leave command.");
            add("command.otherworldinn.team.kicked", "Kicked %s from the team.");
            add("command.otherworldinn.team.you_were_kicked", "You were kicked from the team.");
            add("command.otherworldinn.team.transferred", "Transferred leadership to %s");
            add("command.otherworldinn.team.renamed", "Renamed team to: %s");
            add("command.otherworldinn.team.teleport_set", "Team teleport capability set to: %s");
            add("command.otherworldinn.team.edit_mode_enabled", "Edit mode enabled.");
            add("command.otherworldinn.team.edit_mode_disabled", "Edit mode disabled.");
            add("command.otherworldinn.team.edit_mode_fail", "Cannot enable edit mode! Ensure Inn is closed and has no guests.");
            add("command.otherworldinn.team.open_status_set", "Inn open status set to: %s");
            add("command.otherworldinn.team.point_unlocked", "Unlocked map point: %s (Team: %s)");
            add("command.otherworldinn.team.point_locked", "Locked map point: %s (Team: %s)");
            
            // Protection Messages
            add("message.otherworldinn.protection.deny", "You cannot modify blocks in the Town!");
            add("message.otherworldinn.protection.banned_item", "This item is banned in the Town dimension!");
            
            // Item Messages
            add("item.otherworldinn.recall_scroll.fail_in_town", "Recall Scroll cannot be used in Town!");
            add("tooltip.otherworldinn.banned_in_town", "§cBanned in this dimension");

            // Room Register
            add("message.otherworldinn.room_register.not_edit_mode", "Inn is not in edit mode!");
            add("message.otherworldinn.room_register.pos1_set", "Position 1 set: %s");
            add("message.otherworldinn.room_register.create_success", "Room created successfully! ID: %s");
            add("message.otherworldinn.room_register.remove_success", "Room with ID %s has been removed");
            add("message.otherworldinn.room_register.invalid_room", "Invalid room structure!");
            add("message.otherworldinn.room_register.validation.too_small", "Room is too small!");
            add("message.otherworldinn.room_register.validation.out_of_bounds", "Room is out of inn bounds!");
            add("message.otherworldinn.room_register.validation.hole_in_floor", "Hole in the floor!");
            add("message.otherworldinn.room_register.validation.hole_in_ceiling", "Hole in the ceiling!");
            add("message.otherworldinn.room_register.validation.hole_in_wall", "Hole in the walls!");
            add("message.otherworldinn.room_register.validation.missing_door", "Missing door!");
            add("message.otherworldinn.room_register.validation.missing_bed", "Missing bed!");
            add("message.otherworldinn.room_register.validation.overlap", "Room overlaps with existing room!");
            add("message.otherworldinn.room_register.remove_success_with_reason", "Room with ID %s has been removed. Reason: %s");
            add("message.otherworldinn.room_register.manual_removal", "Manual Removal");
            add("message.otherworldinn.room_register.validation.too_crowded", "Room is too crowded!");
            
            // Biomes
            add("biome.otherworldinn.town", "Town");

            // Furniture Stats
            add("tooltip.otherworldinn.furniture.comfort", "Comfort: %s");
            add("tooltip.otherworldinn.furniture.light", "Light: %s");
            add("tooltip.otherworldinn.furniture.humidity", "Humidity: %s");
        }

        // 生成方块语言键
        for (Map.Entry<DeferredBlock<?>, BlockDataGenInfo> entry : ModBlocks.BLOCK_INFOS.entrySet()) {
            DeferredBlock<?> block = entry.getKey();
            BlockDataGenInfo info = entry.getValue();

            String name = isZh ? info.cnName() : info.enName();
            if (name != null && !name.isEmpty()) {
                add(block.get(), name);
            }
            
            List<String> tooltips = isZh ? info.cnTooltips() : info.enTooltips();
            for (int i = 0; i < tooltips.size(); i++) {
                add(block.get().getDescriptionId() + ".tooltip." + i, tooltips.get(i));
            }
        }

        // 生成物品语言键
        for (Map.Entry<DeferredItem<?>, ItemDataGenInfo> entry : ModItems.ITEM_INFOS.entrySet()) {
            DeferredItem<?> item = entry.getKey();
            ItemDataGenInfo info = entry.getValue();

            String name = isZh ? info.cnName() : info.enName();
            if (name != null && !name.isEmpty()) {
                add(item.get(), name);
            }
            
            List<String> tooltips = isZh ? info.cnTooltips() : info.enTooltips();
            for (int i = 0; i < tooltips.size(); i++) {
                add(item.get().getDescriptionId() + ".tooltip." + i, tooltips.get(i));
            }
        }
    }
}


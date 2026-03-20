package com.otherworldinn.datagen;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.foundation.BlockDataGenInfo;
import com.otherworldinn.foundation.ItemDataGenInfo;
import com.otherworldinn.init.ModBlocks;
import com.otherworldinn.init.ModEntities;
import com.otherworldinn.init.ModItems;
import com.otherworldinn.world.inn.facility.FacilityRegistry;
import java.util.List;
import java.util.Map;
import net.minecraft.data.PackOutput;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * 语言文件生成器
 *
 * <p>负责生成 en_us.json 和 zh_cn.json 语言文件。 根据传入的 locale 参数决定生成哪种语言。
 */
public class ModLanguageProvider extends LanguageProvider {
    private final String locale;

    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, OtherworldInn.MODID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        addManualTranslations();
        addGeneratedTranslations();
    }

    private void addManualTranslations() {
        entry("itemGroup.otherworldinn").zh("旅社物语").en("Otherworld Inn");

        entry("key.categories.otherworldinn").zh("旅社物语").en("Otherworld Inn");

        entry("key.otherworldinn.map_mode").zh("地图视图").en("Map View");
        entry("skill.fishing").zh("钓鱼").en("Fishing");
        entry("skill.magic").zh("钓鱼").en("Fishing");

        // 地图点名称
        entry("map_point.otherworldinn.inn").zh("旅社").en("The Inn");
        entry("map_point.otherworldinn.blacksmith").zh("铁匠铺").en("Blacksmith");
        entry("map_point.otherworldinn.town_gate").zh("城镇大门").en("Town Gate");
        entry("map_point.otherworldinn.locked").zh("未解锁").en("Locked");
        entry("map_point.otherworldinn.cant_teleport").zh("无法传送至").en("Cannot teleport to");
        entry("facility.otherworldinn.overlay.title").zh("设施状态").en("Facility Status");
        entry("facility.otherworldinn.overlay.name").zh("名称: %s").en("Name: %s");
        entry("facility.otherworldinn.overlay.level").zh("等级: %s/%s").en("Level: %s/%s");
        entry("facility.otherworldinn.overlay.repair_cost")
                .zh("维修金币: §f\uE001§r%s")
                .en("Repair Coins: §f\uE001§r %s");
        entry("facility.otherworldinn.overlay.upgrade_cost")
                .zh("升级金币: §f\uE001§r%s")
                .en("Upgrade Coins: §f\uE001§r %s");
        entry("facility.otherworldinn.overlay.repair_items")
                .zh("维修材料: %s")
                .en("Repair Materials: %s");
        entry("facility.otherworldinn.overlay.upgrade_items")
                .zh("升级材料: %s")
                .en("Upgrade Materials: %s");
        entry("facility.otherworldinn.overlay.repair").zh("维修设施").en("Repair Facility");
        entry("facility.otherworldinn.overlay.upgrade").zh("升级设施").en("Upgrade Facility");
        entry("facility.otherworldinn.overlay.no_items").zh("无").en("None");
        entry("facility.otherworldinn.upgrade_max_level")
                .zh("该设施已达到最高等级")
                .en("This facility is already at max level");
        entry("facility.otherworldinn.upgrade_fail_coins")
                .zh("金币不足，需要§f\uE001§r%s，当前§f\uE001§r%s")
                .en("Not enough coins, need %s, have %s");
        entry("facility.otherworldinn.upgrade_fail_items")
                .zh("材料不足，无法进行维修/升级")
                .en("Missing required materials for repair/upgrade");
        entry("facility.otherworldinn.upgrade_fail_structure")
                .zh("结构放置失败，请检查结构模板文件")
                .en("Failed to place structure template");
        entry("facility.otherworldinn.repair_success")
                .zh("%s 已修复")
                .en("%s has been repaired to level %s");
        entry("facility.otherworldinn.upgrade_success")
                .zh("%s 已升级至 %s 级")
                .en("%s has been upgraded to level %s");

        // 字幕
        entry("subtitles.otherworldinn.payment").zh("金币：叮铃").en("Coins: Clink");

        // 队伍命令
        entry("command.otherworldinn.team.already_in_team")
                .zh("你已经在一个队伍中了！")
                .en("You are already in a team!");
        entry("command.otherworldinn.team.created").zh("已创建队伍：%s").en("Created team: %s");
        entry("command.otherworldinn.team.target_no_team")
                .zh("目标玩家不在队伍中！")
                .en("Target player is not in a team!");
        entry("command.otherworldinn.team.joined").zh("已加入队伍：%s").en("Joined team: %s");
        entry("command.otherworldinn.team.not_in_team")
                .zh("你不在一个队伍中！")
                .en("You are not in a team!");
        entry("command.otherworldinn.team.left").zh("已离开队伍。").en("Left the team.");
        entry("command.otherworldinn.team.not_leader")
                .zh("只有队长可以执行此操作！")
                .en("Only the leader can perform this action!");
        entry("command.otherworldinn.team.target_not_in_team")
                .zh("目标玩家不在你的队伍中！")
                .en("Target player is not in your team!");
        entry("command.otherworldinn.team.kick_self")
                .zh("你不能踢出你自己！请使用离开命令。")
                .en("You cannot kick yourself! Use leave command.");
        entry("command.otherworldinn.team.kicked").zh("已将 %s 踢出队伍。").en("Kicked %s from the team.");
        entry("command.otherworldinn.team.you_were_kicked")
                .zh("你已被踢出队伍。")
                .en("You were kicked from the team.");
        entry("command.otherworldinn.team.transferred")
                .zh("队长职位已移交给 %s")
                .en("Transferred leadership to %s");
        entry("command.otherworldinn.team.renamed").zh("队伍已重命名为：%s").en("Renamed team to: %s");
        entry("command.otherworldinn.team.teleport_set")
                .zh("队伍传送功能已设置为：%s")
                .en("Team teleport capability set to: %s");
        entry("command.otherworldinn.team.point_unlocked")
                .zh("已解锁地图点：%s (队伍：%s)")
                .en("Unlocked map point: %s (Team: %s)");
        entry("command.otherworldinn.team.point_locked")
                .zh("已锁定地图点：%s (队伍：%s)")
                .en("Locked map point: %s (Team: %s)");
        entry("command.otherworldinn.team.coins.set")
                .zh("已将队伍 %s 的余额设置为 §f\uE001§r %d")
                .en("Set team %s balance to §f\uE001§r %d");
        entry("command.otherworldinn.team.coins.add")
                .zh("已向队伍 %s 增加 §f\uE001§r %d (当前: §f\uE001§r %d)")
                .en("Added §f\uE001§r %d to team %s (Current: §f\uE001§r %d)");
        entry("command.otherworldinn.team.coins.remove")
                .zh("已从队伍 %s 扣除 §f\uE001§r %d (当前: §f\uE001§r %d)")
                .en("Removed §f\uE001§r %d from team %s (Current: §f\uE001§r %d)");
        entry("command.otherworldinn.team.coins.remove_fail")
                .zh("扣除失败！余额不足 (当前: §f\uE001§r %d)")
                .en("Failed to remove! Not enough balance (Current: §f\uE001§r %d)");
        entry("command.otherworldinn.team.coins.get")
                .zh("队伍 %s 当前余额: §f\uE001§r %d")
                .en("Team %s current balance: §f\uE001§r %d");
        entry("command.otherworldinn.team.rating.set")
                .zh("已将队伍 %s 的旅社星级设置为 %s")
                .en("Set team %s inn rating to %s");

        // 管理员命令
        entry("command.otherworldinn.admin.reset_dimensions.start")
                .zh("§c[管理员] 正在强制触发维度重置...")
                .en("§c[Admin] Triggering forced dimension reset...");

        entry("message.otherworldinn.reset.warning")
                .zh("§c[注意] §e外部维度还有%d分钟重置，请尽快回到城镇")
                .en(
                        "§c[Notice] §eExternal dimensions will reset in %d minutes! Please return to Town ASAP.");
        entry("message.otherworldinn.reset.start")
                .zh("§c[警告] 外部维度重置中，请暂时不要离开城镇")
                .en("§c[WARNING] External dimensions resetting... Please do not leave Town.");
        entry("message.otherworldinn.reset.teleported")
                .zh("§a你已被传送到城镇安全区域")
                .en("§aYou have been teleported to the Town safe zone.");
        entry("message.otherworldinn.reset.complete")
                .zh("§a外部维度重置完成，准备迎接新的冒险")
                .en("§aExternal dimension reset complete. Get ready for new adventures!");

        // 保护提示
        entry("message.otherworldinn.protection.deny")
                .zh("你不能修改城镇内的方块")
                .en("You cannot modify blocks within the Town.");
        entry("message.otherworldinn.protection.deny_renovation")
                .zh("旅社不在装修中...")
                .en("The Inn is not under renovation...");
        entry("message.otherworldinn.protection.banned_item")
                .zh("此物品在城镇维度被禁用")
                .en("This item is banned in the Town dimension.");
        entry("message.otherworldinn.protection.only_in_town")
                .zh("此物品仅限在城镇维度使用")
                .en("This item is usable only in the Town dimension.");

        // 物品提示
        entry("tooltip.otherworldinn.sell_price")
                .zh("售价: §f\uE001§r%s")
                .en("Sell Price: §f\uE001§r%s");
        entry("item.otherworldinn.recall_scroll.fail_in_town")
                .zh("回程卷轴无法在城镇中使用")
                .en("Recall Scroll cannot be used in Town!");
        entry("message.otherworldinn.recall_scroll.overlay.use")
                .zh("长按3秒回到旅社")
                .en("Hold 3s to return to Inn");
        entry("tooltip.otherworldinn.banned_in_town")
                .zh("§c当前维度内禁用")
                .en("§cBanned in this dimension");
        entry("tooltip.otherworldinn.only_in_town")
                .zh("§c仅限城镇维度使用")
                .en("§cOnly usable in Town dimension");

        // 房间登记册
        entry("message.otherworldinn.room_register.not_edit_mode")
                .zh("旅社未处于编辑模式！")
                .en("The Inn is not in edit mode!");
        entry("message.otherworldinn.room_register.pos1_set")
                .zh("位置1已设置：%s")
                .en("Position 1 set: %s");
        entry("message.otherworldinn.room_register.create_success")
                .zh("房间创建成功！")
                .en("Room created successfully!");
        entry("message.otherworldinn.room_register.room_count")
                .zh("当前共有%s个房间")
                .en("There are currently %s rooms");
        entry("message.otherworldinn.room_register.remove_success")
                .zh("%s号房间已被移除")
                .en("Room %s has been removed");
        entry("message.otherworldinn.room_register.invalid_room")
                .zh("无效的房间结构")
                .en("Invalid room structure!");
        entry("message.otherworldinn.room_register.validation.too_small")
                .zh("房间空间太小")
                .en("Room is too small!");
        entry("message.otherworldinn.room_register.validation.out_of_bounds")
                .zh("房间超出旅社范围")
                .en("Room is out of inn bounds!");
        entry("message.otherworldinn.room_register.validation.hole_in_floor")
                .zh("房间的地板有漏洞")
                .en("There is a hole in the floor!");
        entry("message.otherworldinn.room_register.validation.hole_in_ceiling")
                .zh("房间的天花板有漏洞")
                .en("There is a hole in the ceiling!");
        entry("message.otherworldinn.room_register.validation.hole_in_wall")
                .zh("房间的墙壁有漏洞")
                .en("There is a hole in the walls!");
        entry("message.otherworldinn.room_register.validation.missing_door")
                .zh("房间缺少门")
                .en("Room is missing a door!");
        entry("message.otherworldinn.room_register.validation.missing_bed")
                .zh("房间缺少干净的床")
                .en("Room is missing a clean bed!");
        entry("message.otherworldinn.room_register.validation.overlap")
                .zh("房间与已有房间重叠！")
                .en("Room overlaps with an existing room!");
        entry("message.otherworldinn.room_register.remove_success_with_reason")
                .zh("%s号房间已被移除。原因：%s")
                .en("Room %s has been removed. Reason: %s");
        entry("message.otherworldinn.room_register.manual_removal").zh("手动移除").en("Manual removal");
        entry("message.otherworldinn.room_register.validation.too_crowded")
                .zh("房间过于拥挤")
                .en("Room is too crowded!");

        entry("message.otherworldinn.room_register.overlay.delete_room")
                .zh("删除房间")
                .en("Delete Room");
        entry("message.otherworldinn.room_register.overlay.add_room").zh("添加房间").en("Add Room");
        entry("message.otherworldinn.room_register.overlay.show_room").zh("显示房间").en("Show Rooms");

        // 待办事项
        entry("todo.otherworldinn.room_cleaning").zh("%s 号房间需要打扫").en("Room %s needs cleaning");
        entry("todo.otherworldinn.guest_waiting").zh("%s 正在等待办理入住").en("%s is waiting to check in");
        entry("todo.otherworldinn.vip_meal_order").zh("贵宾 %s 选订了餐品 %s").en("VIP %s ordered meal %s");

        // 生物群系
        entry("biome.otherworldinn.town").zh("城镇").en("Town");

        // 家具属性
        entry("tooltip.otherworldinn.furniture.comfort")
                .zh("§f\uE002§r舒适度: %s")
                .en("§f\uE002§rComfort: %s");
        entry("tooltip.otherworldinn.furniture.light")
                .zh("§f\uE003§r光照度: %s")
                .en("§f\uE003§rLight: %s");
        entry("tooltip.otherworldinn.furniture.humidity")
                .zh("§f\uE004§r湿度: %s")
                .en("§f\uE004§rHumidity: %s");

        // 地契
        entry("message.otherworldinn.land_deed.pos1_set").zh("位置1已设置：%s").en("Position 1 set: %s");
        entry("message.otherworldinn.land_deed.success")
                .zh("旅社区域扩展成功！")
                .en("Inn area expanded successfully!");
        entry("message.otherworldinn.land_deed.overlay.set_pos1").zh("设置第一点").en("Set 1st Corner");
        entry("message.otherworldinn.land_deed.overlay.set_pos2").zh("设置第二点").en("Set 2nd Corner");
        entry("message.otherworldinn.land_deed.overlay.set_pos2_with_cost")
                .zh("设置第二点 (预计花费: §f\uE001§r %d)")
                .en("Set 2nd Corner (Est. Cost: §f\uE001§r %d)");
        entry("message.otherworldinn.land_deed.pos2_set_with_cost")
                .zh("成功选定区域 (预计花费: §f\uE001§r %d)")
                .en("Area selected successfully (Est. Cost: §f\uE001§r %d)");
        entry("message.otherworldinn.land_deed.pos2_set_with_cost_fail")
                .zh("余额不足以扩展当前选定的范围！需要 §f\uE001§r %d，当前 §f\uE001§r %d")
                .en(
                        "Insufficient balance to expand the selected area! Need §f\uE001§r %d, have §f\uE001§r %d");
        entry("message.otherworldinn.land_deed.fail_no_money")
                .zh("余额不足！需要 §f\uE001§r %d，当前 §f\uE001§r %d")
                .en("Expansion failed: Not enough balance! Need §f\uE001§r %d, have §f\uE001§r %d");
        entry("message.otherworldinn.land_deed.overlay.confirm_with_cost")
                .zh("确认花费 §f\uE001§r %d 扩展旅社范围")
                .en("Confirm Expansion (Cost: §f\uE001§r %d)");
        entry("message.otherworldinn.land_deed.fail_out_of_bounds")
                .zh("无法扩展：超出最大范围")
                .en("Cannot expand: Exceeds maximum range! ");
        entry("message.otherworldinn.land_deed.fail_rating_limit")
                .zh("无法扩展：超出星级允许上限")
                .en("Cannot expand: Exceeds current star-level land limit");
        entry("message.otherworldinn.land_deed.selection_cleared")
                .zh("已取消选定范围。")
                .en("Selection cleared.");
        entry("message.otherworldinn.land_deed.overlay.cancel").zh("取消选定").en("Cancel Selection");
        entry("tooltip.otherworldinn.land_deed.rating")
                .zh("当前星级: %s")
                .en("Current Rating: %s");
        entry("tooltip.otherworldinn.land_deed.area_status")
                .zh("已扩展范围: %s / 剩余可用: %s")
                .en("Expanded Area: %s / Remaining: %s");

        // 房间钥匙
        entry("item.otherworldinn.room_key.bound").zh("%d号房间钥匙").en("Room %d Key");
        entry("message.otherworldinn.room_key.bound")
                .zh("成功绑定到 %d 号房间")
                .en("Successfully bound to Room %d");
        entry("message.otherworldinn.room_key.unbound").zh("已解除绑定").en("Unbound from room");
        entry("message.otherworldinn.room_key.no_room").zh("此处没有房间").en("No room here");
        entry("message.otherworldinn.room_key.overlay.bind").zh("绑定房间").en("Bind Room");
        entry("message.otherworldinn.room_key.overlay.unbind").zh("解除绑定").en("Unbind Room");
        entry("tooltip.otherworldinn.room_key.room_id").zh("房间号: %d").en("Room ID: %d");
        entry("tooltip.otherworldinn.room_key.pos").zh("位置: %s -> %s").en("Pos: %s -> %s");
        entry("tooltip.otherworldinn.room_key.beds").zh("床位: %d/%d").en("Beds: %d/%d");
        entry("tooltip.otherworldinn.room_key.price")
                .zh("床位价格: §f\uE001§r%d")
                .en("Price: §f\uE001§r%d");

        // 旅客入住
        entry("message.otherworldinn.room_key.checkin_success")
                .zh("旅客成功入住到 %d 号房间！")
                .en("Guest successfully checked into Room %d!");
        entry("message.otherworldinn.room_key.checkin_fail_guest_busy")
                .zh("该旅客已经住在其他房间了")
                .en("This guest is already staying in another room.");
        entry("message.otherworldinn.room_key.checkin_fail_checked_out")
                .zh("该旅客已经退房，准备离开了")
                .en("This guest has checked out and is preparing to leave.");
        entry("message.otherworldinn.room_key.checkin_fail_not_in_inn")
                .zh("错误：旅客不在旅社区域内！")
                .en("Error: Guest is not within any Inn area!");

        // Todo
        entry("message.otherworldinn.todo.new_task").zh("有新的事项待处理: %s").en("New Task: %s");
        entry("message.otherworldinn.room_key.checkin_fail_no_room")
                .zh("房间不存在或已被拆除")
                .en("Room does not exist or has been demolished.");
        entry("message.otherworldinn.room_key.checkin_fail_id_mismatch")
                .zh("房间信息不匹配 请重新绑定钥匙")
                .en("Room info mismatch. Please re-bind key.");
        entry("message.otherworldinn.room_key.checkin_fail_full")
                .zh("这间房似乎满了...")
                .en("This room seems full...");
        entry("message.otherworldinn.room_key.overlay.checkin")
                .zh("安排入住 (消耗钥匙)")
                .en("Arrange Check-in (Consumes Key)");

        // 铃铛
        entry("message.otherworldinn.desk_bell.status.open").zh("旅社营业中").en("The Inn is OPEN");
        entry("message.otherworldinn.desk_bell.status.closed").zh("旅社已打烊").en("The Inn is CLOSED");
        entry("message.otherworldinn.desk_bell.status.edit_mode")
                .zh("旅社装修中")
                .en("The Inn is under RENOVATION");
        entry("message.otherworldinn.store.overlay.open").zh("打开商店").en("Open Store");
        entry("message.otherworldinn.broom.overlay.expel").zh("驱逐旅客（降低声望）").en("Expel Guest");
        entry("message.otherworldinn.inventory.overlay.coins")
                .zh("§f\uE001§r%s")
                .en("§f\uE001§r%s");
        entry("message.otherworldinn.inventory.overlay.reputation_detail")
                .zh("声望：%s/%s")
                .en("Reputation: %s/%s");
        entry("message.otherworldinn.inventory.overlay.income.title")
                .zh("收入统计")
                .en("Income Breakdown");
        entry("message.otherworldinn.inventory.overlay.income.total")
                .zh("总收入")
                .en("Total Income");
        entry("message.otherworldinn.inventory.overlay.income.yesterday")
                .zh("昨日收入")
                .en("Yesterday Income");
        entry("message.otherworldinn.inventory.overlay.income.lodging")
                .zh("住宿：§f\uE001§r%s")
                .en("Lodging: §f\uE001§r%s");
        entry("message.otherworldinn.inventory.overlay.income.dining")
                .zh("餐饮：§f\uE001§r%s")
                .en("Dining: §f\uE001§r%s");

        // 旅社钥匙
        entry("message.otherworldinn.inn_key.no_permission")
                .zh("你没有权限管理这间旅社！")
                .en("You do not have permission to manage this Inn!");
        entry("message.otherworldinn.inn_key.overlay.toggle_state")
                .zh("潜行时切换旅社状态")
                .en("Toggle Inn State while Sneaking");
        entry("message.otherworldinn.inn_key.fail_open")
                .zh("旅社营业期间无法进行装修！")
                .en("Cannot switch to renovation mode while the Inn is OPEN!");
        entry("message.otherworldinn.inn_key.fail_guests")
                .zh("旅社内仍有旅客，无法进行装修！")
                .en("Cannot switch to renovation mode while guests are present!");
        entry("message.otherworldinn.inn_key.open")
                .zh("旅社已开业，今天也要努力")
                .en("The Inn is now OPEN! Let's work hard today.");
        entry("message.otherworldinn.inn_key.closed")
                .zh("旅社已歇业，快去休息吧...")
                .en("The Inn is now CLOSED. Time to rest...");
        entry("message.otherworldinn.inn_key.status").zh("当前状态: %s").en("Current State: %s");
        entry("message.otherworldinn.space_sphere.no_team")
                .zh("你当前不在任何队伍中")
                .en("You are not in any team");
        entry("message.otherworldinn.space_sphere.already_unlocked")
                .zh("当前队伍的地图传送功能已解锁")
                .en("Your team's map teleport is already unlocked");
        entry("message.otherworldinn.space_sphere.teleport_unlocked")
                .zh("地图传送功能已激活")
                .en("Map teleport has been activated");
        entry("message.otherworldinn.guest.tooltip.title").zh("旅客信息").en("Guest Info");
        entry("message.otherworldinn.guest.tooltip.vip").zh("[贵宾]").en("[VIP]");
        entry("message.otherworldinn.guest.tooltip.preference.comfort")
                .zh("§f\uE002§r舒适偏好: %s-%s")
                .en("§f\uE002§rComfort Preference: %s-%s");
        entry("message.otherworldinn.guest.tooltip.preference.light")
                .zh("§f\uE003§r光照偏好: %s-%s")
                .en("§f\uE003§rLight Preference: %s-%s");
        entry("message.otherworldinn.guest.tooltip.preference.humidity")
                .zh("§f\uE004§r湿度偏好: %s-%s")
                .en("§f\uE004§rHumidity Preference: %s-%s");
        entry("message.otherworldinn.guest.tooltip.budget").zh("预算: §f\uE001§r%s").en("Budget: §f\uE001§r%s");
        entry("message.otherworldinn.guest.tooltip.rewards").zh("可能奖励:").en("Possible Rewards:");
        entry("message.otherworldinn.guest.tooltip.rewards.none").zh("无").en("None");
        entry("message.otherworldinn.guest.tooltip.rewards.entry")
                .zh("- %s x%s-%s")
                .en("- %s x%s-%s");
        entry("message.otherworldinn.space_sphere.overlay.use")
                .zh("激活地图点传送")
                .en("Activate map point teleport");
        entry("message.otherworldinn.space_sphere.target_unavailable")
                .zh("目标维度当前不可用")
                .en("Target dimension is currently unavailable");
        entry("message.otherworldinn.nether_space_sphere.overlay.use")
                .zh("传送至下界")
                .en("Teleport to Nether");
        entry("message.otherworldinn.end_space_sphere.overlay.use")
                .zh("传送至末地")
                .en("Teleport to End");

        // 床单
        entry("message.otherworldinn.bed_sheet.overlay.replace")
                .zh("替换脏乱床单")
                .en("Replace Messy Sheet");
        entry("message.otherworldinn.messy_bed_sheet.overlay.wash").zh("清洗床单").en("Wash Sheet");

        // 旅客姓名
        entry("guest.name.format").zh("%s·%s").en("%s %s");

        // First Names
        entry("guest.name.first.1").zh("亚瑟").en("Arthur");
        entry("guest.name.first.2").zh("贝阿特丽丝").en("Beatrice");
        entry("guest.name.first.3").zh("凯斯宾").en("Caspian");
        entry("guest.name.first.4").zh("多里安").en("Dorian");
        entry("guest.name.first.5").zh("埃莉诺").en("Eleanor");
        entry("guest.name.first.6").zh("菲利克斯").en("Felix");
        entry("guest.name.first.7").zh("吉迪恩").en("Gideon");
        entry("guest.name.first.8").zh("海泽尔").en("Hazel");
        entry("guest.name.first.9").zh("艾瑞丝").en("Iris");
        entry("guest.name.first.10").zh("朱利安").en("Julian");
        entry("guest.name.first.11").zh("凯尔").en("Kael");
        entry("guest.name.first.12").zh("莉珊德拉").en("Lysandra");
        entry("guest.name.first.13").zh("马格努斯").en("Magnus");
        entry("guest.name.first.14").zh("诺拉").en("Nora");
        entry("guest.name.first.15").zh("奥赖恩").en("Orion");
        entry("guest.name.first.16").zh("帕西瓦尔").en("Percival");
        entry("guest.name.first.17").zh("奎因").en("Quinn");
        entry("guest.name.first.18").zh("罗伊纳").en("Rowena");
        entry("guest.name.first.19").zh("塞拉斯").en("Silas");
        entry("guest.name.first.20").zh("塔莉亚").en("Thalia");
        entry("guest.name.first.21").zh("奥利弗").en("Oliver");
        entry("guest.name.first.22").zh("乔治").en("George");
        entry("guest.name.first.23").zh("哈利").en("Harry");
        entry("guest.name.first.24").zh("诺亚").en("Noah");
        entry("guest.name.first.25").zh("杰克").en("Jack");
        entry("guest.name.first.26").zh("查理").en("Charlie");
        entry("guest.name.first.27").zh("阿尔菲").en("Alfie");
        entry("guest.name.first.28").zh("利奥").en("Leo");
        entry("guest.name.first.29").zh("奥斯卡").en("Oscar");
        entry("guest.name.first.30").zh("亚齐").en("Archie");
        entry("guest.name.first.31").zh("艾拉").en("Isla");
        entry("guest.name.first.32").zh("奥利维娅").en("Olivia");
        entry("guest.name.first.33").zh("阿米莉亚").en("Amelia");
        entry("guest.name.first.34").zh("艾娃").en("Ava");
        entry("guest.name.first.35").zh("艾米莉").en("Emily");
        entry("guest.name.first.36").zh("伊莎贝拉").en("Isabella");
        entry("guest.name.first.37").zh("格蕾丝").en("Grace");
        entry("guest.name.first.38").zh("芙蕾娅").en("Freya");
        entry("guest.name.first.39").zh("杰西卡").en("Jessica");
        entry("guest.name.first.40").zh("索菲").en("Sophie");

        // Last Names
        entry("guest.name.last.1").zh("阿什福德").en("Ashford");
        entry("guest.name.last.2").zh("布莱克伍德").en("Blackwood");
        entry("guest.name.last.3").zh("克劳利").en("Crowley");
        entry("guest.name.last.4").zh("达文波特").en("Davenport");
        entry("guest.name.last.5").zh("埃弗哈特").en("Everhart");
        entry("guest.name.last.6").zh("弗罗斯特").en("Frost");
        entry("guest.name.last.7").zh("格林").en("Grimm");
        entry("guest.name.last.8").zh("霍桑").en("Hawthorne");
        entry("guest.name.last.9").zh("铁木").en("Ironwood");
        entry("guest.name.last.10").zh("金克斯").en("Jinx");
        entry("guest.name.last.11").zh("奈特").en("Knight");
        entry("guest.name.last.12").zh("洛夫莱斯").en("Lovelace");
        entry("guest.name.last.13").zh("穆恩").en("Moon");
        entry("guest.name.last.14").zh("霍洛韦").en("Holloway");
        entry("guest.name.last.15").zh("哈特").en("Hatter");
        entry("guest.name.last.16").zh("潘德加斯特").en("Pendergast");
        entry("guest.name.last.17").zh("雷文斯克罗夫特").en("Ravenscroft");
        entry("guest.name.last.18").zh("斯托姆").en("Storm");
        entry("guest.name.last.19").zh("索恩").en("Thorne");
        entry("guest.name.last.20").zh("温特").en("Winter");
        entry("guest.name.last.21").zh("史密斯").en("Smith");
        entry("guest.name.last.22").zh("琼斯").en("Jones");
        entry("guest.name.last.23").zh("威廉姆斯").en("Williams");
        entry("guest.name.last.24").zh("泰勒").en("Taylor");
        entry("guest.name.last.25").zh("布朗").en("Brown");
        entry("guest.name.last.26").zh("戴维斯").en("Davies");
        entry("guest.name.last.27").zh("埃文斯").en("Evans");
        entry("guest.name.last.28").zh("威尔逊").en("Wilson");
        entry("guest.name.last.29").zh("托马斯").en("Thomas");
        entry("guest.name.last.30").zh("罗伯茨").en("Roberts");
        entry("guest.name.last.31").zh("约翰逊").en("Johnson");
        entry("guest.name.last.32").zh("刘易斯").en("Lewis");
        entry("guest.name.last.33").zh("沃克").en("Walker");
        entry("guest.name.last.34").zh("赖特").en("Wright");
        entry("guest.name.last.35").zh("罗宾逊").en("Robinson");
        entry("guest.name.last.36").zh("汤普森").en("Thompson");
        entry("guest.name.last.37").zh("怀特").en("White");
        entry("guest.name.last.38").zh("休斯").en("Hughes");
        entry("guest.name.last.39").zh("爱德华兹").en("Edwards");
        entry("guest.name.last.40").zh("格林").en("Green");

        // 实体
        entry(ModEntities.ORDINARY_GUEST.get()).zh("普通旅客").en("Ordinary Guest");
        entry(ModEntities.RICH_GUEST.get()).zh("富有的旅客").en("Wealthy Guest");
        entry(ModEntities.HEAVY_PACK_GUEST.get()).zh("行囊多的旅客").en("Heavy-Pack Guest");
        entry(ModEntities.ULTRA_RICH_GUEST.get()).zh("非常富有的顾客").en("Ultra-Wealthy Customer");
        entry(ModEntities.ORDINARY_VIP_GUEST.get()).zh("普通贵宾").en("Ordinary VIP Guest");
        entry(ModEntities.BLACKSMITH.get()).zh("铁匠").en("Blacksmith");
        entry(ModEntities.MAGICIAN.get()).zh("魔法使").en("Magician");

        // 商店 GUI
        entry("gui.otherworldinn.store.confirm").zh("确定").en("Confirm");
        entry("gui.otherworldinn.store.purchase").zh("§f\uE001§r%s购买").en("§f\uE001§r%sBuy");
        entry("gui.otherworldinn.store.price").zh("价格: §f\uE001§r%s").en("Price: §f\uE001§r%s");
        entry("gui.otherworldinn.store.stock").zh("库存: %s/%s").en("Stock: %s/%s");
        entry("gui.otherworldinn.store.limit_purchase").zh("限购%s个").en("Limit %s");
        entry("gui.otherworldinn.store.stock.infinite").zh("库存: ∞").en("Stock: ∞");
        entry("gui.otherworldinn.store.favor_unlock")
                .zh("%s级好感度解锁")
                .en("Unlocks at Favor Level %s");
        entry("gui.otherworldinn.store.favor.level").zh("好感度: %s").en("Favor Level: %s");
        entry("gui.otherworldinn.store.favor.progress")
                .zh("进度: §f\uE001§r%s/%s")
                .en("Progress: §f\uE001§r%s/%s");

        // 女仆任务
        entry("task.otherworldinn.clean_room").zh("清理房间").en("Room Cleaning");
        entry("task.otherworldinn.clean_room.desc")
                .zh("自动更换脏乱床铺床单，并在需要时清洗脏床单。")
                .en("Automatically replaces messy bed sheets and washes dirty sheets when needed.");
        entry("task.otherworldinn.front_desk").zh("前台接待").en("Front Desk");
        entry("task.otherworldinn.front_desk.desc")
                .zh("旅社营业时，自动为等待旅客匹配最合适房间并办理入住。")
                .en(
                        "When the Inn is open, automatically matches waiting guests to the best room and checks them in.");
    }

    private void addGeneratedTranslations() {
        boolean isZh = "zh_cn".equals(locale);

        // 生成方块语言键
        for (Map.Entry<DeferredBlock<?>, BlockDataGenInfo> entry :
                ModBlocks.BLOCK_INFOS.entrySet()) {
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

        for (FacilityRegistry.FacilityDefinition facility : FacilityRegistry.getAll()) {
            add(facility.translationKey(), isZh ? facility.zhName() : facility.enName());
        }
    }

    private TranslationBuilder entry(String key) {
        return new TranslationBuilder(key);
    }

    private TranslationBuilder entry(EntityType<?> entity) {
        return new TranslationBuilder(entity.getDescriptionId());
    }

    private class TranslationBuilder {
        private final String key;

        public TranslationBuilder(String key) {
            this.key = key;
        }

        public TranslationBuilder zh(String value) {
            if ("zh_cn".equals(locale)) {
                add(key, value);
            }
            return this;
        }

        public TranslationBuilder en(String value) {
            if (!"zh_cn".equals(locale)) {
                add(key, value);
            }
            return this;
        }
    }
}

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
            add("itemGroup.otherworldinn", "异界旅社");
            add("key.categories.otherworldinn", "异界旅社");
            add("key.otherworldinn.strategy_mode", "地图视图");
            
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
            
            // 物品提示
            add("item.otherworldinn.recall_scroll.fail_in_town", "回程卷轴无法在城镇中使用！");
        } else {
            add("itemGroup.otherworldinn", "Otherworld Inn");
            add("key.categories.otherworldinn", "Otherworld Inn");
            add("key.otherworldinn.strategy_mode", "Map View");
            
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
            
            // 物品提示 (Item Messages)
            add("item.otherworldinn.recall_scroll.fail_in_town", "Recall Scroll cannot be used in Town!");
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


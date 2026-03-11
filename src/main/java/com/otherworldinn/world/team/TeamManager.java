package com.otherworldinn.world.team;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import com.otherworldinn.network.ModMessages;
import com.otherworldinn.network.packet.S2CTeamSyncPacket;

/**
 * 队伍管理器
 * <p>
 * 管理所有活跃的队伍数据。
 * 在服务端，这是全局单例。
 * 在客户端，这应该同步当前玩家的队伍数据。
 * </p>
 */
public class TeamManager {

    private static final TeamManager INSTANCE = new TeamManager();

    public static TeamManager getInstance() {
        return INSTANCE;
    }

    public TeamSavedData getData(MinecraftServer server) {
        return TeamSavedData.get(server.overworld());
    }

    /**
     * 获取玩家所在的队伍
     * <p>
     * 如果玩家没有队伍，返回 null。
     * 客户端逻辑会返回本地缓存的队伍数据。
     *
     * @param player 目标玩家
     * @return 队伍数据或 null
     */
    public TeamData getPlayerTeam(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            TeamSavedData data = getData(serverPlayer.getServer());
            UUID teamId = data.getPlayerToTeam().get(player.getUUID());
            if (teamId != null) {
                return data.getTeams().get(teamId);
            }
            return null;
        } else {
            // 客户端逻辑
            return getClientPlayerTeam();
        }
    }
    
    /**
     * 处理玩家加入世界
     * <p>
     * 无论单人多人，如果这是该服务器的第一个玩家，则自动建队。
     * 后续加入的玩家将自动加入已存在的第一个队伍（临时逻辑）。
     *
     * @param player 加入的玩家
     * @param server 服务器实例
     */
    public void onPlayerJoin(Player player, MinecraftServer server) {
        UUID playerId = player.getUUID();
        
        // 尝试获取现有队伍
        TeamData existingTeam = getPlayerTeam(player);
        if (existingTeam != null) {
            // 如果已经在队伍中（可能是重连），同步数据
            if (player instanceof ServerPlayer serverPlayer) {
                syncTeamTeleport(existingTeam, serverPlayer);
            }
            return;
        }
        
        TeamSavedData data = getData(server);
        if (data.getPlayerToTeam().containsKey(playerId)) return;
        
        TeamData joinedTeam = null;
        
        // 只要是第一个玩家，就自动创建队伍
        if (server.getPlayerList().getPlayerCount() <= 1 && data.getTeams().isEmpty()) {
            joinedTeam = createTeam(player, "Team-" + player.getName().getString());
        } else if (!data.getTeams().isEmpty()) {
            // 临时逻辑：后续玩家自动加入第一个队伍
            UUID firstTeamId = data.getTeams().keySet().iterator().next();
            if (joinTeam(player, firstTeamId)) {
                joinedTeam = data.getTeams().get(firstTeamId);
            }
        }
        
        // 如果成功加入或创建队伍，同步数据
        if (joinedTeam != null && player instanceof ServerPlayer serverPlayer) {
            syncTeamTeleport(joinedTeam, serverPlayer);
        }
    }
    
    /**
     * 创建队伍
     *
     * @param player 创建者（队长）
     * @param name   队伍名称
     * @return 创建的队伍数据
     */
    public TeamData createTeam(Player player, String name) {
        UUID playerId = player.getUUID();
        // 离开旧队伍
        leaveTeam(player);
        
        UUID teamId = UUID.randomUUID();
        TeamData team = new TeamData(teamId);
        team.setName(name);
        team.addMember(playerId);
        team.setLeaderId(playerId);
        
        // 初始解锁
        team.unlockMapPoint(ResourceLocation.parse("otherworldinn:inn"));
        
        if (player instanceof ServerPlayer serverPlayer) {
            TeamSavedData data = getData(serverPlayer.getServer());
            data.addTeam(team);
        }
        
        return team;
    }
    
    /**
     * 加入队伍
     *
     * @param player 玩家
     * @param teamId 队伍ID
     * @return 是否成功加入
     */
    public boolean joinTeam(Player player, UUID teamId) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        
        TeamSavedData data = getData(serverPlayer.getServer());
        TeamData team = data.getTeams().get(teamId);
        if (team == null) return false;
        
        leaveTeam(player);
        
        data.addMember(teamId, player.getUUID());
        return true;
    }
    
    /**
     * 离开当前队伍
     *
     * @param player 玩家
     */
    public void leaveTeam(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        TeamSavedData data = getData(serverPlayer.getServer());
        UUID playerId = player.getUUID();
        
        UUID oldTeamId = data.getPlayerToTeam().get(playerId);
        if (oldTeamId != null) {
            data.removeMember(oldTeamId, playerId);
            
            TeamData oldTeam = data.getTeams().get(oldTeamId);
            if (oldTeam != null && oldTeam.getMembers().isEmpty()) {
                data.removeTeam(oldTeamId);
            }
        }
    }

    /**
     * 更新队伍传送状态并同步给所有成员
     *
     * @param team     队伍
     * @param unlocked 是否解锁
     * @param server   服务器实例
     */
    public void setTeamTeleportUnlocked(TeamData team, boolean unlocked, MinecraftServer server) {
        team.setTeleportUnlocked(unlocked);
        getData(server).markDirty();
        
        // 同步给所有在线成员
        // 使用完整同步包，确保所有数据一致
        S2CTeamSyncPacket packet = new S2CTeamSyncPacket(
                team.getTeamId(),
                team.getName(),
                team.getLeaderId(),
                new ArrayList<>(team.getMembers()),
                new ArrayList<>(team.getUnlockedMapPoints()),
                unlocked,
                team.getCoins()
        );
        
        for (UUID memberId : team.getMembers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                ModMessages.sendToPlayer(packet, member);
            }
        }
    }
    
    /**
     * 同步队伍传送状态给特定玩家
     *
     * @param team   队伍
     * @param player 目标玩家
     */
    public void syncTeamTeleport(TeamData team, ServerPlayer player) {
        S2CTeamSyncPacket packet = new S2CTeamSyncPacket(
                team.getTeamId(),
                team.getName(),
                team.getLeaderId(),
                new ArrayList<>(team.getMembers()),
                new ArrayList<>(team.getUnlockedMapPoints()),
                team.isTeleportUnlocked(),
                team.getCoins()
        );
        ModMessages.sendToPlayer(packet, player);
    }


    /**
     * 根据 ID 获取队伍
     *
     * @param teamId 队伍 ID
     * @param server 服务器实例
     * @return 队伍数据或 null
     */
    public TeamData getTeam(UUID teamId, MinecraftServer server) {
        if (server == null) return null;
        TeamSavedData data = getData(server);
        return data.getTeams().get(teamId);
    }
    
    /**
     * 获取包含指定坐标的队伍数据
     * <p>
     * 检查该坐标是否位于某个队伍的旅社区域内。
     * 
     * @param pos 检查的坐标
     * @param server 服务器实例
     * @return 包含该坐标的队伍，如果没有则返回 null
     */
    public TeamData getTeamAt(BlockPos pos, MinecraftServer server) {
        TeamSavedData data = getData(server);
        for (TeamData team : data.getTeams().values()) {
            if (team.isInInnZone(pos)) {
                return team;
            }
        }
        return null;
    }
    
    // --- 辅助修改方法 ---
    
    public void renameTeam(TeamData team, String newName, MinecraftServer server) {
        team.setName(newName);
        getData(server).markDirty();
        // 同步... 这里简化处理，理论上应该广播同步包
        // 目前 setTeamTeleportUnlocked 会广播全量包，可以复用
        setTeamTeleportUnlocked(team, team.isTeleportUnlocked(), server);
    }
    
    public void transferLeader(TeamData team, UUID newLeader, MinecraftServer server) {
        team.setLeaderId(newLeader);
        getData(server).markDirty();
        setTeamTeleportUnlocked(team, team.isTeleportUnlocked(), server);
    }

    public void unlockMapPoint(TeamData team, ResourceLocation pointId, MinecraftServer server) {
        team.unlockMapPoint(pointId);
        getData(server).markDirty();
        setTeamTeleportUnlocked(team, team.isTeleportUnlocked(), server);
    }

    public void lockMapPoint(TeamData team, ResourceLocation pointId, MinecraftServer server) {
        team.lockMapPoint(pointId);
        getData(server).markDirty();
        setTeamTeleportUnlocked(team, team.isTeleportUnlocked(), server);
    }

    // --- 客户端同步逻辑 ---
    
    private TeamData clientTeamCache;

    /**
     * 客户端获取当前玩家的队伍数据
     *
     * @return 客户端缓存的队伍数据
     */
    public TeamData getClientPlayerTeam() {
        if (clientTeamCache == null) {
            // 初始化默认空数据，等待服务端同步
            clientTeamCache = new TeamData(UUID.randomUUID());
            clientTeamCache.setTeleportUnlocked(false);
            // 默认解锁旅社
            clientTeamCache.unlockMapPoint(ResourceLocation.parse("otherworldinn:inn"));
        }
        return clientTeamCache;
    }
    
    /**
     * 更新客户端缓存 (由网络包调用)
     */
    public void updateClientTeamData(UUID teamId, String name, UUID leaderId, Set<UUID> members, Set<ResourceLocation> unlockedPoints, boolean teleportUnlocked, int coins) {
        if (clientTeamCache == null || !clientTeamCache.getTeamId().equals(teamId)) {
            clientTeamCache = new TeamData(teamId);
        }
        
        clientTeamCache.setName(name);
        clientTeamCache.setMembers(members);
        clientTeamCache.setLeaderId(leaderId);
        clientTeamCache.setUnlockedMapPoints(unlockedPoints);
        clientTeamCache.setTeleportUnlocked(teleportUnlocked);
        clientTeamCache.setCoins(coins);
    }
}

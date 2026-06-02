
# 🏰 Otherworld Inn — 异世界旅社

<p align="center">
  <strong>在 Minecraft 中经营你的异世界旅社</strong>
</p>

---

## 📖 简介

**Otherworld Inn** 是一个基于 [NeoForge](https://neoforged.net/) 的 Minecraft 模组，核心主题为**异世界旅社经营模拟**。玩家将在专属的城镇维度中经营旅社，管理房间、接待各色旅客、经营NPC商店、完成城镇委托、组建远征队探索未知维度，并通过积累声望与收入逐步升级旅社星级。

## ✨ 核心特性

### 🏨 旅社经营
- **房间管理** — 使用房间登记簿注册房间，系统自动验证房间完整性（墙壁/地板/天花板/门/床）
- **房间属性** — 每个房间拥有舒适度、光照、湿度、整洁度四维属性，由家具和装饰自动计算
- **房间主题** — 支持房间主题系统，不同主题影响旅客满意度
- **床位管理** — 铺床/换脏床单机制，整洁度影响旅客评价
- **旅社评级** — 0~5 星评级系统，需满足声望、房间数、总收入三重条件方可升星
- **收入统计** — 住宿收入、餐饮收入、其他收入分类统计，支持今日/昨日对比

### 👥 旅客系统
- **多种旅客类型** — 普通旅客、大包旅客、富有旅客、超级富豪、赞助商等
- **VIP 旅客** — VIP 客人可点餐，玩家需及时送餐，超时将扣除声望
- **旅客偏好** — 每位旅客拥有舒适度/光照/湿度偏好，匹配度影响满意度评分和奖励
- **旅客行为** — 完整的 AI 行为树：等待入住→前往房间→睡觉→用餐→退房
- **随机命名** — 旅客自动获得随机名称

### 🛒 商店系统
- **NPC 商人** — 铁匠、农夫、杂货商、魔法师四类商店 NPC
- **好感度系统** — 最高 10 级好感度，满级享受 7 折优惠
- **每日进货** — 商品每日自动刷新，包含固定商品和随机商品
- **对话系统** — NPC 拥有分支对话，可触发商店/虚拟铁砧等功能

### 📜 委托系统
- **委托板** — 队伍可从委托板上接取任务
- **任务类型** — 击杀指定怪物、提交指定物品，或两者兼备
- **自动刷新** — 委托板定期刷新，超时未完成自动过期
- **奖励发放** — 完成委托获得金币和声望奖励

### 🗺️ 远征系统
- **探险图表** — 核心物品，由多种图表组件组合而成
- **动态维度** — 根据图表组件动态生成临时远征维度
- **组件类型** — 世界类型（地表/浮岛/洞穴/下界/末地）、生物群系、岩石类型、特殊效果
- **限时探索** — 远征有严格时间限制，超时自动召回
- **回城卷轴** — 可随时使用回城卷轴返回城镇

### 👥 队伍系统
- **自动组队** — 玩家进入世界自动创建/加入队伍
- **共享资源** — 队伍共享金币、旅社、委托进度、解锁地图点
- **区域管理** — 队伍拥有旅社区域，支持区域扩展和差集运算
- **增量同步** — 仅在数据变化时同步，优化网络性能

### 🗺️ 城镇地图
- **地图视图** — 全屏地图界面，展示城镇 POI（兴趣点）
- **地图点类型** — 商店/功能点、出口、地标
- **解锁机制** — 部分地图点需满足条件方可解锁
- **传送功能** — 解锁后可在地图点之间传送

### 🔧 设施系统
- **可升级设施** — 城镇中的设施可通过消耗金币和物品升级
- **设施影响** — 设施等级影响对话内容、旅客体验等
- **地图联动** — 设施升级后地图点状态同步更新

## 🎮 游戏指令

| 指令 | 权限 | 说明 |
|------|------|------|
| `/innadmin facility set_level` | OP | 设置设施等级 |
| `/innadmin facility list` | OP | 列出所有设施 |
| `/innadmin coins` | OP | 管理队伍金币 |
| `/innadmin expedition` | OP | 远征管理 |
| `/innadmin commission` | OP | 委托管理 |
| `/team` | 玩家 | 队伍管理 |
| `/expedition` | 玩家 | 远征相关指令 |

## 🧩 兼容模组

Otherworld Inn 与以下模组提供深度兼容：

| 模组 | 兼容内容 |
|------|----------|
| [Create](https://www.curseforge.com/minecraft/mc-mods/create) | 桌面铃交互、仓库渲染、风扇加工、机械装置 |
| [Kaleidoscope Cookery](https://www.curseforge.com/minecraft/mc-mods/kaleidoscope-cookery) | 菜肴售价注册、烹饪配方解锁 |
| [Kaleidoscope Tavern](https://www.curseforge.com/minecraft/mc-mods/kaleidoscope-tavern) | 酒类售价注册 |
| [Touhou Little Maid](https://www.curseforge.com/minecraft/mc-mods/touhou-little-maid) | 女仆前台任务、房间清洁任务 |
| [Reskillable Reimagined](https://www.curseforge.com/minecraft/mc-mods/reskillable) | 技能等级经验处理、技能界面定制 |
| [Serene Seasons](https://www.curseforge.com/minecraft/mc-mods/serene-seasons) | 季节性作物生长调整 |
| [FTB Ultimine](https://www.curseforge.com/minecraft/mc-mods/ftb-ultimine) | 城镇内矿脉挖掘配置 |
| [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) | NPC 商店配方展示 |

## 🛠️ 技术信息

| 项目 | 详情 |
|------|------|
| Minecraft 版本 | 1.21.1 |
| 模组加载器 | NeoForge 21.1.219 |
| Java 版本 | 21 |
| 模组 ID | `otherworldinn` |
| 模组版本 | 0.0.1 |
| 许可证 | GPL-3.0 |

## 📦 构建与开发

### 环境要求

- **JDK 21** 或更高版本
- **Gradle**（项目自带 Gradle Wrapper）

### 构建步骤

```bash
# 克隆仓库
git clone <repository-url>
cd OtherworldInn

# 构建项目
./gradlew build

# 运行客户端
./gradlew runClient

# 运行服务端
./gradlew runServer

# 生成数据（模型/语言/标签/配方等）
./gradlew runData
```

构建产物位于 `build/libs/` 目录下。

### 项目结构

```
src/main/java/com/otherworldinn/
├── OtherworldInn.java          # 模组主类入口
├── block/                      # 自定义方块
├── client/                     # 客户端逻辑
│   ├── commission/             # 委托客户端管理
│   ├── control/                # 相机/视觉控制
│   ├── dialogue/               # 对话客户端管理
│   ├── event/listener/         # 客户端事件监听
│   ├── gui/                    # GUI 界面与覆盖层
│   │   ├── overlay/            # HUD 覆盖层
│   │   └── screen/             # 全屏界面
│   ├── map/service/            # 地图页面管理
│   ├── render/                 # 渲染工具
│   ├── renderer/               # 实体/方块渲染器
│   └── util/                   # 客户端工具类
├── command/                    # 指令系统
├── compat/                     # 模组兼容
│   ├── jei/                    # JEI 兼容
│   ├── task/                   # 小女仆任务兼容
│   └── ultimine/               # FTB Ultimine 兼容
├── datagen/                    # 数据生成器
├── entity/                     # 实体定义
│   ├── base/                   # 实体基类
│   ├── guest/                  # 旅客实体
│   └── store/                  # 商店NPC实体
├── foundation/                 # 注册辅助与配置
├── init/                       # DeferredRegister 初始化
├── item/                       # 自定义物品
├── mixin/                      # Mixin 注入
├── network/                    # 网络通信
│   └── packet/                 # C2S/S2C 数据包
├── util/                       # 通用工具类
│   └── service/                # 服务类
└── world/                      # 世界/游戏逻辑
    ├── commission/             # 委托系统
    ├── data/                   # 世界数据持久化
    ├── dialogue/               # 对话系统
    ├── dimension/              # 城镇维度
    ├── economy/                # 经济系统
    ├── entity/projectile/      # 投射物实体
    ├── event/                  # 世界事件
    │   ├── listener/           # 事件监听器
    │   └── runtime/            # 运行时管理
    ├── expedition/             # 远征系统
    │   └── recipe/             # 远征配方
    ├── inn/                    # 旅社核心
    │   ├── facility/           # 设施系统
    │   ├── listener/           # 旅社事件
    │   └── service/            # 旅社服务
    ├── inventory/              # 菜单/容器
    ├── loot/                   # 战利品修改
    ├── map/                    # 地图系统
    ├── team/                   # 队伍系统
    │   └── service/            # 队伍服务
    └── teleport/               # 传送系统
```

## 🏗️ 架构概览

模组采用**模块化分层架构**：

```
┌─────────────────────────────────────────────┐
│               客户端层 (Client)              │
│  GUI / 渲染器 / 覆盖层 / 相机控制 / 网络接收  │
├─────────────────────────────────────────────┤
│               网络层 (Network)               │
│         C2S / S2C 数据包 / 频道注册          │
├─────────────────────────────────────────────┤
│              游戏逻辑层 (World)              │
│  旅社 / 队伍 / 远征 / 委托 / 对话 / 经济     │
├─────────────────────────────────────────────┤
│              实体层 (Entity)                 │
│     旅客基类 / VIP旅客 / 商店NPC / 子类       │
├─────────────────────────────────────────────┤
│            基础设施层 (Foundation)            │
│   注册器 / 配置 / 数据生成 / Mixin / 兼容     │
└─────────────────────────────────────────────┘
```

**关键设计模式**：
- **单例服务** — `TeamManager`、`ExpeditionService`、`CommissionService` 等核心服务采用单例模式
- **数据驱动** — 委托模板、对话定义、设施配置等采用注册表模式管理
- **增量同步** — 队伍数据通过 NBT 快照对比实现增量网络同步
- **Mixin 注入** — 通过 Mixin 修改原版和第三方模组行为，保持兼容性
- **事件驱动** — 基于 NeoForge 事件总线处理游戏事件

## 📄 许可证

本项目基于 [GNU General Public License v3.0](LICENSE) 许可证开源。

---

<p align="center">
  <em>异世界旅社 — 在方块世界中经营你的梦想旅店</em>
</p>

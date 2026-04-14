# Biome Runestone

`Biome Runestone` 是一个围绕符文门的群系传送模组。  
核心目标是让玩家通过不同类型的群系符文石，在主世界快速前往指定群系或同群系的随机地点。

## 玩家玩法总览

1. 搭好同一个符文门框架。
2. 将门框四角放置为同一种群系符文石。
3. 进入符文门后，传送到对应群系的可落地点。

## 三套群系符文石

- 普通群系符文石：传送到目标群系的可用落点，适合稳定往返。
- 随机群系符文石：每次会在同群系内刷新新的目标位置，支持固定刷新窗口（默认 5 秒）和最小间隔距离（默认 1000 格）。
- 玩家群系符文石：按“玩家”锁定目的地。每个玩家首次传送后会记录自己的坐标，之后无论从哪个同类型门进入，都回到该玩家自己的固定点位。

## 传送生效条件

- 只在主世界生效。
- 必须四角是同一种 `Biome Runestone` 符文石方块。
- 四角混用不同符文石不会被识别为本模组群系门。

## 落点安全规则

- 落点必须是露天位置（头顶可见天空）。
- 玩家脚部高度要求 `Y >= 65`。
- 脚下方块不能是液体。
- 脚部和头部位置必须可站立（非阻挡）。

## 当前支持的群系

- ocean（海洋）
- plains（平原）
- desert（沙漠）
- extremeHills（极端丘陵）
- forest（森林）
- taiga（针叶林）
- swampland（沼泽地）
- river（河流）
- frozenOcean（冻洋）
- frozenRiver（冻河）
- icePlains（冰原）
- iceMountains（冰山）
- beach（沙滩）
- desertHills（沙漠丘陵）
- forestHills（森林丘陵）
- taigaHills（针叶林丘陵）
- extremeHillsEdge（极端丘陵边缘）
- jungle（丛林）
- jungleHills（丛林丘陵）
- desertRiver（沙漠河流）
- jungleRiver（丛林河流）
- swampRiver（沼泽河流）

## 服务器/整合包提示

- 配置文件路径：`config/biome_runestone.json`
- 可调项包括：随机门刷新时间、最小间隔距离、随机搜索尝试上限、缓存上限与过期时间。
- 管理指令：`/biome_runestone_rune`（别名：`/trune`、`/brune`、`/br`），可用于查看统计与清理缓存/锁定数据。

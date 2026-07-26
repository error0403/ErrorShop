<!--
ErrorShop MineBBS 常驻发布稿。

维护者更新清单（HTML 注释不会显示在帖子正文中）：
1. 从 build.gradle.kts 与 plugin.yml 同步版本号。
2. 从 build.gradle.kts 同步 Paper API、Java 和发布平台版本。
3. 从 plugin.yml 与 ErrorShopPlugin.onCommand 同步命令、权限和别名。
4. 从 config.yml 同步默认值；预留但未启用的字段不得写成已支持功能。
5. 同步 README.md、本文“最近更新”和 MineBBS 帖子附件。
6. 发布前检查 GitHub Actions、下载链接、QQ 群链接和实测环境。

当前内容基线：ErrorShop 0.18.1，2026-07-26，Paper 1.21.11，Java 21。
本文可直接复制到 MineBBS；上传新版 JAR 后再发布或编辑帖子。
-->

# [服务端插件] ErrorShop 0.18.1｜轻量系统商店 + 玩家市场 + 自定义菜单｜支持 XConomy 与群组服

[![Build](https://github.com/error0403/ErrorShop/actions/workflows/build.yml/badge.svg)](https://github.com/error0403/ErrorShop/actions/workflows/build.yml)
![Version](https://img.shields.io/badge/version-0.18.1-orange)
![Paper](https://img.shields.io/badge/Paper-1.21.11-2f88d8)
![Java](https://img.shields.io/badge/Java-21-ed8b00)
![License](https://img.shields.io/badge/license-MIT-green)

> 想快速搭建官方商店、玩家自由市场、VIP/SVIP 上架额度和功能菜单，又不想从一开始就部署数据库？ErrorShop 默认使用本地 YAML 即装即用，需要群组服时再切换 MySQL + Redis。

- **当前版本：** `0.18.1`
- **推荐环境：** Paper `1.21.11` + Java `21`
- **实测经济：** Vault `1.7.3-b131` + XConomy Paper `2.26.3`
- **开源协议：** MIT
- **源码与完整手册：** <https://github.com/error0403/ErrorShop>
- **云端构建：** <https://github.com/error0403/ErrorShop/actions/workflows/build.yml>
- **下载方式：** MineBBS 帖子附件优先；开发构建可从 GitHub Actions 获取

---

## 插件简介

ErrorShop 是一个面向 Paper 服务器的配置驱动商店插件，包含三部分：

1. **官方商店**：通过 `shops/*.yml` 配置商品、价格、金币/点券组合和访问权限。
2. **玩家市场**：玩家自行定价上架，支持购买、搜索、分页、税率、下架和背包满暂存。
3. **自定义菜单**：通过 `menus/*.yml` 制作主菜单、功能导航、传送入口或商店入口。

默认单服模式不需要 MySQL、Redis、Vault 或 PlayerPoints，适合先搭界面和测试流程；正式启用金币交易时，再安装 Vault 与经济插件并切换配置。

## 为什么选择 ErrorShop

| 常见需求 | ErrorShop 的处理方式 |
| --- | --- |
| 想快速做官方商店 | 一个商店对应一个 YAML 文件，无需写代码 |
| 想让玩家自由交易 | 手持物品执行 `/errorshop sell <价格>` 即可上架 |
| VIP/SVIP 需要更多摊位 | 按权限组配置不同 `slots`，匹配多个组时取最大值 |
| 市场商品太多 | 每页 45 件，支持按材质、显示名和卖家搜索 |
| 卖家离线或在其它子服 | 优先通过 Vault `OfflinePlayer` 入账，失败则持久化待结算 |
| 买家背包满 | 商品进入待领取队列，重新登录或 `/errorshop claim` 领取 |
| 两个子服同时买同一商品 | MySQL 模式先锁定商品，仅成功锁定的一方继续成交 |
| 不想数据库拖慢主线程 | 市场 MySQL/YAML I/O 异步执行，背包与经济操作保持同步线程 |
| 想统一服务器功能入口 | 自定义菜单支持玩家、控制台、商店、市场、菜单和消息动作 |

## 0.18.1 最近更新

- 新增 `/errorshop claim`，玩家清出背包空间后可主动领取暂存物品。
- 新增 `errorshop.market.claim` 权限，默认所有玩家拥有。
- 增加领取中、领取成功、队列为空和背包仍满等中文提示。
- 手动领取复用异步存储、同步背包操作和玩家级操作互斥，兼容本地 YAML 与 MySQL 市场。

此前 `0.18` 已完成：

- 重构玩家市场成交顺序：锁定商品后扣款，再提交成交。
- MySQL 模式下，成交状态、买家待发物品和卖家税后收益在同一事务中写入。
- 增加价格上下限、小数位、税率和异常旧价格校验。
- 增加 45 格分页与 `/errorshop market <关键词>` 搜索。
- 加强重复点击、过期锁、退款、待发货和离线收益提示。
- 在 Paper `1.21.11-132`、Vault `1.7.3-b131`、XConomy Paper `2.26.3` 完成干净启动验证。

## 核心功能

### 1. 官方商店

- 每个 `shops/*.yml` 文件对应一个商店。
- 可配置材质、显示名、Lore、数量、金币价格、点券价格和商店权限。
- 支持 Vault 金币、PlayerPoints 点券，以及 `and` / `or` 双经济模式。
- 商品点击按配置 ID 绑定，不会仅凭相同材质误匹配其它商品。
- 可配置购买后是否关闭商店界面。

> 当前版本的官方商店实现的是**购买**。配置模型中的 `sell` 为预留字段，尚未提供玩家向官方商店回收物品的交互；`/errorshop sell` 表示上架玩家市场。

### 2. 玩家市场

- 玩家手持物品执行 `/errorshop sell <价格>` 上架。
- 普通玩家、VIP、SVIP 可按权限设置不同上架数量。
- 54 格市场界面，其中每页展示 45 件商品。
- `/errorshop market <关键词>` 可搜索材质、显示名或卖家名。
- 买家左键购买；卖家对自己的商品在 5 秒内右键两次确认下架。
- 支持最低价格、最高价格、小数位限制和市场税率。
- 下架返还或购买发货时若背包空间不足，物品会安全进入待领取队列。
- 玩家重新上线时自动尝试发放，也可执行 `/errorshop claim` 手动领取。

### 3. 成交安全与异常恢复

MySQL 群组市场的主要成交顺序：

```text
点击购买
  → 锁定商品
  → 校验价格与余额
  → 扣除买家金币
  → 原子提交：已售状态 + 买家待发物品 + 卖家税后收益
  → 发放物品并尝试向卖家入账
  → 背包满或经济服务暂不可用时保留到持久化队列
```

- 同一商品只能由成功取得锁的一台服务器继续处理。
- 超过 `market.reservation-timeout-seconds` 的异常锁会自动回收。
- 成交尚未提交时会尝试退款；无法立即退款时写入待结算队列。
- 每名玩家同时只处理一个市场操作，降低重复点击造成的竞态。
- 本地 YAML 与 MySQL 后端都将存储 I/O 移出 Paper 主线程。

### 4. 群组服共享市场

- 多个子服可共用同一个 MySQL 玩家市场。
- 商品、状态、离线收益和待发物品统一存储。
- Redis 作为可选市场事件总线，可广播上架、购买、下架等事件。
- `cluster.group` 用于区分服务器组，`cluster.server-id` 必须在同组内唯一。
- MySQL 是商品数据源；Redis 断开不会变成另一份商品数据库。

### 5. 自定义菜单

- 可创建多个 `menus/*.yml`，用于主菜单、功能导航、传送入口或活动入口。
- 支持按字符布局 GUI，并分别配置左键、右键或通用动作。
- 可用动作：`[player]`、`[console]`、`[tell]`、`[shop]`、`[market]`、`[menu]`。
- `menu.allow-console-actions: false` 可统一禁用控制台权限动作。
- 支持 MiniMessage，并兼容传统 `&` 颜色代码。

## 兼容性与依赖

| 项目 | 状态 |
| --- | --- |
| 服务端 | Paper / Paper 兼容分支 |
| 推荐版本 | Paper `1.21.11` |
| 编译目标 | Paper API `1.21.11-R0.1-SNAPSHOT` |
| Java | `21` |
| Vault | 可选；启用真实金币交易时需要 |
| XConomy | 可选；已验证 Paper `2.26.3` 经 Vault 接入 |
| PlayerPoints | 可选；启用点券时需要 |
| MySQL | 可选；群组共享市场时使用 |
| Redis | 可选；群组市场事件通知时使用 |
| PlaceholderAPI | 可选软依赖；当前没有 ErrorShop 自定义变量 |
| License | MIT |

> 插件以 Paper `1.21.11` 为当前开发和验证基线。其它 `1.21.x` 兼容分支建议先在测试服验证；升级前请备份 `plugins/ErrorShop/`。

## 快速安装

1. 准备 Java `21` 和 Paper `1.21.11`。
2. 下载 `ErrorShop-0.18.1.jar`，放入服务器 `plugins/`。
3. 启动服务器一次，生成默认配置。
4. 编辑 `config.yml`、`shops/*.yml`、`menus/*.yml` 和 `lang.yml`。
5. 重启服务器，或执行 `/errorshop reload`。

默认单服预览配置：

```yaml
currency:
  provider: none

market:
  backend: local

cluster:
  enabled: false
```

`currency.provider: none` 只适合预览与测试，不会真实扣除金币。正式服使用 XConomy 时：

1. 安装 Vault 与 XConomy。
2. 确认两者均正常启用。
3. 修改：

```yaml
currency:
  provider: vault
```

启动日志应出现类似：

```text
Vault economy provider connected: me.yic.xconomy.depend.economy.Vault
```

## 玩家市场配置示例

### 单服 YAML 市场

```yaml
market:
  backend: local
  min-price: 0.01
  max-price: 1000000000
  price-decimals: 2
  tax-rate: 0.05
  reservation-timeout-seconds: 60
  groups:
    default:
      slots: 9
    vip:
      permission: "errorshop.market.vip"
      slots: 18
    svip:
      permission: "errorshop.market.svip"
      slots: 27
```

### MySQL + Redis 群组市场

```yaml
market:
  backend: mysql
  fail-policy: "disable-market"
  reservation-timeout-seconds: 60

cluster:
  enabled: true
  group: "survival"
  server-id: "survival-1" # 每个子服必须不同
  redis:
    enabled: true
    host: "127.0.0.1"
    port: 6379
    password: ""
    database: 0
    channel: "errorshop:market"

database:
  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "errorshop"
    username: "errorshop"
    password: "change-this"
    table-prefix: "errorshop_"
    use-ssl: false
```

所有共享市场的子服使用同一个 MySQL 数据库；同一组服务器填写相同 `cluster.group`，但每台服务器必须使用不同 `cluster.server-id`。

## 命令

| 命令 | 说明 |
| --- | --- |
| `/errorshop shop <id>` | 打开指定官方商店 |
| `/errorshop market [关键词]` | 打开或搜索玩家市场 |
| `/errorshop sell <price>` | 将主手物品按指定价格上架玩家市场 |
| `/errorshop claim` | 领取背包满时暂存的物品 |
| `/errorshop menu <id>` | 打开指定自定义菜单 |
| `/errorshop reload` | 异步重载配置与市场存储 |

主命令别名：`/eshop`

## 权限

| 权限 | 默认 | 说明 |
| --- | --- | --- |
| `errorshop.reload` | OP | 重载插件 |
| `errorshop.shop.<id>` | 按节点 | 打开指定商店，例如 `errorshop.shop.default` |
| `errorshop.menu.<id>` | 按节点 | 打开指定菜单，例如 `errorshop.menu.main` |
| `errorshop.market.sell` | 所有玩家 | 上架玩家市场商品 |
| `errorshop.market.buy` | 所有玩家 | 浏览并购买市场商品 |
| `errorshop.market.cancel` | 所有玩家 | 下架自己的市场商品 |
| `errorshop.market.claim` | 所有玩家 | 手动领取暂存物品 |

## 配置文件

| 文件 | 用途 |
| --- | --- |
| `config.yml` | 经济、玩家市场、权限组、MySQL、Redis 和安全开关 |
| `database.yml` | 本地 YAML 市场文件位置 |
| `lang.yml` | 所有主要玩家提示文本 |
| `shops/*.yml` | 官方商店 |
| `menus/*.yml` | 自定义菜单 |
| `market.yml` | 本地市场数据；运行时生成，不建议手动编辑 |

完整字段、商店和菜单示例请查看：[GitHub README](https://github.com/error0403/ErrorShop#readme)。

## 玩家使用说明

### 上架与购买

```text
/errorshop sell 100
/errorshop market
/errorshop market 钻石
```

- 玩家不能购买自己的商品。
- 卖家右键自己的商品一次会看到确认提示，5 秒内再次右键才会下架。
- 下架物品会优先返还背包，空间不足则进入待领取队列。

### 背包满时领取

```text
/errorshop claim
```

- 先清出足够背包空间再执行。
- 能放入的物品会立即发放；剩余物品继续安全保存。
- 玩家重新登录时也会自动尝试领取。

## 常见问题

### 必须安装数据库吗？

不需要。默认 `market.backend: local` 使用本地 YAML。只有多个子服需要共享市场时才建议使用 MySQL；Redis 仍是可选事件总线。

### 支持 XConomy 吗？

支持。已在 Paper `1.21.11-132`、Vault `1.7.3-b131`、XConomy Paper `2.26.3` 验证，离线卖家入账使用 Vault 的 UUID/`OfflinePlayer` 接口。

### 卖家离线时钱会丢吗？

插件会先把卖家税后收益写入持久化待结算记录，再尝试通过 Vault 入账。经济服务暂时拒绝入账时，记录会恢复到队列并在之后重试。

### 买家背包满了怎么办？

商品保存在待领取队列。玩家清出空间后执行 `/errorshop claim`，或者重新登录触发自动发放。

### 支持 PlaceholderAPI 变量吗？

当前没有公开 `%errorshop_*%` 自定义变量。安装 PlaceholderAPI 不会自动产生 ErrorShop 变量，请勿根据旧帖猜测变量名。

### 官方商店能把物品卖回服务器吗？

当前版本尚未启用官方商店回收交互。`sell` 配置字段属于预留项，`/errorshop sell` 是玩家市场上架命令。

## 正式服上线前建议

- 备份 `plugins/ErrorShop/`。
- 用普通玩家账号测试商店权限、市场上架额度和领取权限。
- 完成一次购买、卖家离线入账、下架返还和背包满领取。
- 群组服同时从两台子服购买同一商品，确认只能成交一次。
- 模拟 MySQL/Redis 或经济插件暂时不可用，观察是否给出明确提示且没有复制物品。
- 非必要时将 `menu.allow-console-actions` 设为 `false`。

## 下载、源码与反馈

- **MineBBS 下载：** 请使用本帖附件中的最新版 JAR。
- **GitHub：** <https://github.com/error0403/ErrorShop>
- **GitHub Actions：** <https://github.com/error0403/ErrorShop/actions/workflows/build.yml>
- **问题反馈：** <https://github.com/error0403/ErrorShop/issues>
- **作者 QQ：** `1955008190`
- **ErrorShop 交流群：** <https://qm.qq.com/q/bG3ooHYT3q>

欢迎反馈 Bug、功能建议和实际服务器使用体验。反馈交易问题时，请尽量附上 ErrorShop、Paper、Vault、经济插件版本，以及对应时间段的控制台日志。

---

## English Summary

ErrorShop `0.18.1` is a configuration-first Paper shop plugin with server shops, a paged and searchable player market, custom GUI menus, optional Vault/PlayerPoints economy, and optional MySQL + Redis group-market support.

Highlights:

- Paper `1.21.11` / Java `21` development baseline.
- Verified with Vault `1.7.3-b131` and XConomy Paper `2.26.3`.
- YAML local market by default; no database required for a single server.
- MySQL listing reservations and recoverable sale transactions for server groups.
- Persistent offline seller earnings and full-inventory delivery queues.
- `/errorshop claim` for manual queued-item delivery.
- Permission-based listing limits, 45-item pages, search, cancellation, tax, and price validation.
- MiniMessage and legacy `&` color support.

Source and full documentation: <https://github.com/error0403/ErrorShop>

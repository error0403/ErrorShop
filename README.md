# ErrorShop - 轻量商店、玩家市场和自定义菜单

[![Build](https://github.com/error0403/ErrorShop/actions/workflows/build.yml/badge.svg)](https://github.com/error0403/ErrorShop/actions/workflows/build.yml)

> MineBBS 宣传贴长期维护稿：[Markdown 预览](MINEBBS.md) · [BBCode 直接发布](MINEBBS.bbcode)

## 🚀 为什么用 ErrorShop / Why ErrorShop

开服最烦的不是“有没有商店插件”，而是：商店菜单要改来改去、玩家交易入口不好做、VIP/SVIP 权限市场要单独写、跨服市场又容易重复购买或数据不同步。ErrorShop 就是为这些常见痛点做的：**简单、轻量、易上手，配置文件看得懂，装上就能开始搭商店体系。**

Running a server usually does not fail because there is no shop plugin — it fails because menus are annoying to maintain, player trading needs a clean entry, VIP/SVIP market permissions need extra work, and cross-server markets can easily duplicate sales or desync data. ErrorShop is built for these pain points: **simple, lightweight, easy to use, and configuration-first.**

你可以用 ErrorShop 快速完成：

- **自定义无限个菜单**：用 `menus/` 写多少个 GUI 菜单都可以，适合作为主菜单、商店入口、传送入口、功能导航。
- **快速创建系统商店**：在 `shops/` 里写商品配置，就能搭建服务器官方商店，支持购买、金币/点券组合和权限控制。
- **搭建带权限判断的全球市场**：玩家可自行上架物品，服主可按权限组控制上架数量，VIP/SVIP 市场权限不用额外写插件。
- **群组服共享市场**：需要跨服时可切到 MySQL + Redis，支持共享市场、购买锁定、离线收益和待领取队列。
- **低成本维护**：不追求花哨大而全，优先保证轻量、清晰、可配置、好排错。

With ErrorShop, you can quickly build:

- **Unlimited custom menus** through `menus/` for main menus, shop entrances, teleport menus, and feature navigation.
- **Server shops in minutes** through `shops/`, with configurable purchases, money/points pricing, and permission checks.
- **A permission-gated global player market** where players list items and owners control listing slots by permission groups.
- **A group-server shared market** through MySQL + Redis when you need cross-server trading, listing locks, offline earnings, and queued deliveries.
- **Low-maintenance shop infrastructure** focused on being lightweight, readable, configurable, and easy to troubleshoot.

## 📮 作者联系 / Contact the Author

作者 QQ：`1955008190`

ErrorShop 交流群：<https://qm.qq.com/q/bG3ooHYT3q>

作者高强度冲浪在线，欢迎反馈 Bug、提建议或进群交流。插件会持续维护，基本每天都会修复和更新。最近也支持**低价定制 Minecraft 插件**，如果你的服务器需要专属功能，可以直接联系作者聊需求。

Author QQ: `1955008190`

ErrorShop QQ Group: <https://qm.qq.com/q/bG3ooHYT3q>

The author is frequently online and happy to receive bug reports, suggestions, and server-owner feedback. ErrorShop is actively maintained and updated often. Low-cost custom Minecraft plugin development is also available — contact the author if your server needs custom features.

## ✨ 功能特点 / Features

- 🧩 **无限自定义菜单**：`menus/` 想写几个就写几个，可做主菜单、商店入口、传送入口、活动入口和功能导航。  
  **Unlimited custom menus**: create as many `menus/` GUI files as you need for navigation, shops, teleports, events, and server features.
- 🏪 **快速系统商店**：商品直接写进 `shops/`，不用写代码就能快速搭建官方商店，支持购买、金币/点券组合和商店权限。<br>
  **Fast server shops**: define items in `shops/` and build official server shops without coding, including money/points pricing and shop permissions.
- 📈 **权限全球市场**：玩家可自行上架物品，服主可按权限组限制上架数量，轻松做普通玩家/VIP/SVIP 差异。  
  **Permission-gated global market**: players list items themselves, while owners control listing slots by permission groups such as default/VIP/SVIP.
- 🌐 **群组共享市场**：可选 MySQL + Redis，让多个子服共享玩家市场，并支持购买锁定、离线收益和待领取队列。  
  **Group shared market**: optionally use MySQL + Redis to share one player market across servers with listing locks, offline earnings, and queued deliveries.
- 🔒 **降低重复购买风险**：MySQL 模式购买前会先锁定商品，降低多服同时点击同一商品导致重复成交的风险。  
  **Duplicate-sale protection**: MySQL mode reserves a listing before purchase to reduce duplicate sales from simultaneous cross-server clicks.
- 🧾 **可恢复成交事务**：成交状态、买家待发物品和卖家待结算收益在一次持久化事务中完成；异常退出留下的 MySQL 锁会自动回收。
  **Recoverable sale transactions**: sale state, buyer delivery, and seller earnings commit together, while stale MySQL reservations are released automatically.
- 🔎 **分页与搜索**：市场每页显示 45 件商品，可使用 `/errorshop market <关键词>` 搜索材质、显示名或卖家。
  **Pagination and search**: browse 45 listings per page and search material, display name, or seller with `/errorshop market <query>`.
- ⚡ **异步市场存储**：MySQL 和市场 YAML 的读取、保存、上架、购买、下架与领取均不阻塞 Paper 主线程。
  **Async market storage**: MySQL and market YAML reads, writes, listings, purchases, cancellations, and claims do not block Paper's main thread.
- 💰 **Vault / PlayerPoints 可选**：可以只用 Vault 金币，也可以给系统商店配置点券或双货币价格。  
  **Optional Vault / PlayerPoints**: use Vault money, PlayerPoints, or dual-currency pricing for server-shop items.
- 🎨 **MiniMessage + 旧颜色码兼容**：新服可以用 MiniMessage，老配置也不用马上重写。  
  **MiniMessage + legacy color support**: use modern MiniMessage while keeping old `&` color-code configs working.
- 🪶 **简单、轻量、易用**：不强迫你接一堆依赖，默认单服 YAML 就能跑，需要群组市场时再启用数据库。  
  **Simple, lightweight, easy to use**: no heavy dependency chain by default; local YAML works out of the box, and database mode is optional.

## ✅ 支持版本 / Compatibility

- Minecraft Java Edition
- Paper / compatible Paper forks
- Recommended Java: 21
- Compile target: Paper `1.21.11` (`1.21.x` compatible Paper forks)

说明：0.18.1 使用 Java 21 和 Paper 1.21.11 API 编译。正式服升级前仍建议先备份 `plugins/ErrorShop/` 并在测试服完成一次购买、下架、暂存领取和离线卖家入账测试。

已完成干净启动验证：Paper `1.21.11-132`、Vault `1.7.3-b131`、XConomy Paper `2.26.3`。日志确认 ErrorShop 连接到 XConomy 的 Vault `OfflinePlayer` 经济实现。

Note: 0.18.1 is compiled with Java 21 against the Paper 1.21.11 API. Back up `plugins/ErrorShop/` and test buying, cancellation, queued-item claims, and offline seller payouts on staging before upgrading production.

Clean startup was verified with Paper `1.21.11-132`, Vault `1.7.3-b131`, and XConomy Paper `2.26.3`; ErrorShop connected to XConomy's Vault `OfflinePlayer` economy implementation.

## 🚀 快速开始 / Quick Start

1. 下载 `ErrorShop` jar。  
   Download the `ErrorShop` jar.
2. 放进服务器 `plugins/` 文件夹。  
   Put it into the server `plugins/` folder.
3. 启动服务器生成默认配置。  
   Start the server once to generate default files.
4. 修改你需要的配置文件：  
   Edit the files you need:
   - `config.yml`
   - `database.yml`
   - `lang.yml`
   - `shops/*.yml`
   - `menus/*.yml`
5. 如果你要真实扣钱，请安装 Vault 和经济插件，并把 `currency.provider` 改成 `vault`。  
   If you want real economy transactions, install Vault and an economy plugin, then set `currency.provider` to `vault`.
6. 重启服务器或使用 `/errorshop reload` 重载配置。  
   Restart the server or use `/errorshop reload` to reload configuration.

## ⌨️ 命令 / Commands

| 命令 / Command | 说明 / Description |
| --- | --- |
| `/errorshop reload` | 重载配置 / Reload configuration |
| `/errorshop shop <id>` | 打开指定官方商店 / Open a server shop |
| `/errorshop market [关键词]` | 打开或搜索分页玩家市场 / Open or search the paged player market |
| `/errorshop sell <price>` | 把手上的物品按指定价格上架 / List the item in your hand for sale |
| `/errorshop claim` | 领取背包满时安全暂存的物品 / Claim items queued while the inventory was full |
| `/errorshop menu <id>` | 打开指定自定义菜单 / Open a custom menu |

## 🔐 权限 / Permissions

| 权限 / Permission | 说明 / Description |
| --- | --- |
| `errorshop.reload` | 允许重载插件配置 / Allows reloading plugin configuration |
| `errorshop.shop.<id>` | 允许打开指定商店，例如 `errorshop.shop.default` / Allows opening a specific shop |
| `errorshop.menu.<id>` | 允许打开指定菜单，例如 `errorshop.menu.main` / Allows opening a specific menu |
| `errorshop.market.sell` | 允许上架玩家市场商品 / Allows listing items on the player market |
| `errorshop.market.buy` | 允许打开市场并购买商品 / Allows opening the market and buying items |
| `errorshop.market.cancel` | 允许下架自己发布的市场商品 / Allows cancelling your own market listings |
| `errorshop.market.claim` | 允许手动领取暂存物品，默认所有玩家拥有 / Allows manual queued-item claims; granted by default |

## 🧷 config.yml 怎么配置 / How to configure config.yml

`config.yml` 是主配置，主要控制默认打开哪个商店、默认打开哪个菜单、市场限制和货币模式。

`config.yml` is the main configuration file. It controls the default shop, default menu, player market limit, and economy mode.

```yaml
settings:
  default-shop: default
  default-menu: main

market:
  backend: local # local / mysql
  min-price: 0.01
  max-price: 1000000000
  price-decimals: 2
  tax-rate: 0.0
  reservation-timeout-seconds: 60
  max-listings-per-player: 20
  groups:
    default:
      slots: 9
    vip:
      permission: "errorshop.market.vip"
      slots: 18

currency:
  provider: none

cluster:
  enabled: false
  group: "default"
  server-id: "survival-1"
  redis:
    enabled: false
    host: "localhost"
    port: 6379
    channel: "errorshop:market"

database:
  mysql:
    host: "localhost"
    port: 3306
    database: "errorshop"
    username: "root"
    password: ""
    table-prefix: "errorshop_"
```

### `settings.default-shop`

默认商店 ID。玩家输入 `/errorshop shop` 且没有指定商店 ID 时，会打开这里配置的商店。默认值是 `default`，对应 `shops/default.yml`。

Default shop ID. When a player runs `/errorshop shop` without an ID, this shop will be opened. The default value is `default`, which points to `shops/default.yml`.

### `settings.default-menu`

默认菜单 ID。玩家输入 `/errorshop menu` 且没有指定菜单 ID 时，会打开这里配置的菜单。默认值是 `main`，对应 `menus/main.yml`。

Default menu ID. When a player runs `/errorshop menu` without an ID, this menu will be opened. The default value is `main`, which points to `menus/main.yml`.

### `market.max-listings-per-player`

每个玩家最多能在玩家市场里同时上架多少个商品。默认是 `20`。

Maximum number of active player market listings per player. The default is `20`.

小服可以调低，避免市场刷屏：

For small servers, you can lower it to keep the market clean:

```yaml
market:
  max-listings-per-player: 10
```

交易活跃的服务器可以调高：

For active trading servers, you can raise it:

```yaml
market:
  max-listings-per-player: 50
```


### `market.backend`

玩家市场后端。默认 `local` 使用本地 YAML 文件，适合单服、测试服和不想配置数据库的小服。

Player market backend. The default `local` mode uses local YAML storage, which is best for single-server setups and quick testing.

如果你在群组服里想让多个子服共享同一个玩家市场，可以开启群组模式并切换为 MySQL：

For a server group/network where multiple servers should share one player market, enable cluster mode and switch the backend to MySQL:

```yaml
market:
  backend: mysql
cluster:
  enabled: true
  group: "survival"
  server-id: "survival-1"
database:
  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "errorshop"
    username: "errorshop"
    password: "change-this"
    table-prefix: "errorshop_"
```

MySQL 模式会把上架商品、商品状态、离线卖家收益和买家待领取商品写入数据库。购买时会先把商品从 `ACTIVE` 锁定为 `LOCKED`，只有一个服务器能锁定成功，用来降低跨服重复购买风险。

MySQL mode stores listings, listing status, pending seller earnings, and queued buyer deliveries in the database. A purchase first reserves the listing by moving it from `ACTIVE` to `LOCKED`, then commits the sold state, buyer delivery, and seller earnings in one database transaction. Locks older than `market.reservation-timeout-seconds` are recovered automatically.

### 市场价格与税率 / Market prices and tax

```yaml
market:
  min-price: 0.01
  max-price: 1000000000
  price-decimals: 2
  tax-rate: 0.05
```

`min-price`、`max-price` 和 `price-decimals` 会拒绝 `Infinity`、超大金额及小数位过多的价格；旧数据中不合规的商品仍可由卖家下架，但买家无法购买。`tax-rate: 0.05` 表示收取 5% 市场税，卖家实收金额会在上架和售出通知中明确显示。

`min-price`, `max-price`, and `price-decimals` reject `Infinity`, oversized values, and excessive decimal places. Invalid legacy listings remain cancellable by their owners but cannot be purchased. `tax-rate: 0.05` charges 5%, and notifications show the seller's net earnings.

### `market.groups`

权限组上架数量限制。玩家匹配多个权限组时，取最大的 `slots`。

Permission-based listing limits. If a player matches multiple groups, the largest `slots` value is used.

```yaml
market:
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

### `cluster.redis`

Redis 是群组市场的事件总线，不是商品数据源。商品数据仍以 MySQL 为准。Redis 用于广播上架、购买等市场事件，并通过 `group` 和 `server-id` 过滤自己服务器或其它群组的消息。

Redis is the event bus for group markets, not the source of listing data. MySQL remains the source of truth. Redis broadcasts market events such as listing creation and purchase, while `group` and `server-id` are used to filter events from the same server or from other groups.

```yaml
cluster:
  enabled: true
  group: "survival"
  server-id: "survival-1"
  redis:
    enabled: true
    host: "127.0.0.1"
    port: 6379
    password: ""
    database: 0
    channel: "errorshop:market"
```

说明：当前 Redis 主要用于事件通知和后续界面刷新扩展，市场列表读取仍来自后端存储。正式服启用前建议在测试环境验证 MySQL 和 Redis 连接。

Note: Redis currently acts mainly as event notification and a foundation for future UI refresh behavior. Market listings are still loaded from the backend storage. Test MySQL and Redis connectivity before enabling it on a production network.

### `currency.provider`

货币模式。`none` 表示不接入 Vault 经济，适合先测试菜单、商店显示和市场流程。

Economy mode. `none` means Vault economy is not used. This is useful when you only want to test menus, shop display, and market flow.

```yaml
currency:
  provider: none
```

`vault` 表示使用 Vault 经济。你需要同时安装 Vault 和一个 Vault 兼容经济插件，例如 EssentialsX Economy。

`vault` means ErrorShop will use Vault economy. You also need Vault and a Vault-compatible economy plugin, such as EssentialsX Economy.

Paper 1.21.11 + Vault + XConomy 使用 UUID/`OfflinePlayer` 接口入账，卖家不在线或位于其它子服时也会尝试直接到账；失败时收益进入持久化待结算队列。

On Paper 1.21.11, Vault + XConomy payouts use the UUID/`OfflinePlayer` API. ErrorShop also declares XConomy as a soft dependency so its economy service initializes first. Offline or cross-server sellers are credited directly when possible, with persistent pending earnings as fallback.

```yaml
currency:
  provider: vault
```

启用 Vault 后：

When Vault is enabled:

- 玩家购买官方商店物品会扣钱。 / Buying from server shops costs money.
- 玩家购买市场商品会扣买家的钱。 / Buying market listings costs the buyer money.
- 插件会直接尝试向卖家账户入账，包括经济插件支持的离线账户。 / The plugin attempts to credit the seller account directly, including offline accounts supported by the economy provider.
- 如果经济插件暂时拒绝入账，收益会安全记录下来，并在卖家上线时重试。 / If the economy provider temporarily rejects the deposit, the earnings are saved and retried when the seller joins.

## 🗄️ database.yml 怎么配置 / How to configure database.yml

`database.yml` 用于本地市场文件配置。群组市场的 MySQL 连接项在 `config.yml` 的 `database.mysql` 中配置。

`database.yml` controls local market file storage. Group-market MySQL connection settings are configured under `database.mysql` in `config.yml`.

```yaml
storage:
  yaml:
    file: market.yml
```

### `storage.yaml.file`

玩家市场数据文件名。默认推荐 `market.yml`，通常生成在 `plugins/ErrorShop/market.yml`。

The player market data file name. The recommended default is `market.yml`, usually located at `plugins/ErrorShop/market.yml`.

这个文件会保存：

This file stores:

- 玩家市场上架的商品 / Player market listings
- 卖家 UUID / Seller UUIDs
- 卖家名字 / Seller names
- 商品价格 / Listing prices
- 离线待领取收益 / Pending offline seller earnings

不建议手动编辑 `market.yml`，除非你知道自己在改什么。

Do not edit `market.yml` manually unless you know exactly what you are changing.

## 💬 lang.yml 怎么配置 / How to configure lang.yml

`lang.yml` 用来修改玩家看到的提示文字。

`lang.yml` controls messages shown to players.

```yaml
prefix: "&8[&aErrorShop&8] &f"
listing-limit: "&c你上架的商品已经达到上限。"
vault-missing: "&c没有找到经济插件，无法完成交易。"
not-enough-money: "&c你的余额不足。"
sell-success: "&a已上架，价格：&e{price}"
market-bought: "&a购买成功，价格：&e{price}"
```

说明：

Notes:

- `&a`、`&c`、`&e` 是传统 Minecraft 颜色代码。 / `&a`, `&c`, `&e` are legacy Minecraft color codes.
- `{price}` 会被替换成价格。 / `{price}` will be replaced with the price.
- `prefix` 会加在大多数消息前面。 / `prefix` is added before most plugin messages.

## 🎨 MiniMessage 支持 / MiniMessage support

ErrorShop 0.14 开始支持 MiniMessage 文本格式，同时兼容传统 `&` 颜色代码。

Starting from ErrorShop 0.14, MiniMessage text formatting is supported while legacy `&` color codes remain compatible.

你可以在 `lang.yml`、商店名称、物品名称、lore、菜单标题和菜单文本里使用：

You can use MiniMessage in `lang.yml`, shop names, item names, lore, menu titles, and menu text:

```yaml
prefix: "<gray>[<green>ErrorShop</green>]</gray> "
listing-limit: "<red>你上架的商品已经达到上限。</red>"
sell-success: "<green>已上架，价格：</green><yellow>{price}</yellow>"
```

也可以继续使用旧写法：

You can also keep using the old format:

```yaml
prefix: "&8[&aErrorShop&8] &f"
listing-limit: "&c你上架的商品已经达到上限。"
```

推荐新配置使用 MiniMessage，老配置不用立刻改。

MiniMessage is recommended for new configurations, but existing configs do not need to be changed immediately.


## 💎 点券与双经济 / Points and dual economy

ErrorShop 0.14 开始支持 PlayerPoints 点券，并支持和 PixelShop 类似的双经济价格。

Starting from ErrorShop 0.14, PlayerPoints is supported. Shop items can use money, points, or both currencies like PixelShop.

启用点券：

Enable points:

```yaml
points:
  enabled: true
  provider: playerpoints
  name: 点券
```

商品配置示例：

Shop item example:

```yaml
items:
  diamond:
    material: DIAMOND
    name: "&b钻石"
    buy: 100.0
    points: 5
    currency-mode: and
    amount: 1
```

字段说明：

- `buy`：Vault 金币价格，`0` 表示不需要金币。
- `points`：点券价格，`0` 表示不需要点券。
- `currency-mode: and`：两种货币都需要满足并同时扣除。
- `currency-mode: or`：金币或点券满足其中一种即可购买，优先扣金币，金币不足时尝试扣点券。

Fields:

- `buy`: Vault money cost. `0` means money is not required.
- `points`: points cost. `0` means points are not required.
- `currency-mode: and`: both currencies are required and charged together.
- `currency-mode: or`: either money or points can be used; money is tried first, then points.

## 🏪 shops/ 商店怎么写 / How to write shops/

一个商店就是 `shops/` 里的一个 yml 文件。

A shop is one YAML file inside the `shops/` folder.

```yaml
title: "&6服务器商店"
permission: "errorshop.shop.default"
items:
  stone:
    material: STONE
    name: "&f石头"
    lore:
      - "&7基础建筑材料"
    buy: 10.0
    amount: 16
```

| 字段 / Field | 说明 / Description |
| --- | --- |
| `title` | 商店 GUI 标题 / Shop GUI title |
| `permission` | 打开这个商店需要的权限 / Permission required to open this shop |
| `items.<id>.material` | 物品材质，使用 Bukkit 材质名 / Item material using Bukkit material names |
| `items.<id>.name` | 显示名称 / Display name |
| `items.<id>.lore` | 物品描述 / Item lore |
| `items.<id>.buy` | Vault 金币价格；`0` 表示不需要金币 / Vault money price; `0` means no money is required |
| `items.<id>.sell` | 预留字段；当前版本尚未启用官方商店回收 / Reserved; server-shop sell-back is not enabled yet |
| `items.<id>.amount` | 每次购买/显示的数量 / Amount shown or bought each time |

注意：`/errorshop sell <price>` 是把主手物品上架到玩家市场，不是卖给官方商店。

Note: `/errorshop sell <price>` lists the held item on the player market; it does not sell the item back to a server shop.

## 🧩 menus/ 菜单怎么写 / How to write menus/

菜单文件放在 `menus/` 文件夹。

Menu files are placed inside the `menus/` folder.

```yaml
title: "&a主菜单"
layout:
  - "#########"
  - "#S##M##H#"
  - "#########"
items:
  S:
    material: EMERALD
    name: "&a服务器商店"
    lore:
      - "&7点击打开商店"
    left:
      - "[shop] default"
  M:
    material: CHEST
    name: "&b玩家市场"
    lore:
      - "&7点击打开市场"
    left:
      - "[market]"
  H:
    material: BOOK
    name: "&e帮助"
    lore:
      - "&7查看帮助信息"
```

菜单动作：

Menu actions:

| 动作 / Action | 说明 / Description |
| --- | --- |
| `[shop] <id>` | 打开指定商店 / Open a shop |
| `[market]` | 打开玩家市场 / Open the player market |
| `[menu] <id>` | 打开另一个菜单 / Open another menu |
| `[tell] <message>` | 给玩家发送一条消息 / Send a message to the player |
| `[player] <command>` | 以玩家身份执行命令 / Run a command as the player |
| `[console] <command>` | 以控制台执行命令（受配置控制）/ Run as console when enabled |

`menu.allow-console-actions: false` 会阻止所有 `[console]` 动作；设为 `true` 才允许菜单以控制台权限执行命令。

Set `menu.allow-console-actions: false` to block every `[console]` action. Console-privileged menu commands run only when this option is enabled.

## 📈 玩家市场怎么用 / How to use the player market

玩家手持物品，输入：

Hold an item and run:

```text
/errorshop sell 100
```

插件会把手里的物品按 100 的价格上架到玩家市场。

The plugin will list the item in your hand on the player market for 100.

其他玩家输入：

Other players can run:

```text
/errorshop market
/errorshop market 钻石
/errorshop claim
```

市场每页显示 45 件商品，底部箭头翻页。关键词会匹配商品材质、显示名和卖家名，并在翻页时保留。买家左键商品即可购买；卖家对自己的商品右键两次可确认下架。

The market shows 45 listings per page with navigation arrows. Searches match material, display name, and seller and remain active across pages. Buyers left-click to purchase; sellers right-click their own listing twice to cancel it. When an inventory is full, items remain safely queued; clear some space and run `/errorshop claim` to retrieve them.

注意：

Notes:

- 玩家需要 `errorshop.market.sell` 才能上架。 / Players need `errorshop.market.sell` to list items.
- 玩家需要 `errorshop.market.buy` 才能打开市场购买。 / Players need `errorshop.market.buy` to browse and buy.
- 玩家不能购买自己的商品，但可右键两次安全下架并取回物品。 / Players cannot buy their own listings, but can right-click twice to safely cancel and reclaim them.
- 背包满时物品会安全暂存；清出空间后输入 `/errorshop claim`，也可重新登录触发自动发放。 / Full-inventory items stay queued; run `/errorshop claim` after clearing space, or reconnect for automatic delivery.
- 每个玩家上架数量受 `market.max-listings-per-player` 控制。 / Listing count is controlled by `market.max-listings-per-player`.


## 🌐 群组市场 / Group market

ErrorShop 0.16 增加了面向群组服的玩家市场能力。它的设计思路是：官方商店和自定义菜单继续由各服务器本地 YAML 控制，玩家市场可以选择共享到 MySQL，并通过 Redis 广播市场事件。

ErrorShop 0.16 adds group-server player-market support. Server shops and custom menus remain local YAML configuration, while the player market can be shared through MySQL and market events can be broadcast through Redis.

### 能做到什么 / What it can do

- 多个子服共享同一个玩家市场列表。  
  Multiple servers can share the same player-market listing list.
- 上架、购买、售出、下架状态存入 MySQL。  
  Listing creation, purchase, sold status, and removed status are stored in MySQL.
- 购买前先锁定商品，降低多服同时购买造成重复成交的风险。  
  Listings are locked before purchase to reduce duplicate sales across servers.
- 卖家离线或不在本服时，收益会进入待领取记录。  
  Seller earnings can be stored when the seller is offline or on another server.
- 买家背包满时，商品会进入待领取队列。  
  If the buyer inventory is full, the item is queued for later delivery.
- 支持按权限组设置不同市场上架数量。  
  Permission groups can define different listing limits.
- Redis 可广播市场事件，并按 `cluster.group` 和 `cluster.server-id` 过滤消息。  
  Redis can broadcast market events and filter messages by `cluster.group` and `cluster.server-id`.

### 推荐配置 / Recommended setup

```yaml
market:
  backend: mysql
  fail-policy: "disable-market"
  groups:
    default:
      slots: 9
    vip:
      permission: "errorshop.market.vip"
      slots: 18
    svip:
      permission: "errorshop.market.svip"
      slots: 27

cluster:
  enabled: true
  group: "survival"
  server-id: "survival-1" # 每个子服必须不同 / unique per server
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

每个子服都使用同一个 MySQL 数据库；`cluster.group` 相同的服务器共享同一组市场事件；`cluster.server-id` 每个子服必须不同。

All servers should use the same MySQL database. Servers with the same `cluster.group` share the same event group. `cluster.server-id` must be unique per server.

### 上线前建议测试 / Recommended tests before production

- A 服上架，B 服能看到。  
  List on server A and verify it appears on server B.
- A/B 两服同时点击购买同一商品，只能成功一次。  
  Buy the same listing from A and B at the same time; only one purchase should succeed.
- 卖家在另一个子服或离线时，收益能正确待领取。  
  Seller earnings should be claimable when the seller is offline or on another server.
- 买家背包满时，待领取商品不会丢失。  
  Full-inventory purchases should be queued and not lost.
- MySQL 或 Redis 断开时，插件不会复制物品或破坏市场数据。  
  MySQL/Redis disconnects should not duplicate items or corrupt market data.

## 🔌 PlaceholderAPI 变量 / PlaceholderAPI placeholders

当前版本没有提供确认可用的 ErrorShop 自定义 PAPI 变量。

The current version does not provide confirmed custom ErrorShop PlaceholderAPI placeholders.

也就是说，目前不要写类似：

Do not use placeholders like this unless a later version explicitly adds them:

```text
%errorshop_xxx%
```

如果你的服务器已经安装 PlaceholderAPI，其它插件提供的变量仍然可以按你自己的菜单/文本系统使用，但 ErrorShop 当前没有公开自己的变量列表。

If your server already uses PlaceholderAPI, placeholders from other plugins can still be used by your own menu or text systems. ErrorShop simply does not publish its own placeholder list at the moment.

## 🇬🇧 English Quick Reference

### Main config

```yaml
settings:
  default-shop: default
  default-menu: main

market:
  backend: local
  min-price: 0.01
  max-price: 1000000000
  price-decimals: 2
  tax-rate: 0.0
  max-listings-per-player: 20

currency:
  provider: none

cluster:
  enabled: false
```

- `settings.default-shop`: shop opened by `/errorshop shop` when no ID is provided.
- `settings.default-menu`: menu opened by `/errorshop menu` when no ID is provided.
- `market.backend`: use `local` for single-server YAML storage, or `mysql` for group-market storage.
- `market.max-listings-per-player`: maximum active market listings per player.
- `market.groups`: permission-based listing limits.
- `market.min-price`, `market.max-price`, and `market.price-decimals`: listing price validation.
- `market.tax-rate`: seller fee from `0` to `1` (`0.05` = 5%).
- `cluster.enabled`: enable group-server market mode.
- `cluster.redis`: optional Redis event bus for market notifications.
- `currency.provider`: use `none` for preview/testing, or `vault` to use Vault economy.

### Storage config

```yaml
storage:
  yaml:
    file: market.yml
```

This file stores player market listings and pending seller earnings.

### Player market

```text
/errorshop sell 100
/errorshop market
/errorshop market diamond
/errorshop claim
```

Players need:

- `errorshop.market.sell` to list items
- `errorshop.market.buy` to browse and buy market items
- `errorshop.market.claim` to retrieve queued items after clearing inventory space

### MiniMessage

ErrorShop 0.14 supports MiniMessage while keeping legacy `&` color code compatibility.

### PlaceholderAPI

ErrorShop 0.14 does not currently provide confirmed custom `%errorshop_*%` placeholders.

## 📌 版本更新记录 / Version History

<details open>
<summary><strong>v0.18.1 - 2026-07-26 CST</strong></summary>

- 新增 `/errorshop claim`，玩家清出背包空间后可主动领取购买或下架时暂存的物品。
- 新增 `errorshop.market.claim` 权限（默认所有玩家拥有），并补充领取中、领取成功、队列为空和背包仍满提示。
- 手动领取复用异步存储、同步背包操作和玩家操作互斥，兼容本地 YAML 与 MySQL 市场。

English:

- Added `/errorshop claim` so players can retrieve items queued by full-inventory purchases or listing cancellations.
- Added the default-granted `errorshop.market.claim` permission and clear processing, success, empty-queue, and still-full messages.
- Manual claims reuse asynchronous storage, synchronized inventory access, and per-player operation locking for both YAML and MySQL markets.

</details>

<details>
<summary><strong>v0.18 - 2026-07-24 CST</strong></summary>

- 重构购买流程：锁定后扣款，成交状态、买家待发物品与卖家税后收益原子提交；仅在成交未提交时退款。
- 所有市场 MySQL/YAML I/O 移至异步上下文，Vault、背包和 GUI 操作保持在 Paper 同步上下文。
- 新增价格上下限、小数位校验，拒绝 `Infinity`、超大价格和不合规旧商品。
- 新增 45 格分页与 `/errorshop market <关键词>` 搜索，并在翻页时保留搜索条件。
- 官方商店点击按配置商品 ID 绑定，不再仅凭材质匹配；`market.tax-rate` 与 `menu.allow-console-actions` 正式生效。
- 优化购买、退款、待结算、待发货、下架和异步重载通知；已在 Paper 1.21.11 + Vault 1.7.3 + XConomy 2.26.3 完成干净启动验证。

English:

- Reworked purchases so charging follows reservation and sale state, buyer delivery, and net seller earnings commit atomically; refunds occur only before a sale commits.
- Moved all MySQL/YAML market I/O off the Paper main thread while keeping Vault, inventory, and GUI calls synchronized.
- Added bounded decimal price validation that rejects `Infinity`, oversized prices, and invalid legacy listings.
- Added 45-slot pagination and `/errorshop market <query>` search with query retention across pages.
- Bound server-shop clicks to configured item IDs and enforced `market.tax-rate` and `menu.allow-console-actions`.
- Improved purchase, refund, pending payout, delivery, cancellation, and reload messages for Paper 1.21.11 + Vault/XConomy.

</details>

<details>
<summary><strong>v0.17 - 2026-07-23 CST</strong></summary>

- 新增市场商品右键两次确认下架，物品会安全返还或进入待领取队列。
- 本地 YAML 与 MySQL 后端均增加归属校验；MySQL 下架、状态更新和返还入队在同一事务完成。
- 修复市场界面列表变化时可能点击到其他商品的问题，购买和下架均按界面快照中的商品 ID 处理。
- 优化中文交易通知，新增买卖双方、待入账、下架确认和待领取数量提示。
- 新增 GitHub Actions Java 21 云端构建和 JAR artifact。

English:

- Added double-right-click confirmation for cancelling listings, with safe inventory or delivery-queue returns.
- Added ownership checks to both backends; MySQL cancellation, status update, and queued return run in one transaction.
- Fixed stale market slots by resolving purchases and cancellations from listing IDs captured when the menu opens.
- Improved Chinese transaction notifications for buyers, sellers, pending earnings, cancellation, and deliveries.
- Added Java 21 GitHub Actions builds with downloadable JAR artifacts.

</details>

<details>
<summary><strong>v0.16.1 - 2026-07-23 CST</strong></summary>

- 修复市场购买后离线卖家或其他子服卖家未直接收到金币的问题。
- 修复 Vault 反射调用可能误选字符串重载、导致入账静默失败的问题。
- 使用 Paper 1.21.11 API 编译，并确认入账路径兼容 XConomy 的 Vault `OfflinePlayer` 接口。

English:

- Fixed market purchases not directly crediting sellers who are offline or on another server.
- Fixed Vault reflection selecting an incompatible string overload and silently failing deposits.
- Compiled against the Paper 1.21.11 API and verified the payout path against XConomy's Vault `OfflinePlayer` API.

</details>

<details>
<summary><strong>v0.16 - 2026-05-14 CST</strong></summary>

- 新增 MySQL 玩家市场后端，用于群组服共享玩家市场。
- 新增 Redis Pub/Sub 市场事件广播，用于上架、购买等跨服事件通知。
- 新增商品购买锁定流程：购买前将商品从 `ACTIVE` 锁定为 `LOCKED`，降低多服重复购买风险。
- 新增数据库版离线卖家收益和买家待领取商品队列。
- 保留本地 YAML 市场作为默认模式，小服和单服仍可零数据库使用。

English:

- Added MySQL-backed player-market storage for server groups.
- Added Redis Pub/Sub market event broadcasting for listing and purchase events.
- Added listing reservation: purchases move listings from `ACTIVE` to `LOCKED` first to reduce duplicate cross-server purchases.
- Added database-backed pending seller earnings and queued buyer deliveries.
- Kept local YAML market mode as the default lightweight path for single-server setups.

</details>

<details>
<summary><strong>v0.15 - 2026-05-14 CST</strong></summary>

- 新增群组市场基础结构。
- 新增参考 PixelShop 分组思路的权限组市场上架数量限制。
- 新增群组、服务器 ID、Redis 配置项预留。
- 新增玩家市场后端抽象，为 MySQL + Redis 跨服市场做准备。

English:

- Added the first foundation for group-server market support.
- Added permission-based market listing slots inspired by PixelShop-style market groups.
- Added cluster, server ID, and Redis configuration fields.
- Added the market backend abstraction as preparation for MySQL + Redis cross-server markets.

</details>

<details>
<summary><strong>v0.14 - 2026-05-13 20:23 CST</strong></summary>

- 新增 PlayerPoints 点券支持。
- 新增商店商品双经济价格：金币、点券、金币+点券。
- 新增 `currency-mode: and`：两种货币都需要满足，并同时扣除。
- 新增 `currency-mode: or`：金币或点券满足一种即可购买，优先扣金币，金币不足时尝试点券。
- 新增配置：`points.enabled`、`points.provider`、`points.name`。
- 新增商品字段：`points`、`currency-mode`。
- 新增点券相关语言提示：点券插件缺失、点券不足、金币/点券都不足。
- 已完成 Paper 1.21.1 干净环境启动验证。

English:

- Added PlayerPoints support.
- Added dual-economy shop pricing: money, points, or money + points.
- Added `currency-mode: and`: both currencies are required and charged together.
- Added `currency-mode: or`: either money or points can be used; money is tried first, then points.
- Added config keys: `points.enabled`, `points.provider`, `points.name`.
- Added shop item fields: `points`, `currency-mode`.
- Added points-related language messages.
- Clean Paper 1.21.1 startup verified.

</details>

<details>
<summary><strong>v0.13 - 2026-05-13</strong></summary>

- 新增 MiniMessage 文本格式支持，同时保留传统 `&` 颜色代码兼容。
- 更新 GitHub Wiki、README、Modrinth 与 MineBBS 发布文档。
- 补充作者 QQ 与 ErrorShop 交流群。
- 已完成 Paper 1.21.1 干净环境启动验证。

English:

- Added MiniMessage support while keeping legacy `&` color code compatibility.
- Updated GitHub Wiki, README, Modrinth, and MineBBS release copy.
- Added author QQ and ErrorShop QQ group link.
- Clean Paper 1.21.1 startup verified.

</details>

<details>
<summary><strong>v0.12 - 2026-05-13</strong></summary>

- 重写发布页为自然的用户手册风格。
- 扩展兼容版本到 1.20.6 与 1.21.x。
- 补充完整配置说明、命令、权限、市场上架限制与 PAPI 状态。
- 已完成 Paper 1.21.1 干净环境启动验证。

English:

- Rewrote the release pages into a natural user-guide style.
- Expanded compatibility metadata to 1.20.6 and 1.21.x.
- Added full config explanations, commands, permissions, listing limit notes, and PAPI status.
- Clean Paper 1.21.1 startup verified.

</details>

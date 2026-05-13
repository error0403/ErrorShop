# ErrorShop - 轻量商店、玩家市场和自定义菜单

## 📮 作者联系 / Contact the Author

作者 QQ：`1955008190`

ErrorShop 交流群：<https://qm.qq.com/q/bG3ooHYT3q>

如果你在使用 ErrorShop 时遇到问题、发现 Bug，或者有功能建议，可以通过 QQ、交流群或 GitHub Issues 联系作者。欢迎反馈 Bug 和改进建议，插件会持续维护，基本每天都会修复和更新。

Author QQ: `1955008190`

ErrorShop QQ Group: <https://qm.qq.com/q/bG3ooHYT3q>

If you run into issues, find bugs, or have feature suggestions, you can contact the author through QQ, the QQ group, or GitHub Issues. Bug reports and suggestions are welcome. The plugin is actively maintained and updated frequently.



ErrorShop 是一个给 Paper 服务器用的轻量商店插件。你可以用它做服务器官方商店、玩家交易市场，也可以用 YAML 写一些简单的菜单入口。

ErrorShop is a lightweight shop plugin for Paper servers. You can use it for server shops, player-to-player market listings, and simple YAML-based GUI menus.

## ✨ 功能特点 / Features

- 🏪 官方商店：把商品写在 `shops/` 文件夹里，玩家可以打开商店购买或出售物品。  
  Server shops: define items in the `shops/` folder so players can buy or sell items in a GUI.
- 📈 玩家市场：玩家可以把手上的物品按指定价格上架，其他玩家可以购买。  
  Player market: players can list the item in their hand for a price, and other players can buy it.
- 🧩 自定义菜单：用 `menus/` 配置简单 GUI 菜单，可以作为传送、商店入口或功能导航。  
  Custom menus: create simple YAML GUI menus for navigation, shop entrances, or server features.
- 💬 文本可改：大部分提示都在 `lang.yml`，方便改成你服务器自己的风格。  
  Editable messages: most messages are in `lang.yml`, so you can match your server style.
- 🎨 MiniMessage：支持 MiniMessage，同时兼容旧的 `&` 颜色代码。  
  MiniMessage support: modern MiniMessage formatting is supported, while legacy `&` color codes still work.
- 💰 Vault 可选：需要真实经济时接 Vault，不接也可以先测试菜单和配置。  
  Optional Vault economy: use Vault for real money transactions, or keep it disabled while testing menus and config.
- 🌐 群组市场：可选 MySQL 共享玩家市场数据，并用 Redis 广播市场事件，适合生存群组、子服网络和多节点市场。  
  Group market: optionally share player-market data through MySQL and broadcast market events through Redis, suitable for survival networks and multi-server groups.
- 🔒 防重复购买：MySQL 模式下购买会先锁定商品，降低多服同时点击导致重复成交的风险。  
  Double-buy protection: MySQL mode reserves a listing before purchase, reducing duplicate sales when multiple servers click at the same time.
- 🎁 离线收益与待领取：卖家离线时收益会暂存，买家背包满时商品会进入待领取队列。  
  Offline earnings and queued delivery: seller earnings can be stored while offline, and full-inventory purchases are queued for later delivery.

## ✅ 支持版本 / Compatibility

- Minecraft Java Edition
- Paper / compatible Paper forks
- Recommended Java: 21
- Target versions: `1.20.6`, `1.21`, `1.21.1`, `1.21.2`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.6`, `1.21.7`, `1.21.8`

说明：当前构建已在干净 Paper 1.21.1 环境启动验证。其它 1.20.6/1.21.x 版本按 Paper API 兼容范围标注，正式服使用前建议先在测试服加载一次。

Note: the current build has been startup-tested on a clean Paper 1.21.1 server. Other 1.20.6/1.21.x versions are listed based on Paper API compatibility; please test once on a staging server before using it on a production server.

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
| `/errorshop market` | 打开玩家市场 / Open the player market |
| `/errorshop sell <price>` | 把手上的物品按指定价格上架 / List the item in your hand for sale |
| `/errorshop menu <id>` | 打开指定自定义菜单 / Open a custom menu |

## 🔐 权限 / Permissions

| 权限 / Permission | 说明 / Description |
| --- | --- |
| `errorshop.reload` | 允许重载插件配置 / Allows reloading plugin configuration |
| `errorshop.shop.<id>` | 允许打开指定商店，例如 `errorshop.shop.default` / Allows opening a specific shop |
| `errorshop.menu.<id>` | 允许打开指定菜单，例如 `errorshop.menu.main` / Allows opening a specific menu |
| `errorshop.market.sell` | 允许上架玩家市场商品 / Allows listing items on the player market |
| `errorshop.market.buy` | 允许打开市场并购买商品 / Allows opening the market and buying items |

## 🧷 config.yml 怎么配置 / How to configure config.yml

`config.yml` 是主配置，主要控制默认打开哪个商店、默认打开哪个菜单、市场限制和货币模式。

`config.yml` is the main configuration file. It controls the default shop, default menu, player market limit, and economy mode.

```yaml
settings:
  default-shop: default
  default-menu: main

market:
  backend: local # local / mysql
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

MySQL mode stores listings, listing status, pending seller earnings, and queued buyer deliveries in the database. A purchase first reserves the listing by moving it from `ACTIVE` to `LOCKED`, so only one server can proceed and duplicate cross-server purchases are reduced.

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

```yaml
currency:
  provider: vault
```

启用 Vault 后：

When Vault is enabled:

- 玩家购买官方商店物品会扣钱。 / Buying from server shops costs money.
- 玩家购买市场商品会扣买家的钱。 / Buying market listings costs the buyer money.
- 卖家在线时会直接收到钱。 / Online sellers receive money directly.
- 卖家不在线时，收益会先记录下来，等玩家上线后尝试发放。 / Offline seller earnings are saved and paid when the seller comes online.

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
    sell: 2.0
    amount: 16
```

| 字段 / Field | 说明 / Description |
| --- | --- |
| `title` | 商店 GUI 标题 / Shop GUI title |
| `permission` | 打开这个商店需要的权限 / Permission required to open this shop |
| `items.<id>.material` | 物品材质，使用 Bukkit 材质名 / Item material using Bukkit material names |
| `items.<id>.name` | 显示名称 / Display name |
| `items.<id>.lore` | 物品描述 / Item lore |
| `items.<id>.buy` | 购买价格，`0` 表示不可购买 / Buy price; `0` means not buyable |
| `items.<id>.sell` | 出售价格，`0` 表示不可出售 / Sell price; `0` means not sellable |
| `items.<id>.amount` | 每次购买/显示的数量 / Amount shown or bought each time |

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
```

打开市场后点击商品即可购买。

They can open the market and click a listing to buy it.

注意：

Notes:

- 玩家需要 `errorshop.market.sell` 才能上架。 / Players need `errorshop.market.sell` to list items.
- 玩家需要 `errorshop.market.buy` 才能打开市场购买。 / Players need `errorshop.market.buy` to browse and buy.
- 玩家不能购买自己上架的商品。 / Players cannot buy their own listings.
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
```

Players need:

- `errorshop.market.sell` to list items
- `errorshop.market.buy` to browse and buy market items

### MiniMessage

ErrorShop 0.14 supports MiniMessage while keeping legacy `&` color code compatibility.

### PlaceholderAPI

ErrorShop 0.14 does not currently provide confirmed custom `%errorshop_*%` placeholders.

## 📌 版本更新记录 / Version History


<details open>
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

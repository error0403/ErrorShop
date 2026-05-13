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
  max-listings-per-player: 20

currency:
  provider: none
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

`database.yml` 目前主要用于配置玩家市场数据文件位置。

`database.yml` currently controls where player market data is stored.

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

ErrorShop 0.13 开始支持 MiniMessage 文本格式，同时兼容传统 `&` 颜色代码。

Starting from ErrorShop 0.13, MiniMessage text formatting is supported while legacy `&` color codes remain compatible.

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
  max-listings-per-player: 20

currency:
  provider: none
```

- `settings.default-shop`: shop opened by `/errorshop shop` when no ID is provided.
- `settings.default-menu`: menu opened by `/errorshop menu` when no ID is provided.
- `market.max-listings-per-player`: maximum active market listings per player.
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

ErrorShop 0.13 supports MiniMessage while keeping legacy `&` color code compatibility.

### PlaceholderAPI

ErrorShop 0.13 does not currently provide confirmed custom `%errorshop_*%` placeholders.

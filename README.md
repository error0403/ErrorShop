# ErrorShop - 轻量商店、玩家市场和自定义菜单

ErrorShop 是一个给 Paper 服务器用的轻量商店插件。你可以用它做服务器官方商店、玩家交易市场，也可以用 YAML 写一些简单的菜单入口。

ErrorShop is a lightweight shop plugin for Paper servers. It can be used for server shops, player market listings, and simple YAML-based custom menus.

## ✨ 功能特点 / Features

- 🏪 官方商店：把商品写在 `shops/` 文件夹里，玩家用命令打开购买。
- 📈 玩家市场：玩家可以把手上的物品按指定价格上架，其他玩家可以购买。
- 🧩 自定义菜单：用 `menus/` 配置简单 GUI 菜单，可以作为传送、商店入口或功能导航。
- 💬 文本可改：大部分提示都在 `lang.yml`，方便改成你服务器自己的风格。
- 🗂️ 存储独立：市场数据文件位置在 `database.yml` 里配置。
- 💰 Vault 可选：如果启用 Vault，会走服务器经济插件；不启用时适合先预览菜单和配置。

## ✅ 支持版本 / Compatibility

- Minecraft Java Edition
- Paper / compatible Paper forks
- 推荐 Java 21
- 版本目标：`1.20.6`、`1.21`、`1.21.1`、`1.21.2`、`1.21.3`、`1.21.4`、`1.21.5`、`1.21.6`、`1.21.7`、`1.21.8`

> 说明：当前构建已在干净 Paper 1.21.1 环境启动验证。其它 1.20.6/1.21.x 版本按 Paper API 兼容范围标注，正式服使用前建议先在测试服加载一次。

## 🚀 快速开始 / Quick Start

1. 下载 `ErrorShop` jar。
2. 放进服务器 `plugins/` 文件夹。
3. 启动服务器生成默认配置。
4. 修改：
   - `config.yml`
   - `database.yml`
   - `lang.yml`
   - `shops/*.yml`
   - `menus/*.yml`
5. 如果你要真实扣钱，请安装 Vault 和经济插件，并把 `currency.provider` 改成 `vault`。
6. 重启服务器或使用 `/errorshop reload` 重载配置。

## ⌨️ 命令 / Commands

| 命令 | 说明 |
| --- | --- |
| `/errorshop reload` | 重载配置 |
| `/errorshop shop <id>` | 打开指定官方商店 |
| `/errorshop market` | 打开玩家市场 |
| `/errorshop sell <price>` | 把手上的物品按指定价格上架 |
| `/errorshop menu <id>` | 打开指定自定义菜单 |

## 🔐 权限 / Permissions

| 权限 | 说明 |
| --- | --- |
| `errorshop.reload` | 允许重载插件配置 |
| `errorshop.shop.<id>` | 允许打开指定商店，例如 `errorshop.shop.default` |
| `errorshop.menu.<id>` | 允许打开指定菜单，例如 `errorshop.menu.main` |
| `errorshop.market.sell` | 允许上架玩家市场商品 |
| `errorshop.market.buy` | 允许打开市场并购买商品 |

## 🧷 config.yml 怎么配置

`config.yml` 是主配置，主要控制默认打开哪个商店、默认打开哪个菜单、市场限制和货币模式。

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
默认商店 ID。

如果玩家输入：

```text
/errorshop shop
```

没有指定商店 ID 时，就会打开这里配置的商店。默认值是 `default`，对应：

```text
shops/default.yml
```

### `settings.default-menu`
默认菜单 ID。

如果玩家输入：

```text
/errorshop menu
```

没有指定菜单 ID 时，就会打开这里配置的菜单。默认值是 `main`，对应：

```text
menus/main.yml
```

### `market.max-listings-per-player`
每个玩家最多能在玩家市场里同时上架多少个商品。

默认：

```yaml
market:
  max-listings-per-player: 20
```

意思是每个玩家最多同时上架 20 个商品。如果已经达到上限，再使用 `/errorshop sell <price>` 会提示达到上限。

你可以按服务器规模调整：

```yaml
market:
  max-listings-per-player: 10
```

适合小服，避免市场刷屏。

```yaml
market:
  max-listings-per-player: 50
```

适合玩家多、交易活跃的服务器。

### `currency.provider`
货币模式。

```yaml
currency:
  provider: none
```

`none` 表示不接入 Vault 经济，适合先测试菜单、商店显示和市场流程。

```yaml
currency:
  provider: vault
```

`vault` 表示使用 Vault 经济。你需要同时安装：

- Vault
- 一个经济插件，例如 EssentialsX Economy 或其它 Vault 兼容经济插件

启用 Vault 后：

- 玩家购买官方商店物品会扣钱。
- 玩家购买市场商品会扣买家的钱。
- 卖家在线时会直接收到钱。
- 卖家不在线时，收益会先记录下来，等玩家上线后尝试发放。

## 🗄️ database.yml 怎么配置

`database.yml` 目前主要用于配置玩家市场数据文件位置。

常见写法：

```yaml
storage:
  yaml:
    file: market.yml
```

### `storage.yaml.file`
玩家市场数据文件名。

默认推荐：

```yaml
storage:
  yaml:
    file: market.yml
```

生成位置通常是：

```text
plugins/ErrorShop/market.yml
```

这个文件会保存：

- 玩家市场上架的商品
- 卖家 UUID
- 卖家名字
- 商品价格
- 离线待领取收益

不建议手动编辑 `market.yml`，除非你知道自己在改什么。

## 💬 lang.yml 怎么配置

`lang.yml` 用来修改玩家看到的提示文字。

例如你可以改：

```yaml
prefix: "&8[&aErrorShop&8] &f"
listing-limit: "&c你上架的商品已经达到上限。"
vault-missing: "&c没有找到经济插件，无法完成交易。"
not-enough-money: "&c你的余额不足。"
sell-success: "&a已上架，价格：&e%price%"
market-bought: "&a购买成功，价格：&e%price%"
```

说明：

- `&a`、`&c`、`&e` 这类是 Minecraft 颜色代码。
- `%price%` 会被替换成价格。
- `prefix` 会加在大多数消息前面。


## 🎨 MiniMessage 支持

ErrorShop 0.13 开始支持 MiniMessage 文本格式，同时兼容传统 `&` 颜色代码。

你可以在 `lang.yml`、商店名称、物品名称、lore、菜单标题和菜单文本里使用：

```yaml
prefix: "<gray>[<green>ErrorShop</green>]</gray> "
listing-limit: "<red>你上架的商品已经达到上限。</red>"
sell-success: "<green>已上架，价格：</green><yellow>{price}</yellow>"
```

也可以继续使用旧写法：

```yaml
prefix: "&8[&aErrorShop&8] &f"
listing-limit: "&c你上架的商品已经达到上限。"
```

推荐新配置使用 MiniMessage，老配置不用立刻改。

## 🏪 shops/ 商店怎么写

一个商店就是 `shops/` 里的一个 yml 文件。

例如：

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

字段说明：

| 字段 | 说明 |
| --- | --- |
| `title` | 商店 GUI 标题 |
| `permission` | 打开这个商店需要的权限 |
| `items.<id>.material` | 物品材质，使用 Bukkit 材质名 |
| `items.<id>.name` | 显示名称 |
| `items.<id>.lore` | 物品描述 |
| `items.<id>.buy` | 购买价格，`0` 表示不可购买 |
| `items.<id>.sell` | 出售价格，`0` 表示不可出售 |
| `items.<id>.amount` | 每次购买/显示的数量 |

## 🧩 menus/ 菜单怎么写

菜单文件放在 `menus/` 文件夹。

例如：

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

| 动作 | 说明 |
| --- | --- |
| `[shop] <id>` | 打开指定商店 |
| `[market]` | 打开玩家市场 |
| `[menu] <id>` | 打开另一个菜单 |

## 📈 玩家市场怎么用

玩家手持物品，输入：

```text
/errorshop sell 100
```

就会把手里的物品按 100 的价格上架到玩家市场。

其他玩家输入：

```text
/errorshop market
```

打开市场后点击商品即可购买。

注意：

- 玩家需要 `errorshop.market.sell` 才能上架。
- 玩家需要 `errorshop.market.buy` 才能打开市场购买。
- 玩家不能购买自己上架的商品。
- 每个玩家上架数量受 `market.max-listings-per-player` 控制。

## 🔌 PlaceholderAPI 变量

当前版本没有提供确认可用的 ErrorShop 自定义 PAPI 变量。

也就是说，目前不要写类似：

```text
%errorshop_xxx%
```

这类变量，除非后续版本明确添加。

如果你的服务器已经安装 PlaceholderAPI，其它插件提供的变量仍然可以按你自己的菜单/文本系统使用，但 ErrorShop 当前没有公开自己的变量列表。

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

### PlaceholderAPI

ErrorShop 0.13 does not currently provide confirmed custom `%errorshop_*%` placeholders.

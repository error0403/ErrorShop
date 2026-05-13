# EasyShop

EasyShop 是一个面向 Paper 1.21+ 的免费商店插件，包含官方商店、全球市场和自定义菜单。

## 功能

- 官方商店：管理员通过 shops 文件夹配置商品，玩家打开商店购买。
- 全球市场：玩家上架手持物品，其他玩家浏览并购买。
- 自定义菜单：管理员通过 menus 文件夹配置菜单布局和点击动作。
- 可自定义语言：所有提示文本集中在 lang.yml。
- 独立存储配置：数据库/存储相关设置集中在 database.yml。

## 配置文件

- config.yml：插件主要配置，例如默认菜单、经济模式、市场限制。
- database.yml：数据库或本地存储设置。
- lang.yml：所有消息和提示文本。
- shops 文件夹：官方商店配置。
- menus 文件夹：自定义菜单配置。
- help 文件夹：给用户阅读的中英文指南。

## 命令

- /easyshop reload：重载配置。
- /easyshop shop <名称>：打开官方商店。
- /easyshop market：打开全球市场。
- /easyshop sell <价格>：上架手持物品。
- /easyshop menu <名称>：打开自定义菜单。

## 权限

- easyshop.reload：允许重载插件。
- easyshop.shop.<名称>：允许打开指定官方商店。
- easyshop.menu.<名称>：允许打开指定菜单。
- easyshop.market.sell：允许上架商品。
- easyshop.market.buy：允许购买市场商品。

## 经济系统

默认经济模式为 none，适合预览和测试。正式服务器建议安装 Vault 和经济插件，并在 config.yml 中切换为 vault。

## 发布状态

当前版本为 0.1.0，可编译打包，适合作为发布前预览版。后续建议增强完整 ItemStack 序列化、分页、购买确认和离线卖家收益记录。

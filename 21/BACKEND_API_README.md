# Blackjack Backend API 文档

## 概述

这是一个重构后的21点游戏后端模块，所有数据格式均为JSON安全格式，可直接用于Flask或FastAPI Web API。

## 核心模块说明

### Card (`card.py`)

卡牌类，支持JSON安全的字符串表示。

**主要方法：**
- `to_string()`: 返回JSON安全格式（如 `"AH"`, `"7D"`, `"10S"`）
- `from_string(card_str)`: 从字符串创建Card对象
- `get_value()`: 获取21点数值（1-11）

**示例：**
```python
card = Card('A', 'H')  # 或 Card('A', '♥')
print(card.to_string())  # 输出: "AH"

card2 = Card.from_string("7D")
print(card2.rank)  # 输出: "7"
print(card2.suit_letter)  # 输出: "D"
```

### Deck (`Deck.py`)

牌堆类，管理多副牌。

**主要方法：**
- `shuffle()`: 洗牌
- `draw_card()`: 抽一张牌
- `get_remaining_cards()`: 获取剩余牌（JSON安全格式列表）

### HiLoCounter (`Counter.py`)

Hi-Lo记牌系统。

**主要方法：**
- `update(card)`: 更新计数
- `get_running_count()`: 获取运行计数
- `get_true_count(decks_remaining)`: 获取真实计数

### GameState (`game_state.py`)

游戏状态管理类，核心后端逻辑。

## API 方法

### `start_new_round()`

开始新一局，自动发牌。

**返回：**
```python
{
    "player_hand": ["AH", "7D"],
    "player_total": 18,
    "dealer_hand": ["10S", "?"],
    "dealer_total": None,
    "chips": 500,
    "bet": 10,
    "running_count": 0,
    "true_count": 0.0,
    "can_hit": True,
    "can_stand": True,
    "can_double": True,
    "is_busted": False,
    "is_resolved": False,
    "result": None
}
```

### `player_hit()`

玩家要牌（抽一张牌）。

**返回：** 更新后的游戏状态字典

**注意：** 如果玩家爆牌，会自动结束并返回结果。

### `player_stand()`

玩家停牌，庄家自动玩到17点。

**返回：** 包含最终结果的游戏状态字典

### `player_double()` / `player_double_down()`

玩家加倍（将赌注翻倍，抽一张牌后自动停牌）。

**返回：** 包含最终结果的游戏状态字典

**错误：** 如果手牌不是2张，抛出 `ValueError`

### `get_status(reveal_dealer=False)`

获取当前游戏状态。

**参数：**
- `reveal_dealer`: 是否显示庄家的隐藏牌

**返回：** 完整的游戏状态字典

## 状态字典字段说明

```python
{
    "player_hand": ["AH", "7D"],           # 玩家手牌（JSON安全格式）
    "player_total": 18,                    # 玩家点数
    "dealer_hand": ["10S", "?"],           # 庄家手牌（隐藏第二张）
    "dealer_total": None,                  # 庄家点数（未reveal时为None）
    "chips": 500,                          # 当前筹码
    "bet": 10,                             # 当前下注额
    "running_count": 0,                    # Hi-Lo运行计数
    "true_count": 0.0,                     # Hi-Lo真实计数
    "can_hit": True,                       # 是否可以要牌
    "can_stand": True,                     # 是否可以停牌
    "can_double": True,                    # 是否可以加倍
    "is_busted": False,                    # 是否爆牌
    "is_resolved": False,                  # 游戏是否已结束
    "result": None                         # 结果消息（结束时才有）
}
```

## 使用示例

### 基本用法

```python
from game_state import GameState

# 初始化游戏
game = GameState(num_decks=2, initial_chips=500, bet_amount=10)

# 开始新一局
status = game.start_new_round()
print(status["player_hand"])  # 例如: ["AH", "7D"]

# 玩家要牌
status = game.player_hit()

# 玩家停牌
status = game.player_stand()
print(status["result"])  # 例如: "Player wins"

# 获取状态
status = game.get_status(reveal_dealer=True)
```

### Flask API 示例

```python
from flask import Flask, jsonify
from game_state import GameState

app = Flask(__name__)
game = GameState()

@app.route('/api/start', methods=['POST'])
def start():
    status = game.start_new_round()
    return jsonify(status)

@app.route('/api/hit', methods=['POST'])
def hit():
    status = game.player_hit()
    return jsonify(status)

@app.route('/api/stand', methods=['POST'])
def stand():
    status = game.player_stand()
    return jsonify(status)

@app.route('/api/status', methods=['GET'])
def status():
    reveal = request.args.get('reveal', 'false') == 'true'
    return jsonify(game.get_status(reveal_dealer=reveal))
```

### FastAPI 示例

```python
from fastapi import FastAPI
from game_state import GameState

app = FastAPI()
game = GameState()

@app.post("/api/start")
def start():
    return game.start_new_round()

@app.post("/api/hit")
def hit():
    return game.player_hit()

@app.post("/api/stand")
def stand():
    return game.player_stand()

@app.get("/api/status")
def status(reveal: bool = False):
    return game.get_status(reveal_dealer=reveal)
```

## 向后兼容

保留了以下旧方法（建议使用新的 `get_status()` 方法）：
- `get_player_hand()`: 返回玩家手牌字符串列表
- `get_dealer_hand(reveal=False)`: 返回庄家手牌字符串列表
- `get_running_count()`: 返回运行计数
- `get_true_count()`: 返回真实计数
- `get_chips()`: 返回筹码数

## 花色格式

- **内部使用：** Unicode符号（♠, ♥, ♦, ♣）
- **API输出：** 字母格式（S, H, D, C）
  - Spades (♠) → "S"
  - Hearts (♥) → "H"
  - Diamonds (♦) → "D"
  - Clubs (♣) → "C"

## 错误处理

- `player_double()` 在手牌不是2张时会抛出 `ValueError`
- `Deck.draw_card()` 在牌堆为空时会抛出 `ValueError`
- 所有错误消息均为英文，可直接返回给API客户端

## 注意事项

1. **状态管理：** 在生产环境中，应该为每个用户/会话创建独立的 `GameState` 实例
2. **线程安全：** 当前实现不是线程安全的，多线程环境需要加锁
3. **持久化：** 如需保存游戏状态，可以将 `get_status()` 返回的字典序列化为JSON存储


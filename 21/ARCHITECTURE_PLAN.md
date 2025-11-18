# Blackjack 训练器架构设计与迁移规划

## 📋 项目概述

- **当前阶段**: Kivy 原型开发（本地调试与逻辑验证）
- **目标阶段**: Flutter 生产版本（移动端部署）
- **核心功能**: 策略学习、胜率计算、记牌训练

---

## 🏗️ 阶段一：Kivy 原型架构重构

### 1.1 当前问题分析

**问题**:
- `mobile_app.py` 中 UI 层与业务逻辑混合（`GameState`、`advise_action`、`simulate_ev` 直接调用）
- 缺少适配器层，迁移到 Flutter 时需要大量重写
- EV 计算是 CPU 密集型，在 UI 线程中可能卡顿

**目标**:
- 实现 MVC/MVP 架构，明确分层
- UI 层通过 Controller/Presenter 间接访问业务逻辑
- 为后续 API 化做准备

### 1.2 推荐项目结构

```
21点/
├── core/                          # 核心业务逻辑层（纯 Python，无 UI 依赖）
│   ├── __init__.py
│   ├── models/
│   │   ├── __init__.py
│   │   ├── card.py               # Card 类（已有）
│   │   └── game_state.py         # GameState（重构，移除 print）
│   ├── game/
│   │   ├── __init__.py
│   │   ├── deck.py               # Deck（已有）
│   │   ├── counter.py            # HiLoCounter（已有）
│   │   └── evaluator.py          # EV 模拟（已有）
│   └── strategy/
│       ├── __init__.py
│       ├── basic_strategy.py     # strategy.py 重命名
│       └── strategy_engine.py    # 策略决策引擎（包装器）
│
├── ui_kivy/                       # Kivy UI 层（可选，原型期可直接用）
│   ├── __init__.py
│   ├── views/
│   │   ├── __init__.py
│   │   ├── game_screen.py        # 主游戏界面
│   │   └── widgets/
│   │       ├── card_widget.py    # 牌面显示组件（文本渲染）
│   │       ├── hand_panel.py     # 手牌面板
│   │       └── info_panel.py     # 信息面板（筹码、计数、建议）
│   ├── controllers/
│   │   ├── __init__.py
│   │   └── game_controller.py    # 游戏控制器（连接 UI ↔ Core）
│   └── app.py                     # Kivy App 入口
│
├── mobile_app.py                  # 临时：简化版 Kivy（保留用于快速测试）
│
├── main.py                        # 命令行版本（保留）
│
└── requirements.txt
```

### 1.3 核心模块职责分离

#### Core 层（无 UI 依赖）

```python
# core/models/game_state.py
class GameState:
    """纯业务逻辑，返回结构化数据，不打印"""
    def get_game_status(self) -> Dict:
        return {
            'player_hand': self.player_hand,
            'dealer_hand': self.dealer_hand,
            'chips': self.chips,
            'running_count': self.counter.get_running_count(),
            'true_count': self.get_true_count(),
            'can_split': self._can_split(),
            'can_double': len(self.player_hand) == 2
        }
```

```python
# core/strategy/strategy_engine.py
class StrategyEngine:
    """策略决策引擎（统一接口）"""
    def get_advice(self, player_hand, dealer_upcard) -> Dict:
        action = advise_action(player_hand, dealer_upcard)
        evs = simulate_ev(...)  # 可选：异步计算
        return {
            'action': action,
            'ev_breakdown': evs,
            'win_rate': evs.get(action, (0, 0))[0],
            'expected_value': evs.get(action, (0, 0))[1]
        }
```

#### UI 层（Kivy，通过 Controller）

```python
# ui_kivy/controllers/game_controller.py
class GameController:
    """连接 UI 与 Core，处理异步计算"""
    def __init__(self):
        self.game_state = GameState(...)
        self.strategy_engine = StrategyEngine()
    
    def start_new_round(self) -> Dict:
        self.game_state.start_new_round()
        return self._get_display_data()
    
    def _get_display_data(self) -> Dict:
        status = self.game_state.get_game_status()
        advice = self.strategy_engine.get_advice(
            status['player_hand'],
            status['dealer_hand'][0]
        )
        return {**status, 'advice': advice}
```

### 1.4 异步 EV 计算（可选优化）

EV 计算（`simulate_ev`）可能耗时 1-3 秒，建议异步：

```python
# ui_kivy/controllers/game_controller.py
from threading import Thread
from queue import Queue

class GameController:
    def __init__(self):
        self.ev_queue = Queue()
        self.ev_thread = None
    
    def get_advice_async(self, callback):
        """异步获取建议，避免 UI 卡顿"""
        def _compute():
            advice = self.strategy_engine.get_advice(...)
            self.ev_queue.put(advice)
        Thread(target=_compute).start()
        # UI 定期检查队列
```

---

## 🚀 阶段二：Flutter 迁移路径

### 2.1 模块迁移决策矩阵

| 模块 | 当前实现 | 迁移策略 | 原因 |
|------|---------|---------|------|
| **Card/Deck** | Python | ✅ **Flutter 重写** | 简单逻辑，本地实现性能更好 |
| **HiLoCounter** | Python | ✅ **Flutter 重写** | 纯数学计算，无依赖 |
| **GameState** | Python | ✅ **Flutter 重写** | 状态管理，Flutter 有 State 管理优势 |
| **Basic Strategy** | Python | ✅ **Flutter 重写** | 查表逻辑，适合本地 |
| **EV 模拟** | Python (CPU 密集) | ⚠️ **API 调用** | 计算量大，Python 更适合数值计算 |
| **Split 逻辑** | 未实现 | ✅ **Flutter 实现** | UI 交互为主 |

### 2.2 推荐方案：混合架构

**方案 A：轻量 API 服务（推荐）**

```
Flutter App (移动端)
    ↓ HTTP/gRPC
FastAPI/Flask Service (后端)
    ├── /api/evaluate_ev       # EV 计算
    ├── /api/get_advice         # 策略建议（可选，或本地实现）
    └── /api/simulate_round     # 批量模拟（高级功能）
```

**优点**:
- EV 计算在服务端，可缓存结果、优化算法
- Flutter 端保持轻量，快速响应
- 可扩展（多用户统计、云端策略更新）

**缺点**:
- 需要部署服务（开发期可用本地服务）
- 网络延迟（但 EV 计算不频繁，可接受）

**方案 B：完全本地（备选）**

- 将 EV 模拟用 Dart 重写
- 适合离线优先或性能敏感场景
- 实现成本较高（需要重写蒙特卡洛）

### 2.3 API 服务设计（FastAPI 示例）

```python
# api_service/
├── __init__.py
├── main.py                      # FastAPI app
├── routers/
│   ├── __init__.py
│   ├── evaluation.py           # EV 计算端点
│   └── strategy.py              # 策略端点
└── services/
    ├── __init__.py
    ├── evaluator_service.py     # 包装 evaluator.py
    └── strategy_service.py      # 包装 strategy.py
```

**API 端点设计**:

```python
# POST /api/evaluate_ev
{
    "player_hand": [{"rank": "A", "suit": "♠"}, ...],
    "dealer_hand": [{"rank": "10", "suit": "♥"}, ...],
    "remaining_cards": [...],    # 或仅传牌堆状态描述
    "trials": 3000
}
→ {
    "H": {"win_rate": 0.52, "ev": 0.023},
    "S": {"win_rate": 0.48, "ev": -0.015},
    "D": {"win_rate": 0.55, "ev": 0.045}
}

# GET /api/basic_strategy
{
    "player_hand": [...],
    "dealer_upcard": {...}
}
→ {"action": "D", "reason": "basic_strategy"}
```

---

## 📅 工作任务规划

### Phase 1: Kivy 架构重构（1-2 周）

#### Week 1: 核心逻辑重构
- [ ] **D1-2**: 重构 `core/` 目录结构
  - 移动 `card.py`, `Deck.py`, `Counter.py` → `core/`
  - 重构 `game_state.py`，移除 `print`，返回结构化数据
  - 创建 `StrategyEngine` 统一接口
- [ ] **D3-4**: 实现 `ui_kivy/controllers/game_controller.py`
  - 连接 Core 与 UI
  - 实现异步 EV 计算（可选）
- [ ] **D5**: 重构 `mobile_app.py` 使用 Controller
  - 验证架构是否清晰

#### Week 2: UI 组件化
- [ ] **D1-3**: 拆分 UI 组件
  - `CardWidget`, `HandPanel`, `InfoPanel`
- [ ] **D4-5**: 添加 Split 功能支持
  - 扩展 `GameState` 支持多手
  - UI 展示分牌后的多手状态

### Phase 2: API 服务开发（1 周）

#### Week 3: FastAPI 后端
- [ ] **D1-2**: 搭建 FastAPI 项目结构
  - 创建 `api_service/` 目录
  - 实现 `/api/evaluate_ev` 端点
- [ ] **D3-4**: 优化 EV 计算性能
  - 考虑缓存常见牌局组合
  - 支持批量请求
- [ ] **D5**: 本地测试 API
  - 用 Postman/curl 验证端点
  - 性能测试（响应时间 < 2s）

### Phase 3: Flutter 迁移（3-4 周）

#### Week 4-5: Flutter 基础功能
- [ ] **D1-3**: 搭建 Flutter 项目
  - 创建 `lib/models/` (Card, Deck, Counter)
  - 实现本地 GameState
- [ ] **D4-7**: UI 界面开发
  - 主游戏界面
  - 手牌展示（文本/SVG）
  - 按钮交互（Hit/Stand/Double/Split）
- [ ] **D8-10**: API 集成
  - HTTP 客户端（`http` 或 `dio`）
  - 调用 `/api/evaluate_ev`
  - 错误处理与加载状态

#### Week 6-7: 高级功能与优化
- [ ] **D1-3**: Split 功能完整实现
  - 多手管理
  - 分牌后的策略建议
- [ ] **D4-5**: 性能优化
  - 异步加载 EV 计算
  - UI 动画优化
- [ ] **D6-7**: 测试与修复
  - 单元测试核心逻辑
  - UI 测试（Widget 测试）

---

## 🎯 关键技术决策

### 1. 是否使用 API？

**建议：开发期用本地服务，生产期可选项**

- **开发期**: Flutter 连接本地 `localhost:8000`（FastAPI）
- **生产期选项**:
  - 选项 A: 打包 API 为本地服务（`pyinstaller` + 嵌入式 Python）
  - 选项 B: 云端部署 API（用户联网使用）
  - 选项 C: Flutter 本地实现 EV（Dart 重写，牺牲性能）

### 2. 数据序列化

**Card/Hand 序列化格式**:

```python
# Python → JSON
{"rank": "A", "suit": "♠"}

# Flutter → Dart
class Card {
  final String rank;
  final String suit;
}
```

### 3. 状态同步

- **方案**: 客户端维护 GameState，仅 EV 计算请求 API
- **理由**: 减少网络请求，保持 UI 响应性

---

## 📊 时间估算（Gantt 风格）

```
Week 1:  [████████████████████] Core 重构
Week 2:  [████████████████████] UI 组件化
Week 3:  [████████████████████] API 开发
Week 4-5:[████████████████████████████] Flutter 基础
Week 6-7:[████████████████████] Flutter 优化
```

**总计**: 6-7 周（按 1 人全职开发）

---

## 🚨 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| EV 计算在 Flutter 中性能差 | 中 | 优先使用 API，或降级为简化算法 |
| API 网络延迟影响体验 | 低 | 本地缓存常见组合，异步加载 |
| Split 逻辑复杂度高 | 中 | 先实现单分牌，再扩展多分 |
| Kivy → Flutter 迁移成本 | 低 | 架构分离后，UI 层独立重写 |

---

## 📝 下一步行动

1. **立即开始**: Phase 1 Week 1（核心逻辑重构）
2. **并行准备**: 学习 Flutter 基础（如果还不熟悉）
3. **决策点**: Week 3 结束时决定 API 部署方案（本地 vs 云端）

---

## 🔗 参考资料

- [Kivy 最佳实践](https://kivy.org/doc/stable/guide/best-practices.html)
- [Flutter 状态管理](https://docs.flutter.dev/development/data-and-backend/state-mgmt)
- [FastAPI 文档](https://fastapi.tiangolo.com/)
- [Blackjack 基础策略表](用户提供的图片)


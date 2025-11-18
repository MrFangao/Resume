# Blackjack 训练器开发任务清单

## ✅ Phase 1: Kivy 架构重构（目标：逻辑/UI 分离）

### 1.1 核心逻辑层重构

#### 任务 1.1.1: 创建 `core/` 目录结构
- [ ] 创建 `core/` 主目录
- [ ] 创建 `core/models/` 子目录
- [ ] 创建 `core/game/` 子目录
- [ ] 创建 `core/strategy/` 子目录
- [ ] 在每个目录添加 `__init__.py`

**验收标准**: 目录结构清晰，符合架构文档

---

#### 任务 1.1.2: 迁移基础模块到 `core/`
- [ ] 移动 `card.py` → `core/models/card.py`
- [ ] 移动 `Deck.py` → `core/game/deck.py`，更新导入
- [ ] 移动 `Counter.py` → `core/game/counter.py`，更新导入
- [ ] 更新所有引用这些模块的文件

**验收标准**: `python -m core.models.card` 可正常运行

---

#### 任务 1.1.3: 重构 `game_state.py`
- [ ] 移动 `game_state.py` → `core/models/game_state.py`
- [ ] 移除所有 `print()` 语句
- [ ] 创建 `get_game_status()` 方法，返回 Dict：
  ```python
  {
      'player_hand': [...],
      'dealer_hand': [...],
      'chips': int,
      'bet': int,
      'running_count': int,
      'true_count': float,
      'can_split': bool,
      'can_double': bool,
      'player_total': int,
      'dealer_upcard': Card
  }
  ```
- [ ] 保持 `player_hit()`, `player_stand()`, `player_double_down()` 等方法不变

**验收标准**: `main.py` 仍可正常运行（重构后更新导入）

---

#### 任务 1.1.4: 重构策略模块
- [ ] 移动 `strategy.py` → `core/strategy/basic_strategy.py`
- [ ] 创建 `core/strategy/strategy_engine.py`:
  ```python
  class StrategyEngine:
      def get_advice(self, player_hand, dealer_upcard) -> Dict:
          # 返回: {'action': 'D', 'ev_breakdown': {...}, ...}
  ```
- [ ] 将 `advise_action()` 封装到 `StrategyEngine`
- [ ] 集成 `evaluator.simulate_ev()` 到策略引擎

**验收标准**: 可通过 `StrategyEngine` 获取建议和 EV

---

#### 任务 1.1.5: 优化 EV 计算接口
- [ ] 移动 `evaluator.py` → `core/game/evaluator.py`
- [ ] 确保 `simulate_ev()` 接受序列化的 Card 列表或 Card 对象
- [ ] 添加参数验证和错误处理

**验收标准**: EV 计算返回格式统一为 `Dict[str, Tuple[float, float]]`

---

### 1.2 UI 层重构（Kivy）

#### 任务 1.2.1: 创建 UI 目录结构
- [ ] 创建 `ui_kivy/` 目录
- [ ] 创建 `ui_kivy/views/` 和 `ui_kivy/controllers/`
- [ ] 创建 `ui_kivy/views/widgets/` 子目录

---

#### 任务 1.2.2: 实现 `GameController`
- [ ] 创建 `ui_kivy/controllers/game_controller.py`
- [ ] 实现 `start_new_round()`, `player_hit()`, `player_stand()`, `player_double_down()`
- [ ] 实现 `get_display_data()` 返回 UI 所需的所有数据
- [ ] （可选）实现异步 EV 计算：
  ```python
  def get_advice_async(self, callback):
      # 使用 Thread + Queue
  ```

**验收标准**: Controller 可独立测试，不依赖 Kivy

---

#### 任务 1.2.3: 创建 UI 组件
- [ ] 创建 `ui_kivy/views/widgets/card_widget.py`:
  - 显示单张牌（文本格式：`A♠`）
- [ ] 创建 `ui_kivy/views/widgets/hand_panel.py`:
  - 显示一手牌（多张 CardWidget）
- [ ] 创建 `ui_kivy/views/widgets/info_panel.py`:
  - 显示筹码、计数、策略建议、EV

**验收标准**: 每个组件可独立渲染和测试

---

#### 任务 1.2.4: 重构 `mobile_app.py`
- [ ] 将 `mobile_app.py` 移动到 `ui_kivy/app.py`
- [ ] 使用 `GameController` 替代直接调用 `GameState`
- [ ] UI 组件化：使用 `HandPanel`, `InfoPanel` 等
- [ ] 移除业务逻辑代码

**验收标准**: `python ui_kivy/app.py` 运行正常，功能与之前一致

---

### 1.3 测试与文档

#### 任务 1.3.1: 更新导入路径
- [ ] 更新 `main.py` 的导入
- [ ] 确保所有模块导入正确
- [ ] 运行 `main.py` 验证命令行版本仍可用

---

#### 任务 1.3.2: 编写架构文档
- [ ] 在 `README.md` 中说明新的目录结构
- [ ] 添加模块职责说明
- [ ] 提供使用示例

---

## 🚀 Phase 2: API 服务开发

### 2.1 FastAPI 项目搭建

#### 任务 2.1.1: 创建 API 目录
- [ ] 创建 `api_service/` 目录
- [ ] 创建 `api_service/routers/` 和 `api_service/services/`
- [ ] 创建 `api_service/main.py`

---

#### 任务 2.1.2: 实现基础服务层
- [ ] 创建 `api_service/services/evaluator_service.py`:
  - 包装 `core/game/evaluator.py` 的 `simulate_ev()`
  - 处理序列化/反序列化 Card 对象
- [ ] 创建 `api_service/services/strategy_service.py`:
  - 包装 `core/strategy/strategy_engine.py`

**验收标准**: 服务层可独立导入和测试

---

#### 任务 2.1.3: 实现 API 端点
- [ ] 创建 `api_service/routers/evaluation.py`:
  ```python
  @router.post("/evaluate_ev")
  async def evaluate_ev(request: EvaluationRequest):
      # 返回: {"H": {"win_rate": 0.52, "ev": 0.023}, ...}
  ```
- [ ] 创建 `api_service/routers/strategy.py`:
  ```python
  @router.get("/basic_strategy")
  async def get_basic_strategy(...):
      # 返回: {"action": "D", "reason": "basic_strategy"}
  ```
- [ ] 在 `main.py` 注册路由

**验收标准**: 用 Postman 测试端点，返回正确 JSON

---

#### 任务 2.1.4: 数据模型定义
- [ ] 创建 `api_service/models/request.py`:
  ```python
  class EvaluationRequest(BaseModel):
      player_hand: List[CardDict]
      dealer_hand: List[CardDict]
      remaining_cards: List[CardDict]
      trials: int = 3000
  ```
- [ ] 创建 `api_service/models/response.py`:
  - 定义响应格式

**验收标准**: Pydantic 验证通过

---

#### 任务 2.1.5: 性能优化（可选）
- [ ] 实现结果缓存（LRU Cache）
- [ ] 支持批量请求
- [ ] 添加请求限流（Rate Limiting）

**验收标准**: 相同请求响应时间 < 100ms（命中缓存）

---

### 2.2 本地测试

#### 任务 2.2.1: 本地运行测试
- [ ] 运行 `uvicorn api_service.main:app --reload`
- [ ] 用 Postman/curl 测试所有端点
- [ ] 测试错误处理（无效输入、空牌堆等）

**验收标准**: 所有端点返回 200 或合理的错误码

---

#### 任务 2.2.2: 编写 API 文档
- [ ] 使用 FastAPI 自动生成 OpenAPI 文档
- [ ] 添加端点描述和示例
- [ ] 访问 `http://localhost:8000/docs` 验证文档

---

## 📱 Phase 3: Flutter 迁移

### 3.1 Flutter 项目初始化

#### 任务 3.1.1: 创建 Flutter 项目
- [ ] 运行 `flutter create blackjack_trainer`
- [ ] 配置项目结构：
  ```
  lib/
  ├── models/          # 数据模型
  ├── services/        # API 调用
  ├── state/           # 状态管理
  ├── ui/              # UI 组件
  └── main.dart
  ```

---

#### 任务 3.1.2: 实现核心模型（本地）
- [ ] 创建 `lib/models/card.dart`:
  ```dart
  class Card {
    final String rank;
    final String suit;
    int getValue() { ... }
  }
  ```
- [ ] 创建 `lib/models/deck.dart`:
  - 实现 `Deck` 类（洗牌、发牌）
- [ ] 创建 `lib/models/counter.dart`:
  - 实现 `HiLoCounter` 类

**验收标准**: 单元测试通过

---

#### 任务 3.1.3: 实现 GameState（本地）
- [ ] 创建 `lib/models/game_state.dart`:
  - 参考 Python `GameState`，用 Dart 实现
  - 管理玩家手牌、庄家手牌、筹码、计数
- [ ] 实现 `player_hit()`, `player_stand()`, `player_double_down()`

**验收标准**: GameState 测试通过

---

### 3.2 UI 开发

#### 任务 3.2.1: 主界面布局
- [ ] 创建 `lib/ui/game_screen.dart`:
  - 上下布局：信息区 + 控制区
- [ ] 实现手牌展示：
  - 庄家手牌（第一张隐藏）
  - 玩家手牌（支持多手，如果已分牌）
- [ ] 实现信息面板：
  - 筹码显示
  - Hi-Lo 计数显示
  - 策略建议 + EV 显示

**验收标准**: 静态 UI 渲染正确

---

#### 任务 3.2.2: 按钮交互
- [ ] 实现 "新一局" 按钮
- [ ] 实现 "Hit" 按钮
- [ ] 实现 "Stand" 按钮
- [ ] 实现 "Double" 按钮（仅在 2 张牌时启用）
- [ ] 实现 "Split" 按钮（仅在可分牌时显示）

**验收标准**: 按钮点击触发对应 GameState 方法

---

#### 任务 3.2.3: 牌面渲染优化
- [ ] 方案 A：文本渲染（快速）
  - 显示 `A♠ 10♥` 等文本
- [ ] 方案 B：SVG/Canvas 绘制（可选）
  - 使用单一模板动态绘制牌面
- [ ] 添加牌面颜色（红/黑）

**验收标准**: 牌面清晰可读

---

### 3.3 API 集成

#### 任务 3.3.1: HTTP 客户端设置
- [ ] 添加依赖：`http` 或 `dio`
- [ ] 创建 `lib/services/api_client.dart`:
  ```dart
  class ApiClient {
    Future<Map<String, dynamic>> evaluateEV(...) async { ... }
  }
  ```

---

#### 任务 3.3.2: 集成 EV 计算
- [ ] 在 `GameScreen` 中调用 API
- [ ] 实现异步加载（显示 Loading 状态）
- [ ] 显示 EV 结果到信息面板
- [ ] 错误处理（网络错误、超时）

**验收标准**: EV 计算结果正确显示

---

#### 任务 3.3.3: 状态管理（可选，使用 Provider/Riverpod）
- [ ] 实现 `GameStateProvider`（如果使用状态管理库）
- [ ] 实现 `ApiServiceProvider`
- [ ] UI 通过 Provider 访问状态

**验收标准**: 状态更新触发 UI 刷新

---

### 3.4 Split 功能实现

#### 任务 3.4.1: 扩展 GameState 支持多手
- [ ] 修改 `GameState` 支持 `List<List<Card>> player_hands`
- [ ] 实现 `player_split()` 方法
- [ ] 实现多手的轮流操作逻辑

---

#### 任务 3.4.2: UI 支持多手显示
- [ ] 修改手牌面板支持显示多手
- [ ] 添加当前激活手牌的指示器
- [ ] 实现切换手牌功能

**验收标准**: 分牌后可分别操作每手

---

### 3.5 测试与优化

#### 任务 3.5.1: 单元测试
- [ ] 测试 `Card`, `Deck`, `Counter` 模型
- [ ] 测试 `GameState` 逻辑
- [ ] 测试 API 调用（Mock）

---

#### 任务 3.5.2: Widget 测试
- [ ] 测试 `GameScreen` 渲染
- [ ] 测试按钮交互

---

#### 任务 3.5.3: 性能优化
- [ ] 优化 EV 计算请求（减少不必要的请求）
- [ ] 添加结果缓存（本地）
- [ ] UI 动画优化（如果需要）

---

## 📦 Phase 4: 打包与部署（未来）

### 4.1 移动端打包
- [ ] Android APK 打包
- [ ] iOS IPA 打包（需 Apple 开发者账号）
- [ ] 测试真机运行

### 4.2 API 部署（如果选择云端）
- [ ] 部署到云服务（AWS/Heroku/...）
- [ ] 配置域名和 HTTPS
- [ ] 监控和日志

---

## 📊 进度跟踪

### 当前阶段: Phase 1
### 完成度: 0%

**每周更新此清单的完成状态**


import 'package:flutter/material.dart';
import '../models/blackjack_game.dart';
import '../utils/card_utils.dart';

/// 游戏主界面
class GameScreen extends StatefulWidget {
  final BlackjackGame game;

  const GameScreen({super.key, required this.game});

  @override
  State<GameScreen> createState() => _GameScreenState();
}

class _GameScreenState extends State<GameScreen> {
  int? _selectedBetAmount;

  /// 判断是否显示底部按钮栏
  bool _shouldShowBottomBar() {
    final game = widget.game;
    
    // 必须有session
    if (game.sessionId == null) return false;
    
    // 加载中不显示
    if (game.isLoading) return false;
    
    // 游戏进行中或已结束都显示按钮栏（已结束时显示New Hand按钮）
    return true;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('21点 Hi-Lo 训练器'),
        backgroundColor: Colors.green[700],
        foregroundColor: Colors.white,
        leading: IconButton(
                icon: const Icon(Icons.arrow_back),
                onPressed: () {
                  // 回退到初始页面（清空session）
                  widget.game.clearSession();
                },
          tooltip: '返回主页面',
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () => _showSettingsDialog(context),
            tooltip: '设置',
          ),
        ],
      ),
      body: ListenableBuilder(
        listenable: widget.game,
        builder: (context, child) {
          // 只有在真正没有session且不在加载时才显示开始界面
          if (widget.game.sessionId == null && !widget.game.isLoading) {
            return _buildStartScreen(context);
          }

          if (widget.game.isLoading) {
            return const Center(
              child: CircularProgressIndicator(),
            );
          }

          // 始终保持在同一页面，不切换（即使游戏结束也保持在同一页面）
          return _buildGameScreen(context);
        },
      ),
      // 底部操作按钮栏 - 使用ListenableBuilder确保响应状态变化
      bottomNavigationBar: ListenableBuilder(
        listenable: widget.game,
        builder: (context, child) {
          if (_shouldShowBottomBar()) {
            return _buildBottomActionBar(context);
          }
          return const SizedBox.shrink();
        },
      ),
    );
  }

  /// 开始界面
  Widget _buildStartScreen(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Text(
            '21点 Hi-Lo 训练器',
            style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 20),
          ElevatedButton(
            onPressed: () => widget.game.startNewGame(),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.green[700],
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(
                horizontal: 40,
                vertical: 15,
              ),
            ),
            child: const Text('开始新游戏'),
          ),
        ],
      ),
    );
  }

  /// 下注选择界面
  Widget _buildBetSelectionScreen(BuildContext context) {
    final betOptions = [10, 25, 50, 100];
    _selectedBetAmount ??= widget.game.bet;

    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Text(
            '选择下注金额',
            style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 32),
          Text(
            '当前筹码: ${widget.game.chips}',
            style: const TextStyle(fontSize: 18),
          ),
          const SizedBox(height: 32),
          Wrap(
            spacing: 16,
            runSpacing: 16,
            alignment: WrapAlignment.center,
            children: betOptions.map((amount) {
              final isSelected = _selectedBetAmount == amount;
              final canAfford = widget.game.chips >= amount;
              return ChoiceChip(
                label: Text('$amount'),
                selected: isSelected,
                onSelected: canAfford
                    ? (selected) {
                        setState(() {
                          _selectedBetAmount = amount;
                        });
                      }
                    : null,
                selectedColor: Colors.green[300],
                disabledColor: Colors.grey[300],
                labelStyle: TextStyle(
                  color: isSelected ? Colors.white : Colors.black,
                  fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                ),
              );
            }).toList(),
          ),
          const SizedBox(height: 32),
          ElevatedButton(
            onPressed: _selectedBetAmount != null &&
                    widget.game.chips >= _selectedBetAmount!
                ? () {
                    widget.game.startNewRoundWithBet(_selectedBetAmount!);
                    _selectedBetAmount = null;
                  }
                : null,
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.green[700],
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(
                horizontal: 40,
                vertical: 15,
              ),
            ),
            child: const Text('开始新一局'),
          ),
        ],
      ),
    );
  }

  /// 游戏主界面
  Widget _buildGameScreen(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 60), // 底部padding，为按钮栏留出空间
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 信息面板
          _buildInfoPanel(),

          const SizedBox(height: 20),

          // 策略建议
          if (widget.game.suggestion != null) _buildSuggestionBanner(),

          const SizedBox(height: 20),

          // 错误提示
          if (widget.game.errorMessage != null) _buildErrorBanner(),

          const SizedBox(height: 20),

          // 结果提示
          if (widget.game.isResolved && widget.game.result != null)
            _buildResultBanner(),

          const SizedBox(height: 20),

          // 庄家手牌
          _buildDealerSection(),

          const SizedBox(height: 30),

          // 玩家手牌（支持多手）
          _buildPlayerHandsSection(),

          const SizedBox(height: 30),

          // 历史记录
          _buildHistorySection(),

          // 为底部按钮栏留出额外空间
          const SizedBox(height: 20),
        ],
      ),
    );
  }

  /// 信息面板
  Widget _buildInfoPanel() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.grey[200],
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        children: [
          // 第一行：筹码、Score、下注
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              Expanded(
                child: _buildInfoItem('筹码', '${widget.game.chips}'),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _buildInfoItem('Score', '${widget.game.strategyPercentage.toStringAsFixed(1)}%'),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _buildInfoItem('下注', '${widget.game.bet}'),
              ),
            ],
          ),
          // 游戏结束后显示下注滑动条
          if (widget.game.isResolved) ...[
            const SizedBox(height: 12),
            _buildBetSlider(),
          ],
          const SizedBox(height: 12),
          // 第二行：Running Count、True Count、剩余牌数
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              Expanded(
                child: _buildInfoItem('Running Count', '${widget.game.runningCount}'),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _buildInfoItem('True Count', widget.game.trueCount.toStringAsFixed(2)),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _buildInfoItem('剩余牌数', '${widget.game.cardsRemaining}/${widget.game.totalCards}'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildInfoItem(String label, String value) {
    return Column(
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 12,
            color: Colors.grey,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
      ],
    );
  }

  /// 下注滑动条（游戏结束后显示在筹码旁边）
  Widget _buildBetSlider() {
    final maxBet = widget.game.chips > 0 ? widget.game.chips : 500;
    _selectedBetAmount ??= widget.game.bet;
    if (_selectedBetAmount! > maxBet) {
      _selectedBetAmount = maxBet;
    }
    if (_selectedBetAmount! < 10) {
      _selectedBetAmount = 10;
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '选择下注: $_selectedBetAmount',
          style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 4),
        Slider(
          value: _selectedBetAmount!.toDouble(),
          min: 10,
          max: maxBet.toDouble(),
          divisions: ((maxBet - 10) / 10).round().clamp(1, 50),
          label: '$_selectedBetAmount',
          onChanged: (value) {
            setState(() {
              _selectedBetAmount = value.round();
            });
          },
        ),
      ],
    );
  }

  /// 策略建议横幅
  Widget _buildSuggestionBanner() {
    String suggestionText = widget.game.suggestion ?? '';
    String displayText = '';
    
    // 将建议转换为中文显示
    switch (suggestionText) {
      case 'H':
        displayText = '建议: 要牌 (Hit)';
        break;
      case 'S':
        displayText = '建议: 停牌 (Stand)';
        break;
      case 'D':
        displayText = '建议: 加倍 (Double)';
        break;
      case 'SP':
        displayText = '建议: 分牌 (Split)';
        break;
      default:
        displayText = '建议: $suggestionText';
    }

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.blue[100],
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.blue),
      ),
      child: Row(
        children: [
          const Icon(Icons.lightbulb, color: Colors.blue),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              displayText,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: Colors.blue,
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 错误提示横幅
  Widget _buildErrorBanner() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.red[100],
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.red),
      ),
      child: Row(
        children: [
          const Icon(Icons.error, color: Colors.red),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              widget.game.errorMessage ?? '未知错误',
              style: const TextStyle(color: Colors.red),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.close, size: 20),
            onPressed: () => widget.game.clearError(),
          ),
        ],
      ),
    );
  }

  /// 结果横幅
  Widget _buildResultBanner() {
    String resultText = '';
    if (widget.game.result is List) {
      resultText = (widget.game.result as List).join('\n');
    } else {
      resultText = widget.game.result.toString();
    }

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.green[100],
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.green),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '结算结果',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 8),
          Text(resultText),
        ],
      ),
    );
  }

  /// 庄家区域
  Widget _buildDealerSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '庄家 ${widget.game.dealerTotal != null ? "(${widget.game.dealerTotal})" : ""}',
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 8),
        _buildCardRow(widget.game.dealerHand),
      ],
    );
  }

  /// 玩家手牌区域（支持多手）
  Widget _buildPlayerHandsSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '玩家手牌',
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 8),
        ...List.generate(
          widget.game.numHands,
          (index) => Padding(
            padding: const EdgeInsets.only(bottom: 16),
            child: _buildPlayerHand(index),
          ),
        ),
      ],
    );
  }

  /// 单个玩家手牌
  Widget _buildPlayerHand(int handIndex) {
    final isActive = handIndex == widget.game.currentHandIndex;
    final handTotal = handIndex < widget.game.playerTotals.length
        ? widget.game.playerTotals[handIndex]
        : null;
    final handBet = handIndex < widget.game.handBets.length
        ? widget.game.handBets[handIndex]
        : widget.game.bet;
    final handCards = widget.game.playerHands[handIndex];

    // 计算手牌的可能点数（当有A时显示两个值）
    final handValues = CardUtils.calculateHandValuesWithAce(handCards);
    String pointsText = '';
    if (handValues.length == 2) {
      // 有A，显示两个点数
      pointsText = '${handValues[0]}/${handValues[1]}点';
    } else if (handValues.length == 1) {
      // 没有A或只有一个值
      pointsText = '${handValues[0]}点';
    } else if (handTotal != null) {
      // 备用：使用API返回的值
      pointsText = '$handTotal点';
    }

    return Container(
      decoration: BoxDecoration(
        color: isActive ? Colors.blue[50] : Colors.grey[100],
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: isActive ? Colors.blue : Colors.grey[300]!,
          width: isActive ? 2 : 1,
        ),
      ),
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '手牌 ${handIndex + 1} ${isActive ? "(当前)" : ""}',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: isActive ? FontWeight.bold : FontWeight.normal,
                  color: isActive ? Colors.blue : Colors.black,
                ),
              ),
              Text(
                '$pointsText 下注: $handBet',
                style: const TextStyle(fontSize: 14),
              ),
            ],
          ),
          const SizedBox(height: 8),
          _buildCardRow(handCards),
        ],
      ),
    );
  }

  /// 显示牌面行
  Widget _buildCardRow(List<String> cards) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: cards.map((card) => _buildCardImage(card)).toList(),
    );
  }

  /// 单个牌面图片 - 使用PNG
  Widget _buildCardImage(String card) {
    final imagePath = CardUtils.getCardImagePath(card);
    return Container(
      width: 60,
      height: 84,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(8),
        color: Colors.white,
        border: Border.all(color: Colors.grey[400]!, width: 1),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.2),
            blurRadius: 4,
            offset: const Offset(2, 2),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: Image.asset(
          imagePath,
          fit: BoxFit.cover,
          width: 60,
          height: 84,
          errorBuilder: (context, error, stackTrace) {
            // 如果图片加载失败，显示文字备用
            return Container(
              color: Colors.grey[200],
              child: Center(
                child: Text(
                  card,
                  style: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                  ),
                  textAlign: TextAlign.center,
                ),
              ),
            );
          },
        ),
      ),
    );
  }

  /// 底部操作按钮栏（一行四个按钮）
  Widget _buildBottomActionBar(BuildContext context) {
    // 如果游戏已结束，显示一个大的 New Hand 按钮
    final bool isResolved = widget.game.isResolved;
    
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 4,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: SafeArea(
        child: isResolved
            ? // 游戏结束：显示一个大的 New Hand 按钮
              _buildLargeNewHandButton()
            : // 游戏进行中：显示四个操作按钮
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  _buildBottomButton(
                    '要牌',
                    widget.game.canHit && !widget.game.isLoading,
                    Colors.green,
                    () => widget.game.hit(),
                    Icons.add_card,
                  ),
                  _buildBottomButton(
                    '停牌',
                    widget.game.canStand && !widget.game.isLoading,
                    Colors.orange,
                    () => widget.game.stand(),
                    Icons.stop,
                  ),
                  _buildBottomButton(
                    '加倍',
                    widget.game.canDouble && !widget.game.isLoading,
                    Colors.blue,
                    () => widget.game.doubleDown(),
                    Icons.double_arrow,
                  ),
                  _buildBottomButton(
                    '分牌',
                    widget.game.canSplit && !widget.game.isLoading,
                    Colors.purple,
                    () => widget.game.split(),
                    Icons.call_split,
                  ),
                ],
              ),
      ),
    );
  }

  /// 大的 New Hand 按钮（游戏结束后显示）
  Widget _buildLargeNewHandButton() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: SizedBox(
        width: double.infinity,
        height: 50,
        child: ElevatedButton(
          onPressed: widget.game.isLoading
              ? null
              : () => widget.game.startNewRoundWithBet(_selectedBetAmount ?? widget.game.bet),
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.green[700],
            foregroundColor: Colors.white,
            disabledBackgroundColor: Colors.grey[400],
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
            elevation: 2,
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.refresh, size: 24),
              const SizedBox(width: 8),
              const Text(
                'New Hand',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildBottomButton(
    String label,
    bool enabled,
    Color color,
    VoidCallback onPressed,
    IconData icon,
  ) {
    return Expanded(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 4),
        child: SizedBox(
          height: 44, // 减小高度，避免溢出
          child: ElevatedButton(
            onPressed: enabled ? onPressed : null,
            style: ElevatedButton.styleFrom(
              backgroundColor: enabled ? color : Colors.grey[400],
              foregroundColor: Colors.white,
              disabledBackgroundColor: Colors.grey[400],
              padding: const EdgeInsets.symmetric(horizontal: 2, vertical: 4),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
              minimumSize: Size.zero, // 允许按钮尽可能小
              tapTargetSize: MaterialTapTargetSize.shrinkWrap, // 减小点击区域
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(icon, size: 18), // 稍微减小图标
                const SizedBox(height: 2),
                Text(
                  label,
                  style: const TextStyle(fontSize: 11), // 稍微减小字体
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// 显示设置对话框
  void _showSettingsDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return Dialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          child: Container(
            width: MediaQuery.of(context).size.width * 0.75,
            height: MediaQuery.of(context).size.height * 0.5,
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 标题
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      '设置',
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.close),
                      onPressed: () => Navigator.of(context).pop(),
                    ),
                  ],
                ),
                const SizedBox(height: 24),
                
                // Blackjack赔付比例选择
                const Text(
                  'Blackjack赔付比例',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 12),
                
                // 3:2 选项
                _buildPayoutOption(
                  context,
                  '3:2',
                  'Blackjack pays 3:2',
                  widget.game.blackjackPayoutRatio == '3:2',
                  () {
                    widget.game.setBlackjackPayoutRatio('3:2');
                    Navigator.of(context).pop();
                  },
                ),
                const SizedBox(height: 12),
                
                // 6:5 选项
                _buildPayoutOption(
                  context,
                  '6:5',
                  'Blackjack pays 6:5',
                  widget.game.blackjackPayoutRatio == '6:5',
                  () {
                    widget.game.setBlackjackPayoutRatio('6:5');
                    Navigator.of(context).pop();
                  },
                ),
                
                const Spacer(),
                
                // 关闭按钮
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: () => Navigator.of(context).pop(),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.green[700],
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    child: const Text('关闭'),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  /// 构建赔付比例选项
  Widget _buildPayoutOption(
    BuildContext context,
    String ratio,
    String label,
    bool isSelected,
    VoidCallback onTap,
  ) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: isSelected ? Colors.green[50] : Colors.grey[100],
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
            color: isSelected ? Colors.green[700]! : Colors.grey[300]!,
            width: isSelected ? 2 : 1,
          ),
        ),
        child: Row(
          children: [
            // 选中标记
            Icon(
              isSelected ? Icons.check_circle : Icons.circle_outlined,
              color: isSelected ? Colors.green[700] : Colors.grey,
              size: 24,
            ),
            const SizedBox(width: 12),
            // 比例文本
            Text(
              ratio,
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: isSelected ? Colors.green[700] : Colors.black87,
              ),
            ),
            const SizedBox(width: 12),
            // 标签
            Expanded(
              child: Text(
                label,
                style: TextStyle(
                  fontSize: 16,
                  color: isSelected ? Colors.green[700] : Colors.black87,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 历史记录区域
  Widget _buildHistorySection() {
    if (widget.game.gameHistory.isEmpty) {
      return const SizedBox.shrink();
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '历史记录',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 8),
        SizedBox(
          height: 200, // 固定高度，可滑动
          child: ListView.builder(
            scrollDirection: Axis.horizontal,
            itemCount: widget.game.gameHistory.length,
            itemBuilder: (context, index) {
              final history = widget.game.gameHistory[index];
              return _buildHistoryCard(history);
            },
          ),
        ),
      ],
    );
  }

  /// 构建历史记录卡片
  Widget _buildHistoryCard(Map<String, dynamic> history) {
    final timestamp = history['timestamp'] as String? ?? '';
    final profitLoss = history['profit_loss'] as int? ?? 0;
    final winRate = history['win_rate'] as num? ?? 0.0;
    final playerHands = history['player_hands'] as List? ?? [];
    final dealerHand = history['dealer_hand'] as List? ?? [];
    final wins = history['wins'] as int? ?? 0;
    final totalHands = history['total_hands'] as int? ?? 1;

    // 解析时间戳
    String timeStr = '';
    try {
      final dt = DateTime.parse(timestamp);
      timeStr = '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
    } catch (e) {
      timeStr = '';
    }

    // 盈利/损失颜色
    final profitColor = profitLoss >= 0 ? Colors.green : Colors.red;
    final profitText = profitLoss >= 0 ? '+$profitLoss' : '$profitLoss';

    return Container(
      width: 180,
      margin: const EdgeInsets.only(right: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey[300]!),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 4,
            offset: const Offset(2, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          // 时间和盈利/损失
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                timeStr,
                style: TextStyle(
                  fontSize: 12,
                  color: Colors.grey[600],
                ),
              ),
              Text(
                profitText,
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: profitColor,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          
          // 胜率
          Text(
            '胜率: ${winRate.toStringAsFixed(1)}% ($wins/$totalHands)',
            style: const TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 8),
          
          // 玩家手牌
          Text(
            '玩家:',
            style: TextStyle(
              fontSize: 11,
              color: Colors.grey[700],
            ),
          ),
          const SizedBox(height: 4),
          Wrap(
            spacing: 4,
            runSpacing: 4,
            children: playerHands.expand((hand) {
              final handList = hand as List;
              return handList.map((card) {
                return Container(
                  padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                  decoration: BoxDecoration(
                    color: Colors.blue[50],
                    borderRadius: BorderRadius.circular(4),
                    border: Border.all(color: Colors.blue[200]!),
                  ),
                  child: Text(
                    card.toString(),
                    style: const TextStyle(fontSize: 10),
                  ),
                );
              });
            }).toList(),
          ),
          const SizedBox(height: 8),
          
          // 庄家手牌
          Text(
            '庄家:',
            style: TextStyle(
              fontSize: 11,
              color: Colors.grey[700],
            ),
          ),
          const SizedBox(height: 4),
          Wrap(
            spacing: 4,
            children: (dealerHand as List).map((card) {
              return Container(
                padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                decoration: BoxDecoration(
                  color: Colors.red[50],
                  borderRadius: BorderRadius.circular(4),
                  border: Border.all(color: Colors.red[200]!),
                ),
                child: Text(
                  card.toString(),
                  style: const TextStyle(fontSize: 10),
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }
}



import 'package:flutter/foundation.dart';
import '../api_service.dart';

/// 21点游戏状态管理类
class BlackjackGame extends ChangeNotifier {
  final ApiService _apiService = ApiService();

  String? _sessionId;
  List<List<String>> _playerHands = [];
  List<int> _playerTotals = [];
  int _currentHandIndex = 0;
  List<String> _dealerHand = [];
  int? _dealerTotal;
  List<int> _handBets = [];
  List<bool> _handDoubled = []; // 跟踪哪些手牌进行了double down
  int _chips = 500;
  int _bet = 10;
  int _runningCount = 0;
  double _trueCount = 0.0;
  int _cardsRemaining = 0; // 剩余牌数
  int _totalCards = 104; // 总牌数（默认2副牌）
  double _remainingRatio = 1.0; // 剩余牌数比例
  String? _suggestion; // 策略建议
  bool _canHit = false;
  bool _canStand = false;
  bool _canDouble = false;
  bool _canSplit = false;
  bool _isBusted = false;
  bool _isResolved = false;
  dynamic _result; // 可能是字符串或列表
  String? _errorMessage;
  bool _isLoading = false;
  bool _waitingForBet = false; // 等待下注
  String _blackjackPayoutRatio = '3:2'; // Blackjack赔付比例

  // Getters
  String? get sessionId => _sessionId;
  List<List<String>> get playerHands => _playerHands;
  List<int> get playerTotals => _playerTotals;
  int get currentHandIndex => _currentHandIndex;
  List<String> get dealerHand => _dealerHand;
  int? get dealerTotal => _dealerTotal;
  List<int> get handBets => _handBets;
  List<bool> get handDoubled => _handDoubled;
  int get chips => _chips;
  int get bet => _bet;
  int get runningCount => _runningCount;
  double get trueCount => _trueCount;
  int get cardsRemaining => _cardsRemaining;
  int get totalCards => _totalCards;
  double get remainingRatio => _remainingRatio;
  String? get suggestion => _suggestion;
  bool get waitingForBet => _waitingForBet;
  bool get canHit => _canHit;
  bool get canStand => _canStand;
  bool get canDouble => _canDouble;
  bool get canSplit => _canSplit;
  bool get isBusted => _isBusted;
  bool get isResolved => _isResolved;
  dynamic get result => _result;
  String? get errorMessage => _errorMessage;
  bool get isLoading => _isLoading;
  int get numHands => _playerHands.length;
  String get blackjackPayoutRatio => _blackjackPayoutRatio;
  
  // 策略得分
  int _strategyCorrect = 0;
  int _strategyTotal = 0;
  double _strategyPercentage = 0.0;
  
  int get strategyCorrect => _strategyCorrect;
  int get strategyTotal => _strategyTotal;
  double get strategyPercentage => _strategyPercentage;
  
  // 历史记录
  List<Map<String, dynamic>> _gameHistory = [];
  List<Map<String, dynamic>> get gameHistory => _gameHistory;

  /// 当前手牌
  List<String> get currentHand =>
      _currentHandIndex < _playerHands.length
          ? _playerHands[_currentHandIndex]
          : [];

  /// 当前手牌点数
  int? get currentHandTotal =>
      _currentHandIndex < _playerTotals.length
          ? _playerTotals[_currentHandIndex]
          : null;

  /// 创建新游戏
  Future<void> startNewGame({
    int numDecks = 2,
    int initialChips = 500,
    int betAmount = 10,
  }) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _sessionId = await _apiService.newGame(
        numDecks: numDecks,
        initialChips: initialChips,
        betAmount: betAmount,
      );

      // 获取初始状态
      await refreshStatus();
    } catch (e) {
      _errorMessage = e.toString();
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 刷新游戏状态
  Future<void> refreshStatus({bool reveal = false}) async {
    if (_sessionId == null) return;

    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final status = await _apiService.getStatus(_sessionId!, reveal: reveal);
      _updateFromStatus(status);
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _errorMessage = e.toString();
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 玩家要牌
  Future<void> hit() async {
    if (_sessionId == null) {
      _errorMessage = 'Session不存在';
      notifyListeners();
      return;
    }
    
    if (!_canHit) {
      _errorMessage = '当前不能要牌';
      notifyListeners();
      return;
    }

    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final status = await _apiService.hit(_sessionId!);
      _updateFromStatus(status);
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _errorMessage = '要牌失败: $e';
      _isLoading = false;
      notifyListeners();
      // 注意：即使出错也不清空sessionId，保持在当前页面
    }
  }

  /// 玩家停牌
  Future<void> stand() async {
    if (_sessionId == null) {
      _errorMessage = 'Session不存在';
      notifyListeners();
      return;
    }
    
    if (!_canStand) {
      _errorMessage = '当前不能停牌';
      notifyListeners();
      return;
    }

    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final status = await _apiService.stand(_sessionId!);
      _updateFromStatus(status);
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _errorMessage = '停牌失败: $e';
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 玩家加倍
  Future<void> doubleDown() async {
    if (_sessionId == null) {
      _errorMessage = 'Session不存在';
      notifyListeners();
      return;
    }
    
    if (!_canDouble) {
      _errorMessage = '当前不能加倍';
      notifyListeners();
      return;
    }

    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final status = await _apiService.doubleDown(_sessionId!);
      _updateFromStatus(status);
      // 在_updateFromStatus之后标记当前手牌为double down
      // 确保列表长度足够
      while (_handDoubled.length <= _currentHandIndex) {
        _handDoubled.add(false);
      }
      _handDoubled[_currentHandIndex] = true;
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _errorMessage = '加倍失败: $e';
      _isLoading = false;
      notifyListeners();
      // 注意：即使出错也不清空sessionId，保持在当前页面
    }
  }

  /// 玩家分牌
  Future<void> split() async {
    if (_sessionId == null || !_canSplit) return;

    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final status = await _apiService.split(_sessionId!);
      _updateFromStatus(status);
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _errorMessage = e.toString();
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 开始新一局（带下注金额）
  Future<void> newRound({int? betAmount}) async {
    if (_sessionId == null) return;

    _isLoading = true;
    _errorMessage = null;
    _waitingForBet = false;
    // 重置double down状态
    _handDoubled = [];
    notifyListeners();

    try {
      final status = await _apiService.newRound(_sessionId!, betAmount: betAmount);
      _updateFromStatus(status);
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _errorMessage = e.toString();
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 设置下注金额并开始新一局
  Future<void> startNewRoundWithBet(int betAmount) async {
    if (_sessionId == null) {
      _errorMessage = 'Session不存在，请先开始新游戏';
      notifyListeners();
      return;
    }

    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      await newRound(betAmount: betAmount);
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _errorMessage = '开始新一局失败: $e';
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 从API返回的状态更新游戏状态
  void _updateFromStatus(Map<String, dynamic> status) {
    // 玩家手牌（支持多手）
    if (status.containsKey('player_hands')) {
      final hands = status['player_hands'] as List;
      _playerHands = hands
          .map((hand) => (hand as List).cast<String>())
          .toList();
    } else if (status.containsKey('player_hand')) {
      // 向后兼容单手牌格式
      _playerHands = [
        (status['player_hand'] as List).cast<String>(),
      ];
    }

    // 玩家点数
    if (status.containsKey('player_totals')) {
      _playerTotals = (status['player_totals'] as List).cast<int>();
    } else if (status.containsKey('player_total')) {
      _playerTotals = [status['player_total'] as int];
    }

    // 当前手牌索引
    _currentHandIndex = status['current_hand_index'] as int? ?? 0;

    // 庄家手牌
    if (status.containsKey('dealer_hand')) {
      _dealerHand = (status['dealer_hand'] as List).cast<String>();
    }

    // 庄家点数
    _dealerTotal = status['dealer_total'] as int?;

    // 下注
    if (status.containsKey('hand_bets')) {
      final newHandBets = (status['hand_bets'] as List).cast<int>();
      _handBets = newHandBets;
      
      // 检查API是否返回了hand_doubled信息
      if (status.containsKey('hand_doubled')) {
        // 如果API返回了hand_doubled，使用API的值
        final apiDoubled = (status['hand_doubled'] as List).cast<bool>();
        // 确保列表长度一致，合并API返回的值和已有的标记
        while (_handDoubled.length < apiDoubled.length) {
          _handDoubled.add(false);
        }
        // 合并：如果API返回true，或者我们已经标记为true，则保持true
        for (int i = 0; i < apiDoubled.length; i++) {
          if (i < _handDoubled.length) {
            _handDoubled[i] = _handDoubled[i] || apiDoubled[i];
          } else {
            _handDoubled.add(apiDoubled[i]);
          }
        }
      } else {
        // 如果没有，根据下注金额判断（double down时下注会翻倍）
        // 确保_handDoubled列表长度与_handBets一致
        while (_handDoubled.length < _handBets.length) {
          _handDoubled.add(false);
        }
        // 如果下注是原始下注的两倍，可能是double down
        // 但这个方法不够准确，因为split后也可能有不同下注
        // 所以我们主要依赖在doubleDown()方法中标记
      }
    }
    _bet = status['bet'] as int? ?? _bet;

    // 筹码
    _chips = status['chips'] as int? ?? _chips;

    // 计数
    _runningCount = status['running_count'] as int? ?? 0;
    _trueCount = (status['true_count'] as num?)?.toDouble() ?? 0.0;

    // 剩余牌数
    _cardsRemaining = status['cards_remaining'] as int? ?? 0;
    _totalCards = status['total_cards'] as int? ?? 104;
    _remainingRatio = (status['remaining_ratio'] as num?)?.toDouble() ?? 1.0;

    // 策略建议
    _suggestion = status['suggestion'] as String?;

    // 操作可用性（先更新，再判断waitingForBet）
    _canHit = status['can_hit'] as bool? ?? false;
    _canStand = status['can_stand'] as bool? ?? false;
    _canDouble = status['can_double'] as bool? ?? false;
    _canSplit = status['can_split'] as bool? ?? false;

    // 游戏状态
    _isBusted = status['is_busted'] as bool? ?? false;
    _isResolved = status['is_resolved'] as bool? ?? false;
    _result = status['result'];

    // Blackjack赔付比例
    _blackjackPayoutRatio = status['blackjack_payout_ratio'] as String? ?? '3:2';

    // 策略得分
    if (status.containsKey('strategy_score')) {
      final score = status['strategy_score'] as Map<String, dynamic>;
      _strategyCorrect = score['correct'] as int? ?? 0;
      _strategyTotal = score['total'] as int? ?? 0;
      _strategyPercentage = (score['percentage'] as num?)?.toDouble() ?? 0.0;
    }

    // 历史记录
    if (status.containsKey('game_history')) {
      final history = status['game_history'] as List;
      _gameHistory = history.cast<Map<String, dynamic>>();
      // 倒序排列（最新的在前）
      _gameHistory = _gameHistory.reversed.toList();
    }

    // 检查是否需要等待下注（游戏已结束，所有手牌都完成）
    _waitingForBet = (_isResolved && !_canHit && !_canStand);
  }

  /// 清除错误信息
  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }

  /// 设置Blackjack赔付比例
  Future<void> setBlackjackPayoutRatio(String ratio) async {
    if (_sessionId == null) {
      _errorMessage = 'Session不存在';
      notifyListeners();
      return;
    }
    
    if (ratio != '3:2' && ratio != '6:5') {
      _errorMessage = '赔付比例必须是3:2或6:5';
      notifyListeners();
      return;
    }

    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final status = await _apiService.setBlackjackPayout(_sessionId!, ratio);
      _updateFromStatus(status);
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _errorMessage = '设置失败: $e';
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 清除session，返回初始页面
  void clearSession() {
    _sessionId = null;
    _playerHands = [];
    _playerTotals = [];
    _currentHandIndex = 0;
    _dealerHand = [];
    _dealerTotal = null;
    _handBets = [];
    _handDoubled = [];
    _runningCount = 0;
    _trueCount = 0.0;
    _cardsRemaining = 0;
    _totalCards = 104;
    _remainingRatio = 1.0;
    _suggestion = null;
    _canHit = false;
    _canStand = false;
    _canDouble = false;
    _canSplit = false;
    _isBusted = false;
    _isResolved = false;
    _result = null;
    _errorMessage = null;
    _isLoading = false;
    _waitingForBet = false;
    // 保留筹码和下注，以便继续游戏
    notifyListeners();
  }
}


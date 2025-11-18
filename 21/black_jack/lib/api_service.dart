import 'dart:convert';
import 'package:http/http.dart' as http;
import 'config/app_config.dart';

/// API服务类，负责与Flask后端通信
class ApiService {
  static const String baseUrl = AppConfig.apiBaseUrl;
  
  /// 创建新游戏
  /// 返回session_id
  Future<String> newGame({
    int numDecks = 2,
    int initialChips = 500,
    int betAmount = 10,
  }) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/game/new'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'num_decks': numDecks,
          'initial_chips': initialChips,
          'bet_amount': betAmount,
        }),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        return data['session_id'] as String;
      } else {
        throw Exception('创建游戏失败: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('网络错误: $e');
    }
  }

  /// 获取游戏状态
  Future<Map<String, dynamic>> getStatus(
    String sessionId, {
    bool reveal = false,
  }) async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/api/game/$sessionId/status')
            .replace(queryParameters: {'reveal': reveal.toString()}),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('获取状态失败: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('网络错误: $e');
    }
  }

  /// 玩家要牌
  Future<Map<String, dynamic>> hit(String sessionId) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/game/$sessionId/hit'),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        final error = jsonDecode(response.body);
        throw Exception(error['error'] ?? '要牌失败');
      }
    } catch (e) {
      throw Exception('网络错误: $e');
    }
  }

  /// 玩家停牌
  Future<Map<String, dynamic>> stand(String sessionId) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/game/$sessionId/stand'),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        final error = jsonDecode(response.body);
        throw Exception(error['error'] ?? '停牌失败');
      }
    } catch (e) {
      throw Exception('网络错误: $e');
    }
  }

  /// 玩家加倍
  Future<Map<String, dynamic>> doubleDown(String sessionId) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/game/$sessionId/double'),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        final error = jsonDecode(response.body);
        throw Exception(error['error'] ?? '加倍失败');
      }
    } catch (e) {
      throw Exception('网络错误: $e');
    }
  }

  /// 玩家分牌
  Future<Map<String, dynamic>> split(String sessionId) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/game/$sessionId/split'),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        final error = jsonDecode(response.body);
        throw Exception(error['error'] ?? '分牌失败');
      }
    } catch (e) {
      throw Exception('网络错误: $e');
    }
  }

  /// 开始新一局
  Future<Map<String, dynamic>> newRound(String sessionId, {int? betAmount}) async {
    try {
      final body = betAmount != null ? jsonEncode({'bet_amount': betAmount}) : null;
      final response = await http.post(
        Uri.parse('$baseUrl/api/game/$sessionId/new-round'),
        headers: {'Content-Type': 'application/json'},
        body: body,
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        final error = jsonDecode(response.body);
        throw Exception(error['error'] ?? '开始新局失败');
      }
    } catch (e) {
      throw Exception('网络错误: $e');
    }
  }

  /// 设置Blackjack赔付比例
  Future<Map<String, dynamic>> setBlackjackPayout(String sessionId, String ratio) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/game/$sessionId/set-blackjack-payout'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'ratio': ratio}),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        final error = jsonDecode(response.body);
        throw Exception(error['error'] ?? '设置失败');
      }
    } catch (e) {
      throw Exception('网络错误: $e');
    }
  }
}


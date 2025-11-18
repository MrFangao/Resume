/// 牌面工具类：将API返回的牌面字符串转换为PNG图片路径
class CardUtils {
  /// 将牌面字符串转换为图片文件名
  /// "AH" -> "HA.png", "10S" -> "ST.png", "7D" -> "D7.png"
  /// 格式：{suit}{rank}.png
  static String convertCardToImageName(String card) {
    if (card == '?') {
      return 'CoverCard.png';
    }

    if (card.length < 2) {
      return 'CoverCard.png'; // 默认返回暗牌
    }

    String rank;
    String suit;

    // 处理两位数的rank (10)
    if (card.startsWith('10')) {
      rank = 'T'; // 10用T表示
      suit = card[2];
    } else {
      rank = card[0];
      suit = card[1];
    }

    // 转换为图片文件名格式: suit + rank
    // 例如："AH" -> "HA.png", "7D" -> "D7.png", "10S" -> "ST.png"
    return '$suit$rank.png';
  }

  /// 获取完整的图片资源路径（PNG格式）
  static String getCardImagePath(String card) {
    final imageName = convertCardToImageName(card);
    return 'assets/Poker_card/$imageName';
  }

  /// 检查手牌中是否有A
  static bool hasAce(List<String> cards) {
    return cards.any((card) => card.startsWith('A'));
  }

  /// 计算手牌的点数（单个值）
  static int calculateHandValue(List<String> cards) {
    int total = 0;
    int aceCount = 0;

    for (String card in cards) {
      if (card == '?') continue; // 跳过暗牌

      String rank;
      if (card.startsWith('10')) {
        rank = '10';
      } else {
        rank = card[0];
      }

      if (rank == 'A') {
        aceCount++;
        total += 11; // 先按11计算
      } else if (rank == 'J' || rank == 'Q' || rank == 'K') {
        total += 10;
      } else {
        total += int.tryParse(rank) ?? 0;
      }
    }

    // 如果超过21且有A，将A从11调整为1
    while (total > 21 && aceCount > 0) {
      total -= 10;
      aceCount--;
    }

    return total;
  }

  /// 计算手牌的两个可能点数（当有A时）
  /// 返回一个包含两个值的列表：[较小值, 较大值]
  /// 如果没有A或两个值相同，返回包含单个值的列表
  static List<int> calculateHandValuesWithAce(List<String> cards) {
    int nonAceTotal = 0;
    int aceCount = 0;

    // 先计算非A牌的点数和A的数量
    for (String card in cards) {
      if (card == '?') continue; // 跳过暗牌

      String rank;
      if (card.startsWith('10')) {
        rank = '10';
      } else {
        rank = card[0];
      }

      if (rank == 'A') {
        aceCount++;
      } else if (rank == 'J' || rank == 'Q' || rank == 'K') {
        nonAceTotal += 10;
      } else {
        nonAceTotal += int.tryParse(rank) ?? 0;
      }
    }

    // 如果没有A，返回单个值
    if (aceCount == 0) {
      return [nonAceTotal];
    }

    // 计算所有A都算作1的点数（最小值）
    int minTotal = nonAceTotal + aceCount;

    // 计算至少一个A算作11的点数（最大值，不超过21）
    // 尝试让尽可能多的A算作11，但不超过21
    int maxTotal = nonAceTotal;
    int remainingAces = aceCount;
    
    // 先尝试让所有A都算11
    maxTotal += aceCount * 11;
    
    // 如果超过21，逐步将A从11调整为1
    while (maxTotal > 21 && remainingAces > 0) {
      maxTotal -= 10; // 将一个A从11调整为1
      remainingAces--;
    }

    // 如果两个值相同，只返回一个
    if (minTotal == maxTotal) {
      return [minTotal];
    }

    // 返回两个不同的值（较小值在前）
    return [minTotal, maxTotal];
  }
}


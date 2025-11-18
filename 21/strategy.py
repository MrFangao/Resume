# strategy.py

# 依据“庄家在软17停牌(S17)”的基础策略，优先给出分牌(SP)与加倍下注(D)建议，
# 其余情况给出 Hit(H)/Stand(S) 兜底建议，方便训练模式直接使用。

from typing import List


def _card_value(rank: str) -> int:
    if rank in ['J', 'Q', 'K']:
        return 10
    if rank == 'A':
        return 11
    return int(rank)


def _hand_total_and_soft(player_hand: List) -> tuple:
    total = 0
    aces = 0
    for c in player_hand:
        v = _card_value(c.rank)
        total += v
        if c.rank == 'A':
            aces += 1
    # 调整 A=1 以避免爆
    soft = False
    while total > 21 and aces > 0:
        total -= 10
        aces -= 1
    # 若保留至少一个 A 作为11，则为软手
    if any(c.rank == 'A' for c in player_hand):
        # 如果把所有 A 都当作1依旧<=21，则不是软手
        base_total = sum(1 if c.rank == 'A' else _card_value(c.rank) for c in player_hand)
        soft = (total != base_total)
    return total, soft


def _is_pair(player_hand: List) -> bool:
    return len(player_hand) == 2 and player_hand[0].rank == player_hand[1].rank


def advise_action(player_hand: List, dealer_upcard) -> str:
    """
    返回基础策略动作：'SP' 分牌 / 'D' 加倍 / 'H' 要牌 / 'S' 停牌。
    规则基于 S17，4或8副牌的常见表格（与用户图片一致的主流版本）。
    """
    dealer = dealer_upcard.rank

    # 先处理分牌
    if _is_pair(player_hand):
        rank = player_hand[0].rank
        return _pair_strategy(rank, dealer)

    total, soft = _hand_total_and_soft(player_hand)

    if soft:
        act = _soft_total_strategy(total, dealer)
        if act:
            return act
    else:
        act = _hard_total_strategy(total, dealer)
        if act:
            return act

    # 兜底
    return 'H' if total < 17 else 'S'


def _pair_strategy(rank: str, dealer: str) -> str:
    # 按列 2-10-A
    order = ['2','3','4','5','6','7','8','9','10','A']
    idx = order.index('10') if dealer in ['10','J','Q','K'] else order.index(dealer)

    # 表：仅保留分牌/停牌/要牌的主要规则（与图一致）
    # A,A 对所有dealer都分牌
    if rank == 'A':
        return 'SP'
    
    # 8,8 对 dealer 2-9,A 分牌，对 dealer 10 是 X/P (Surrender if possible, otherwise Split)
    # 由于我们不支持Surrender，所以对dealer 10也返回SP
    if rank == '8':
        if dealer in ['10', 'J', 'Q', 'K']:
            # 图片显示 X/P，但我们不支持Surrender，所以返回SP
            return 'SP'
        return 'SP'

    if rank == '2' or rank == '3':
        return 'SP' if idx <= order.index('7') else 'H'
    if rank == '4':
        return 'SP' if order.index('5') <= idx <= order.index('6') else 'H'
    if rank == '5':
        # 5,5 不分牌，作为硬10走加倍逻辑
        return _hard_total_strategy(10, dealer)
    if rank == '6':
        return 'SP' if idx <= order.index('6') else 'H'
    if rank == '7':
        return 'SP' if idx <= order.index('7') else 'H'
    if rank == '9':
        # 9,9 对 7,10,A 不分；其余分
        return 'SP' if dealer not in ['7','10','J','Q','K','A'] else ('S' if dealer in ['7','10','J','Q','K','A'] and dealer != '7' else 'S')
    if rank == '10' or rank in ['J','Q','K']:
        return 'S'

    return 'H'


def _soft_total_strategy(total: int, dealer: str) -> str:
    # 软 13-18 的加倍与停牌
    dealer_ten = dealer in ['10','J','Q','K']
    if total in [13,14]:
        if dealer in ['5','6']:
            return 'D'
        return 'H'
    if total in [15,16]:
        if dealer in ['4','5','6']:
            return 'D'
        return 'H'
    if total == 17:
        if dealer in ['3','4','5','6']:
            return 'D'
        return 'H'
    if total == 18:
        if dealer in ['3','4','5','6']:
            return 'D'
        if dealer in ['2','7','8']:
            return 'S'
        return 'H'
    if total >= 19:
        return 'S'
    return None


def _hard_total_strategy(total: int, dealer: str) -> str:
    dealer_ten = dealer in ['10','J','Q','K']
    if total <= 8:
        return 'H'
    if total == 9:
        return 'D' if dealer in ['3','4','5','6'] else 'H'
    if total == 10:
        return 'D' if dealer not in ['10','J','Q','K','A'] else 'H'
    if total == 11:
        return 'D' if dealer != 'A' else 'H'
    if total == 12:
        return 'S' if dealer in ['4','5','6'] else 'H'
    if 13 <= total <= 16:
        return 'S' if dealer in ['2','3','4','5','6'] else 'H'
    return 'S'


def get_all_valid_actions(player_hand: List, dealer_upcard) -> List[str]:
    """
    获取所有可能的正确行动。
    在某些情况下，可能有多个可接受的行动（例如，Double如果不可用，可以Hit或Stand）。
    
    Returns:
        List of valid action codes: ['SP', 'D', 'H', 'S'] 等
    """
    primary_action = advise_action(player_hand, dealer_upcard)
    valid_actions = [primary_action]
    
    # 如果主要行动是D（Double），但Double不可用（手牌数>2），则H和S也是可接受的
    if primary_action == 'D' and len(player_hand) > 2:
        valid_actions.extend(['H', 'S'])
    
    # 如果主要行动是DS（Double if possible, otherwise Stand），则D和S都是可接受的
    # 注意：我们的策略表返回的是'D'，但在某些情况下Stand也是可接受的
    # 由于我们的策略表不返回'DS'，这里暂时不处理
    
    return list(set(valid_actions))  # 去重





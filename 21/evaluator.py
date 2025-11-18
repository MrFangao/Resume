# evaluator.py

import random
from typing import List, Tuple, Dict

from card import Card
from strategy import advise_action


def _value(hand: List[Card]) -> int:
    total = 0
    aces = 0
    for c in hand:
        if c.rank in ['J', 'Q', 'K']:
            total += 10
        elif c.rank == 'A':
            total += 11
            aces += 1
        else:
            total += int(c.rank)
    while total > 21 and aces > 0:
        total -= 10
        aces -= 1
    return total


def _draw(deck_list: List[Card]) -> Card:
    return deck_list.pop()


def _dealer_play(dealer_hand: List[Card], deck_list: List[Card]) -> None:
    while _value(dealer_hand) < 17:
        dealer_hand.append(_draw(deck_list))


def _play_follow_basic(player_hand: List[Card], dealer_up: Card, deck_list: List[Card]) -> None:
    # 循环直到停牌或爆牌；'D'在>2张时不可用，视为'H'
    while True:
        total = _value(player_hand)
        if total >= 21:
            break
        act = advise_action(player_hand, dealer_up)
        if act == 'S':
            break
        if act == 'D' and len(player_hand) == 2:
            player_hand.append(_draw(deck_list))
            break
        # 'SP' 在模拟随后的流程里忽略，按'H'继续（分牌另行实现）
        player_hand.append(_draw(deck_list))


def simulate_ev(remaining_cards: List[Card], player_hand: List[Card], dealer_hand: List[Card], base_bet: int = 1, trials: int = 5000) -> Dict[str, Tuple[float, float]]:
    """
    返回每个动作的 (winrate, EV)；EV 以每单位下注为基准。
    支持动作：'H','S','D'。'SP' 可在外层决定是否参与比较。
    """
    actions = ['H', 'S', 'D']
    stats = {a: {'profit': 0.0, 'wins': 0, 'trials': 0} for a in actions}

    for action in actions:
        for _ in range(trials):
            deck_list = remaining_cards.copy()
            random.shuffle(deck_list)

            ph = [Card(c.rank, c.suit) for c in player_hand]
            dh = [Card(c.rank, c.suit) for c in dealer_hand]

            # 从牌堆中移除已在手的牌的近似做法：忽略（因 deck_list 是剩余牌本身，主程应传入当前剩余牌）

            # 执行动作
            if action == 'S':
                pass
            elif action == 'H':
                ph.append(_draw(deck_list))
                _play_follow_basic(ph, dh[0], deck_list)
            elif action == 'D':
                # 玩家抽一张停
                ph.append(_draw(deck_list))
            else:
                continue

            # 玩家爆则直接判负
            if _value(ph) > 21:
                profit = -base_bet if action != 'D' else -2 * base_bet
                stats[action]['profit'] += profit
                stats[action]['trials'] += 1
                continue

            # 庄家走完
            _dealer_play(dh, deck_list)

            pt = _value(ph)
            dt = _value(dh)

            if dt > 21 or pt > dt:
                profit = base_bet if action != 'D' else 2 * base_bet
                stats[action]['wins'] += 1
            elif pt == dt:
                profit = 0
            else:
                profit = -base_bet if action != 'D' else -2 * base_bet

            stats[action]['profit'] += profit
            stats[action]['trials'] += 1

    result: Dict[str, Tuple[float, float]] = {}
    for a in actions:
        t = max(1, stats[a]['trials'])
        winrate = stats[a]['wins'] / t
        ev = stats[a]['profit'] / (t * base_bet)
        result[a] = (round(winrate, 4), round(ev, 4))
    return result





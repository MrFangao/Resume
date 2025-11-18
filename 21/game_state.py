# game_state.py

from typing import Dict, List, Optional, Tuple
from datetime import datetime
from Deck import Deck
from Counter import HiLoCounter
from card import Card
from strategy import advise_action, get_all_valid_actions


class GameState:
    """
    Manages Blackjack game state for API backend.
    All card representations are JSON-safe strings (e.g., 'AH', '7D').
    """
    
    def __init__(self, num_decks=1, initial_chips=500, bet_amount=10, blackjack_payout_ratio="3:2"):
        """
        Initialize game state.
        
        Args:
            num_decks: Number of decks to use
            initial_chips: Starting chip count
            bet_amount: Bet amount per hand
            blackjack_payout_ratio: Blackjack payout ratio, either "3:2" (1.5x) or "6:5" (1.2x)
        """
        self.deck = Deck(num_decks=num_decks)
        self.counter = HiLoCounter()
        self.initial_chips = initial_chips
        self.chips = initial_chips
        self.bet = bet_amount
        self.num_decks = num_decks
        self.blackjack_payout_ratio = blackjack_payout_ratio  # "3:2" or "6:5"
        
        # 支持Split：使用列表存储多手牌
        self.player_hands = []  # List[List[Card]] - 支持多手牌
        self.hand_bets = []  # List[int] - 每手牌的下注
        self.current_hand_index = 0  # 当前正在操作的手牌索引
        self.dealer_hand = []
        self._last_result = None  # Store last round result (可以是列表，每手牌一个结果)
        self.bet_per_hand = bet_amount  # 默认每手牌的下注（向后兼容）
        
        # 向后兼容：player_hand指向当前手牌
        self.player_hand = []
        
        # 成功率追踪
        self._strategy_tracking = []  # List of (player_hand, dealer_upcard, suggested_action, player_action, is_correct)
        self._total_decisions = 0  # 总决策次数
        self._correct_decisions = 0  # 正确决策次数
        
        # 历史记录
        self._game_history = []  # List of game round history records
    
    def _get_blackjack_multiplier(self):
        """Get Blackjack payout multiplier based on ratio."""
        if self.blackjack_payout_ratio == "6:5":
            return 1.2  # 6:5 = 1.2倍
        else:  # 默认 3:2
            return 1.5  # 3:2 = 1.5倍
    
    def set_blackjack_payout_ratio(self, ratio):
        """
        Set Blackjack payout ratio.
        
        Args:
            ratio: Either "3:2" or "6:5"
        """
        if ratio not in ["3:2", "6:5"]:
            raise ValueError("Blackjack payout ratio must be '3:2' or '6:5'")
        self.blackjack_payout_ratio = ratio

    def start_new_round(self):
        """
        Deal a new hand. Automatically reshuffles if needed.
        
        Returns:
            Dict with initial game status
        """
        # Auto-shuffle logic: 剩余30%时自动洗牌（保留筹码，只重置计数）
        if self.deck.needs_shuffle(threshold=0.3):
            self.deck.shuffle()
            self.counter.reset()

        # 检查筹码是否足够
        if self.chips < self.bet:
            raise ValueError("Insufficient chips to start new round")
        
        # 记录本局开始时的筹码（用于计算盈利/损失）
        self._round_start_chips = self.chips
        
        # 扣除初始下注
        self.chips -= self.bet

        # 重置为单 hand 状态
        self.player_hands = [[self.draw_and_count(), self.draw_and_count()]]
        self.hand_bets = [self.bet]  # 第一手牌的下注
        self.current_hand_index = 0
        self.dealer_hand = [self.draw_and_count(), self.draw_and_count()]
        self._last_result = None
        self.bet_per_hand = self.bet
        self.player_hand = self.player_hands[0]  # 向后兼容
        
        # 注意：不重置成功率追踪，让成功率在整个游戏会话中持续累积
        
        # 检查玩家是否有Blackjack
        player_has_blackjack = self._is_blackjack(self.player_hands[0])
        dealer_has_blackjack = self._is_blackjack(self.dealer_hand)
        
        # 如果玩家有Blackjack而庄家没有，立即结束游戏并赔付
        if player_has_blackjack and not dealer_has_blackjack:
            hand_bet = self.hand_bets[0]
            multiplier = self._get_blackjack_multiplier()
            # 赔付：下注已经扣除，所以需要返还下注 + multiplier倍下注 = (1 + multiplier)倍下注
            self.chips += int(hand_bet * (1 + multiplier))
            ratio_text = self.blackjack_payout_ratio
            self._last_result = [f"Hand 1: Blackjack! Player wins ({ratio_text} payout)"]
            
            # 记录历史（Blackjack立即结束的情况）
            profit_loss = self.chips - self._round_start_chips
            player_hands_str = [[card.to_string() if isinstance(card, Card) else card 
                                for card in self.player_hands[0]]]
            dealer_hand_str = [card.to_string() if isinstance(card, Card) else card 
                             for card in self.dealer_hand]
            
            history_entry = {
                'timestamp': datetime.now().isoformat(),
                'player_hands': player_hands_str,
                'dealer_hand': dealer_hand_str,
                'profit_loss': profit_loss,
                'win_rate': 100.0,  # Blackjack算100%胜率
                'wins': 1,
                'total_hands': 1,
                'chips_after': self.chips,
                'results': self._last_result
            }
            self._game_history.append(history_entry)
            if len(self._game_history) > 100:
                self._game_history = self._game_history[-100:]
            
            return self.get_status(reveal_dealer=True)
        
        # 如果双方都有Blackjack，平局
        if player_has_blackjack and dealer_has_blackjack:
            self._last_result = [f"Hand 1: Both have Blackjack - Push"]
            
            # 记录历史（双方Blackjack平局的情况）
            profit_loss = self.chips - self._round_start_chips
            player_hands_str = [[card.to_string() if isinstance(card, Card) else card 
                                for card in self.player_hands[0]]]
            dealer_hand_str = [card.to_string() if isinstance(card, Card) else card 
                             for card in self.dealer_hand]
            
            history_entry = {
                'timestamp': datetime.now().isoformat(),
                'player_hands': player_hands_str,
                'dealer_hand': dealer_hand_str,
                'profit_loss': profit_loss,
                'win_rate': 0.0,  # 平局算0%胜率（不算赢也不算输）
                'wins': 0,
                'total_hands': 1,
                'chips_after': self.chips,
                'results': self._last_result
            }
            self._game_history.append(history_entry)
            if len(self._game_history) > 100:
                self._game_history = self._game_history[-100:]
            
            return self.get_status(reveal_dealer=True)
        
        return self.get_status()

    def draw_and_count(self):
        """Draw a card and update counter."""
        card = self.deck.draw_card()
        self.counter.update(card)
        return card

    def hand_value(self, hand):
        """
        Calculate hand value (handles Aces as 1 or 11).
        
        Args:
            hand: List of Card objects or card strings
            
        Returns:
            Total hand value
        """
        total = 0
        aces = 0
        
        for card in hand:
            if isinstance(card, str):
                # Handle string like "AH" or "?"
                if card == '?':
                    continue
                card_obj = Card.from_string(card)
            else:
                card_obj = card
                
            val = card_obj.get_value()
            total += val
            if card_obj.rank == 'A':
                aces += 1

        # Adjust Aces from 11 to 1 if needed
        while total > 21 and aces > 0:
            total -= 10
            aces -= 1
        return total
    
    def _is_soft_17(self, hand):
        """
        检查是否是软17（有A且可以算作11，总值为17）。
        
        Args:
            hand: List of Card objects or card strings
            
        Returns:
            True if soft 17, False otherwise
        """
        # 计算不使用A作为11的情况下的总值
        base_total = 0
        ace_count = 0
        
        for card in hand:
            if isinstance(card, str):
                if card == '?':
                    continue
                card_obj = Card.from_string(card)
            else:
                card_obj = card
            
            if card_obj.rank == 'A':
                ace_count += 1
                base_total += 1  # A作为1
            else:
                base_total += card_obj.get_value()
        
        # 如果有A，尝试将其作为11
        if ace_count > 0:
            soft_total = base_total + 10  # 将一个A从1改为11
            # 如果总值是17，且不超过21，则是软17
            if soft_total == 17:
                return True
        
        return False
    
    def _is_blackjack(self, hand):
        """
        检查是否是Blackjack（两张牌，A + 10/J/Q/K，总值为21）。
        
        Args:
            hand: List of Card objects or card strings
            
        Returns:
            True if blackjack, False otherwise
        """
        # Blackjack必须是恰好两张牌
        if len(hand) != 2:
            return False
        
        # 检查是否有A和10点牌（10/J/Q/K）
        has_ace = False
        has_ten = False
        
        for card in hand:
            if isinstance(card, str):
                if card == '?':
                    continue
                card_obj = Card.from_string(card)
            else:
                card_obj = card
            
            if card_obj.rank == 'A':
                has_ace = True
            elif card_obj.rank in ['10', 'J', 'Q', 'K']:
                has_ten = True
        
        # Blackjack需要同时有A和10点牌
        return has_ace and has_ten
    
    def _record_decision(self, player_action: str):
        """
        记录玩家的决策并判断是否正确。
        
        Args:
            player_action: 玩家的实际行动 ('H', 'S', 'D', 'SP')
        """
        if self.current_hand_index >= len(self.player_hands):
            return
        
        current_hand = self.player_hands[self.current_hand_index]
        if len(self.dealer_hand) == 0:
            return
        
        try:
            dealer_upcard = self.dealer_hand[0]
            dealer_card = dealer_upcard if isinstance(dealer_upcard, Card) else Card.from_string(dealer_upcard)
            player_cards = [card if isinstance(card, Card) else Card.from_string(card) 
                           for card in current_hand]
            
            # 获取建议行动和所有有效行动
            suggested_action = advise_action(player_cards, dealer_card)
            valid_actions = get_all_valid_actions(player_cards, dealer_card)
            
            # 判断是否正确
            is_correct = player_action in valid_actions
            
            # 记录
            self._total_decisions += 1
            if is_correct:
                self._correct_decisions += 1
            
            self._strategy_tracking.append({
                'player_hand': [c.to_string() if isinstance(c, Card) else c for c in current_hand],
                'dealer_upcard': dealer_card.to_string() if isinstance(dealer_card, Card) else str(dealer_card),
                'suggested_action': suggested_action,
                'valid_actions': valid_actions,
                'player_action': player_action,
                'is_correct': is_correct
            })
        except Exception:
            # 如果出错，不记录
            pass
    
    def get_strategy_score(self) -> Tuple[int, int, float]:
        """
        获取策略得分。
        
        Returns:
            Tuple of (correct_decisions, total_decisions, percentage)
        """
        if self._total_decisions == 0:
            return (0, 0, 0.0)
        percentage = (self._correct_decisions / self._total_decisions) * 100.0
        return (self._correct_decisions, self._total_decisions, percentage)

    def player_hit(self, hand_index=None):
        """
        Player draws a card for the specified hand (or current hand).
        
        Args:
            hand_index: Index of hand to hit (defaults to current_hand_index)
        
        Returns:
            Dict with updated game status
        """
        if hand_index is None:
            hand_index = self.current_hand_index
        
        if hand_index >= len(self.player_hands):
            raise ValueError(f"Invalid hand index: {hand_index}")
        
        # 记录决策
        self._record_decision('H')
        
        card = self.draw_and_count()
        self.player_hands[hand_index].append(card)
        self.player_hand = self.player_hands[self.current_hand_index]  # 向后兼容
        
        player_total = self.hand_value(self.player_hands[hand_index])
        # If player busts on this hand, move to next hand or finish round
        if player_total > 21:
            # 如果所有手牌都完成了，则结束
            if self._all_hands_complete():
                self._last_result = self.evaluate_all_hands()
                return self.get_status(reveal_dealer=True)
            # 否则移动到下一手牌
            else:
                self._move_to_next_hand()
        
        return self.get_status()

    def player_stand(self, hand_index=None):
        """
        Player stands on the specified hand (or current hand).
        If all hands are complete, dealer plays.
        
        Args:
            hand_index: Index of hand to stand (defaults to current_hand_index)
        
        Returns:
            Dict with game status
        """
        if hand_index is None:
            hand_index = self.current_hand_index
        
        if hand_index >= len(self.player_hands):
            raise ValueError(f"Invalid hand index: {hand_index}")
        
        # 记录决策
        self._record_decision('S')
        
        # 停牌：移动到下一手牌（增加索引，标记当前手牌已完成）
        self.current_hand_index += 1
        
        # 如果所有手牌都完成了，庄家开始操作
        if self._all_hands_complete():
            # 检查是否有任何手牌是Blackjack
            # 如果有Blackjack，庄家不需要继续要牌（除非庄家也是Blackjack，但这种情况在start_new_round已经处理了）
            has_any_blackjack = False
            for hand in self.player_hands:
                if self._is_blackjack(hand):
                    has_any_blackjack = True
                    break
            
            # 如果玩家有Blackjack，庄家不需要继续要牌
            if not has_any_blackjack:
                # Dealer plays: S17规则（软17需要继续要牌）
                while True:
                    dealer_value = self.hand_value(self.dealer_hand)
                    if dealer_value >= 17:
                        # 检查是否是软17（有A且可以算作11）
                        is_soft_17 = self._is_soft_17(self.dealer_hand)
                        if not is_soft_17:
                            # 硬17或以上，停牌
                            break
                        # 软17，继续要牌（S17规则）
                    # 小于17或软17，继续要牌
                    self.dealer_hand.append(self.draw_and_count())
            
            self._last_result = self.evaluate_all_hands()
            return self.get_status(reveal_dealer=True)
        
        # 如果还有未完成的手牌，更新player_hand指向当前手牌
        if self.current_hand_index < len(self.player_hands):
            self.player_hand = self.player_hands[self.current_hand_index]
        
        return self.get_status()

    def player_double(self, hand_index=None):
        """
        Player doubles down on the specified hand (double bet, draw one card, then stand).
        
        Args:
            hand_index: Index of hand to double (defaults to current_hand_index)
        
        Returns:
            Dict with game status
        """
        if hand_index is None:
            hand_index = self.current_hand_index
        
        if hand_index >= len(self.player_hands):
            raise ValueError(f"Invalid hand index: {hand_index}")
        
        if len(self.player_hands[hand_index]) != 2:
            raise ValueError("Double down only available with 2 cards")
        
        # 检查筹码是否足够
        current_bet = self.hand_bets[hand_index]
        if self.chips < current_bet:
            raise ValueError("Insufficient chips to double down")
        
        # 扣除额外的下注
        self.chips -= current_bet
        
        # 记录决策
        self._record_decision('D')
        
        # 加倍这一手牌的下注
        self.hand_bets[hand_index] *= 2
        card = self.draw_and_count()
        self.player_hands[hand_index].append(card)
        self.player_hand = self.player_hands[self.current_hand_index]  # 向后兼容
        
        # After double, automatically stand on this hand
        return self.player_stand(hand_index)
    
    def player_double_down(self):
        """Alias for player_double() for backward compatibility."""
        return self.player_double()

    def player_split(self):
        """
        Split the current hand into two hands.
        Requires: current hand has exactly 2 cards of the same rank.
        
        Returns:
            Dict with updated game status
        """
        if self.current_hand_index >= len(self.player_hands):
            raise ValueError("No active hand to split")
        
        current_hand = self.player_hands[self.current_hand_index]
        
        if len(current_hand) != 2:
            raise ValueError("Split only available with exactly 2 cards")
        
        # 检查两张牌是否是相同的rank
        card1 = current_hand[0] if isinstance(current_hand[0], Card) else Card.from_string(current_hand[0])
        card2 = current_hand[1] if isinstance(current_hand[1], Card) else Card.from_string(current_hand[1])
        
        if card1.rank != card2.rank:
            raise ValueError("Split only available when both cards have the same rank")
        
        # 获取当前手牌的下注
        current_bet = self.hand_bets[self.current_hand_index]
        
        # 检查筹码是否足够（需要额外下注）
        if self.chips < current_bet:
            raise ValueError("Insufficient chips to split")
        
        # 扣除额外的下注
        self.chips -= current_bet
        
        # 记录决策
        self._record_decision('SP')
        
        # 将第一张牌保留在第一手，第二张牌移到新手
        first_card = current_hand[0]
        second_card = current_hand[1]
        
        # 更新第一手牌：只有第一张牌
        self.player_hands[self.current_hand_index] = [first_card]
        
        # 给第一手发一张新牌
        new_card1 = self.draw_and_count()
        self.player_hands[self.current_hand_index].append(new_card1)
        
        # 创建新手（第二手），使用相同的下注
        new_hand = [second_card]
        new_card2 = self.draw_and_count()
        new_hand.append(new_card2)
        self.player_hands.append(new_hand)
        self.hand_bets.append(current_bet)  # 新手使用相同的下注
        
        self.player_hand = self.player_hands[self.current_hand_index]  # 向后兼容
        
        return self.get_status()
    
    def _move_to_next_hand(self):
        """Move to the next active hand (not busted)."""
        while self.current_hand_index < len(self.player_hands):
            total = self.hand_value(self.player_hands[self.current_hand_index])
            if total <= 21:
                break
            self.current_hand_index += 1
        if self.current_hand_index < len(self.player_hands):
            self.player_hand = self.player_hands[self.current_hand_index]  # 向后兼容
    
    def _all_hands_complete(self):
        """Check if all hands are complete (busted or stood)."""
        # 如果当前手牌索引超出范围，说明所有手牌都完成了
        if self.current_hand_index >= len(self.player_hands):
            return True
        # 或者检查所有手牌是否都爆牌了
        for hand in self.player_hands:
            if self.hand_value(hand) <= 21:
                return False
        return True
    
    def evaluate_all_hands(self):
        """
        Evaluate all player hands against dealer and update chips.
        
        Returns:
            List of result messages, one per hand
        """
        dealer_total = self.hand_value(self.dealer_hand)
        dealer_has_blackjack = self._is_blackjack(self.dealer_hand)
        results = []
        
        # 统计胜率
        total_hands = len(self.player_hands)
        wins = 0
        player_hands_str = []
        dealer_hand_str = []
        
        for i, hand in enumerate(self.player_hands):
            player_total = self.hand_value(hand)
            hand_bet = self.hand_bets[i] if i < len(self.hand_bets) else self.bet_per_hand
            player_has_blackjack = self._is_blackjack(hand)
            
            # 记录手牌
            hand_str = [card.to_string() if isinstance(card, Card) else card for card in hand]
            player_hands_str.append(hand_str)
            
            # 记录庄家手牌（只在第一手时记录一次）
            if i == 0:
                dealer_hand_str = [card.to_string() if isinstance(card, Card) else card 
                                 for card in self.dealer_hand]
            
            # 计算净收益/损失
            # hand_bet已经是总下注（如果是double，则已经是2倍）
            # 初始下注和double时的额外下注都已经在相应的时候扣除了
            # 所以这里只需要计算输赢的差额
            
            if player_total > 21:
                # 玩家爆牌：总下注已经扣除，不需要再扣除
                results.append(f"Hand {i+1}: Bust - Dealer wins")
            elif player_has_blackjack and not dealer_has_blackjack:
                # 玩家Blackjack，庄家不是：赔付
                multiplier = self._get_blackjack_multiplier()
                # 下注已经扣除，所以需要返还下注 + multiplier倍下注 = (1 + multiplier)倍下注
                self.chips += int(hand_bet * (1 + multiplier))
                ratio_text = self.blackjack_payout_ratio
                results.append(f"Hand {i+1}: Blackjack! Player wins ({ratio_text} payout)")
                wins += 1
            elif dealer_has_blackjack and not player_has_blackjack:
                # 庄家Blackjack，玩家不是：庄家赢
                results.append(f"Hand {i+1}: Dealer Blackjack - Dealer wins")
            elif player_has_blackjack and dealer_has_blackjack:
                # 双方都是Blackjack：平局
                results.append(f"Hand {i+1}: Both have Blackjack - Push")
            elif dealer_total > 21 or player_total > dealer_total:
                # 玩家赢：赔付2倍总下注（21点规则：赢了赔付2倍下注）
                # 例如：下注10，double后总下注20，赢了赔付40
                self.chips += hand_bet * 2
                results.append(f"Hand {i+1}: Player wins")
                wins += 1
            elif player_total == dealer_total:
                # 平局：总下注已经扣除，不需要再扣除，也不赔付
                results.append(f"Hand {i+1}: Push")
            else:
                # 庄家赢：总下注已经扣除，不需要再扣除
                results.append(f"Hand {i+1}: Dealer wins")
        
        # 记录历史
        profit_loss = self.chips - getattr(self, '_round_start_chips', self.chips)
        win_rate = (wins / total_hands * 100.0) if total_hands > 0 else 0.0
        
        history_entry = {
            'timestamp': datetime.now().isoformat(),
            'player_hands': player_hands_str,
            'dealer_hand': dealer_hand_str,
            'profit_loss': profit_loss,
            'win_rate': round(win_rate, 1),
            'wins': wins,
            'total_hands': total_hands,
            'chips_after': self.chips,
            'results': results
        }
        self._game_history.append(history_entry)
        
        # 限制历史记录数量（保留最近100局）
        if len(self._game_history) > 100:
            self._game_history = self._game_history[-100:]
        
        return results

    def evaluate_round(self):
        """
        Evaluate round outcome and update chips (backward compatibility for single hand).
        
        Returns:
            Result message string
        """
        if len(self.player_hands) == 0:
            return "No hands to evaluate"
        
        # 对于单hand情况，使用第一手
        player_total = self.hand_value(self.player_hands[0])
        dealer_total = self.hand_value(self.dealer_hand)
        hand_bet = self.hand_bets[0] if len(self.hand_bets) > 0 else self.bet_per_hand
        player_has_blackjack = self._is_blackjack(self.player_hands[0])
        dealer_has_blackjack = self._is_blackjack(self.dealer_hand)

        # 计算净收益/损失
        # hand_bet已经是总下注（如果是double，则已经是2倍）
        # 初始下注和double时的额外下注都已经在相应的时候扣除了
        # 所以这里只需要计算输赢的差额

        if player_total > 21:
            # 玩家爆牌：总下注已经扣除，不需要再扣除
            return "Player busts - Dealer wins"
        elif player_has_blackjack and not dealer_has_blackjack:
            # 玩家Blackjack，庄家不是：赔付
            multiplier = self._get_blackjack_multiplier()
            # 下注已经扣除，所以需要返还下注 + multiplier倍下注 = (1 + multiplier)倍下注
            self.chips += int(hand_bet * (1 + multiplier))
            ratio_text = self.blackjack_payout_ratio
            return f"Blackjack! Player wins ({ratio_text} payout)"
        elif dealer_has_blackjack and not player_has_blackjack:
            # 庄家Blackjack，玩家不是：庄家赢
            return "Dealer Blackjack - Dealer wins"
        elif player_has_blackjack and dealer_has_blackjack:
            # 双方都是Blackjack：平局
            return "Both have Blackjack - Push"
        elif dealer_total > 21 or player_total > dealer_total:
            # 玩家赢：赔付2倍总下注（21点规则：赢了赔付2倍下注）
            # 例如：下注10，double后总下注20，赢了赔付40
            self.chips += hand_bet * 2
            return "Player wins"
        elif player_total == dealer_total:
            # 平局：总下注已经扣除，不需要再扣除，也不赔付
            return "Push"
        else:
            # 庄家赢：总下注已经扣除，不需要再扣除
            return "Dealer wins"

    def get_status(self, reveal_dealer=False):
        """
        Get current game status as JSON-safe dict.
        
        Args:
            reveal_dealer: Whether to reveal dealer's hidden card
        
        Returns:
            Dict with all game state information
        """
        player_total = self.hand_value(self.player_hand)
        dealer_total = self.hand_value(self.dealer_hand) if reveal_dealer else None
        
        # Convert hands to JSON-safe strings
        player_hand_str = [card.to_string() if isinstance(card, Card) else card 
                          for card in self.player_hand]
        
        if reveal_dealer:
            dealer_hand_str = [card.to_string() if isinstance(card, Card) else card 
                              for card in self.dealer_hand]
        else:
            # Show only first card, second as "?"
            if len(self.dealer_hand) > 0:
                dealer_hand_str = [
                    self.dealer_hand[0].to_string() if isinstance(self.dealer_hand[0], Card) else self.dealer_hand[0],
                    "?"
                ]
            else:
                dealer_hand_str = []

        decks_remaining = len(self.deck.cards) / 52 if self.deck.cards else 0
        cards_remaining = len(self.deck.cards) if self.deck.cards else 0
        total_cards = 52 * self.num_decks
        remaining_ratio = cards_remaining / total_cards if total_cards > 0 else 0
        
        # 处理多手牌
        player_hands_str = []
        player_totals = []
        current_hand_total = None
        
        for i, hand in enumerate(self.player_hands):
            hand_str = [card.to_string() if isinstance(card, Card) else card 
                       for card in hand]
            hand_total = self.hand_value(hand)
            player_hands_str.append(hand_str)
            player_totals.append(hand_total)
            
            if i == self.current_hand_index:
                current_hand_total = hand_total
        
        # 如果没有当前手牌，使用第一手
        if current_hand_total is None and len(player_totals) > 0:
            current_hand_total = player_totals[0]
        
        # 检查是否可以Split（当前手牌有两张相同rank的牌）
        can_split = False
        if self.current_hand_index < len(self.player_hands):
            current_hand = self.player_hands[self.current_hand_index]
            if len(current_hand) == 2:
                card1 = current_hand[0] if isinstance(current_hand[0], Card) else Card.from_string(current_hand[0])
                card2 = current_hand[1] if isinstance(current_hand[1], Card) else Card.from_string(current_hand[1])
                current_bet = self.hand_bets[self.current_hand_index] if self.current_hand_index < len(self.hand_bets) else self.bet_per_hand
                if card1.rank == card2.rank and self.chips >= current_bet:
                    can_split = True
        
        # 策略建议（仅在游戏进行中且未爆牌时）
        suggestion = None
        if not reveal_dealer and current_hand_total is not None and current_hand_total <= 21 and \
            self.current_hand_index < len(self.player_hands) and len(self.dealer_hand) > 0:
            try:
                current_hand = self.player_hands[self.current_hand_index]
                dealer_upcard = self.dealer_hand[0]
                # 转换为Card对象以便调用advise_action
                dealer_card = dealer_upcard if isinstance(dealer_upcard, Card) else Card.from_string(dealer_upcard)
                player_cards = [card if isinstance(card, Card) else Card.from_string(card) 
                               for card in current_hand]
                suggestion = advise_action(player_cards, dealer_card)
            except Exception:
                suggestion = None
        
        status = {
            # 向后兼容字段
            "player_hand": player_hand_str,
            "player_total": player_total,
            # 多手牌支持
            "player_hands": player_hands_str,
            "player_totals": player_totals,
            "current_hand_index": self.current_hand_index,
            "current_hand_total": current_hand_total,
            "num_hands": len(self.player_hands),
            # 庄家信息
            "dealer_hand": dealer_hand_str,
            "dealer_total": dealer_total,
            # 游戏状态
            "chips": self.chips,
            "bet": self.hand_bets[self.current_hand_index] if self.current_hand_index < len(self.hand_bets) else self.bet_per_hand,  # 当前手牌的下注
            "hand_bets": self.hand_bets,  # 所有手牌的下注列表
            "running_count": self.counter.get_running_count(),
            "true_count": self.counter.get_true_count(decks_remaining),
            # 剩余牌数信息
            "cards_remaining": cards_remaining,
            "total_cards": total_cards,
            "remaining_ratio": remaining_ratio,
            # 策略建议
            "suggestion": suggestion,
            # 操作可用性（基于当前手牌）
            "can_hit": current_hand_total is not None and current_hand_total <= 21 and not reveal_dealer,
            "can_stand": current_hand_total is not None and current_hand_total <= 21 and not reveal_dealer,
            "can_double": (self.current_hand_index < len(self.player_hands) and 
                          len(self.player_hands[self.current_hand_index]) == 2 and 
                          current_hand_total is not None and current_hand_total <= 21 and not reveal_dealer),
            "can_split": can_split and not reveal_dealer,
            # 游戏状态
            "is_busted": current_hand_total is not None and current_hand_total > 21,
            "is_resolved": reveal_dealer,
            "result": self._last_result if reveal_dealer else None,
            # Blackjack赔付比例
            "blackjack_payout_ratio": self.blackjack_payout_ratio,
            # 策略得分
            "strategy_score": {
                "correct": self._correct_decisions,
                "total": self._total_decisions,
                "percentage": round((self._correct_decisions / self._total_decisions * 100.0) if self._total_decisions > 0 else 0.0, 1)
            },
            # 历史记录（最近的历史，倒序排列）
            "game_history": self._game_history[-20:] if len(self._game_history) > 20 else self._game_history
        }
        
        return status

    # Backward compatibility methods
    def get_player_hand(self):
        """Get current player hand as card strings (backward compatibility)."""
        if len(self.player_hands) == 0:
            return []
        return [card.to_string() if isinstance(card, Card) else card 
                for card in self.player_hands[self.current_hand_index if self.current_hand_index < len(self.player_hands) else 0]]

    def get_dealer_hand(self, reveal=False):
        """Get dealer hand as card strings."""
        if reveal:
            return [card.to_string() if isinstance(card, Card) else card 
                    for card in self.dealer_hand]
        else:
            if len(self.dealer_hand) > 0:
                return [
                    self.dealer_hand[0].to_string() if isinstance(self.dealer_hand[0], Card) else self.dealer_hand[0],
                    "?"
                ]
            return []

    def get_running_count(self):
        """Get running count."""
        return self.counter.get_running_count()

    def get_true_count(self):
        """Get true count."""
        decks_remaining = len(self.deck.cards) / 52 if self.deck.cards else 0
        return self.counter.get_true_count(decks_remaining)

    def get_chips(self):
        """Get current chip count."""
        return self.chips

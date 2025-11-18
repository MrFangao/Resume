# main.py

from game_state import GameState
from strategy import advise_action
from evaluator import simulate_ev

def main():
    print("🎮 Blackjack Hi-Lo 训练器")
    num_decks = int(input("请选择副数（输入1或2）: ").strip())
    initial_chips = int(input("设置初始筹码（如 500 或 1000）: ").strip())

    game = GameState(num_decks=num_decks, initial_chips=initial_chips)

    while True:
        print("\n", "-" * 40)
        print(f"💰 当前筹码: {game.get_chips()}")

        try:
            bet = int(input("请输入本局下注额（例如10）: ").strip())
            game.bet = bet
        except ValueError:
            print("❌ 输入无效，使用默认下注 10")
            game.bet = 10

        game.start_new_round()

        print("\n🂠 玩家手牌:", game.get_player_hand())
        print("🃏 庄家明牌:", game.get_dealer_hand(reveal=False))
        try:
            advice = advise_action(game.player_hand, game.dealer_hand[0])
            print(f"📊 策略建议：{advice} (SP=分牌, D=加倍, H=要牌, S=停牌)")
            # 仅评估 H/S/D（三个动作）
            evs = simulate_ev(game.deck.cards, game.player_hand, game.dealer_hand, base_bet=1, trials=3000)
            if advice in evs:
                wr, ev = evs[advice]
                print(f"🔎 推荐动作胜率: {wr:.2%}，EV: {ev:+.3f} 单位/手")
            else:
                # 若推荐为 SP，展示 H/S/D 的最佳者
                best = max(evs.items(), key=lambda x: x[1][1])
                print(f"🔎 近似评估(不含分牌)：最佳动作 {best[0]}，胜率 {best[1][0]:.2%}，EV {best[1][1]:+.3f}")
        except Exception:
            pass

        # 玩家操作
        while True:
            try:
                advice = advise_action(game.player_hand, game.dealer_hand[0])
                print(f"📊 当前策略建议：{advice}")
                evs = simulate_ev(game.deck.cards, game.player_hand, game.dealer_hand, base_bet=1, trials=2000)
                if advice in evs:
                    wr, ev = evs[advice]
                    print(f"   ↳ 胜率 {wr:.2%}，EV {ev:+.3f}")
            except Exception:
                pass

            move = input("👉 选择操作：Hit (h) / Stand (s) / Double down (d): ").lower().strip()
            if move == 'h':
                card = game.player_hit()
                print(f"你抽到: {card}")
                print("玩家当前手牌:", game.get_player_hand())

                if game.hand_value(game.player_hand) > 21:
                    print("💥 爆牌！")
                    break
            elif move == 's':
                break 
            elif move == 'd':
                if len(game.player_hand) == 2:
                    result = game.player_double_down()
                    print(f"你加倍下注，现在下注: {game.bet}")
                    print("玩家当前手牌:", game.get_player_hand())
                    print(result)
                    break
                else:
                    print("❌ 只能在刚发完两张牌后才能 Double Down。")

            else:
                print("⚠️ 请输入 h 或 s")

        # 回合结束
        result = game.player_stand()
        print("庄家手牌:", game.get_dealer_hand(reveal=True))
        print(" 结果:", result)

        print(f" 当前筹码: {game.get_chips()}")
        print(f" Hi-Lo 计数: Running Count = {game.get_running_count()}, True Count = {game.get_true_count()}")

        # 继续？
        again = input("\n是否继续游戏？(y/n): ").lower().strip()
        if again != 'y':
            print("👋 感谢游玩！")
            break

if __name__ == "__main__":
    main()

# mobile_app.py

from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.properties import StringProperty
from kivy.core.window import Window
from kivy.graphics import Color, Rectangle

from game_state import GameState
from strategy import advise_action
from evaluator import simulate_ev



class InfoPanel(BoxLayout):
    dealer_text = StringProperty("")
    player_text = StringProperty("")
    advice_text = StringProperty("")
    count_text = StringProperty("")


class BlackjackApp(App):
    def build(self):
        self.title = "Blackjack 训练器"
        
        # 设置窗口背景颜色（浅灰色）
        Window.clearcolor = (0.2, 0.2, 0.25, 1)
        
        root = BoxLayout(orientation='vertical', padding=16, spacing=16)
        # 设置根布局背景颜色（深灰色）
        with root.canvas.before:
            Color(0.15, 0.15, 0.2, 1)
            self.root_bg = Rectangle(size=root.size, pos=root.pos)
        root.bind(size=self._update_bg, pos=self._update_bg)

        self.info = InfoPanel(orientation='vertical', size_hint=(1, 0.7), spacing=12)
        
        # 使用更大的字体并居中
        banker_title = Label(
            text='Banker', 
            font_size=54,  # 18 * 3
            size_hint=(1, None), 
            height=80,
            halign='center',
            valign='middle'
        )
        banker_title.bind(size=banker_title.setter('text_size'))
        self.info.add_widget(banker_title)
        
        self.dealer_label = Label(
            text='', 
            font_size=72,  # 24 * 3
            size_hint=(1, None), 
            height=100,
            halign='center',
            valign='middle',
            text_size=(None, None),
            markup=True  # 启用 markup 以支持颜色和样式
        )
        self.dealer_label.bind(size=self.dealer_label.setter('text_size'))
        self.info.add_widget(self.dealer_label)
        
        player_title = Label(
            text='Player', 
            font_size=54,  # 18 * 3
            size_hint=(1, None), 
            height=80,
            halign='center',
            valign='middle'
        )
        player_title.bind(size=player_title.setter('text_size'))
        self.info.add_widget(player_title)
        
        self.player_label = Label(
            text='', 
            font_size=72,  # 24 * 3
            size_hint=(1, None), 
            height=100,
            halign='center',
            valign='middle',
            text_size=(None, None),
            markup=True  # 启用 markup 以支持颜色和样式
        )
        self.player_label.bind(size=self.player_label.setter('text_size'))
        self.info.add_widget(self.player_label)
        
        self.advice_label = Label(
            text='', 
            font_size=48,  # 16 * 3
            size_hint=(1, None), 
            height=70,
            halign='center',
            valign='middle'
        )
        self.advice_label.bind(size=self.advice_label.setter('text_size'))
        self.info.add_widget(self.advice_label)
        
        self.count_label = Label(
            text='', 
            font_size=48,  # 16 * 3
            size_hint=(1, None), 
            height=70,
            halign='center',
            valign='middle'
        )
        self.count_label.bind(size=self.count_label.setter('text_size'))
        self.info.add_widget(self.count_label)

        root.add_widget(self.info)

        # 控制区 - 设置按钮背景颜色（深灰色）
        controls = GridLayout(cols=4, size_hint=(1, 0.3), spacing=8, padding=8)
        controls.bind(size=self._update_controls_bg, pos=self._update_controls_bg)
        
        # 按钮样式
        button_style = {
            'font_size': 28,
            'background_color': (0.3, 0.3, 0.35, 1),
            'color': (1, 1, 1, 1)
        }
        
        self.btn_new = Button(text='New Hand', on_press=self.on_new_round, **button_style)
        self.btn_hit = Button(text='Hit', on_press=self.on_hit, **button_style)
        self.btn_stand = Button(text='Stand', on_press=self.on_stand, **button_style)
        self.btn_double = Button(text='Double', on_press=self.on_double, **button_style)
        
        controls.add_widget(self.btn_new)
        controls.add_widget(self.btn_hit)
        controls.add_widget(self.btn_stand)
        controls.add_widget(self.btn_double)
        root.add_widget(controls)
        
        self.controls_bg = None

        # 初始化游戏
        self.game = GameState(num_decks=2, initial_chips=500, bet_amount=10)
        self.on_new_round()
        return root
    
    def _update_bg(self, instance, value):
        """更新根布局背景"""
        self.root_bg.pos = instance.pos
        self.root_bg.size = instance.size
    
    def _update_controls_bg(self, instance, value):
        """更新控制区背景"""
        if self.controls_bg is None:
            with instance.canvas.before:
                Color(0.25, 0.25, 0.3, 1)
                self.controls_bg = Rectangle(size=instance.size, pos=instance.pos)
        else:
            self.controls_bg.pos = instance.pos
            self.controls_bg.size = instance.size

    def refresh(self, reveal_dealer=False):
        # 文本显示 - 使用 markup 支持颜色
        dealer_cards = self.game.get_dealer_hand(reveal=reveal_dealer)
        dealer_text = self._format_hand_with_color(dealer_cards)
        player_text = self._format_hand_with_color(self.game.get_player_hand())
        self.dealer_label.text = dealer_text
        self.player_label.text = player_text

        # 检查玩家是否爆牌或游戏是否结束
        player_total = self.game.hand_value(self.game.get_player_hand())
        is_busted = player_total > 21
        is_hand_resolved = reveal_dealer

        # 禁用/启用按钮
        self.btn_hit.disabled = is_busted or is_hand_resolved
        self.btn_stand.disabled = is_busted or is_hand_resolved
        self.btn_double.disabled = is_busted or is_hand_resolved or len(self.game.get_player_hand()) != 2

        # 策略 + EV（仅在游戏未结束且未爆牌时显示）
        if is_hand_resolved:
            # 结果已经在其他地方设置了（on_stand, on_double等），这里不做处理
            pass
        elif is_busted:
            # 如果爆牌，显示提示
            self.advice_label.text = "Player busts - Hand ended"
        else:
            # 游戏进行中，显示策略建议
            try:
                advice = advise_action(self.game.player_hand, self.game.dealer_hand[0])
                evs = simulate_ev(self.game.deck.cards, self.game.player_hand, self.game.dealer_hand, base_bet=1, trials=1500)
                if advice in evs:
                    wr, ev = evs[advice]
                    extra = f"(Winrate {wr:.1%}, EV {ev:+.3f})"
                    self.advice_label.text = f"Suggestion: {advice}  {extra}"
                else:
                    wr, ev = max(evs.values(), key=lambda x: x[1])
                    extra = f"(Best: {wr:.1%}, EV {ev:+.3f})"
                    self.advice_label.text = f"Suggestion: {advice}  {extra}"
            except Exception as e:
                self.advice_label.text = "Suggestion: -"

        # 计数
        try:
            self.count_label.text = f"Chips: {self.game.get_chips()} | Count: {self.game.get_running_count()} | Real Count: {self.game.get_true_count():.2f}"
        except Exception:
            self.count_label.text = ""
    
    def _format_hand_with_color(self, cards):
        """格式化手牌并用颜色字母替代花色符号（使用 Kivy markup）"""
        parts = []
        for c in cards:
            if isinstance(c, str):  # 比如 '?'
                parts.append(c)
            else:
                rank = c.rank
                suit = c.suit
                # 用颜色字母替代花色符号
                if suit == '♥':  # Hearts -> 红色 H
                    parts.append(f'[color=ff3333]{rank}H[/color]')
                elif suit == '♦':  # Diamonds -> 蓝色 D
                    parts.append(f'[color=3399ff]{rank}D[/color]')
                elif suit == '♣':  # Clubs -> 绿色 C
                    parts.append(f'[color=33ff33]{rank}C[/color]')
                elif suit == '♠':  # Spades -> 黑色 S
                    parts.append(f'{rank}S')
                else:
                    parts.append(f'{rank}{suit}')
        return ' '.join(parts)

    def on_new_round(self, *args):
        self.game.start_new_round()
        # 新一局开始时重新启用所有按钮
        self.btn_hit.disabled = False
        self.btn_stand.disabled = False
        self.btn_double.disabled = False
        self.refresh(reveal_dealer=False)

    def on_hit(self, *args):
        if not self.btn_hit.disabled:
            self.game.player_hit()
            player_total = self.game.hand_value(self.game.get_player_hand())
            # 如果爆牌，立即显示结果并结束这一局
            if player_total > 21:
                result = self.game.evaluate_round()
                self.refresh(reveal_dealer=True)
                self.advice_label.text = f"Result: {result}"
            else:
                self.refresh(reveal_dealer=False)

    def on_stand(self, *args):
        if not self.btn_stand.disabled:
            result = self.game.player_stand()
            self.refresh(reveal_dealer=True)
            self.advice_label.text = f"Result: {result}"

    def on_double(self, *args):
        if not self.btn_double.disabled:
            if len(self.game.player_hand) == 2:
                result = self.game.player_double_down()
                self.refresh(reveal_dealer=True)
                self.advice_label.text = f"Result: {result}"
            else:
                self.advice_label.text = "Error: Only available with 2 cards"


if __name__ == '__main__':
    BlackjackApp().run()




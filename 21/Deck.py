# deck.py

import random
from card import Card

class Deck:
    """Deck of playing cards for Blackjack."""
    
    def __init__(self, num_decks=1):
        self.num_decks = num_decks
        # Use Unicode suits internally for compatibility
        self.suits = ['♠', '♥', '♦', '♣']
        self.ranks = ['2', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K', 'A']
        self.full_deck = self._generate_deck()
        self.shuffle()

    def _generate_deck(self):
        return [
            Card(rank, suit)
            for _ in range(self.num_decks)
            for suit in self.suits
            for rank in self.ranks
        ]

    def shuffle(self):
        """Shuffle the deck."""
        self.cards = self.full_deck.copy()
        random.shuffle(self.cards)

    def draw_card(self):
        """Draw a card from the deck."""
        if len(self.cards) == 0:
            raise ValueError("Deck is empty, please shuffle")
        return self.cards.pop()

    def remaining_ratio(self):
        """Calculate remaining cards ratio."""
        total = 52 * self.num_decks
        return len(self.cards) / total

    def needs_shuffle(self, threshold=0.3):
        """Check if deck needs reshuffling."""
        return self.remaining_ratio() <= threshold
    
    def get_remaining_cards(self):
        """Return list of remaining cards as JSON-safe strings."""
        return [card.to_string() for card in self.cards]

# card.py

class Card:
    """Playing card with JSON-safe string representation for API use."""
    
    # Suit mapping: Unicode symbols to letters
    SUIT_TO_LETTER = {
        '♠': 'S',  # Spades
        '♥': 'H',  # Hearts
        '♦': 'D',  # Diamonds
        '♣': 'C'   # Clubs
    }
    
    LETTER_TO_SUIT = {v: k for k, v in SUIT_TO_LETTER.items()}
    
    def __init__(self, rank, suit):
        """
        Args:
            rank: Card rank ('A', '2'-'10', 'J', 'Q', 'K')
            suit: Suit (can be Unicode '♠♥♦♣' or letter 'SHDC')
        """
        self.rank = rank  # e.g. 'A', '10', 'K'
        # Convert letter to Unicode for internal use, or keep Unicode
        if suit in self.LETTER_TO_SUIT:
            self._suit_unicode = self.LETTER_TO_SUIT[suit]
            self._suit_letter = suit
        else:
            self._suit_unicode = suit  # Assume Unicode
            self._suit_letter = self.SUIT_TO_LETTER.get(suit, suit)

    @property
    def suit(self):
        """Returns Unicode suit for compatibility."""
        return self._suit_unicode
    
    @property
    def suit_letter(self):
        """Returns letter suit (H, D, C, S) for API."""
        return self._suit_letter

    def __repr__(self):
        return f"{self.rank}{self._suit_unicode}"
    
    def to_string(self):
        """
        Returns JSON-safe string representation (e.g., 'AH', '7D', 'KS').
        Format: {rank}{suit_letter}
        """
        return f"{self.rank}{self._suit_letter}"

    def get_value(self):
        """Returns the Blackjack numeric value (1-11)."""
        if self.rank in ['J', 'Q', 'K']:
            return 10
        elif self.rank == 'A':
            return 11  # Default, game logic should handle soft/hard Aces
        else:
            return int(self.rank)
    
    @classmethod
    def from_string(cls, card_str):
        """
        Create Card from string (e.g., 'AH', '7D').
        
        Args:
            card_str: String in format '{rank}{suit_letter}' (e.g., 'AH', '10S')
        
        Returns:
            Card instance
        """
        if len(card_str) < 2:
            raise ValueError(f"Invalid card string: {card_str}")
        
        # Handle 10 (two-digit rank)
        if card_str.startswith('10'):
            rank = '10'
            suit_letter = card_str[2]
        else:
            rank = card_str[0]
            suit_letter = card_str[1]
        
        return cls(rank, suit_letter)

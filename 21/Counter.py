# counter.py

class HiLoCounter:
    """Hi-Lo card counting system."""
    
    def __init__(self):
        self.running_count = 0

    def update(self, card):
        """Update count based on drawn card."""
        rank = card.rank
        if rank in ['2', '3', '4', '5', '6']:
            self.running_count += 1
        elif rank in ['10', 'J', 'Q', 'K', 'A']:
            self.running_count -= 1
        # 7, 8, 9 remain unchanged

    def reset(self):
        """Reset counter to zero."""
        self.running_count = 0

    def get_running_count(self):
        """Get current running count."""
        return self.running_count

    def get_true_count(self, num_remaining_decks):
        """Calculate true count based on remaining decks."""
        if num_remaining_decks == 0:
            return 0
        return round(self.running_count / num_remaining_decks, 2)

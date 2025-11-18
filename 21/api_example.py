# api_example.py
# 
# Example API implementation using the refactored GameState backend
# Compatible with Flask and FastAPI

from flask import Flask, request, jsonify
from flask_cors import CORS
# Alternative: from fastapi import FastAPI, HTTPException

from game_state import GameState

# Flask example
app = Flask(__name__)
# 启用CORS支持，允许跨域请求
CORS(app)

# In production, store game states in session/database per user
# For demo, using a simple dict
game_sessions = {}


@app.route('/api/game/new', methods=['POST'])
def new_game():
    """Start a new game session."""
    data = request.get_json() or {}
    num_decks = data.get('num_decks', 2)
    initial_chips = data.get('initial_chips', 500)
    bet_amount = data.get('bet_amount', 10)
    blackjack_payout_ratio = data.get('blackjack_payout_ratio', '3:2')
    
    # Create new game state
    game = GameState(
        num_decks=num_decks, 
        initial_chips=initial_chips, 
        bet_amount=bet_amount,
        blackjack_payout_ratio=blackjack_payout_ratio
    )
    status = game.start_new_round()
    
    # Generate session ID (in production, use proper session management)
    import uuid
    session_id = str(uuid.uuid4())
    game_sessions[session_id] = game
    
    return jsonify({
        "session_id": session_id,
        "status": status
    })


@app.route('/api/game/<session_id>/status', methods=['GET'])
def get_status(session_id):
    """Get current game status."""
    if session_id not in game_sessions:
        return jsonify({"error": "Session not found"}), 404
    
    game = game_sessions[session_id]
    reveal = request.args.get('reveal', 'false').lower() == 'true'
    status = game.get_status(reveal_dealer=reveal)
    
    return jsonify(status)


@app.route('/api/game/<session_id>/hit', methods=['POST'])
def player_hit(session_id):
    """Player hits (draws a card)."""
    if session_id not in game_sessions:
        return jsonify({"error": "Session not found"}), 404
    
    game = game_sessions[session_id]
    status = game.player_hit()
    
    return jsonify(status)


@app.route('/api/game/<session_id>/stand', methods=['POST'])
def player_stand(session_id):
    """Player stands (dealer plays)."""
    if session_id not in game_sessions:
        return jsonify({"error": "Session not found"}), 404
    
    game = game_sessions[session_id]
    status = game.player_stand()
    
    return jsonify(status)


@app.route('/api/game/<session_id>/double', methods=['POST'])
def player_double(session_id):
    """Player doubles down."""
    if session_id not in game_sessions:
        return jsonify({"error": "Session not found"}), 404
    
    game = game_sessions[session_id]
    try:
        status = game.player_double()
        return jsonify(status)
    except ValueError as e:
        return jsonify({"error": str(e)}), 400


@app.route('/api/game/<session_id>/new-round', methods=['POST'])
def new_round(session_id):
    """Start a new round in existing session."""
    if session_id not in game_sessions:
        return jsonify({"error": "Session not found"}), 404
    
    game = game_sessions[session_id]
    
    # 检查是否有新的下注金额
    data = request.get_json() or {}
    bet_amount = data.get('bet_amount')
    if bet_amount:
        game.bet = bet_amount
    
    status = game.start_new_round()
    
    return jsonify(status)


@app.route('/api/game/<session_id>/split', methods=['POST'])
def player_split(session_id):
    """Player splits the current hand."""
    if session_id not in game_sessions:
        return jsonify({"error": "Session not found"}), 404
    
    game = game_sessions[session_id]
    try:
        status = game.player_split()
        return jsonify(status)
    except ValueError as e:
        return jsonify({"error": str(e)}), 400


@app.route('/api/game/<session_id>/set-blackjack-payout', methods=['POST'])
def set_blackjack_payout(session_id):
    """Set Blackjack payout ratio."""
    if session_id not in game_sessions:
        return jsonify({"error": "Session not found"}), 404
    
    game = game_sessions[session_id]
    data = request.get_json() or {}
    ratio = data.get('ratio', '3:2')
    
    try:
        game.set_blackjack_payout_ratio(ratio)
        status = game.get_status()
        return jsonify(status)
    except ValueError as e:
        return jsonify({"error": str(e)}), 400


# FastAPI alternative example (commented out)
"""
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI()

class NewGameRequest(BaseModel):
    num_decks: int = 2
    initial_chips: int = 500
    bet_amount: int = 10

@app.post("/api/game/new")
def new_game(req: NewGameRequest):
    game = GameState(
        num_decks=req.num_decks,
        initial_chips=req.initial_chips,
        bet_amount=req.bet_amount
    )
    status = game.start_new_round()
    session_id = str(uuid.uuid4())
    game_sessions[session_id] = game
    
    return {
        "session_id": session_id,
        "status": status
    }

@app.get("/api/game/{session_id}/status")
def get_status(session_id: str, reveal: bool = False):
    if session_id not in game_sessions:
        raise HTTPException(status_code=404, detail="Session not found")
    
    game = game_sessions[session_id]
    return game.get_status(reveal_dealer=reveal)

@app.post("/api/game/{session_id}/hit")
def player_hit(session_id: str):
    if session_id not in game_sessions:
        raise HTTPException(status_code=404, detail="Session not found")
    
    game = game_sessions[session_id]
    return game.player_hit()

@app.post("/api/game/{session_id}/stand")
def player_stand(session_id: str):
    if session_id not in game_sessions:
        raise HTTPException(status_code=404, detail="Session not found")
    
    game = game_sessions[session_id]
    return game.player_stand()

@app.post("/api/game/{session_id}/double")
def player_double(session_id: str):
    if session_id not in game_sessions:
        raise HTTPException(status_code=404, detail="Session not found")
    
    game = game_sessions[session_id]
    try:
        return game.player_double()
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
"""

if __name__ == '__main__':
    # Run Flask app
    app.run(debug=True, port=5000)


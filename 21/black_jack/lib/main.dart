import 'package:flutter/material.dart';
import 'models/blackjack_game.dart';
import 'ui/game_screen.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '21点 Hi-Lo 训练器',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.green),
        useMaterial3: true,
      ),
      home: GameScreen(game: BlackjackGame()),
    );
  }
}

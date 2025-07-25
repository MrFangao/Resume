**SnakeGame Quick Start Guide**

**Team Members**
- Yunzu Hou & Xinyu Sun
- Student IDs: u1355461 & u1397324
- Updated: December 7, 2023

**Project Introduction**
PS9 aims to create a multiplayer snake game that allows players to interact under a server. After carefully studying the project requirements, we formulated a step-by-step implementation strategy (MVC) and successfully realized the core functions from data reading to game logic with MVC. Starting from PS7, we encountered many difficulties, including incompatibility with the MacBook system and bugs in VS Code, some of which even multiple TAs could not solve.

**Development Process**
We started from the underlying network communication and gradually built the game logic framework, focusing particularly on implementing the snake's natural behavior and the logical judgment of objects within the game.

**Problems & Challenges Encountered**
We faced several technical obstacles in multiplayer synchronization and stability, and we are working hard to solve them to improve the overall quality of the game.

**Player's Guide**
Upon entering the game, players will control a snake that moves on a fixed map. The snake moves forward automatically and can change direction through keyboard commands. Eating Powerups will make the snake grow, and new Powerups will continuously refresh on the map. Avoid hitting walls or yourself, or the snake will "explode" and be regenerated after a short time. The game design ensures the rationality and balance of object generation.
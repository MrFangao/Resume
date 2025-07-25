
using Newtonsoft.Json;
namespace SnakeGame
{
 
    public class World
    {
        /// <summary>
        /// Size of the world as defined by the server. This typically represents the 
        /// dimensions or boundary limits of the game world.
        /// </summary>
        public int size { get; set; }

        /// <summary>
        /// Collection of snakes within the world, keyed by their unique identifiers. 
        /// This dictionary tracks the snakes' current states and positions.
        /// </summary>
        public Dictionary<int, Snake> Snakes { get; private set; } = new Dictionary<int, Snake>();

        /// <summary>
        /// Collection of walls within the world, keyed by their unique identifiers. 
        /// Walls represent static obstacles that can affect snake movement.
        /// </summary>
        public Dictionary<int, Wall> Walls { get; private set; } = new Dictionary<int, Wall>();

        /// <summary>
        /// Collection of powerups within the world, keyed by their unique identifiers. 
        /// Powerups provide special abilities or bonuses to snakes upon collection.
        /// </summary>
        public Dictionary<int, Powerup> Powerups { get; private set; } = new Dictionary<int, Powerup>();

        /// <summary>
        /// A empty constructor for new world.
        /// </summary>
        public World()
        { 
        }

        // Used to store the respawnrate.
        private int respawnrate;

        /// <summary>
        /// Initializing the worlds
        /// </summary>
        /// <param name="size"></param>
        /// <param name="walls"></param>
        /// <param name="respawnrate"></param>
        public World(int size, IEnumerable<Wall> walls, int respawnrate)
        {
            this.size = size;
            this.respawnrate = respawnrate;
            foreach (Wall wall in walls)
            {
                Walls[wall.GetID()] = wall;
            }
        }

        /// <summary>
        /// Adds a new snake to the world with a random location and direction.
        /// </summary>
        /// <param name="name">The name of the snake.</param>
        /// <param name="ID">The unique identifier for the snake.</param>
        /// <returns>The newly created snake.</returns>
        public Snake AddSnake(string name, int ID)
        {
            Vector2D randomLocation = RandomLocation();
            Vector2D randomDirection = RandomDirection();
            Snake snake = new Snake(name, randomLocation, randomDirection, ID);

            Snakes.Add(ID, snake);
            snake.joined = true;

            return snake;
        }

        /// <summary>
        /// Generates a random position within the world, ensuring it does not overlap with any walls.
        /// The method will repeatedly attempt to find a valid location within the specified boundaries.
        /// </summary>
        /// <returns>A Vector2D object representing a random, valid location within the world.</returns>
        private Vector2D RandomLocation()
        {
            Random rand = new Random();
            int x, y;
            Vector2D potentialLocation;

            do
            {
                x = rand.Next(-size / 2 + 240, size / 2 - 240);
                y = rand.Next(-size / 2 + 240, size / 2 - 240);
                potentialLocation = new Vector2D(x, y);
            }
            while (!CheckWalls(potentialLocation));

            return potentialLocation;
        }


        // Used to manage the distance from the location of the wall.
        private int p1 = 27;
        private int p2 = 18;

        public bool CheckWalls(Vector2D location)
        {
            foreach (Wall wall in Walls.Values)
            {
                if (IsLocationNearWall(location, wall))
                {
                    return false;
                }
            }
            return true;
        }

        private bool IsLocationNearWall(Vector2D location, Wall wall)
        {
            return wall.P1.X == wall.P2.X ?
                   IsWithinVerticalWallBounds(location, wall) :
                   IsWithinHorizontalWallBounds(location, wall);
        }

        private bool IsWithinVerticalWallBounds(Vector2D location, Wall wall)
        {
            double minY = Math.Min(wall.P1.Y, wall.P2.Y);
            double maxY = Math.Max(wall.P1.Y, wall.P2.Y);
            return IsWithinRange(location.GetY(), minY, maxY, p1) &&
                   IsWithinRange(location.GetX(), wall.P1.X - p2, wall.P1.X + p2);
        }

        private bool IsWithinHorizontalWallBounds(Vector2D location, Wall wall)
        {
            double minX = Math.Min(wall.P1.X, wall.P2.X);
            double maxX = Math.Max(wall.P1.X, wall.P2.X);
            return IsWithinRange(location.GetX(), minX, maxX, p1) &&
                   IsWithinRange(location.GetY(), wall.P1.Y - p2, wall.P1.Y + p2);
        }

        private bool IsWithinRange(double value, double min, double max, int padding = 0)
        {
            return value >= min - padding && value <= max + padding;
        }



        /// <summary>
        /// Generates a random direction for a snake.
        /// </summary>
        /// <returns>A Vector2D representing one of the four cardinal directions.</returns>
        private static Vector2D RandomDirection()
        {
            Random rand = new Random();
            switch (rand.Next(4))
            {
                case 0: return new Vector2D(1f, 0f);  // right
                case 1: return new Vector2D(-1f, 0f); // left
                case 2: return new Vector2D(0f, 1f);  // up
                case 3: return new Vector2D(0f, -1f); // down
                default: return new Vector2D(1f, 0f); // stationary (should not happen)
            }
        }

        /// <summary>
        /// Processes a command for a snake, typically received from keyboard input.
        /// </summary>
        /// <param name="command">The JSON string representing the keyboard control.</param>
        /// <param name="snake">The snake to which the command applies.</param>
        public void Command(string command, Snake snake)
        {
            KeyboardControl key;
            try
            {
                key = JsonConvert.DeserializeObject<KeyboardControl>(command)!;
            }
            catch (JsonException)
            {
                // Optionally log the error or notify about the invalid command.
                return;
            }

            // Direct the snake based on the deserialized keyboard control.
            if (key != null)
            {
                snake.Turn(key.moving, this);
            }
        }


        // These variables are used to update the world.
        private int diedcount = 0;
        private int timer = 0;
        private int nextPowerup = 0;
        private LinkedList<Vector2D> allSnakes = new LinkedList<Vector2D>();
        /// <summary>
        /// Manages the generation and regeneration of PowerUps in the world.
        /// </summary>
        private void ManagePowerUps()
        {
            Random rand = new Random();

            // Initialize the next powerup regeneration time if it's not already set.
            if (nextPowerup == 0)
            {
                nextPowerup = timer + rand.Next(0, 200);
            }

            // Add a new PowerUp if there are fewer than 20, or if the conditions for regeneration are met.
            if (Powerups.Count < 20 || (diedcount >= 1 && nextPowerup <= timer))
            {
                AddPowerup();
                diedcount = 0;
                nextPowerup = timer + rand.Next(0, 200); // Reset the timer for the next PowerUp regeneration.
            }
        }


        /// <summary>
        /// This method is used to update objects in the world
        /// </summary>
        public void Update()
        {
            ManagePowerUps();
            UpdateSnakes();
            // Increase the timer.
            timer++;
        }
        /// <summary>
        /// Updates the state and position of each snake in the world.
        /// </summary>
        public void UpdateSnakes()
        {
            foreach (Snake snake in Snakes.Values)
            {
                CheckCollisionWithWall(snake);
                CheckCollisionWithSelf(snake);
                CheckCollisionWithOtherSnakes(snake);
                snake.Movement();
                HandleBoundaryTeleport(snake);
                RegenerateIfDead(snake);
                foreach (Powerup p in Powerups.Values)
                {
                    if (!p.died && (p.location - snake.Head).Length() <= 10)
                    {
                        snake.SnakeGrow();
                        p.died = true;
                        diedcount++;
                    }
                }
            }
        }

        /// <summary>
        /// Checks and handles collision of the snake with walls.
        /// </summary>
        /// <param name="snake">The snake to check.</param>
        private void CheckCollisionWithWall(Snake snake)
        {
            if (!CheckWalls(snake.Head) && snake.holdTimer)
            {
                snake.die(timer);
                snake.holdTimer = false;
            }
        }

        /// <summary>
        /// Checks and handles collision of the snake with itself.
        /// </summary>
        /// <param name="snake">The snake to check.</param>
        private void CheckCollisionWithSelf(Snake snake)
        {
            foreach (Vector2D bodyPart in snake.copyBody)
            {
                if (bodyPart.Equals(snake.Head))
                {
                    snake.die(timer);
                    snake.holdTimer = false;
                    break;
                }
            }
            snake.copyBody.Clear();
        }

        /// <summary>
        /// Regenerates the snake if it has died and the respawn time has passed.
        /// </summary>
        /// <param name="snake">The snake to regenerate.</param>
        private void RegenerateIfDead(Snake snake)
        {
            if (!snake.alive && timer - snake.death >= respawnrate && !snake.holdTimer)
            {
                Vector2D newLocation = RandomLocation();
                snake.Respawn(newLocation, RandomDirection());
                snake.alive = true;
                snake.holdTimer = true;
            }
        }

        /// <summary>
        /// Handles the boundary collision by teleporting the snake to the opposite side.
        /// </summary>
        /// <param name="snake">The snake to handle.</param>
        private void HandleBoundaryTeleport(Snake snake)
        {
            if (Math.Abs(snake.Head.GetX()) >= size / 2 || Math.Abs(snake.Head.GetY()) >= size / 2)
            {
                TeleportSnake(snake);
            }
        }

        /// <summary>
        /// Teleports a snake to the opposite side of the map when it reaches the boundary.
        /// </summary>
        /// <param name="snake">The snake to teleport.</param>
        private void TeleportSnake(Snake snake)
        {
            // Adjust the head position to the opposite side
            snake.Head = new Vector2D(AdjustBoundaryPosition(snake.Head.GetX(), size),
                                      AdjustBoundaryPosition(snake.Head.GetY(), size));

            // Adjust the body parts to the opposite side
            LinkedList<Vector2D> adjustedBody = new LinkedList<Vector2D>();
            foreach (Vector2D bodyPart in snake.copyBody)
            {
                adjustedBody.AddLast(new Vector2D(AdjustBoundaryPosition(bodyPart.GetX(), size),
                                                  AdjustBoundaryPosition(bodyPart.GetY(), size)));
            }
            snake.body = adjustedBody;
        }

        /// <summary>
        /// Adjusts a position value to be within the boundary of the map.
        /// </summary>
        /// <param name="position">The position value (either X or Y).</param>
        /// <param name="boundary">The size of the map boundary.</param>
        /// <returns>The adjusted position value.</returns>
        private double AdjustBoundaryPosition(double position, double boundary)
        {
            double halfBoundary = boundary / 2;
            if (position > halfBoundary) return -(boundary - position);
            if (position < -halfBoundary) return boundary + position;
            return position;
        }

        /// <summary>
        /// Checks and handles collisions between the given snake and other snakes.
        /// </summary>
        /// <param name="snake">The snake to check for collisions.</param>
        private void CheckCollisionWithOtherSnakes(Snake snake)
        {
            foreach (Snake otherSnake in Snakes.Values)
            {
                // Skip checking collision with itself
                if (snake == otherSnake) continue;

                // Check collision with other snake's head
                if ((snake.Head - otherSnake.Head).Length() <= 20)
                {
                    snake.die(timer);
                    snake.holdTimer = false;
                    return;
                }

                // Check collision with other snake's body
                foreach (Vector2D bodyPart in otherSnake.copyBody)
                {
                    if ((snake.Head - otherSnake.Head).Length() <= 20)
                    {
                        snake.die(timer);
                        snake.holdTimer = false;
                        return;
                    }
                }
            }
        }

            /// <summary>
            /// Used to add the powerups.
            /// </summary>
            /// <returns></returns>
            private Powerup AddPowerup()
        {
            // Randomly location
            Powerup powerup = new Powerup(RandomLocation());
            // Add it
            Powerups.Add(powerup.GetID(), powerup);
            return powerup;
        }
    }
}


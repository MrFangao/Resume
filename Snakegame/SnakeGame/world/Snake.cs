using Newtonsoft.Json;
using System.Runtime.Serialization;

namespace SnakeGame
{
    [DataContract(Name = "Snake", Namespace = "")]
    [JsonObject(MemberSerialization.OptIn)]
    public class Snake
    {
        /// <summary>
        /// An int representing the snake's unique ID. 
        /// </summary>
        [DataMember]
        [JsonProperty(PropertyName = "snake")]
        public int ID { get; set; }

        /// <summary>
        /// A string representing the player's name.
        /// </summary>
        [DataMember]
        [JsonProperty(PropertyName = "body")]
        public LinkedList<Vector2D> body { get; set; }

        /// <summary>
        /// A List<Vector2D> representing the entire body of the snake.
        /// </summary>
        [DataMember]
        [JsonProperty(PropertyName = "dir")]
        public Vector2D direction { get; set; }

        /// <summary>
        /// A Vector2D representing the snake's orientation. 
        /// </summary>
        [DataMember]
        [JsonProperty(PropertyName = "name")]
        public string name { get; set; } = "";

        /// <summary>
        /// An int representing the player's score
        /// </summary>
        [DataMember]
        [JsonProperty(PropertyName = "score")]
        public int score { get; set; }

        /// <summary>
        /// A bool indicating if the snake died on this frame.
        /// </summary>
        [DataMember]
        [JsonProperty(PropertyName = "died")]
        public bool died { get; set; }

        /// <summary>
        /// A bool indicating whether a snake is alive or dead.
        /// </summary>
        [DataMember]
        [JsonProperty(PropertyName = "alive")]
        public bool alive { get; set; } = true;

        /// <summary>
        /// A bool indicating if the player controlling that snake disconnected on that frame.
        /// </summary>
        [DataMember]
        [JsonProperty(PropertyName = "dc")]
        public bool disconnected { get; set; }

        /// <summary>
        /// A bool indicating if the player joined on this frame.
        /// </summary>
        [DataMember]
        [JsonProperty(PropertyName = "join")]
        public bool joined { get; set; }

        /// <summary>
        /// This method get the Snake ID.
        /// </summary>
        /// <returns></returns>
        public int GetID() => ID;

        /// <summary>
        /// Gets or sets the head position of the snake.
        /// </summary>
        public Vector2D Head
        {

            get => body.Count > 0 ? body.Last!.Value : null!;
            set
            {
                if (body.Count > 0)
                {
                    body.RemoveLast();
                }
                body.AddLast(value);
            }
        }

        /// <summary>
        /// Gets or sets the tail position of the snake.
        /// </summary>
        public Vector2D Tail
        {
            get => body.Count > 0 ? body.First!.Value : null!;
            set
            {
                if (body.Count > 0)
                {
                    body.RemoveFirst();
                }
                body.AddFirst(value);
            }
        }

        /// <summary>
        /// Generates a sequence of tuples representing the segments of the snake's body.
        /// </summary>
        /// <returns>A sequence of (Vector2D v1, Vector2D v2) representing body segments.</returns>
        public IEnumerable<(Vector2D v1, Vector2D v2)> snakeSegmentLength()
        {
            var node = body.First;
            while (node != null && node.Next != null)
            {
                yield return (node.Value, node.Next.Value);
                node = node.Next;
            }
        }


        /// <summary>
        /// This method sets the snake death data.
        /// </summary>
        /// <param name="time"></param>
        public void die(int time)
        {
            canmove = false;
            died = true;
            alive = false;
            death = time;
            score = 0;
        }

        /// <summary>
        /// This method sets the player disconnection data
        /// </summary>
        public void disconnect()
        {
            alive = false;
            died = true;
            disconnected = true;

        }

        [JsonConstructor]
        public Snake()
        {
            // 如果需要，可以在这里初始化属性的默认值
        body = new LinkedList<Vector2D>();
        direction = new Vector2D(); // 假设Vector2D有一个默认构造函数
        name = "";
        score = 0;
        died = false;
        alive = true;
        disconnected = false;
        joined = false;
        }
        // These variables are used to track the snake's current direction
        bool currentUp = false;
        bool currentDown = false;
        bool currentLeft = false;
        bool currentRight = false;

        /// <summary>
        /// Initializes a new instance of the Snake class with specified parameters.
        /// </summary>
        /// <param name="name">Name of the snake.</param>
        /// <param name="v1">Initial position of the snake's head.</param>
        /// <param name="v2">Initial direction of the snake.</param>
        /// <param name="id">Unique identifier for the snake.</param>
        public Snake(string name, Vector2D v1, Vector2D v2, int id)
        {
            ID = id;
            this.name = name;
            body = new LinkedList<Vector2D>();
            body.AddLast(v1); // Add the head of the snake
            direction = v2;
            score = 0;
            died = false;
            disconnected = false;
            joined = false;

            InitializeBodyDirection(v2); // Initialize the body direction
            direction.Normalize(); // Normalize the direction vector
        }

        /// <summary>
        /// Initializes the body of the snake based on its direction.
        /// </summary>
        /// <param name="direction">The initial direction of the snake.</param>
        private void InitializeBodyDirection(Vector2D direction)
        {
            Vector2D tailPosition = CalculateTailPosition(body.First.Value, direction);
            if (tailPosition != null)
            {
                body.AddLast(tailPosition); // Add the tail of the snake
            }

            // Set the current direction flags
            currentRight = direction.GetX() == 1 && direction.GetY() == 0;
            currentLeft = direction.GetX() == -1 && direction.GetY() == 0;
            currentDown = direction.GetX() == 0 && direction.GetY() == 1;
            currentUp = direction.GetX() == 0 && direction.GetY() == -1;
        }

        /// <summary>
        /// Calculates the tail position based on the head position and direction.
        /// </summary>
        /// <param name="head">The position of the head.</param>
        /// <param name="direction">The direction of the snake.</param>
        /// <returns>The calculated position of the tail.</returns>
        private Vector2D CalculateTailPosition(Vector2D head, Vector2D direction)
        {
            const int bodyLength = 120; // The length of the snake body segment

            if (direction.GetX() == 1 && direction.GetY() == 0)
                return new Vector2D(head.X + bodyLength, head.Y);
            else if (direction.GetX() == -1 && direction.GetY() == 0)
                return new Vector2D(head.X - bodyLength, head.Y);
            else if (direction.GetX() == 0 && direction.GetY() == 1)
                return new Vector2D(head.X, head.Y + bodyLength);
            else if (direction.GetX() == 0 && direction.GetY() == -1)
                return new Vector2D(head.X, head.Y - bodyLength);

            return null!; // or handle unexpected direction
        }


        /// <summary>
        /// Changes the direction of the snake based on the given input, ensuring the snake does not turn 180 degrees.
        /// </summary>
        /// <param name="moveCommand">The command indicating the new direction ('up', 'down', 'left', 'right').</param>
        /// <param name="world">The game world context, if needed for future extensions.</param>
        public void Turn(string moveCommand, World world)
        {
            Vector2D newDirection = GetNewDirection(moveCommand);
            if (newDirection != null && IsOppositeDirection(newDirection) == false)
            {
                direction = newDirection;
                UpdateCurrentDirectionFlags(newDirection);
            }
        }

        /// <summary>
        /// Determines the new direction based on the command
        /// </summary>
        /// <param name="command">The command for the new direction.</param>
        /// <returns>The new direction as a Vector2D, or null if the command is invalid.</returns>
        private Vector2D GetNewDirection(string command)
        {
            return command switch
            {
                "up" => new Vector2D(0.0, -1.0),
                "down" => new Vector2D(0.0, 1.0),
                "left" => new Vector2D(-1.0, 0.0),
                "right" => new Vector2D(1.0, 0.0),
                _ => null!
            };
        }

        /// <summary>
        /// Checks if the given direction is the opposite of the current direction
        /// </summary>
        /// <param name="newDirection">The new direction to check.</param>
        /// <returns>True if the new direction is opposite to the current direction, false otherwise.</returns>
        private bool IsOppositeDirection(Vector2D newDirection)
        {
            if (currentUp && newDirection.GetY() == 1.0) return true;
            if (currentDown && newDirection.GetY() == -1.0) return true;
            if (currentLeft && newDirection.GetX() == 1.0) return true;
            if (currentRight && newDirection.GetX() == -1.0) return true;

            return false;
        }

        /// <summary>
        /// Updates the flags indicating the current direction of the snake
        /// </summary>
        /// <param name="newDirection">The new direction of the snake.</param>
        private void UpdateCurrentDirectionFlags(Vector2D newDirection)
        {
            currentUp = newDirection.GetY() == -1.0;
            currentDown = newDirection.GetY() == 1.0;
            currentLeft = newDirection.GetX() == -1.0;
            currentRight = newDirection.GetX() == 1.0;
        }


        // Used to track whether the snake needs to grow
        private bool grow = false;
        // Used to store the rest of the head except for the position
        public LinkedList<Vector2D> copyBody = new LinkedList<Vector2D>();

        /// <summary>
        /// This method is used to set the snake growth when the snake eats the Powerup
        /// </summary>
        public void SnakeGrow()
        {
            score++;
            grow = true;

        }

        /// <summary>
        /// Used for the movement of snakes
        /// </summary>
        public void Movement()
        {
            if (canmove is true)
            {
                // Add the head.
                body.AddLast(new Vector2D(Head));
                Head += direction * 3f;
                
                // Get the direction
                Vector2D vector = body.First!.Next!.Value - Tail;
                vector.Normalize();

                // Add the Tail
                Tail += vector * 3f;

                // Grow the snake
                if (grow is true)
                {
                    Tail += vector * 12f;
                    grow = false;
                }

                // Remove the tail
                if (Tail.Equals(body.First.Next.Value))
                {
                    body.RemoveFirst();
                }

                // Copy the body positions
                foreach (Vector2D b in body)
                {
                    copyBody.AddFirst(b);
                }
                // remove the head position
                copyBody.RemoveFirst();
            }           
        }

        // These variables are used to calculate the time interval for the snake to regenerate,
        // and to immobilize the snake during this period
        public bool holdTimer = true;
        public int death;
        public bool canmove = true;

        /// <summary>
        /// Override the tostring method to Serialize data.
        /// </summary>
        /// <returns></returns>
        public override string ToString() => JsonConvert.SerializeObject(this);

        /// <summary>
        /// This method is used to initialize the snake after it is reborn
        /// </summary>
        /// <param name="v1"></param>
        /// <param name="v2"></param>
        public void Respawn(Vector2D v1, Vector2D v2)
        {
            // Initializing data
            canmove = true;
            body = new LinkedList<Vector2D>();
            body.AddLast(v1);
            direction = v2;
            score = 0;
            alive = true;
            died = false;
            // Initialize the snake according to the direction
            // Reset the current direction flags
            ResetDirectionFlags();

            // Set the initial body position based on the direction
            if (v2.GetX() == 1 && v2.GetY() == 0) // Moving right
            {
                AddBodySegment(v1.X + 120, v1.Y);
                currentRight = true;
            }
            else if (v2.GetX() == -1 && v2.GetY() == 0) // Moving left
            {
                AddBodySegment(v1.X - 120, v1.Y);
                currentLeft = true;
            }
            else if (v2.GetX() == 0 && v2.GetY() == 1) // Moving down
            {
                AddBodySegment(v1.X, v1.Y + 120);
                currentDown = true;
            }
            else if (v2.GetX() == 0 && v2.GetY() == -1) // Moving up
            {
                AddBodySegment(v1.X, v1.Y - 120);
                currentUp = true;
            }

            // Normalize the direction vector
            direction.Normalize();
        }

        /// <summary>
        /// Resets the flags indicating the current direction of the snake.
        /// </summary>
        private void ResetDirectionFlags()
        {
            currentUp = false;
            currentDown = false;
            currentLeft = false;
            currentRight = false;
        }

        /// <summary>
        /// Adds a new body segment to the snake at the specified coordinates.
        /// </summary>
        /// <param name="x">The X coordinate of the new body segment.</param>
        /// <param name="y">The Y coordinate of the new body segment.</param>
        private void AddBodySegment(double x, double y)
        {
            body.AddLast(new Vector2D(x, y));
        }

    }
}

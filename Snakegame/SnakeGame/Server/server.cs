using NetworkUtil;
using System.Runtime.Serialization;
using System.Text;
using System.Xml;
using System.Diagnostics;


namespace SnakeGame
{

    public class Server
    {
        // Stores all client connections
        public static Dictionary<long, SocketState> clients = new Dictionary<long, SocketState>();

        // Stopwatch for tracking read/display intervals
        private static Stopwatch watch = new Stopwatch();

        // Server settings loaded from an XML file
        private static Settings set = new Settings();

        // The game world model
        public static World? world;

        /// <summary>
        /// Main entry point for the server. Initializes the game world and starts the server.
        /// </summary>
        /// <param name="args">Command line arguments.</param>
        private static void Main(string[] args)
        {
            LoadSettings();
            InitializeGameWorld();
            StartServer();

            Console.ReadLine(); // Keeps the server running until a key is pressed
        }

        /// <summary>
        /// Loads server settings from an XML file
        /// </summary>
        private static void LoadSettings()
        {
            using (XmlReader reader = XmlReader.Create(@"../../../settings.xml"))
            {
                set = (Settings)new DataContractSerializer(typeof(Settings)).ReadObject(reader)!;
            }
        }

        /// <summary>
        /// Initializes the game world with walls, size, and respawn rate
        /// </summary>
        private static void InitializeGameWorld()
        {
            world = new World(set.GetSize(), set.GetWalls(), set.GetRespawnRate());
        }

        /// <summary>
        /// Starts the server and begins accepting client connections
        /// </summary>
        private static void StartServer()
        {
            watch.Start();
            Start(); // Assuming 'Start' is a method to start the server
        }

        /// <summary>
        /// Starts the server and begins the game loop
        /// </summary>
        private static void Start()
        {
            Networking.StartServer(NewClient, 11000);
            Console.WriteLine("The server is ready, please connect.");

            GameLoop();
        }

        /// <summary>
        /// Executes the main game loop, updating the game world and clients.
        /// </summary>
        private static void GameLoop()
        {
            while (true)
            {
                UpdateClients();
            }
        }

        /// <summary>
        /// Updates the game world and sends the updated state to all connected clients.
        /// </summary>
        private static void UpdateClients()
        {
            if (ShouldUpdate())
            {
                watch.Restart();
                string gameState = BuildGameState();

                lock (world!)
                {
                    world.Update();
                    SendUpdatesToClients(gameState);
                }
            }
        }

        /// <summary>
        /// Determines if the game world should be updated based on the elapsed time.
        /// </summary>
        /// <returns>True if it's time to update the game world, false otherwise.</returns>
        private static bool ShouldUpdate()
        {
            if (!watch.IsRunning)
            {
                watch.Start();
                return false;
            }

            return watch.ElapsedMilliseconds >= set.GetMSPerFrame();
        }

        /// <summary>
        /// Builds the current game state as a string.
        /// </summary>
        /// <returns>The game state string.</returns>
        private static string BuildGameState()
        {
            StringBuilder stringBuilder = new StringBuilder();
            foreach (Snake snake in world!.Snakes.Values)
            {
                stringBuilder.Append(snake.ToString() + "\n");
            }
            foreach (Powerup powerup in world.Powerups.Values)
            {
                stringBuilder.Append(powerup.ToString() + "\n");
            }

            return stringBuilder.ToString();
        }

        /// <summary>
        /// Sends updates to all connected clients.
        /// </summary>
        /// <param name="gameState">The current game state.</param>
        private static void SendUpdatesToClients(string gameState)
        {
            foreach (SocketState client in clients.Values.ToList())
            {
                if (Networking.Send(client.TheSocket, gameState))
                {
                    Networking.Send(client.TheSocket, gameState);
                }
                else
                {
                    HandleClientDisconnect(client);
                }
            }
        }

        /// <summary>
        /// Handles the disconnection of a client.
        /// </summary>
        /// <param name="client">The disconnected client.</param>
        private static void HandleClientDisconnect(SocketState client)
        {
            Console.WriteLine($"Player({client.ID}) disconnected.");
            lock (world!)
            {
                if (world.Snakes.ContainsKey((int)client.ID))
                {
                    world.Snakes[(int)client.ID].disconnect();
                }
            }
            lock (clients)
            {
                clients.Remove(client.ID);
            }
        }

        /// <summary>
        /// Handles a new client connection.
        /// </summary>
        /// <param name="client">The newly connected client.</param>
        private static void NewClient(SocketState client)
        {
            if (client.ErrorOccurred) return;

            client.OnNetworkAction = ReceiveName;
            Networking.GetData(client);
        }

        /// <summary>
        /// Receives the player's name from the client and sends initial game data
        /// </summary>
        /// <param name="client">The client from whom the name is received.</param>
        private static void ReceiveName(SocketState client)
        {
            if (client.ErrorOccurred) return;

            string name = client.GetData().Trim();
            Console.WriteLine($"Player({client.ID}) joined as '{name}'.");

            client.OnNetworkAction = ReceiveClientData;
            AddClientAndSendInitialData(client, name);
            Networking.GetData(client);
        }

        /// <summary>
        /// Adds a new client and sends them initial game data
        /// </summary>
        /// <param name="client">The client to add.</param>
        /// <param name="name">The name of the player.</param>
        private static void AddClientAndSendInitialData(SocketState client, string name)
        {
            Snake player = world!.AddSnake(name, (int)client.ID);
            string data = $"{player.GetID()}\n{world.size}\n";
            Networking.Send(client.TheSocket, data);

            string wallData = GetWallsData();
            Networking.Send(client.TheSocket, wallData);

            lock (clients)
            {
                clients.Add(client.ID, client);
            }
        }

        /// <summary>
        /// Retrieves wall data as a string
        /// </summary>
        /// <returns>A string representing the wall data.</returns>
        private static string GetWallsData()
        {
            StringBuilder stringBuilder = new StringBuilder();
            foreach (Wall wall in world!.Walls.Values)
            {
                stringBuilder.Append(wall.ToString() + "\n");
            }
            return stringBuilder.ToString();
        }

        /// <summary>
        /// Processes client data and updates the game world accordingly
        /// </summary>
        /// <param name="client">The client sending the data.</param>
        private static void ReceiveClientData(SocketState client)
        {
            if (client.ErrorOccurred) return;

            ProcessClientCommands(client);
            Networking.GetData(client);
        }

        /// <summary>
        /// Processes commands sent by the client
        /// </summary>
        /// <param name="client">The client sending the commands.</param>
        private static void ProcessClientCommands(SocketState client)
        {
            string[] data = client.GetData().Split('\n', StringSplitOptions.RemoveEmptyEntries);
            foreach (string command in data)
            {
                lock (world!)
                {
                    if (world.Snakes.ContainsKey((int)client.ID))
                    {
                        world.Command(command, world.Snakes[(int)client.ID]);
                    }
                }
                client.RemoveData(0, command.Length + 1);
            }
        }
    }
}
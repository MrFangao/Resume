
using Newtonsoft.Json;
using System.Net.Sockets;
using System.Text.RegularExpressions;
using NetworkUtil;
using Newtonsoft.Json.Linq;


namespace SnakeGame
{
    
    public class GameController
    {
        /// <summary>
        ///  the g]Game World object
        /// </summary>
        public World world { get; set; } = new World();
      
        private SocketState? server;
        private int playerID;
        private string? playerName;
        public event Error? ErrorEvent;

        /// <summary>
        /// Delegate for update server
        /// </summary>
        /// <param name="m"></param>
        public delegate void MassageHandler(string m);
      
        /// <summary>
        /// Delegate error meaage.
        /// </summary>
        /// <param name="errorMessage"></param>
        public delegate void Error(string errorMessage = "");
       
        /// <summary>
        /// Event for updates server 
        /// </summary>
        public event MassageHandler? MassageArrivedEvent;
      
        /// <summary>
        /// Delegate method recieved player ID
        /// </summary>
        /// <param name="i"></param>
        public delegate void PlayerID(int i);

        /// <summary>
        /// Event for recieved player ID
        /// </summary>
        public event PlayerID? ReceivedPlayerID;


        /// <summary>
        /// Start Networking, connect to the target server
        /// </summary>
        /// <param name="playerName"></param>
        /// <param name="serverName"></param>
        public void Connect(string playerName, string serverName)
        {
            //player name
            this.playerName = playerName;
          
            Networking.ConnectToServer(OnConnect, serverName, 11000);      
        }

        /// <summary>
        /// Callback for connecting to the server
        /// </summary>
        /// <param name="state"></param>
        public void OnConnect(SocketState state)
        {
            // If any error occurs, it will fire the event.
            if (state.ErrorOccurred)
            {
                ErrorEvent?.Invoke("Error connecting to server");
                return;
            }
            Networking.Send(state.TheSocket, playerName + '\n');
            server = state;
            // Change delegate to accept the startup info
            state.OnNetworkAction = ReceiveStartupInfo;

            // Continue the event loop that receives messages from this client
            Networking.GetData(state);
        }

        /// <summary>
        /// This method receives the startup info sent by the server.
        /// </summary>
        /// <param name="state"></param>
        private void ReceiveStartupInfo(SocketState state)
        {
            // If any error occurs, it will fire the event.
            if (state.ErrorOccurred)
            {
                ErrorEvent?.Invoke("Unable to receive snake ID and world size");
                return;
            }
            // Splits the data and stores it in a string array
            string[] startingInfo = Regex.Split(state.GetData(), @"(?<=[\n])");

            // If the starting Info is empty, the event loop continues.
            if (startingInfo.Length <= 2 || !startingInfo[1].EndsWith("\n"))
            {
                Networking.GetData(state);
                return;
            }
            // Set player ID and world size 
            playerID = Int32.Parse(startingInfo[0]);
            world.size = Int32.Parse(startingInfo[1]);

            // Trigger the event when receive the player id
            ReceivedPlayerID!(playerID);

            // Remove processed data
            lock (world)
            {
                state.RemoveData(0, startingInfo[0].Length + startingInfo[1].Length);
            }
            // Change delegate to accept the Json
            state.OnNetworkAction = ReceiveJson;

            // Continue the event loop
            Networking.GetData(state);
        }

        /// <summary>
        /// Parses JSON received from the server to update the model
        /// </summary>
        /// <param name="state"></param>
        public void ReceiveJson(SocketState state)
        {
            // If any error occurs, it will fire the event.
            if (state.ErrorOccurred)
            {
                if (!IsServerShutDown(state.TheSocket))
                {
                    ErrorEvent?.Invoke("server is closed");
                }
                else
                {
                    ErrorEvent?.Invoke("Error when connect to server。");
                }
                return; ;
            }
            // Calls the method that handles data received through the socket
            ProcessMessages(state);

            // Continue the event loop
            Networking.GetData(state);
        }
        private bool IsServerShutDown(Socket socket)
        {
            //trying to connect with server
            //server shut down, assume server closed.
            try
            {
                // testing connection
                socket.Send(new byte[] { 0x1 }, 0, 1, SocketFlags.None);
                return false;
            }
            catch (SocketException e)
            {
                return e.SocketErrorCode == SocketError.NotConnected;
            }
        }

        /// <summary>
        /// This method processes the data received through the socket and splits the data and deserializes each string.
        /// </summary>
        /// <param name="state"></param>
        private void ProcessMessages(SocketState state)
        {
            // Splits the string 
            string[] parts = Regex.Split(state.GetData(), @"(?<=[\n])");

            // Loop until we have processed all messages.
            // We may have received more than one.
            lock (world)
            {
                foreach (string p in parts)
                {
                    // Ignore empty strings added by the regex splitter
                    if (p.Length == 0)
                    {
                        continue;
                    }

                    if (p[p.Length - 1] != '\n')
                    {
                        break;
                    }
                    // Trigger event
                    MassageArrivedEvent!(p);

                    // Call this method to deserialize the data
                    UpdateObject(p);

                    // Then remove it from the SocketState growable buffer
                    state.RemoveData(0, p.Length);
                }
            }
        }

        /// <summary>
        /// This method deserializes the object from the input string and processes it.
        /// </summary>
        /// <param name="part"></param>
        private void UpdateObject(string part)
        {
            JObject obj = JObject.Parse(part);

            // Snake handling part
            JToken? SnakeToken = obj["snake"];
            if (SnakeToken != null)
            {
                // Deserializes the object and set it
                Snake snake = JsonConvert.DeserializeObject<Snake>(part)!;
                world.Snakes[snake.GetID()] = snake;

                // If it disconnected, remove the snake
                if (snake.disconnected)
                {
                    world.Snakes.Remove(snake.ID);
                }
                return;
            }

            // Powerup handling part
            JToken PowerupToken = obj["power"]!;
            if (PowerupToken != null)
            {
                // Deserializes the object and set it
                Powerup power = JsonConvert.DeserializeObject<Powerup>(part)!;

                // Set it when they alive
                if (!power.Died)
                {
                    world.Powerups[power.GetID()] = power;
                }
                // Removes powerups when they die
                else
                {
                    world.Powerups.Remove(power.GetID());
                }                  
                return;
            }

            // Wall handling part
            JToken WallToken = obj["wall"]!;
            if (WallToken != null)
            {
                // Deserializes the object and set it
                Wall wall = JsonConvert.DeserializeObject<Wall>(part)!;

                // Set the wall ID
                world.Walls[wall.GetID()] = wall;
                return;
            }
        }

        /// <summary>
        /// Send a message to the server
        /// </summary>
        /// <param name="message"></param>
        public void MessageEntered(string message)
        {
            if (server is not null)
                Networking.Send(server.TheSocket, message + "\n");
        }



    }
}
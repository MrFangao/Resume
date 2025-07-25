
using System.Runtime.Serialization;


#nullable enable
namespace SnakeGame
{
    [DataContract(Name = "GameSettings", Namespace = "")]
    public class Settings
    {
        [DataMember]
        public int UniverseSize;

        [DataMember]
        public int MSPerFrame;

        [DataMember]
        public int FramesPerShot;

        [DataMember]
        public int RespawnRate;

        [DataMember]
        public List<Wall>? Walls;

        /// <summary>
        /// Gets the size of the game universe.
        /// </summary>
        /// <returns>The size of the universe.</returns>
        public int GetSize() => UniverseSize;

        /// <summary>
        /// Gets the number of milliseconds per frame for game updates
        /// </summary>
        /// <returns>The milliseconds per frame.</returns>
        public int GetMSPerFrame() => MSPerFrame;

        /// <summary>
        /// Gets the number of frames per shot for determining firing rate
        /// </summary>
        /// <returns>The frames per shot.</returns>
        public int GetFramesPerShot() => FramesPerShot;

        /// <summary>
        /// Gets the respawn rate for respawning entities in the game
        /// </summary>
        /// <returns>The respawn rate in frames.</returns>
        public int GetRespawnRate() => RespawnRate;

        /// <summary>
        /// Gets the list of walls defined in the game settings
        /// </summary>
        /// <returns>A list of walls.</returns>
        public IEnumerable<Wall> GetWalls() => new List<Wall>(Walls!);
    }

}
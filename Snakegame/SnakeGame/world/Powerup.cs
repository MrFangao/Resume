using Newtonsoft.Json;
using System.Runtime.Serialization;

namespace SnakeGame
{
    [DataContract(Name = "Powerup", Namespace = "")]
    [JsonObject(MemberSerialization.OptIn)]
    public class Powerup
    {
        /// <summary>
        /// An int representing the powerup's unique ID
        /// </summary>
        [DataMember]
        [JsonProperty(PropertyName = "power")]
        public int ID { get; internal set; } = 0;

        /// <summary>
        /// A Vector2D representing the location of the powerup
        /// </summary>
        [DataMember]
        [JsonProperty(PropertyName = "loc")]
        public Vector2D location { get; internal set; }

        /// <summary>
        /// A bool indicating if the powerup "died" (was collected by a player) on this frame
        /// </summary>
        [DataMember]
        [JsonProperty(PropertyName = "died")]
        public bool died { get; internal set; } = false;

        /// <summary>
        /// This method get the Powerup ID
        /// </summary>
        /// <returns></returns>
        public int GetID() => this.ID;

        /// <summary>
        /// This method check if the Powerup is eaten
        /// </summary>
        /// <returns></returns>
        public bool Died => this.died;

        // Used to get the next powerup id.
        private static int nextID;

        [JsonConstructor]
        public Powerup()
        { 
            // 在这里初始化属性的默认值
            location = new Vector2D(); // 假设Vector2D有一个默认构造函数
            // ID由nextID自动赋值
            ID = nextID++;
            died = false;
        }
        // Initialize the Powerups
        public Powerup(Vector2D v)
        {
            ID = nextID++;
            location = new Vector2D(v);
        }

        // Override the tostring method to Serialize data.
        public override string ToString() => JsonConvert.SerializeObject(this);
    }
}

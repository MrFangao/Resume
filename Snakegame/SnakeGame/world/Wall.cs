using Newtonsoft.Json;
using System.Runtime.Serialization;

namespace SnakeGame
{
    [DataContract(Name = "Wall", Namespace = "")]
    [JsonObject(MemberSerialization.OptIn)]
    public class Wall
    {
        /// <summary>
        /// A int representing the wall's unique ID.
        /// </summary>
        [DataMember(Order = 0)]
        [JsonProperty(PropertyName = "wall")]
        private int ID;

        /// <summary>
        /// A Vector2D representing one endpoint of the wall.
        /// </summary>
        [DataMember(Name = "p1", Order = 1)]
        [JsonProperty(PropertyName = "p1")]
        public Vector2D P1 { get; private set; }

        /// <summary>
        /// A Vector2D representing the other endpoint of the wall.
        /// </summary>
        [DataMember(Name = "p2", Order = 2)]
        [JsonProperty(PropertyName = "p2")]
        public Vector2D P2 { get; private set; }

        /// <summary>
        /// This method get the Wall ID.
        /// </summary>
        /// <returns></returns>
        public int GetID() => ID;

        // Variables for storing wall boundaries to prevent recalculating
        private int left;
        private int top;
        private int right;
        private int bottom;
        private bool wallBuilt = false; // Flag to indicate if the wall has been initialized

        /// <summary>
        /// Generates a sequence of Vector2D instances representing segments of a wall.
        /// If the wall hasn't been built, it calculates the wall boundaries based on the 
        /// points P1 and P2. It then yields Vector2D instances spaced 50 units apart along
        /// the wall, either vertically or horizontally.
        /// </summary>
        /// <returns>An IEnumerable of Vector2D representing the wall segments.</returns>
        public IEnumerable<Vector2D> GenerateSegments()
        {
            // Initialize wall boundaries only once
            if (!wallBuilt)
            {
                wallBuilt = true;
                left = (int)Math.Min(P1.GetX(), P2.GetX());
                right = (int)Math.Max(P1.GetX(), P2.GetX());
                top = (int)Math.Min(P1.GetY(), P2.GetY());
                bottom = (int)Math.Max(P1.GetY(), P2.GetY());
            }

            // Determine if the wall is vertical or horizontal based on the equalities of the coordinates
            bool isVertical = left == right;

            // Generate wall segments
            for (int i = 0; i <= (isVertical ? bottom - top : right - left); i += 50)
            {
                yield return new Vector2D(isVertical ? left : left + i, isVertical ? top + i : top);
            }
        }


        // Override the tostring method to Serialize data.
        public override string ToString() => JsonConvert.SerializeObject(this);

    }

}
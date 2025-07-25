using Newtonsoft.Json;

namespace SnakeGame
{

    public class KeyboardControl
    {
        // Storage request
        public string moving;
        public KeyboardControl(string m) => this.moving = m;

        // SerializeObject move request
        public override string ToString() => JsonConvert.SerializeObject(this);
    }
}

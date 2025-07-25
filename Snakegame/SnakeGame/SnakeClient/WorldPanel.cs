using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using IImage = Microsoft.Maui.Graphics.IImage;
#if MACCATALYST
using Microsoft.Maui.Graphics.Platform;
#else
using Microsoft.Maui.Graphics.Win2D;
#endif
using Font = Microsoft.Maui.Graphics.Font;
using SizeF = Microsoft.Maui.Graphics.SizeF;
using Color = Microsoft.Maui.Graphics.Color;
using System.Reflection;




namespace SnakeGame;
public class WorldPanel : IDrawable
{

    // Image variables and world object variables
    private IImage wall;
    private IImage background;
    private IImage explosion;
    private IImage mouse;
    private IImage bird;
    private World world;

    // Keep track the playerID
    private int playerID;

    // Trace whether to initialize Drawing
    private bool initializedForDrawing = false;

    // Store different colors
    private IEnumerable<Color> color;

    // A delegate for DrawObjectWithTransform
    // Methods matching this delegate can draw whatever they want onto the canvas  
    public delegate void ObjectDrawer(object o, ICanvas canvas);

    // Keep track the view size 
    private int viewSize = 900;

    // Graph view object
    private GraphicsView graphicsView = new();

#if MACCATALYST
    private IImage loadImage(string name)
    {
        Assembly assembly = GetType().GetTypeInfo().Assembly;
        string path = "SnakeGame.Resources.Images";
        return PlatformImage.FromStream(assembly.GetManifestResourceStream($"{path}.{name}"));
    }
#else
    /// <summary>
    /// Loads an image using either Mac or Windows image loading API
    /// </summary>
    /// <param name="name"></param>
    /// <returns></returns>
    private IImage loadImage(string name)
    {
        Assembly assembly = GetType().GetTypeInfo().Assembly;
        string path = "SnakeGame.Resources.Images";
        var service = new W2DImageLoadingService();
        return service.FromStream(assembly.GetManifestResourceStream($"{path}.{name}"));
    }
#endif

    /// <summary>
    /// Initialize the graph view
    /// </summary>
    public void Invalidate()
    {
        graphicsView.Invalidate();
    }

    /// <summary>
    /// Set the world
    /// </summary>
    /// <param name="w"></param>
    public void SetWorld(World w)
    {
        world = w;
    }

    /// <summary>
    /// Set the playerID
    /// </summary>
    /// <param name="ID"></param>
    public void SetPlayerID(int ID)
    {
        playerID = ID;
    }

    /// <summary>
    /// This is the signature of a delegate that provide to do the actual drawing.
    /// </summary>
    /// <param name="canvas"></param>
    /// <param name="o"></param>
    /// <param name="worldX"></param>
    /// <param name="worldY"></param>
    /// <param name="angle"></param>
    /// <param name="drawer"></param>
    private void DrawObjectWithTransform(ICanvas canvas, object o, double worldX, double worldY, double angle, ObjectDrawer drawer)
    {
        // "push" the current transform
        canvas.SaveState();

        canvas.Translate((float)worldX, (float)worldY);
        canvas.Rotate((float)angle);
        drawer(o, canvas);

        // "pop" the transform
        canvas.RestoreState();
    }

    /// <summary>
    /// This is the initialize drawing method.
    /// </summary>
    private void InitializeDrawing()
    {
        wall = loadImage("WallSprite.png");
        background = loadImage("Background.png");
        explosion = loadImage("Explosion.png");
        mouse = loadImage("Mouse.png");
        bird = loadImage("Bird.png");
        initializedForDrawing = true;
        color = Colors();
    }

    /// <summary>
    /// This method get the different colors
    /// </summary>
    /// <returns></returns>
    private IEnumerable<Color> Colors()
    { //colors
        List<Color> predefinedColors = new List<Color>
 
    {
        Color.FromRgb(0, 0, 0),      // Black
        Color.FromRgb(255, 165, 0),  // Orange
        Color.FromRgb(0, 0, 255),    // Blue
        Color.FromRgb(0, 128, 0),    // Green
        Color.FromRgb(255, 192, 203),// Pink
        Color.FromRgb(128, 128, 128), // Grey
        Color.FromRgb(223, 100, 23),
        Color.FromRgb(18, 156, 200),
    };

        foreach (Color color in predefinedColors)
        {
            yield return color;
        }

        // Generate random colors for additional players
        Random rand = new Random(8);
        while (true)
        {
            yield return Color.FromRgb(rand.Next(0, 255), rand.Next(0, 255), rand.Next(0, 255));
        }
    }

    /// <summary>
    /// This is the method to draw the wall.
    /// </summary>
    /// <param name="o"></param>
    /// <param name="canvas"></param>
    private void WallDrawer(object o, ICanvas canvas)
    {
        canvas.DrawImage(wall, (float)(-48f / 2), (float)(-48f / 2), 48f, 48f);
    }

    /// <summary>
    /// This is the method to draw the snake.
    /// </summary>
    /// <param name="o"></param>
    /// <param name="canvas"></param>
    private void SnakeDrawer(object o, ICanvas canvas)
    {
        // The appearance of a snake
        double? s = o as double?;
        canvas.StrokeSize = 10f;
        canvas.DrawLine(0.0f, 0.0f, 0.0f, (float)-s.Value);

        // Snack circle.
        canvas.FillCircle(0.0f, 0.0f, 5f);
        canvas.FillCircle(0.0f, (float)-s.Value, 5f);
    }

    /// <summary>
    /// This is the method to draw the snake name.
    /// </summary>
    /// <param name="o"></param>
    /// <param name="canvas"></param>
    private void SnakeNameDrawer(object o, ICanvas canvas)
    {
        Snake s = o as Snake;

        // Sets the font color, size, and format
        string nameplate = s.name + ": " + s.score;
        SizeF stringSize = canvas.GetStringSize(nameplate, Font.Default, 10 + 10 * 2f);
        canvas.Font = Font.Default;
        canvas.FontSize = 10;
        canvas.FontColor = Color.FromRgb(255, 255, 255);


        canvas.DrawString(nameplate, (float)(-stringSize.Width / 2.0), 0f, stringSize.Width, stringSize.Height, HorizontalAlignment.Center, VerticalAlignment.Center);
    }

    /// <summary>
    /// This is the method to draw the powerup.
    /// </summary>
    /// <param name="o"></param>
    /// <param name="canvas"></param>
    private void PowerupDrawer(object o, ICanvas canvas)
    {
        Powerup p = o as Powerup;
        if (p.ID % 2 == 0)
        {
            canvas.DrawImage(mouse, (float)(-40f / 2), (float)(-40f / 2), 40f, 40f);
        }
        else
        {
            canvas.DrawImage(bird, (float)(-40f / 2), (float)(-40f / 2), 40f, 40f);
        }
    }

    /// <summary>
    /// This is the method to draw the explosion.
    /// </summary>
    /// <param name="o"></param>
    /// <param name="canvas"></param>
    private void ExplosionDrawer(object o, ICanvas canvas)
    {
        float imageSize = 20f;

        for (int i = 0; i < 150; i++)
        {
            imageSize += 0.5f;
            canvas.DrawImage(explosion, (float)(-imageSize / 2), (float)(-imageSize / 2), imageSize, imageSize);

        }

    }
    /// <summary>
    /// This is the draw method for the panel.
    /// </summary>
    /// <param name="canvas"></param>
    /// <param name="dirtyRect"></param>
    public void Draw(ICanvas canvas, RectF dirtyRect)
    {
        // If the image is not initialized or there is no snakeID
        if (!world.Snakes.ContainsKey(playerID)) 
        {
            return;
        }
        if (!initializedForDrawing)
        {
            InitializeDrawing();
        }
        // undo any leftover transformations from last frame
        canvas.ResetState();

        // Draw the bankground
        Snake playerSnake = world.Snakes[playerID];
        float playerX = (float)playerSnake.Head.GetX();
        float playerY = (float)playerSnake.Head.GetY();
        // center the view on the middle of the world
        canvas.Translate(-playerX + (viewSize / 2), -playerY + (viewSize / 2));
        canvas.DrawImage(background, (-world.size / 2), (-world.size / 2), world.size, world.size);

        // Avoid thread conflicts
        lock (world)
        {
            // Draw the snake
            foreach (Snake s in world.Snakes.Values)
            {
                // Check to see if the snake is alive
                if (s.alive)
                {
                    foreach ((Vector2D v1, Vector2D v2) segment in s.snakeSegmentLength())
                    {
                        Vector2D vector2D = segment.v2 - segment.v1;
                        vector2D.Normalize();

                        // Set the snake color
                        canvas.FillColor = color.ElementAt<Color>(s.ID);
                        canvas.StrokeColor = Colors().ElementAt<Color>(s.ID);

                        // Check is the snake out of the world
                        if(Math.Abs(segment.v1.X-segment.v2.X) < world.size && Math.Abs(segment.v1.Y-segment.v2.Y) < world.size)
                        {
                            DrawObjectWithTransform(canvas, (segment.v2 - segment.v1).Length(), segment.v1.X, segment.v1.Y, vector2D.ToAngle(), new ObjectDrawer(SnakeDrawer));
                        }
                    }
                }
                else
                {
                    // If the snake died, draw the explosion image.
                    DrawObjectWithTransform(canvas, 0, s.Head.X, s.Head.Y, 0, new ObjectDrawer(ExplosionDrawer));
                }
                // draw the name
                DrawObjectWithTransform(canvas, s, s.Head.X, s.Head.Y, 0, new ObjectDrawer(SnakeNameDrawer));
            }
            // Draw the powerup
            foreach (var p in world.Powerups.Values)
            {
                DrawObjectWithTransform(canvas, p, p.location.GetX(), p.location.GetY(), 0, PowerupDrawer);
            }

            // Draw the walls
            foreach (Wall w in world.Walls.Values)
            {
                foreach (Vector2D segment in w.GenerateSegments())
                {
                    DrawObjectWithTransform(canvas, w, segment.GetX(), segment.GetY(), 0, new ObjectDrawer(WallDrawer));
                }
            }

        }
    }

}

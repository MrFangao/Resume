


namespace SnakeGame;

/// <summary>
/// This class is the View part of the game.
/// </summary>
/// <remarks>
/// </remarks>
public partial class MainPage : ContentPage
{
    /// <summary>
    /// This is the GameController object
    /// </summary>
    private GameController controller = new();

    /// <summary>
    /// Constructor of MainPage class
    /// </summary>
    public MainPage()
    {
        // Initialization
        InitializeComponent();
        graphicsView.Invalidate();

        // Handler for MassageArrivedEvent Event
        controller.MassageArrivedEvent += DisplayMessage;

        // Handler for ReceivedPlayerID Event
        controller.ReceivedPlayerID += SetPlayerID;

        // Set the worldPanel
        World world = controller.world;
        worldPanel.SetWorld(world);

        // controller's Error event
        controller.ErrorEvent += ShowError;
    }

    /// <summary>
    /// Handler for the controller's Error event
    /// </summary>
    /// <param name="err"></param>
    private void ShowError(string err)
    {
        // Show the error
        Dispatcher.Dispatch(() => DisplayAlert("Error : ", err, "OK"));
        controller.world = new World();
        worldPanel.SetWorld(controller.world);
        OnFrame();
        // Then re-enable the controlls so the user can reconnect

        connectButton.IsEnabled = true;
        serverText.IsEnabled = true;

    }

    /// <summary>
    /// This method set the playerID to worldPanel
    /// </summary>
    /// <param name="i"></param>
    private void SetPlayerID(int i)
    {
        worldPanel.SetPlayerID(i);

    }

    /// <summary>
    /// Handler for the controller's MessagesArrived event
    /// </summary>
    /// <param name="m"></param>
    private void DisplayMessage(string m)
    {
        graphicsView.Invalidate();
    }

    void OnTapped(object sender, EventArgs args)
    {
        keyboardHack.Focus();
    }

    /// <summary>
    /// This method controls movement.
    /// </summary>
    /// <param name="sender"></param>
    /// <param name="args"></param>
    void OnTextChanged(object sender, TextChangedEventArgs args)
    {
        Entry entry = (Entry)sender;
        String text = entry.Text.ToLower();
        if (text == "w")
        {
            controller.MessageEntered("{\"moving\":\"up\"}");
        }
        else if (text == "a")
        {
            controller.MessageEntered("{\"moving\":\"left\"}");
        }
        else if (text == "s")
        {
            controller.MessageEntered("{\"moving\":\"down\"}");
        }
        else if (text == "d")
        {
            controller.MessageEntered("{\"moving\":\"right\"}");
        }
        entry.Text = "";
    }
    /// <summary>
    /// Event handler for the connect button
    /// We will put the connection attempt logic here in the view, instead of the controller,
    /// because it is closely tied with disabling/enabling buttons, and showing dialogs.
    /// </summary>
    /// <param name="sender"></param>
    /// <param name="args"></param>
    private void ConnectClick(object sender, EventArgs args)
    {
        if (serverText.Text == "")
        {
            DisplayAlert("Error", "Please enter a server address", "OK");
            return;
        }
        if (nameText.Text == "")
        {
            DisplayAlert("Error", "Please enter a name", "OK");
            return;
        }
        if (nameText.Text.Length > 16)
        {
            DisplayAlert("Error", "Name must be less than 16 characters", "OK");
            return;
        }

        // Disable the controls and try to connect
        connectButton.IsEnabled = false;
        serverText.IsEnabled = false;

        // Call the controller to connect
        controller.Connect(nameText.Text, serverText.Text);

        keyboardHack.Focus();
    }

    /// <summary>
    /// This method displays help information.
    /// </summary>
    /// <param name="sender"></param>
    /// <param name="e"></param>
    private void ControlsButton_Clicked(object sender, EventArgs e)
    {
        DisplayAlert("Controls",
                     "W:\t\t Move up\n" +
                     "A:\t\t Move left\n" +
                     "S:\t\t Move down\n" +
                     "D:\t\t Move right\n",
                     "OK");
    }

    /// <summary>
    /// This method displays About information
    /// </summary>
    /// <param name="sender"></param>
    /// <param name="e"></param>
    private void AboutButton_Clicked(object sender, EventArgs e)
    {
        DisplayAlert("About",
      "SnakeGame solution\nArtwork by Jolie Uk and Alex Smith\nGame design by Daniel Kopta and Travis Martin\n" +
      "Implementation by YunzuHou\n" +
        "CS 3500 Fall 2022, University of Utah", "OK");
    }

    /// <summary>
    /// This method controls the state of the connect button
    /// </summary>
    /// <param name="sender"></param>
    /// <param name="e"></param>
    private void ContentPage_Focused(object sender, FocusEventArgs e)
    {
        if (!connectButton.IsEnabled)
            keyboardHack.Focus();
    }
    public void OnFrame()
    {
        Dispatcher.Dispatch(() => graphicsView.Invalidate());
    }

}
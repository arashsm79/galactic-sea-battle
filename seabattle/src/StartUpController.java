
/*

    Developed by @ArashSM79

*/
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;



public class StartUpController implements Initializable {


//================================================================

    /////// FXML Variable Declaration //////

    @FXML
    private TextField server_textField;

    @FXML
    private TextField port_textField;

    @FXML
    private MenuButton gridSize_menu;

    @FXML
    private MenuItem boardSizeOption1_menuItem;

    @FXML
    private MenuItem boardSizeOption2_menuItem;

    @FXML
    private MenuItem boardSizeOption3_menuItem;

    @FXML
    private Button connect_button;

    @FXML
    private GridPane client_grid;

    @FXML
    private GridPane fleet_grid;

    @FXML
    private CheckBox horizontal_checkBox;

    @FXML
    private Label chosenUnit_label;

    @FXML
    private TextField userName_label;

    @FXML
    private ProgressIndicator connectProgress_ProgressIndicator;

    @FXML
    private AnchorPane gridsAnchorPane;

    /////// FXML Variable Declaration END //////

//================================================================

    /////// Variable Declaration //////

    private Scene scene;

    private Stage mainStage;

    private SeaBattleButton[][] clientGridArray;

    private SeaBattleButton selectedFleet = null;

    private int clientGridSize = 10;

    private final PseudoClass hoverClass = PseudoClass.getPseudoClass("hover");

    private int numberOfShipsPlaced = 0;

    private Connection connectionToServer;

    /////// Variable Declaration END //////

//================================================================

    /////// Initialization //////

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {

        // add in the troops
        populateFleetGrid(fleet_grid);

    }

    /////// Initialization END //////

//================================================================

    /////// FXML Method Declaration //////

        @FXML
        void boardSizeOption1_menuItemOnAction(ActionEvent event) {

            gridSize_menu.setText(boardSizeOption1_menuItem.getText());
            clientGridSize = 10;
            populateGridWithEmptyCells(client_grid, clientGridSize);
            resetFleetGrid(fleet_grid);

        }

        @FXML
        void boardSizeOption2_menuItemOnAction(ActionEvent event) {

            gridSize_menu.setText(boardSizeOption2_menuItem.getText());
            clientGridSize = 15;
            populateGridWithEmptyCells(client_grid, clientGridSize);
            resetFleetGrid(fleet_grid);
        }

        @FXML
        void boardSizeOption3_menuItemOnAction(ActionEvent event) {

            gridSize_menu.setText(boardSizeOption3_menuItem.getText());
            clientGridSize = 20;
            populateGridWithEmptyCells(client_grid, clientGridSize);
            resetFleetGrid(fleet_grid);
        }
        
        boolean debug = true;
        @FXML
        void connect_buttonOnAction(ActionEvent event) {

            // get the current scene this controller is set to
            // we can do this using any component inside the controller
            scene = connect_button.getScene();
            mainStage = (Stage) scene.getWindow();
            

            //if user hasnt placed all of the fleet, display an error
            if(!checkFleetSelection())
            {
                popupAlert(AlertType.ERROR, "Select some fleet by dragging them across");

                return;
            }

            //get the data from the text fields
            String username = userName_label.getText();
            String ip = server_textField.getText();
            int port = 0;
            try {
                port = Integer.parseInt(port_textField.getText());
            } catch (Exception e) {
                popupAlert(AlertType.ERROR, "Please enter a valid port.");
            }

            //convert the board to json
            String boardJson = convertBoardToJson();

            //lunch a service and start it
            connectionToServer = new Connection(ip, port, username, clientGridSize, boardJson, this);
            connectionToServer.start();
            

            //bind the visible property of progress indicator to the running property of service
            connectProgress_ProgressIndicator.visibleProperty().bind(connectionToServer.runningProperty());

        }

        @FXML
        void onHelpMenuButtonAction(ActionEvent event) {
            popupAlert(AlertType.INFORMATION, "Developed by @ArashSM79\n 4th of May 2020");
        }

        @FXML
        void onNewMenuButtonAction(ActionEvent event) {

            gridSize_menu.setText("10 x 10");
            clientGridSize = 10;
            populateGridWithEmptyCells(client_grid, clientGridSize);
            resetFleetGrid(fleet_grid);
        }

        @FXML
        void onQuitMenuButtonAction(ActionEvent event) {
            // get the current scene this controller is set to
            // we can do this using any component inside the controller
            if(connect_button != null)
            {
                Scene scene = connect_button.getScene();
                if(scene != null)
                {
                    mainStage = (Stage) scene.getWindow();
                    mainStage.close();
                }
            }
        }

    /////// FXML Method Declaration END //////

    //================================================================

    /////// Method Declaration //////

    //this method pupolates the fleet grid with available ships
    //and attaches the related listeners to them
    private void populateFleetGrid(GridPane gridPane) {
        
        gridPane.getStylesheets().addAll(getClass().getResource("/resources/styles/startUpchooseFleet.css").toExternalForm());
        
        //adding 4 soldiers
        for(int i = 0; i < 5; i++)
        {   
            SeaBattleButton btn = new SeaBattleButton(SeaBattleButton.CellType.SOLDIER, i, 0);
            btn.setPrefSize(50, 50);
            
            btn.setOnDragDetected(onFleetGridButtonDragDetected);
            btn.setOnMouseEntered(onFleetGridButtonMouseEntered);
         

            btn.setId("soldier");
            gridPane.add(btn , i, 0, 1, 1);
        }

        
        //3 calvaries
        for(int i = 0; i < 3; i++)
        {   
            SeaBattleButton btn = new SeaBattleButton(SeaBattleButton.CellType.CAVALRY, i, 2);
            btn.setMinSize(50, 100);

            btn.setOnDragDetected(onFleetGridButtonDragDetected);
            btn.setOnMouseEntered(onFleetGridButtonMouseEntered);
            
            btn.setId("cavalry");
            gridPane.add(btn , i, 1, 1, 2);
        }

        
        //2 forts
        for(int i = 0; i < 2; i++)
        {   
            SeaBattleButton btn = new SeaBattleButton(SeaBattleButton.CellType.FORT, i, 4);
            btn.setMinSize(100, 100);

            btn.setOnDragDetected(onFleetGridButtonDragDetected);
            btn.setOnMouseEntered(onFleetGridButtonMouseEntered);
            btn.setId("fort");
            gridPane.add(btn , i, 3, 2, 2);

            
        }

        
        //a headquarters
            SeaBattleButton btn = new SeaBattleButton(SeaBattleButton.CellType.HEADQUARTERS, 0, 5);
            btn.setMinSize(150, 150);

            btn.setOnDragDetected(onFleetGridButtonDragDetected);
            btn.setOnMouseEntered(onFleetGridButtonMouseEntered);  

            btn.setId("hq");

            gridPane.add(btn , 0, 5, 3, 3);
    }



    //makes sure that all of the available fleet are selected before connecting to the server
    private boolean checkFleetSelection() {
        if(numberOfShipsPlaced <= 11)
        {
            return true;
        }else
        {

            return false;
        }
    }



    //displays a simple pop up alert
    private void popupAlert(AlertType alertType, String msg) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Attention!");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.initOwner(mainStage);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.showAndWait();
    }
    


    //this method handles the game ready response from our server
    //then launches the game controller passes to it the necessary data to it
    public void handleServerMessage(HashMap<String, String> msg)
    {
        Platform.runLater(() -> 
        {
            switch (msg.get(ASBTP.ACTION_CODE)) {
                case ASBTP.GAME_READY:
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("resources/fxml/game.fxml"));
                        Parent root = loader.load();
                        GameController gameController = (GameController)loader.getController();
                        connectionToServer.setGameController(gameController);
                        gameController.initValues(connectionToServer, clientGridArray, clientGridSize, (Stage) scene.getWindow());    
                        gameController.setOpponent_labelText(msg.get(ASBTP.BODY));                    
                        scene.setRoot(root);
                    } catch (IOException e) {
                        popupException(e);
                    }
                    break;
    
                default:
                    break;
            }

        });
    }



    //converts the final board to a json 
    private String convertBoardToJson() 
    {

        ArrayList<Cell> board  = new ArrayList<>();
        for(int i = 0; i < clientGridSize; i++)
        {
            for(int j = 0; j < clientGridSize; j++)
            {
                //i = col and j = row
                SeaBattleButton currentBtn = clientGridArray[i][j];
                board.add(currentBtn.getCell());
        
            }
        }
        Gson gson = new Gson();
        String json = gson.toJson(board);

		return json;
	}



    //each time the client chooses a new board size this method will be called to clear the client grid
    private void resetFleetGrid(GridPane gridPane)
    {
        gridPane.getChildren().forEach(new Consumer<Node>() {

            @Override
            public void accept(Node t) {
                SeaBattleButton btn = (SeaBattleButton) t;
                btn.setDisable(false);

            }
        });
        selectedFleet = null;
        numberOfShipsPlaced = 0;
    }


    //first makes sure that the spot is available for placement and then places the ship based on the selected orientaation
    private boolean placeShip(GridPane gridPane, SeaBattleButton[][] buttonArray, int size, SeaBattleButton clickedBtn, SeaBattleButton selectedShip)
    {
        //check if the area around the chosen button is empty for placement
        //if its vertical 
        if(!horizontal_checkBox.isSelected())
        {
            return placeShipWithinLimits(gridPane, buttonArray,  size,  clickedBtn, selectedShip, selectedShip.getShip().getAreaRow(), selectedShip.getShip().getAreaCol());

        }else
        {
            //if its horizontal then change the place of the limits
            return placeShipWithinLimits(gridPane, buttonArray,  size,  clickedBtn, selectedShip, selectedShip.getShip().getAreaCol(), selectedShip.getShip().getAreaRow());
        }
  
    }


    //this function places the ship based on the selected orientation
    //we now how much space a ship allocates to it self
    //for examlpe cavalry is 1 x 2
    //now if horizontal check box is selected we will change the allocated space by cavalry to 2 x 1
    private boolean placeShipWithinLimits(GridPane gridPane, SeaBattleButton[][] buttonArray, int size, 
    SeaBattleButton clickedBtn, SeaBattleButton selectedShip, int rowLimit, int colLimit)
    {
        int clickedBtnRow = clickedBtn.getCell().getRow();
        int clickedBtnCol = clickedBtn.getCell().getCol();

        for(int i = 0; i < rowLimit; i++)
        {
            for(int j = 0; j < colLimit; j++)
            {
                int curCol = clickedBtnCol + j;
                int curRow = clickedBtnRow + i;
                if( curCol >= clientGridSize || curRow >= clientGridSize || (buttonArray[curCol][curRow].getCellType() != SeaBattleButton.CellType.EMPTY ))
                {
                    return false;
                }
            }
        }

        //After the validation we now place the button
        int imageIndexCounter = 1;
        for(int i = 0; i < rowLimit; i++)
        {
            for(int j = 0; j < colLimit; j++)
            {
                SeaBattleButton currentBtn =  buttonArray[clickedBtnCol + j][clickedBtnRow + i];
                currentBtn.setShip(selectedShip.getCellType());
                currentBtn.getCell().setHeadShipCordinates(new Point(clickedBtnCol, clickedBtnRow));
                currentBtn.pseudoClassStateChanged(hoverClass, false);
                
                currentBtn.setId(currentBtn.getCellType().toString().toLowerCase() + imageIndexCounter);
                //add the child ships to the headship
                if(!currentBtn.equals(clickedBtn))
                {
                    clickedBtn.getCell().getChildShips().add(new Point(currentBtn.getCell().getCol(), currentBtn.getCell().getRow()));
                }
                imageIndexCounter++;
                
            }
        }
        return true;
    }



    //populates the client grid with empty cells and attaches the necessary listeners to it
    private void populateGridWithEmptyCells(GridPane gridPane, int size)
    {
        clientGridArray = new SeaBattleButton[size][size];
        gridPane.getStylesheets().addAll(getClass().getResource("/resources/styles/startUpClientGrid.css").toExternalForm());
        gridPane.getChildren().clear();
        
        
        for(int i = 0; i < size; i++)
        {
           for(int j = 0; j < size; j++)
           {
               SeaBattleButton btn = new SeaBattleButton(SeaBattleButton.CellType.EMPTY, i, j);
               btn.setPrefSize(80, 80);
               btn.getStyleClass().add("empty-cell");

               btn.setOnDragOver(onClientGridButtonDragOver);
               btn.setOnDragEntered(onClientGridButtonMouseDragEntered);
               btn.setOnDragExited(onClientGridButtonMouseDragExited);
               btn.setOnDragDropped(onClientGridButtonDragDropped);
               
               gridPane.add(btn, i, j);

               clientGridArray[i][j] = btn;
           }

        } 

    }

    
    //another simple popup alert that prints out the exception trace
    private void popupException(Exception ex)
    {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("An Error Occured");
        alert.setHeaderText("Opps! something went wrong!");
        alert.setContentText("The program cought the following exception: ");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);

        alert.initModality(Modality.WINDOW_MODAL);
        alert.showAndWait();
    }

    //marks the client grid buttons based on the selected button
    //if mark is true it changes the pseudoclass to true and if it is false it changes it back to false
    private void clientGridButtonHoverMark(SeaBattleButton hoveredBtn, int rowLimit, int colLimit, boolean mark)
    {
        boolean isPlaceable = true;
        //check whether the button is placeable here
        int clickedBtnRow = hoveredBtn.getCell().getRow();
        int clickedBtnCol = hoveredBtn.getCell().getCol();


        for(int i = 0; i < rowLimit; i++)
        {
            for(int j = 0; j < colLimit; j++)
            {
                int curCol = clickedBtnCol + j;
                int curRow = clickedBtnRow + i;
                if( curCol >= clientGridSize || curRow >= clientGridSize || (clientGridArray[curCol][curRow].getCellType() != SeaBattleButton.CellType.EMPTY ))
                {
                    isPlaceable = false;
                }
            }
        }
        if(isPlaceable)
        {

            //After the validation we now mark the button
            for(int i = 0; i < rowLimit; i++)
            {
                for(int j = 0; j < colLimit; j++)
                {
                    SeaBattleButton currentBtn =  clientGridArray[clickedBtnCol + j][clickedBtnRow + i];
                    
                    currentBtn.pseudoClassStateChanged(hoverClass, mark);
                                        
                }
            }
        }
    }
    /////// Method Declaration END//////
    
    //================================================================
    
    /////// Event Handlers Declaration //////
    

    //starts the drag and drop procedure
    private EventHandler<MouseEvent> onFleetGridButtonDragDetected = (MouseEvent event) -> {

        SeaBattleButton btn = (SeaBattleButton) event.getSource();
        selectedFleet = btn;
        chosenUnit_label.setText(selectedFleet.getCellType().toString());
        Dragboard db = btn.startDragAndDrop(TransferMode.ANY);
        SnapshotParameters param = new SnapshotParameters();
        param.setFill(Color.TRANSPARENT);
        db.setDragView(btn.snapshot(param, null));
        ClipboardContent content = new ClipboardContent();
        content.putString(btn.getCellType().toString());
        db.setContent(content);
        event.consume();
             
    };

    //when mouse enters a fleet button it changes its curson image to hand to indicate it's draggable
    private EventHandler<MouseEvent> onFleetGridButtonMouseEntered = (MouseEvent e) -> {
            SeaBattleButton button = (SeaBattleButton) e.getSource();
            button.setCursor(Cursor.HAND);
        
    };


    //after the drop event places the ship if possible
    private EventHandler<DragEvent> onClientGridButtonDragDropped = (DragEvent event) -> {
        
        SeaBattleButton btn = (SeaBattleButton) event.getSource();

        if (selectedFleet != null) {
            if (placeShip(client_grid, clientGridArray, clientGridSize, btn, selectedFleet)) {
                selectedFleet.setDisable(true);
                numberOfShipsPlaced++;

            } else {

                selectedFleet.setDisable(false);
                selectedFleet = null;
            }

            selectedFleet = null;
        }
        event.consume();
    };


    //marks the possition of the selected ship on the client grid
    private EventHandler<DragEvent> onClientGridButtonMouseDragEntered = (DragEvent event) -> 
    {
        SeaBattleButton hoveredBtn = (SeaBattleButton) event.getSource();

        if(selectedFleet != null && selectedFleet.getCellType() != SeaBattleButton.CellType.EMPTY)
        {

            //horizontally
            if(!horizontal_checkBox.isSelected())
            {
                clientGridButtonHoverMark(hoveredBtn, selectedFleet.getShip().getAreaRow(), selectedFleet.getShip().getAreaCol(), true);
            //vertically
            }else
            {
                clientGridButtonHoverMark(hoveredBtn, selectedFleet.getShip().getAreaCol(), selectedFleet.getShip().getAreaRow(), true);
            }

        }else 
        {
            hoveredBtn.pseudoClassStateChanged(hoverClass, true);
        }
        event.consume();

    };


    //this method makes the button accept drag events
    private EventHandler<DragEvent> onClientGridButtonDragOver = (DragEvent event ) -> {

        SeaBattleButton hoveredBtn = (SeaBattleButton) event.getSource();

        if (event.getGestureSource() != hoveredBtn && event.getDragboard().hasString()) {

            event.acceptTransferModes(TransferMode.ANY);
        }

        event.consume();
    };


    //on the drag exit remove the selected ship's mark from client grid
    private EventHandler<DragEvent> onClientGridButtonMouseDragExited = (DragEvent event) -> 
    {

        SeaBattleButton hoveredBtn = (SeaBattleButton) event.getSource();
        if(selectedFleet != null && selectedFleet.getCellType() != SeaBattleButton.CellType.EMPTY)
        {
            //horizontally
            if(!horizontal_checkBox.isSelected())
            {
                clientGridButtonHoverMark(hoveredBtn, selectedFleet.getShip().getAreaRow(), selectedFleet.getShip().getAreaCol(), false);
            //vertically
            }else
            {
                clientGridButtonHoverMark(hoveredBtn, selectedFleet.getShip().getAreaCol(), selectedFleet.getShip().getAreaRow(), false);
            }

        }else 
        {
            hoveredBtn.pseudoClassStateChanged(hoverClass, false);
        }
        event.consume();


    };
    
    /////// Event Handlers Declaration END//////



}

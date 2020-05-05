/*

    Developed by @ArashSM79

*/
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Cursor;

public class GameController implements Initializable {

    //================================================================

    /////// FXML Variable Declaration //////
    @FXML
    private Label clientUsername_label;

    @FXML
    private Label money_label;

    @FXML
    private Button move_button;

    @FXML
    private MenuButton buy_menuButton;

    @FXML
    private Button cancel_Button;

    @FXML
    private Label opponent_label;

    @FXML
    private GridPane client_grid;

    @FXML
    private GridPane opponent_grid;

    @FXML
    private ImageView clientAvatar;

    @FXML
    private ImageView opponantAvatar;

    @FXML
    private CheckBox horizontal_checkBox;

    @FXML
    private Button loseTurn_button;

    @FXML
    private MenuButton chat_menuButton;

    @FXML
    private VBox chat_vbox;


    /////// FXML Variable Declaration  END//////

    //================================================================

    /////// Variable Declaration //////

    private Connection connectionToServer;
    private SeaBattleButton[][] clientGridArray;
    private SeaBattleButton[][] opponantGridArray;
    private int clientGridSize;

    private Gson gson = new Gson();
    
    private boolean turn = false;
    
    private ActionType actionType = ActionType.NOTHING;
    
    private final PseudoClass markClass = PseudoClass.getPseudoClass("mark");
    private final PseudoClass moveClass = PseudoClass.getPseudoClass("move");
    private final PseudoClass destroyedClass = PseudoClass.getPseudoClass("destroyed");
    private final PseudoClass attackClass = PseudoClass.getPseudoClass("attack");

    
    private ArrayList<SeaBattleButton> targetCells = null;
    private SeaBattleButton selectedAttackingCell = null;
    
    
    private Stage mainStage = null;
    
    private int numberOfForts = 2;

    /////// Variable Declaration END//////

    //================================================================

    /////// Initialization //////
    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {

        client_grid.getStylesheets()
                .addAll(getClass().getResource("/resources/styles/gameClientGrid.css").toExternalForm());
        opponent_grid.getStylesheets()
                .addAll(getClass().getResource("/resources/styles/gameClientGrid.css").toExternalForm());

    }

    //this function is called from the startup controller after the initialization of game controller
    public void initValues(Connection connectionToServer, SeaBattleButton[][] clientGridArray, int clientGridSize, Stage stage) {
        this.connectionToServer = connectionToServer;
        this.clientGridArray = clientGridArray;
        this.clientGridSize = clientGridSize;
        this.mainStage = stage;

        startUp();
    }


    // starts after initValues()
    public void startUp() {
        populateClientGrid(client_grid, clientGridSize);
        populateOpponantGridWithEmptyCells(opponent_grid, clientGridSize);

        clientUsername_label.setText(connectionToServer.getUsername());

        changeTurnGUI(turn);

        setupBuyMenuButton();
        setupChatMenuButton();
    }

    /////// Initialization END //////

    //================================================================

    /////// FXML Method Declaration //////
    @FXML
    void onCancelButtinAction(ActionEvent event) {

        cancelActionType();
    }

    @FXML
    void onMoveButtinAction(ActionEvent event) {

        actionType = ActionType.MOVE;
        actionModeGUI(true);

    }

    @FXML
    void onLoseTurnActionButton(ActionEvent event) {
        if(turn)
        {
            connectionToServer.sendToServer(ASBTP.LOSE_TURN);
            cancelActionType();

        }
    }

    @FXML
        void onHelpMenuButtonAction(ActionEvent event) {
            popupAlert(AlertType.INFORMATION, "Developed by @ArashSM79\n 4th of May 2020");
        }


    @FXML
    void onQuitMenuButtonAction(ActionEvent event) {

        if(mainStage != null)
        {
            mainStage.close();
        }
    }

    /////// FXML Method Declaration END//////

    //================================================================

    ///////  Method Declaration //////


    //this method handles the incoming responses from the server
    //the service calls back to this function when ever it receives data from the server
    public void handleServerMessage(HashMap<String, String> msg) {

        //get the data from the service thats communicating with the server
        String responseCode = msg.get(ASBTP.ACTION_CODE);
        String body = msg.get(ASBTP.BODY);

        switch (responseCode) {

            //indicates who's turn it is
            case ASBTP.CLIENT_TURN: {

                if (body.equals(ASBTP.TRUE)) {
                    turn = true;
                } else {
                    turn = false;
                }

                // this function substracts 1 from all the non zero cool downs
                calculateColdDowns();

                Platform.runLater(() -> {
                    changeTurnGUI(turn);
                });
                break;
            }

            //this response code means that this player has been attacked by the other player
            //it receives a list of attacked cells and updates client grid occurdingly 
            case ASBTP.ATTACK: {

                String json = body;
                Type collectionType = new TypeToken<ArrayList<Cell>>() {}.getType();
                
                //parse the json
                ArrayList<Cell> clientAttackedCells = gson.fromJson(json, collectionType);

                //update the client board
                Platform.runLater(() -> 
                {

                    //for each attacked cell, set the pseudoclass to destroyed if it's a nont empty cell
                    //if it was an empty cell set it to nothingdestroyed
                    for (Cell c : clientAttackedCells) {
                        SeaBattleButton btn = clientGridArray[c.getCol()][c.getRow()];
                        btn.getCell().setDestroyed(true);
                        if (btn.getCellType() != SeaBattleButton.CellType.EMPTY) {
                            btn.pseudoClassStateChanged(destroyedClass, true);
                        } else {
                            btn.setId("nothingdestroyed");

                        }
                    }

                    checkIfFortIsDestroyed(clientAttackedCells);

                });
                break;
            }


            // this means that the attack the client just did was a success
            //updates the opponant grid based on the received cells from the server
            case ASBTP.ATTACK_OK: {
                String json = body;
                Type collectionType = new TypeToken<ArrayList<Cell>>() {}.getType();

                ArrayList<Cell> opponantAttackedCells = gson.fromJson(json, collectionType);

                // add cold down for the attacking cells
                addCoolDown();

                Platform.runLater(() -> {

                    //update the opponant grid 
                    for (Cell c : opponantAttackedCells) {
                        SeaBattleButton btn = opponantGridArray[c.getCol()][c.getRow()];
                        btn.setCell(c);
                        btn.getCell().setDestroyed(true);
                        if (btn.getCell().getCellType() != ASBTP.EMPTY) {
                            btn.setId("destroyed");
                        } else {
                            btn.setId("nothingdestroyed");

                        }

                    }
                });
                break;
            }

            //the client updates its balance
            case ASBTP.BALANCE: {
                Platform.runLater(() -> {
                    money_label.setText(body);
                });
                break;
            }

            //if the buying action was succcessfull then update the client grid based on the boughts cells received from the server
            case ASBTP.BUY: {
                String[] lines = body.split(System.getProperty("line.separator"));

                String json = lines[0];
                
                //the second line is the type of ship that was bought
                SeaBattleButton.CellType shipType = parseShipTypeFromInt(Integer.parseInt(lines[1]));

                Type collectionType = new TypeToken<ArrayList<Point>>() {}.getType();

                //the first line is the cells that have been bought
                ArrayList<Point> boughtCellsCords = gson.fromJson(json, collectionType);
                
                //place the boughts ship and update the board accordingly
                Platform.runLater(() -> {
                    placeShip(boughtCellsCords, shipType);
                    cancelActionType();

                });

                break;
            }

            //move the ships that were receved from the server
            //it receives the source head ship and the destination points
            case ASBTP.MOVE: {
                String[] lines = body.split(System.getProperty("line.separator"));

                String json = lines[0];
                Point incomingSourceCords = gson.fromJson(json, Point.class);
                Cell sourceCell = clientGridArray[incomingSourceCords.getCol()][incomingSourceCords.getRow()].getCell();

                json = lines[1];
                Type collectionType = new TypeToken<ArrayList<Point>>() {}.getType();
                ArrayList<Point> destionationPoints = gson.fromJson(json, collectionType);

                //update the client grid
                Platform.runLater(() -> {
                    placeMovedShip(destionationPoints, sourceCell);
                    cancelActionType();
                   
                });

                break;
            }


            //on game end simply popup a dialogue
            case ASBTP.GAME_END:
            {

                Platform.runLater(()-> 
                {
                    endGameAlert(body);
                });
                break;
            }

            //receive the chat message and the user name of the person who has sent it
            case ASBTP.CHAT:
            {
                String[] lines = body.split(System.getProperty("line.separator"));
                String senderUsername = lines[0];
                String senderMsg = lines[1];

                //update the chat panel accordingly
                Platform.runLater(()-> 
                {   
                    Label chatMsgLabel = new Label(" " + senderUsername + ": \n   " + senderMsg);
                    chatMsgLabel.setMaxWidth(Double.MAX_VALUE);
                    chatMsgLabel.setStyle("-fx-text-fill:white; -fx-background-color: rgba(124, 106, 106, 0.35);" + 
                    "-fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.6) , 5, 0.0 , 0 , 1 );");
                    chatMsgLabel.setWrapText(true);
                    chatMsgLabel.setPrefWidth(130);
                    chat_vbox.getChildren().add(chatMsgLabel);
                });
                break;
            }

            //if an error happened, display a pop up
            case ASBTP.ERROR:

                Platform.runLater(()-> 
                {
                    popupAlert(AlertType.ERROR, "Ops! Something went wrong.\nPlease Try again.");
                });
                break;

            default:
                break;
        }

    }


    //populates the client grid and attaches the necessary listeners
    private void populateClientGrid(GridPane gridpane, int size)
    {
        gridpane.getChildren().clear();
        for(int i = 0; i < size; i++)
        {
           for(int j = 0; j < size; j++)
           {
               SeaBattleButton btn = clientGridArray[i][j];
               btn.setOnAction(onClientGridButtonClick);
               btn.setOnMouseEntered(onClientGridButtonHover);
               btn.setOnMouseExited(onClientGridButtonHoverExit);

               btn.setOnDragDetected(onClientGridButtonDragDetected);

               btn.setOnDragDropped(onClientGridButtonDragDropped);

               btn.setOnDragOver(onClientGridButtonDragOver);

               btn.setOnDragEntered(onClientGridButtonMouseDragEntered);

               btn.setOnDragExited(onClientGridButtonMouseDragExited);

               btn.setOnDragDone(onClientGridButtonDragDone); 


               gridpane.add(btn, i, j);
           }

        } 

    }


    //populates the opponant grid and attaches the necessary listeners
    private void populateOpponantGridWithEmptyCells(GridPane gridpane, int size)
    {
        opponantGridArray = new SeaBattleButton[size][size];
        gridpane.getChildren().clear();
        for(int i = 0; i < size; i++)
        {
           for(int j = 0; j < size; j++)
           {
               SeaBattleButton btn = new SeaBattleButton(SeaBattleButton.CellType.EMPTY, i, j);
               btn.setPrefSize(80, 80);

               btn.getStyleClass().add("empty-cell");
               btn.setOnAction(onOpponantGridButtonClick);
               btn.setOnMouseEntered(onOpponantGridButtonMouseHover);
               btn.setOnMouseExited(onOpponantGridButtonMouseHoverExit);
               gridpane.add(btn, i, j);
               opponantGridArray[i][j] = btn;
           }

        } 

    }


    //////// ATTACK Methods ////////

     // send the attacking cell and target cell cordinates to the server
     private void attack(SeaBattleButton targetButton, SeaBattleButton attackingButton) {

        Point attackingCell = new Point(attackingButton.getCell().getCol(), attackingButton.getCell().getRow());
        Point targetCell = new Point(targetButton.getCell().getCol(), targetButton.getCell().getRow());

        connectionToServer.sendToServer(ASBTP.ATTACK);
        connectionToServer.sendToServer(gson.toJson(attackingCell));
        connectionToServer.sendToServer(gson.toJson(targetCell));

        selectedAttackingCell = attackingButton;

        cancelActionType();


    }


    //checks to see whether attacking is possible given the attacked and defender cells
    private boolean checkAttackPossiblility(SeaBattleButton selectedBtn, SeaBattleButton attackerButton) {

        // make sure it doesnt exceed grid size
        int col = selectedBtn.getCell().getCol();
        int row = selectedBtn.getCell().getRow();
        switch (attackerButton.getCellType()) {
            case SOLDIER:

                break;
            case CAVALRY:
                if (row + 1 < clientGridSize && col + 1 < clientGridSize) {
                    break;
                } else {
                    return false;
                }
            case FORT:
                if (row + 1 < clientGridSize && col + 1 < clientGridSize) {
                    break;
                } else {
                    return false;
                }
            case HEADQUARTERS:
                if (row + 1 < clientGridSize && col + 1 < clientGridSize && row - 1 >= 0 && col - 1 >= 0) {
                    break;
                } else {
                    return false;
                }

            default:

                return false;
        }

        // make sure all of the cells are not destroyed (if they're all destroyed then they have a cool down of -1)
        // check for cold down of the attacking cell and its head ship and the
        // headship's children
        SeaBattleButton attackerHeadShip = clientGridArray[attackerButton.getCell().getHeadShipCordinates().getCol()][attackerButton.getCell().getHeadShipCordinates().getRow()];

        if (attackerHeadShip.getCell().getCoodDown() != 0) {
            return false;
        }

        for (Point p : attackerHeadShip.getCell().getChildShips()) {
            SeaBattleButton childCell = clientGridArray[p.getCol()][p.getRow()];

            if (childCell.getCell().getCoodDown() != 0) {
                return false;
            }

        }

        return true;

    }


    //check if any of the cells have cool down of -1 (completely destroyed) if yes check whether its a fort or not
    //so basically all this piece of code does is it checks whether a fort has been destroyed completely
    //if yes then substracts one from the number of client forts
    private void checkIfFortIsDestroyed(ArrayList<Cell> clientAttackedCells)
    {
        for (Cell c : clientAttackedCells) 
        {
            if(c.getCellType() == ASBTP.FORT)
            {

                Cell destroyedCellHeadShip = clientGridArray[c.getHeadShipCordinates().getCol()][c.getHeadShipCordinates().getRow()].getCell();

                //just a flag not to count this whole ship twice or more
                if(destroyedCellHeadShip.getCoodDown() > -1)
                {

                    boolean hasDestroyedEntireShip = true;
                    for(Point p : destroyedCellHeadShip.getChildShips())
                    {
                        Cell childCell = clientGridArray[p.getCol()][p.getRow()].getCell();

                        if(!childCell.isDestroyed())
                        {
                            hasDestroyedEntireShip = false;
                        }
                    }
                    if(!destroyedCellHeadShip.isDestroyed())
                    {
                        hasDestroyedEntireShip = false;
                    }
            
                    if(hasDestroyedEntireShip)
                    {
                        //just a flag not no add the bounty for this whole ship twice or more
                        for(Point p : destroyedCellHeadShip.getChildShips())
                        {
                            Cell childCell = clientGridArray[p.getCol()][p.getRow()].getCell();
                
                            childCell.setCoolDown(-1);
                        
                        }
                        destroyedCellHeadShip.setCoolDown(-1);

                        //if the ship that has completely been destroyed is a fort then decrease the number of target player forts
               
                        numberOfForts -= 1;
                        
                    }
                    
                }
            }
        }
    }


    //////// ATTACK Methods END ////////





    //////// BUY Methods ////////

    //if the buying conditions are met and the placement area is empty
    //then send buy request to server
    private boolean buyWithinLimits(GridPane gridPane, SeaBattleButton[][] buttonArray, int size,
            SeaBattleButton clickedBtn, SeaBattleButton.CellType selectedShip, int rowLimit, int colLimit) {

        //check if there is enough balance
        int balance = 0;

        try {
            balance = Integer.parseInt(money_label.getText());
        } catch (Exception e) {

        }

        //chek if the player has atlwast one fort
        if(numberOfForts < 1)
        {
            return false;
        }

        //check if the player has enough money
        if(balance < selectedShip.getCode() * ASBTP.PRICE_MULTIPLIER)
        {
            return false;
        }

        // check if placement is available
        int clickedBtnRow = clickedBtn.getCell().getRow();
        int clickedBtnCol = clickedBtn.getCell().getCol();

        for (int i = 0; i < rowLimit; i++) {
            for (int j = 0; j < colLimit; j++) {
                int curCol = clickedBtnCol + j;
                int curRow = clickedBtnRow + i;
                if (curCol >= clientGridSize || curRow >= clientGridSize
                        || (buttonArray[curCol][curRow].getCellType() != SeaBattleButton.CellType.EMPTY)) {
                    return false;
                }
            }
        }

        // send the cell you want to purchase and the type of ship to server
        connectionToServer.sendToServer(ASBTP.BUY);
        connectionToServer.sendToServer(gson.toJson(new Point(clickedBtnCol, clickedBtnRow)));
        connectionToServer.sendToServer(Boolean.toString(horizontal_checkBox.isSelected()).toUpperCase());
        connectionToServer.sendToServer(Integer.toString(actionType.getActionButtonCellType().getCode()));
        return true;

    }


    //buy the selected ship
    //calls buyWithinLimits mehotd based on the selected property of the horizontal checkbox
    private boolean buy(GridPane gridPane, SeaBattleButton[][] buttonArray, int size, SeaBattleButton clickedBtn,
            SeaBattleButton.CellType selectedShip) {

        // check if the area around the chosen button is empty for placement
        // if its vertical
        if (!horizontal_checkBox.isSelected()) {
            return buyWithinLimits(gridPane, buttonArray, size, clickedBtn, selectedShip,
                    selectedShip.getShip().getAreaRow(), selectedShip.getShip().getAreaCol());

        } else {
            // if its horizontal then change the place of the limits
            return buyWithinLimits(gridPane, buttonArray, size, clickedBtn, selectedShip,
                    selectedShip.getShip().getAreaCol(), selectedShip.getShip().getAreaRow());
        }

    }


    //places the bought ship
    private void placeShip(ArrayList<Point> cellCords, SeaBattleButton.CellType shipType)
    {
        //if the bought ship is a fort increase the number of forts
        if(shipType == SeaBattleButton.CellType.FORT)
        {
            numberOfForts += 1;
        }

        
         int headShipCordRow = cellCords.get(0).getRow();
         int headShipCordCol = cellCords.get(0).getCol();

         SeaBattleButton headShip = clientGridArray[headShipCordCol][headShipCordRow];
         
         int imageIndexCounter = 1;

        for(Point p : cellCords)
        {
            SeaBattleButton currentBtn =  clientGridArray[p.getCol()][p.getRow()];
            currentBtn.setShip(shipType);

            currentBtn.getCell().setHeadShipCordinates(new Point(headShipCordCol, headShipCordRow));
            
            currentBtn.setId(currentBtn.getCellType().toString().toLowerCase() + imageIndexCounter);

            //add the child ships to the headship
            if(!currentBtn.equals(headShip))
            {
                headShip.getCell().getChildShips().add(new Point(currentBtn.getCell().getCol(), currentBtn.getCell().getRow()));
            }

            imageIndexCounter++;



        }
                 
          
    }
    //////// BUY Methods END ////////



    //////// MOVE Methods ////////
    private void move(SeaBattleButton sourceButton, SeaBattleButton destButton) {

        if (!checkMovePossibility(sourceButton, destButton)) {
            popupAlert(AlertType.ERROR, "Can't move there!");

            return;
        }

        // send the ship's headship cordinates you want to move and the destination cell
        // to server
        connectionToServer.sendToServer(ASBTP.MOVE);
        connectionToServer.sendToServer(gson.toJson(sourceButton.getCell().getHeadShipCordinates()));
        connectionToServer
                .sendToServer(gson.toJson(new Point(destButton.getCell().getCol(), destButton.getCell().getRow())));


        actionModeGUI(false);

    }


    //check the moving possibility of the selected button
    private boolean checkMovePossibility(SeaBattleButton sourceButton, SeaBattleButton destButton) {


        // only soldiers and cavalries are allowed to move
        if (!(sourceButton.getCellType() == SeaBattleButton.CellType.SOLDIER
                || sourceButton.getCellType() == SeaBattleButton.CellType.CAVALRY)) {
            return false;
        }

        Cell sourceCell = sourceButton.getCell();

        Point sourceCellHeadShipCords = sourceCell.getHeadShipCordinates();
        Cell sourceCellHeadShip = clientGridArray[sourceCellHeadShipCords.getCol()][sourceCellHeadShipCords.getRow()].getCell();

        Cell destCell = destButton.getCell();

        //for soldiers its only 2 blocks
        //for cavalries only 1 block
        int rowDelta = destCell.getRow() - sourceCellHeadShip.getRow();
        int colDelta = destCell.getCol() - sourceCellHeadShip.getCol();

        switch (sourceButton.getCellType()) {
            case SOLDIER:
                if (!((Math.abs(rowDelta) <= 2 && Math.abs(rowDelta) > 0 && Math.abs(colDelta) == 0)
                        || (Math.abs(colDelta) <= 2 && Math.abs(colDelta) > 0 && Math.abs(rowDelta) == 0))) {
                    return false;
                }
                break;

            case CAVALRY:
                if (!((Math.abs(rowDelta) == 1 && Math.abs(colDelta) == 0)
                        || (Math.abs(colDelta) == 1 && Math.abs(rowDelta) == 0))) {
                    return false;
                }
                break;
            default:
                return false;
        }


        //put all of the source ships points including the headship into one arraylist
        ArrayList<Point> allSourcePoints = new ArrayList<>();
        allSourcePoints.add(sourceCellHeadShipCords);
        allSourcePoints.addAll(sourceCellHeadShip.getChildShips());

        // make sure its not destroyed
        for (Point p : allSourcePoints) {
            Cell btn = clientGridArray[p.getCol()][p.getRow()].getCell();
            if (btn.isDestroyed()) {
                return false;
            }
        }

        //validates the destionation cells
        for (Point p : allSourcePoints) {
            SeaBattleButton checkingBtn = clientGridArray[p.getCol() + colDelta][p.getRow() + rowDelta];
            Point checkingCellCords = new Point(checkingBtn.getCell().getCol(), checkingBtn.getCell().getRow());

            if (checkingBtn.getCellType() != SeaBattleButton.CellType.EMPTY) {
                if (checkingBtn.getCellType() == sourceButton.getCellType()) {

                    boolean isInTheSameShip = false;
                    for (Point sourceChildCord : allSourcePoints) {

                        if (checkingCellCords.equals(sourceChildCord)) {
                            isInTheSameShip = true;
                        }
                    }

                    if (!isInTheSameShip)
                        return false;

                } else {
                    return false;
                }
            }

            //if the place this button wants to move is destroyed it cant be moved there
            if(checkingBtn.getCell().isDestroyed())
            {
                return false;
            }

        }

        return true;

    }


    //places the moved ship
    private void placeMovedShip(ArrayList<Point> destionationPoints, Cell sourceCell) {
        
        SeaBattleButton.CellType shipType = parseShipTypeFromInt(sourceCell.getCellType());

        int sourceCoolDown = sourceCell.getCoodDown();

        //remove the mevoed ship
        removeMovedShip(sourceCell);

        int headShipCordRow = destionationPoints.get(0).getRow();
        int headShipCordCol = destionationPoints.get(0).getCol();
        SeaBattleButton destionationHeadship = clientGridArray[headShipCordCol][headShipCordRow];
         
         int imageIndexCounter = 1;

        for(Point p : destionationPoints)
        {
            SeaBattleButton currentBtn =  clientGridArray[p.getCol()][p.getRow()];
            currentBtn.setShip(shipType);

            currentBtn.getCell().setHeadShipCordinates(new Point(headShipCordCol, headShipCordRow));
            currentBtn.getCell().setCoolDown(sourceCoolDown);

            //set back the newly placed cells cooldown effect
            if(currentBtn.getCell().getCoodDown() > 0)
            {
                currentBtn.getCell().setCoolDown(currentBtn.getCell().getCoodDown() - 1);
                currentBtn.setEffect(new GaussianBlur(8));                  
            }else
            {
                currentBtn.setEffect(null);                  
            }

            currentBtn.setId(currentBtn.getCellType().toString().toLowerCase() + imageIndexCounter);

            //add the child ships to the headship
            if(!currentBtn.equals(destionationHeadship))
            {
                destionationHeadship.getCell().getChildShips().add(new Point(currentBtn.getCell().getCol(), currentBtn.getCell().getRow()));
            }

            imageIndexCounter++;
        }

    }


    //marks the surrounding cells that the selected cell can move to
    private void markMoveableSpots(SeaBattleButton btn, boolean mark) {

        switch (btn.getCellType()) {
            case SOLDIER:
            {
                int movingBtnRow = btn.getCell().getRow();
                int movingBtnCol = btn.getCell().getCol();
                //mark the two buttons on each 4 side of this button
                for(int i = 1; i <=2; i++)
                {
                    //up
                    if(movingBtnRow - i >=  0)
                    {
                        SeaBattleButton toBeMovedToBtn =  clientGridArray[movingBtnCol][movingBtnRow - i];
                        if(toBeMovedToBtn.getCellType() == SeaBattleButton.CellType.EMPTY && !toBeMovedToBtn.getCell().isDestroyed())
                        {
                            toBeMovedToBtn.pseudoClassStateChanged(moveClass, mark);
                        }
                    }


                    //down
                    if(movingBtnRow + i < clientGridSize)
                    {
                        SeaBattleButton toBeMovedToBtn =  clientGridArray[movingBtnCol][movingBtnRow + i];
                        if(toBeMovedToBtn.getCellType() == SeaBattleButton.CellType.EMPTY && !toBeMovedToBtn.getCell().isDestroyed())
                        {
                            toBeMovedToBtn.pseudoClassStateChanged(moveClass, mark);
                        }
                    }


                    //left
                    if(movingBtnCol + i < clientGridSize)
                    {
                        SeaBattleButton toBeMovedToBtn =  clientGridArray[movingBtnCol + i][movingBtnRow];
                        if(toBeMovedToBtn.getCellType() == SeaBattleButton.CellType.EMPTY && !toBeMovedToBtn.getCell().isDestroyed())
                        {
                            toBeMovedToBtn.pseudoClassStateChanged(moveClass, mark);
                        }
                    }


                    //right
                    if(movingBtnCol - i >= 0)
                    {
                        SeaBattleButton toBeMovedToBtn =  clientGridArray[movingBtnCol - i][movingBtnRow];
                        if(toBeMovedToBtn.getCellType() == SeaBattleButton.CellType.EMPTY && !toBeMovedToBtn.getCell().isDestroyed())
                        {
                            toBeMovedToBtn.pseudoClassStateChanged(moveClass, mark);
                        }

                    }

                }
            }
                

            case CAVALRY:
            {
                //mark the one button on top and bottom and mark the two buttons directly tp the sides of this button
                //create a list of points including the head ship
                ArrayList<Point> allShipCellCords = new ArrayList<Point> (btn.getCell().getChildShips());
                allShipCellCords.add(btn.getCell().getHeadShipCordinates());

                for(Point p: allShipCellCords)
                {
                    int movingBtnRow = p.getRow();
                    int movingBtnCol = p.getCol();
                    //cavalry can only move one cell hence:
                    int i = 1;

                    //up
                    if(movingBtnRow - i >= 0)
                    {
                        SeaBattleButton toBeMovedToBtn =  clientGridArray[movingBtnCol][movingBtnRow - i];
                        if(toBeMovedToBtn.getCellType() == SeaBattleButton.CellType.EMPTY && !toBeMovedToBtn.getCell().isDestroyed())
                        {
                            toBeMovedToBtn.pseudoClassStateChanged(moveClass, mark);
                        }
                    }


                    //down
                    if(movingBtnRow + i < clientGridSize)
                    {
                        SeaBattleButton toBeMovedToBtn =  clientGridArray[movingBtnCol][movingBtnRow + i];
                        if(toBeMovedToBtn.getCellType() == SeaBattleButton.CellType.EMPTY && !toBeMovedToBtn.getCell().isDestroyed())
                        {
                            toBeMovedToBtn.pseudoClassStateChanged(moveClass, mark);
                        }
                    }


                    //left
                    if(movingBtnCol + i < clientGridSize)
                    {
                        SeaBattleButton toBeMovedToBtn =  clientGridArray[movingBtnCol + i][movingBtnRow];
                        if(toBeMovedToBtn.getCellType() == SeaBattleButton.CellType.EMPTY && !toBeMovedToBtn.getCell().isDestroyed())
                        {
                            toBeMovedToBtn.pseudoClassStateChanged(moveClass, mark);
                        }
                    }


                    //right
                    if(movingBtnCol - i >= 0)
                    {
                        SeaBattleButton toBeMovedToBtn =  clientGridArray[movingBtnCol - i][movingBtnRow];
                        if(toBeMovedToBtn.getCellType() == SeaBattleButton.CellType.EMPTY && !toBeMovedToBtn.getCell().isDestroyed())
                        {
                            toBeMovedToBtn.pseudoClassStateChanged(moveClass, mark);
                        }

                    }

                }
                
            }
        
            default:
                break;
        }
    }

    private void removeMovedShip(Cell sourceCell)
    {
        //change back the buttons of the source ship to normal
        ArrayList<Point> allSourcePoints = new ArrayList<Point>();
        allSourcePoints.add(sourceCell.getHeadShipCordinates());
        allSourcePoints.addAll(sourceCell.getChildShips());

        for(Point p : allSourcePoints)
        {
            SeaBattleButton btn = clientGridArray[p.getCol()][p.getRow()];

            btn.setShip(SeaBattleButton.CellType.EMPTY);
            btn.getCell().setHeadShipCordinates(null);
            btn.getCell().resetChildShips();
            btn.getCell().setCoolDown(0);
            btn.setId("");
            btn.setEffect(null);            
        }


    
    }
    //////// MOVE Methods END ////////


    //disables and enables the move, buy, cancel buttons accordingly
    private void actionModeGUI(boolean shouldDisable)
    {
        move_button.setDisable(shouldDisable);
        buy_menuButton.setDisable(shouldDisable);
        horizontal_checkBox.setDisable(shouldDisable);

        cancel_Button.setDisable(!shouldDisable);
    }



    //mark the client grid buttons based on the given parameters
    private void clientGridButtonHoverMark(SeaBattleButton hoveredBtn, int rowLimit, int colLimit, boolean mark) {

        boolean isPlaceable = true;
        // check whether the button is placeable here
        int clickedBtnRow = hoveredBtn.getCell().getRow();
        int clickedBtnCol = hoveredBtn.getCell().getCol();

        for (int i = 0; i < rowLimit; i++) {
            for (int j = 0; j < colLimit; j++) {
                int curCol = clickedBtnCol + j;
                int curRow = clickedBtnRow + i;
                if (curCol >= clientGridSize || curRow >= clientGridSize
                        || (clientGridArray[curCol][curRow].getCellType() != SeaBattleButton.CellType.EMPTY)) {
                    isPlaceable = false;
                }
            }
        }
        if (isPlaceable) {

            // After the validation we now mark the button
            for (int i = 0; i < rowLimit; i++) {
                for (int j = 0; j < colLimit; j++) {
                    SeaBattleButton currentBtn = clientGridArray[clickedBtnCol + j][clickedBtnRow + i];

                    currentBtn.pseudoClassStateChanged(markClass, mark);

                }
            }
        }

    }


    //sets up the buy menu button and attaches listeners to its items
    private void setupBuyMenuButton() {

        buy_menuButton.getItems().forEach(new Consumer<MenuItem>() {

            @Override
            public void accept(MenuItem t) {
                t.setOnAction((ActionEvent event) -> {
                    MenuItem mi = (MenuItem) event.getSource();
                    try {
                        actionType = ActionType.BUY;
                        String str = mi.getText().toUpperCase();

                        actionType.setActionButtonCellType(SeaBattleButton.CellType.valueOf(str.substring(str.indexOf(" ") + 1)));

                        actionModeGUI(true);

                    } catch (Exception e) {
                        popupException(e);
                    }
                });

            }
        });
    }

    //sets up the chat menu button and attaches listeners to its items
    private void setupChatMenuButton() {

        chat_menuButton.getItems().forEach(new Consumer<MenuItem>() {

            @Override
            public void accept(MenuItem t) {
                t.setOnAction((ActionEvent event) -> {
                    MenuItem mi = (MenuItem) event.getSource();

                    //send the message to server
                    try {
                        if(turn)
                        {
                            connectionToServer.sendToServer(ASBTP.CHAT);
                            connectionToServer.sendToServer(mi.getText());
                        }

                    } catch (Exception e) {
                        popupException(e);
                    }
                });

            }
        });
    }


    //a simple popup alert
    private void popupAlert(AlertType alertType, String msg) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Attention!");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.initOwner(mainStage);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.showAndWait();
    }


    //the end game alert
    private void endGameAlert(String result) {
        Alert alert = new Alert(AlertType.INFORMATION);
        String title = "";
        String msg = "";
        switch (result) {
            case ASBTP.WON: {
                title = "Winner Winner, Chicken Dinner!";
                msg = "You Won!\nThanks for playing.\nClick OK to return to the main menu";
                break;
            }

            case ASBTP.LOST: {
                title = "You Lost!";
                msg = "I'm sorry you lost.\nThanks for playing; Better luck next time.\nClick OK to return to the main menu";

                break;
            }

            case ASBTP.OPPONENT_DISCONNECTED: {
                title = "Opponent Left";
                msg = "Opponent Disconnected\nClick OK to return to the main menu";

                break;
            }

            case ASBTP.CLIENT_DISCONNECTED: {
                title = "Lost Connection!";
                msg = "You lost your connection to the server.\nClick OK to return to the main menu";

                break;
            }
            default:
                title = "Game Ended!";
                msg = "Click OK to return to the main menu";
                break;
        }

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.initOwner(mainStage);
        alert.initModality(Modality.WINDOW_MODAL);
        Optional<ButtonType> alertResult = alert.showAndWait();

        if (!alertResult.isPresent()) {
            // end the program
            connectionToServer.closeSocket();
            mainStage.close();

        } else if (alertResult.get() == ButtonType.OK) {
            connectionToServer.closeSocket();
            mainStage.close();

        }else if(alertResult.get() == ButtonType.CANCEL)
        {
            // end the program
            connectionToServer.closeSocket();

            mainStage.close();
        }
    }



    //sets the actiontype to nothing
    //and updates the ation buttons GUI
    private void cancelActionType()
    {
        actionType = ActionType.NOTHING;
        actionType.setActionButton(null);
        actionModeGUI(false);

    }



    private SeaBattleButton.CellType parseShipTypeFromInt(int shipType)
    {
        switch (shipType) {
            case ASBTP.SOLDIER:
                return SeaBattleButton.CellType.SOLDIER;

            case ASBTP.CAVALRY:
                return SeaBattleButton.CellType.CAVALRY;

            case ASBTP.FORT:
                return SeaBattleButton.CellType.FORT;

            case ASBTP.HEADQUARTERS:
                return SeaBattleButton.CellType.HEADQUARTERS;

            default:
                return SeaBattleButton.CellType.EMPTY;

        }
    }


    //remove one from the cooldowns of all non empty cells
    private void calculateColdDowns() {
        for(SeaBattleButton btnCol[] : clientGridArray)
        {
            for(SeaBattleButton btn : btnCol)
            {

                if(btn.getCellType() != SeaBattleButton.CellType.EMPTY && btn.getCell().getCoodDown() > 0)
                {
                    btn.getCell().setCoolDown(btn.getCell().getCoodDown() - 1);
                    btn.setEffect(new GaussianBlur(8));                  

                }else
                {
                    btn.setEffect(null);
                }
            }
        }
    }



    //sets the buttons enable/disable based on the turn
    private void changeTurnGUI(boolean clientTurn)
    {
        double clientAvatarOpacity = clientTurn ? 1: 0.5;
        double opponantAvatarOpacity = clientTurn ? 0.5: 1.0;
    
        move_button.setDisable(!clientTurn);
        cancel_Button.setDisable(!clientTurn);
        buy_menuButton.setDisable(!clientTurn);
        clientUsername_label.setDisable(!clientTurn);
        horizontal_checkBox.setDisable(!clientTurn);
        money_label.setDisable(!clientTurn);
        loseTurn_button.setDisable(!clientTurn);
        chat_vbox.setDisable(!clientTurn);
        chat_menuButton.setDisable(!clientTurn);

        clientAvatar.setOpacity(clientAvatarOpacity);
        opponantAvatar.setOpacity(opponantAvatarOpacity);
   
    }



    //adds cooldown for the cell that just attacked
    private void addCoolDown() {

        //add cold down for the attacking cell and its head ship and the headship's children
        SeaBattleButton attackerHeadShip =clientGridArray[selectedAttackingCell.getCell().getHeadShipCordinates().getCol()][selectedAttackingCell.getCell().getHeadShipCordinates().getRow()];
        attackerHeadShip.getCell().setCoolDown(attackerHeadShip.getCell().getCellType() * 2);

        for(Point p : attackerHeadShip.getCell().getChildShips())
        {
            SeaBattleButton childCell = clientGridArray[p.getCol()][p.getRow()];
            childCell.getCell().setCoolDown(childCell.getCell().getCellType() * 2);

        }

        selectedAttackingCell = null;
    }


    /**
     * @return the opponent_label
     */
    public void setOpponent_labelText(String text) {
        opponent_label.setText(text);
    }


    //an enum for the action that the client has chosen
    public enum ActionType {
        BUY,
        MOVE,
        ATTACK,
        NOTHING;

        private SeaBattleButton actionButton;
        private SeaBattleButton.CellType actionButtonCellType;
        /**
         * @param actionButton the actionButton to set
         */
        public void setActionButton(SeaBattleButton actionButton) {
            this.actionButtonCellType = null;
            this.actionButton = actionButton;
        }
        /**
         * @return the actionButton
         */
        public SeaBattleButton getActionButton() {
            return actionButton;
        }

        /**
         * @param actionButtonCellType the actionButtonCellType to set
         */
        public void setActionButtonCellType(SeaBattleButton.CellType actionButtonCellType) {
            this.actionButtonCellType = actionButtonCellType;
        }
        /**
         * @return the actionButtonCellType
         */
        public SeaBattleButton.CellType getActionButtonCellType() {
            return actionButtonCellType;
        }
    }

    //simple popup for displaying an exception
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

        alert.initOwner(mainStage);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.showAndWait();


    }

    //================================================================


    /////// Event Handlers //////


    // %% Drag and Drop Moving Functionality 
    //initiates the drag and drop procedure
    private EventHandler<MouseEvent> onClientGridButtonDragDetected = (MouseEvent event) -> {

        //do so only if the move button has been clicked
        if (actionType == ActionType.MOVE) {

            SeaBattleButton btn = (SeaBattleButton) event.getSource();
            actionType.setActionButton(btn);

            markMoveableSpots(btn, true);

            Dragboard db = btn.startDragAndDrop(TransferMode.ANY);
            SnapshotParameters param = new SnapshotParameters();
            param.setFill(Color.TRANSPARENT);
            db.setDragView(btn.snapshot(param, null));
            ClipboardContent content = new ClipboardContent();
            content.putString(btn.getCellType().toString());
            db.setContent(content);
            event.consume();

        }
    };

    // %% Drag and Drop Moving Functionality 
    // remove the marked cells after the dragging is done
    private EventHandler<DragEvent> onClientGridButtonDragDone = (DragEvent event) -> {

        if (actionType == ActionType.MOVE) {

            markMoveableSpots(actionType.getActionButton(), false);

        }
    };


    
    // %% Drag and Drop Moving Functionality 
    //makes the buttons to be able to accept drag events
    private EventHandler<DragEvent> onClientGridButtonDragOver = (DragEvent event) -> {

        SeaBattleButton hoveredBtn = (SeaBattleButton) event.getSource();

        if (event.getGestureSource() != hoveredBtn && event.getDragboard().hasString()) {

            event.acceptTransferModes(TransferMode.ANY);
        }

        event.consume();
    };


    // %% Drag and Drop Moving Functionality 
    //on drag dropped, move the button
    private EventHandler<DragEvent> onClientGridButtonDragDropped = (DragEvent event) -> {

        SeaBattleButton btn = (SeaBattleButton) event.getSource();

        if (actionType == ActionType.MOVE) {

            if (actionType.getActionButton() != null) {

                move(actionType.getActionButton(), btn);
            }

        }
        event.consume();
    };


    // %% Drag and Drop Moving Functionality 
    //mark the cells based one the size of the ship thats being dragged
    private EventHandler<DragEvent> onClientGridButtonMouseDragEntered = (DragEvent event) -> {
        SeaBattleButton hoveredBtn = (SeaBattleButton) event.getSource();

        if (actionType == ActionType.MOVE) {

            //only soldier and cavalry can move
            if (actionType.getActionButton() != null
                    && (actionType.getActionButton().getCellType() == SeaBattleButton.CellType.SOLDIER
                            || actionType.getActionButton().getCellType() == SeaBattleButton.CellType.CAVALRY)) {

                // horizontally
                if (!horizontal_checkBox.isSelected()) {
                    clientGridButtonHoverMark(hoveredBtn, actionType.getActionButton().getShip().getAreaRow(),
                            actionType.getActionButton().getShip().getAreaCol(), true);
                    // vertically
                } else {
                    clientGridButtonHoverMark(hoveredBtn, actionType.getActionButton().getShip().getAreaCol(),
                            actionType.getActionButton().getShip().getAreaRow(), true);
                }

            }
        }
        event.consume();

    };


    // %% Drag and Drop Moving Functionality 
    //on drag exit, remove the marked buttons
    private EventHandler<DragEvent> onClientGridButtonMouseDragExited = (DragEvent event) -> {

        SeaBattleButton hoveredBtn = (SeaBattleButton) event.getSource();
        if (actionType == ActionType.MOVE) {

            if (actionType.getActionButton() != null
                    && (actionType.getActionButton().getCellType() == SeaBattleButton.CellType.SOLDIER
                            || actionType.getActionButton().getCellType() == SeaBattleButton.CellType.CAVALRY)) {

                // horizontally
                if (!horizontal_checkBox.isSelected()) {
                    clientGridButtonHoverMark(hoveredBtn, actionType.getActionButton().getShip().getAreaRow(),
                            actionType.getActionButton().getShip().getAreaCol(), false);
                    // vertically
                } else {
                    clientGridButtonHoverMark(hoveredBtn, actionType.getActionButton().getShip().getAreaCol(),
                            actionType.getActionButton().getShip().getAreaRow(), false);
                }

            }
        }
        event.consume();

    };


    //what happens when the user clicks on a client grid button based on the action type
    //if the action type is not move or buy then it means that the user want to attack with this button
    private EventHandler<ActionEvent> onClientGridButtonClick = (ActionEvent event) -> {
        SeaBattleButton btn = (SeaBattleButton) event.getSource();
        if (turn) {
            switch (actionType) {
                case BUY: {
                    if (actionType.getActionButtonCellType() != null) {
                        //if the action type is buy then try to buy 
                        if( !buy(client_grid, clientGridArray, clientGridSize, btn, actionType.getActionButtonCellType())) {

                            popupAlert(AlertType.ERROR, "Cannot buy that");
                        }
                    }

                    //unmark the ship
                    if (actionType.getActionButtonCellType() != null) {
                        // horizontally
                        if (!horizontal_checkBox.isSelected()) {
                            clientGridButtonHoverMark(btn, actionType.getActionButtonCellType().getShip().getAreaRow(),
                                    actionType.getActionButtonCellType().getShip().getAreaCol(), false);
                            // vertically
                        } else {
                            clientGridButtonHoverMark(btn, actionType.getActionButtonCellType().getShip().getAreaCol(),
                                    actionType.getActionButtonCellType().getShip().getAreaRow(), false);
                        }
        
                    }

                    break;
                }
                case MOVE: {

                    break;
                }
                default:
                    //if the action type is not move or buy then it means that the user want to attack with this button
                    actionType = ActionType.ATTACK;
                    actionType.setActionButton(btn);
                    break;
            }

        }

    };


    //if the action type is buy the mark the cell and its surroundings based on the sheep thats selected for buying
    //if the move button has been clicked then change the cursor image to hand to indicate it's draggable
    private EventHandler<MouseEvent> onClientGridButtonHover = (MouseEvent event) -> {

        SeaBattleButton hoveredBtn = (SeaBattleButton) event.getSource();
        switch (actionType) {
            case BUY: {
                if (actionType.getActionButtonCellType() != null) {

                    // horizontally
                    if (!horizontal_checkBox.isSelected()) {
                        clientGridButtonHoverMark(hoveredBtn,
                                actionType.getActionButtonCellType().getShip().getAreaRow(),
                                actionType.getActionButtonCellType().getShip().getAreaCol(), true);
                        // vertically
                    } else {
                        clientGridButtonHoverMark(hoveredBtn,
                                actionType.getActionButtonCellType().getShip().getAreaCol(),
                                actionType.getActionButtonCellType().getShip().getAreaRow(), true);
                    }

                }
                break;
            }
            case MOVE: {
                hoveredBtn.setCursor(Cursor.HAND);
                break;
            }
            
            default:
                break;
        }

    };

    

    //if the action type is buy the remove mark from the cell and its surroundings based on the sheep thats selected for buying
    //if the move button has been clicked then change the cursor image to default to indicate it's draggable
    private EventHandler<MouseEvent> onClientGridButtonHoverExit = (MouseEvent event) -> {

        SeaBattleButton hoveredBtn = (SeaBattleButton) event.getSource();
        switch (actionType) {
            case BUY:
            {
                if (actionType.getActionButtonCellType() != null) {
                    // horizontally
                    if (!horizontal_checkBox.isSelected()) {
                        clientGridButtonHoverMark(hoveredBtn, actionType.getActionButtonCellType().getShip().getAreaRow(),
                                actionType.getActionButtonCellType().getShip().getAreaCol(), false);
                        // vertically
                    } else {
                        clientGridButtonHoverMark(hoveredBtn, actionType.getActionButtonCellType().getShip().getAreaCol(),
                                actionType.getActionButtonCellType().getShip().getAreaRow(), false);
                    }
    
                }
                break;
            }

            case MOVE:
            {
                hoveredBtn.setCursor(Cursor.DEFAULT);
                break;
            }

            default:
                break;

        }

    };


    //send the attack request only if its the users turn and no other action button has been clicked
    private EventHandler<ActionEvent> onOpponantGridButtonClick = (ActionEvent event) -> {

        if (turn) {
            SeaBattleButton btn = (SeaBattleButton) event.getSource();

            if (actionType == ActionType.ATTACK
                    && actionType.getActionButton().getCellType() != SeaBattleButton.CellType.EMPTY) {

                if (checkAttackPossiblility(btn, actionType.getActionButton())) {
                    attack(btn, actionType.getActionButton());
                }else
                {
                    popupAlert(AlertType.ERROR, "Can't attack!");
                }

                if(targetCells != null)
                {
                    for(SeaBattleButton sbb : targetCells)
                    {

                        sbb.pseudoClassStateChanged(attackClass, false);
                    }
                }
                cancelActionType();

            }

        }

    };


    //mark the opponants grid cells based on the selected attacking ship
    private EventHandler<Event> onOpponantGridButtonMouseHover = (Event event) -> {
        SeaBattleButton hoveredBtn = (SeaBattleButton) event.getSource();

        if (actionType == ActionType.ATTACK
                && actionType.getActionButton().getCellType() != SeaBattleButton.CellType.EMPTY) {

            //check if its possible to attack this cell using the selected ship
            if (checkAttackPossiblility(hoveredBtn, actionType.getActionButton())) {


                //create a list of cell thar are going to be attacked
                targetCells = new ArrayList<>();
                Cell targetCell = hoveredBtn.getCell();

                //create a list of the cells
                switch (actionType.getActionButton().getCellType()) {
                    case SOLDIER:
                        // in case of soldier the only attacked cell is the target cell itself
                        targetCells.add(hoveredBtn);
                        break;
                    case CAVALRY:
                        // in case of a calvary the attacked cells are the cell itself and the other
                        // cell diagonally on the bottom right side of the targetcell
                        targetCells.add(hoveredBtn);
                        targetCells.add(opponantGridArray[targetCell.getCol() + 1][targetCell.getRow() + 1]);
                        break;
                    case FORT:
                        // in case of a fort the attacked cells are a 2 x 2 square with the target cell
                        // on the top right of it
                        targetCells.add(hoveredBtn);
                        targetCells.add(opponantGridArray[targetCell.getCol()][targetCell.getRow() + 1]);
                        targetCells.add(opponantGridArray[targetCell.getCol() + 1][targetCell.getRow()]);
                        targetCells.add(opponantGridArray[targetCell.getCol() + 1][targetCell.getRow() + 1]);
                        break;
                    case HEADQUARTERS:
                        // in case of a fort the attacked cells are a the top left and right and bottom
                        // left and right of the target cell
                        targetCells.add(hoveredBtn);
                        targetCells.add(opponantGridArray[targetCell.getCol() + 1][targetCell.getRow() + 1]);
                        targetCells.add(opponantGridArray[targetCell.getCol() - 1][targetCell.getRow() + 1]);
                        targetCells.add(opponantGridArray[targetCell.getCol() - 1][targetCell.getRow() - 1]);
                        targetCells.add(opponantGridArray[targetCell.getCol() + 1][targetCell.getRow() - 1]);
                        break;
                    default:
                        break;
                }

                //mark these using the attack pseudo class
                for (SeaBattleButton sb : targetCells) {
                    sb.pseudoClassStateChanged(attackClass, true);
                }
            }

        }
    };


    //on exit, remove the attack mark of the cells that we put into the targetCells arraylist
    //then make it null
    private EventHandler<Event> onOpponantGridButtonMouseHoverExit = (Event event) -> {

        if (actionType == ActionType.ATTACK
                && actionType.getActionButton().getCellType() != SeaBattleButton.CellType.EMPTY) {
            if (targetCells != null) {
                for (SeaBattleButton sb : targetCells) {
                    sb.pseudoClassStateChanged(attackClass, false);
                }
                targetCells = null;
            }

        }
    };
    /////// Event Handlers END //////

    //================================================================
}
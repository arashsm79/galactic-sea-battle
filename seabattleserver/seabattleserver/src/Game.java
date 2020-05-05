
/*

    Developed by @ArashSM79

*/
import java.util.ArrayList;
import java.util.Iterator;

import com.google.gson.Gson;
import ship.*;

public class Game {

    //================================================================
    /////// Variable Declaration //////

    private int numberOfPlayers = 0;

    private Player[] players = new Player[2];

    private long gameID = 0;
    
    private int gridSize;

    private Gson gson = new Gson();

    /////// Variable Declaration END//////

    //================================================================

    Game(int gridSize) {

        this.gridSize = gridSize;
    }

    //================================================================

    public void addPlayer(Connection connection, ArrayList<Cell> board) {

        if (players[0] == null) {
            players[0] = new Player(connection, board);
            numberOfPlayers++;

        } else if (players[1] == null) {
            players[1] = new Player(connection, board);
            numberOfPlayers++;

        }
    }



    public void chat(String username, String msg)
    {
        if(msg.length() > 20)
        {
            msg = msg.substring(0, 20);
        }

        for(Player p : players)
        {
            p.getConnection().sendToClient(ASBTP.CHAT);
            p.getConnection().sendToClient(username);
            p.getConnection().sendToClient(msg);
        }
    }



    public Player changeTurn(boolean shouldChangeTurn) {
        
        //check for winner before every turn change
        checkForWinner();
        
        
        Player currentPlayerTurn = null;
        //if the turns should be changeed
        if(shouldChangeTurn)
        {

            for (Player pl : players) {
                if (pl.isTurn()) {
                    pl.setTurn(false);
                } else {
                    pl.setTurn(true);
                    currentPlayerTurn = pl;
                }
            }

        }else
        //else dont change the turn
        {
            for (Player pl : players) {
                if (pl.isTurn()) {
                    pl.setTurn(true);
                    currentPlayerTurn = pl;
                } else {
                    pl.setTurn(false);
                }
            }
        }


        calculateColdDowns();

        return currentPlayerTurn;
    }




    private void checkForWinner() {

        Player winningPlayer = null;
        Player losingPlayer = null;
        for(Player p : players)
        {
            if(p.getNumberOfShipCells() < 1)
            {
                losingPlayer = p;
                break;
            }
        }


        //determine the winning player
        if(losingPlayer != null)
        {
            if (players[0].equals(losingPlayer)) {
                winningPlayer = players[1];
            } else {
                winningPlayer = players[0];
            }

            winningPlayer.getConnection().sendToClient(ASBTP.GAME_END);
            winningPlayer.getConnection().sendToClient(ASBTP.WON);

            losingPlayer.getConnection().sendToClient(ASBTP.GAME_END);
            losingPlayer.getConnection().sendToClient(ASBTP.LOST);

        }
    }



    //closes all the connections removes the game from the game list and closes the threads
    public void endGame(Player disconnectingPlayer)
    {
        disconnectingPlayer.getConnection().getMainServer().removeGame(this);
        disconnectingPlayer.getConnection().setGame(null);

    }


    //if a connection is disconnected this method ends the game and sends a message to the other connection
    public void connectionDisconnected(Connection connection) {

        //determine the other player that hasnt disconnected yet
        Player stillConnedtedPlayer;
        Player disconnectingPlayer;
        // determine which player is moving
        if (players[0].getConnection().equals(connection)) {

            disconnectingPlayer = players[0];
            stillConnedtedPlayer = players[1];
        } else {

            disconnectingPlayer = players[1];
            stillConnedtedPlayer = players[0];
        }



        if(stillConnedtedPlayer != null)
        {
            stillConnedtedPlayer.getConnection().sendToClient(ASBTP.GAME_END);
            stillConnedtedPlayer.getConnection().sendToClient(ASBTP.OPPONENT_DISCONNECTED);
            stillConnedtedPlayer.getConnection().setGame(null);
            stillConnedtedPlayer.getConnection().closeSocket();

        }else
        {
            //there is only one player in the game and player has disconnected
            //then nothing to do here 
            // we simply end the game
        }

        endGame(disconnectingPlayer);

	}



    // when two players have joined a game. Send a signal to both of them indicating
    // that the game is about to start
    public void gameReadySignal() {
        //before signakling the players, proccess their boards and cout their ships
        countPlayersShips();
        
        //send the signal and the username of the other player
        players[0].getConnection().sendToClient(ASBTP.GAME_READY);
        players[0].getConnection().sendToClient(players[1].getUsername());

        players[1].getConnection().sendToClient(ASBTP.GAME_READY);
        players[1].getConnection().sendToClient(players[0].getUsername());
        

        // start the match by giving the first player the turn
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {

            e.printStackTrace();
        }
        players[0].setTurn(true);
        System.out.println("Game with ID: " + this.getGameID() + " started.");
    }

    private void countPlayersShips() {
        for(Player p : players)
        {
            int notDestroyedShips = 0;
            for(Cell c :p.getBoard())
            {
                if(c.getCellType() != ASBTP.EMPTY && !c.isDestroyed())
                {
                    notDestroyedShips += 1;
                }
            }
            p.setNumberOfShipCells(notDestroyedShips);

        }
    }



    public void buy(Connection connection, Point buyingCell, int shipType, String isHorizontal)
    {

        Player buyingPlayer;
        // determine which player is buying
        if (players[0].getConnection().equals(connection)) {
            buyingPlayer = players[0];
        } else {
            buyingPlayer = players[1];
        }

        //check if the player has more than 1 fort
        if(buyingPlayer.getNumberOfForts() < 1)
        {
            buyingPlayer.getConnection().sendToClient(ASBTP.ERROR);
            return;
        }
        //check if the player has enough balance to buy a ship of this type
        if(buyingPlayer.getBalance() < shipType * ASBTP.PRICE_MULTIPLIER)
        {
            buyingPlayer.getConnection().sendToClient(ASBTP.ERROR);
            return;
        }


        Ship buyingShip = getShip(shipType);
        ArrayList<Point> buyingCellsCordinates = new ArrayList<>();
        //check if the place the player ewants to place the ship is empty and nt destroyed

        if(isHorizontal.equals(ASBTP.TRUE))
        {
            if(!checkPlacementWithinLimits(buyingPlayer, buyingCell,  buyingShip.getAreaCol(), buyingShip.getAreaRow(),shipType, buyingCellsCordinates))
            {
                buyingPlayer.getConnection().sendToClient(ASBTP.ERROR);
                return;
            }
        }else
        {
            if(!checkPlacementWithinLimits(buyingPlayer, buyingCell, buyingShip.getAreaRow(), buyingShip.getAreaCol(),shipType, buyingCellsCordinates))
            {
                buyingPlayer.getConnection().sendToClient(ASBTP.ERROR);
                return;
            }
        }

        //remove some money from the buying player
        buyingPlayer.addBalance(-(shipType * ASBTP.PRICE_MULTIPLIER));

        if(shipType == ASBTP.FORT)
        {
            buyingPlayer.addFort(1);
        }

        //add the boughts shipps cell to the number of ships cells of the buying player
        buyingPlayer.addShipCell(buyingCellsCordinates.size());

        buyingPlayer.getConnection().sendToClient(ASBTP.BUY);
        buyingPlayer.getConnection().sendToClient(gson.toJson(buyingCellsCordinates));
        buyingPlayer.getConnection().sendToClient(Integer.toString(shipType));

        changeTurn(true);
    }


    //checks whether the buying conditions are met and the placement area is empty
    private boolean checkPlacementWithinLimits(Player buyingPlayer, Point buyingCellCord, int rowLimit, int colLimit, int shipType, ArrayList<Point> buyingCellsCordinates)
    {
        Cell buyingCell = buyingPlayer.getCell(buyingCellCord.getCol(), buyingCellCord.getRow());
        int clickedBtnRow = buyingCellCord.getRow();
        int clickedBtnCol = buyingCellCord.getCol();

        for(int i = 0; i < rowLimit; i++)
        {
            for(int j = 0; j < colLimit; j++)
            {
                int curCol = clickedBtnCol + j;
                int curRow = clickedBtnRow + i;
                if( curCol >= gridSize || curRow >= gridSize || (buyingPlayer.getCell(curCol, curRow).getCellType() != ASBTP.EMPTY ) 
                || buyingPlayer.getCell(curCol, curRow).isDestroyed() )
                {
                    return false;
                }
            }
        }

        //AFTER THE VALIDATION WE NOW ADD the cells to the buyingCellsCordinates array list and update the players board
        for(int i = 0; i < rowLimit; i++)
        {
            for(int j = 0; j < colLimit; j++)
            {
                Cell currentCell = buyingPlayer.getCell(clickedBtnCol + j, clickedBtnRow + i);
                buyingCellsCordinates.add(new Point(clickedBtnCol + j, clickedBtnRow + i));

                currentCell.setCellType(shipType);
                currentCell.setHeadShipCordinates(new Point(clickedBtnCol, clickedBtnRow));
                
                //add the child ships to the headship
                if(!currentCell.equals(buyingCell))
                {
                    buyingCell.getChildShips().add(new Point(currentCell.getCol(), currentCell.getRow()));
                }
            }
        }

        return true;
    }


    public void move(Connection connection, Point sourcePoint, Point destPoint) {

        Player movingPlayer;
        // determine which player is moving
        if (players[0].getConnection().equals(connection)) {
            movingPlayer = players[0];
        } else {
            movingPlayer = players[1];
        }
        
        Cell sourceCell = movingPlayer.getCell(sourcePoint.getCol(), sourcePoint.getRow());
        Cell destCell = movingPlayer.getCell(destPoint.getCol(), destPoint.getRow());


        ArrayList<Point> destionationPoints = new ArrayList<>();
        if(!checkMovePossibility(movingPlayer, sourceCell, destCell, destionationPoints))
        {
            movingPlayer.getConnection().sendToClient(ASBTP.ERROR);
            return;
        }


        
        //if the move is valid send back the cordinates of the source point and destpoint


        Point sourceCellHeadShipCords = sourceCell.getHeadShipCordinates();
        Cell sourceCellHeadShip = movingPlayer.getCell(sourceCellHeadShipCords.getCol(), sourceCellHeadShipCords.getRow());

        //clear the moved cells
        for(Point p : sourceCellHeadShip.getChildShips())
        {
            Cell movedCell = movingPlayer.getCell(p.getCol() , p.getRow());
            
            movedCell.setCellType(ASBTP.EMPTY);
            movedCell.setHeadShipCordinates(null);
            movedCell.resetChildShips();
            movedCell.setCoolDown(0);
        }

        
        Cell destinationHeadShip = movingPlayer.getCell(destionationPoints.get(0).getCol() , destionationPoints.get(0).getRow());
        destinationHeadShip.resetChildShips();
        
        for(Point p : destionationPoints)
        {
            Cell movedIntoCell = movingPlayer.getCell(p.getCol() , p.getRow());
            movedIntoCell.resetChildShips();
            movedIntoCell.setCellType(sourceCellHeadShip.getCellType());
            movedIntoCell.setCoolDown(sourceCellHeadShip.getCoodDown());
            movedIntoCell.setHeadShipCordinates(destPoint);

            if(!p.equals(destPoint))
            {
                destinationHeadShip.getChildShips().add(p);
            }

        }


        //clear the moved cell headship
        sourceCellHeadShip.setCellType(ASBTP.EMPTY);
        sourceCellHeadShip.setHeadShipCordinates(null);
        sourceCellHeadShip.resetChildShips();
        sourceCellHeadShip.setCoolDown(0);
        

        //send the relust to the client
        movingPlayer.getConnection().sendToClient(ASBTP.MOVE);
        movingPlayer.getConnection().sendToClient(gson.toJson(sourcePoint));
        movingPlayer.getConnection().sendToClient(gson.toJson(destionationPoints));

        changeTurn(true);

    }
    


    //check the moving possibility of the selected cell
    private boolean checkMovePossibility(Player movingPlayer, Cell sourceCell, Cell destCell, ArrayList<Point> destinationPoints)
    {
        //only soldiers and cavalries are allowed to move
        if( !(sourceCell.getCellType() == ASBTP.SOLDIER || sourceCell.getCellType() == ASBTP.CAVALRY))
        {
            return false;
        }

        

        Point sourceCellHeadShipCords = sourceCell.getHeadShipCordinates();
        Cell sourceCellHeadShip = movingPlayer.getCell(sourceCellHeadShipCords.getCol(), sourceCellHeadShipCords.getRow());

        //for soldiers its only 2 blocks
        //for cavalries only 1 block
        int rowDelta = destCell.getRow() - sourceCellHeadShip.getRow();
        int colDelta = destCell.getCol() - sourceCellHeadShip.getCol();

        switch (sourceCell.getCellType()) {
            case ASBTP.SOLDIER:
                if( !((Math.abs(rowDelta) <= 2 && Math.abs(rowDelta) > 0 && Math.abs(colDelta) == 0) ||
                (Math.abs(colDelta) <= 2 && Math.abs(colDelta) > 0 && Math.abs(rowDelta) == 0)) )
                {
                    return false;
                }
                break;
                
            case ASBTP.CAVALRY:
            if( !((Math.abs(rowDelta) == 1  && Math.abs(colDelta) == 0) ||
                (Math.abs(colDelta) == 1 && Math.abs(rowDelta) == 0)) )
                {
                    return false;
                }
                break;
            default:
                return false;
        }

        //put all of the source ships points including the headship into one arraylist
        ArrayList<Point> allSourcePoints = new ArrayList<>();
        allSourcePoints.add(sourceCellHeadShipCords);
        if(sourceCellHeadShip.getChildShips() != null)
        {
            
            allSourcePoints.addAll(sourceCellHeadShip.getChildShips());
        }

        //make sure its not destroyed
        for(Point p: allSourcePoints)
        {
            Cell btn = movingPlayer.getCell(p.getCol(), p.getRow());
            if(btn.isDestroyed())
            {
                return false;
            }
        }
      
        //validates the destionation cells
        for(Point p: allSourcePoints)
        {
            Cell checkingCell = movingPlayer.getCell(p.getCol() + colDelta, p.getRow() + rowDelta);
            Point checkingCellCords = new Point(checkingCell.getCol(), checkingCell.getRow());

            if(checkingCell.getCellType() != ASBTP.EMPTY)
            {
                if(checkingCell.getCellType() == sourceCell.getCellType())
                {          
             
                    boolean isInTheSameShip = false;
                    for(Point sourceChildCord : allSourcePoints)
                    {
                        
                        if(checkingCellCords.equals(sourceChildCord))
                        {
                            isInTheSameShip = true;
                        }
                    }

                    if(!isInTheSameShip) return false;
                }else
                {
                    return false;
                }
                //if the place this cell wants to move is destroyed it cant be moved there
                if(checkingCell.isDestroyed())
                {
                    return false;
                }
            }


            //its a valid point
            destinationPoints.add(new Point(checkingCell.getCol(), checkingCell.getRow()));
            
        }

        return true;


    }


    //get the ship based on the ship type
    private Ship getShip(int shipType)
    {
        switch (shipType) {
            case ASBTP.SOLDIER:
                return new Soldier();
            case ASBTP.CAVALRY:
                
                return new Cavalry();
            case ASBTP.FORT:
                
            return new Fort();
            case ASBTP.HEADQUARTERS:
                
            return new HeadQuarters();
            default:
                return null;
        }
    }




    public void attack(Connection connection, Point attackerCordinates, Point targetCordinates) {

        Player attackerPlayer, targetPlayer;

        // determine which connection is attacking and which one is the target
        if (players[0].getConnection().equals(connection)) {
            attackerPlayer = players[0];
            targetPlayer = players[1];
        } else {
            attackerPlayer = players[1];
            targetPlayer = players[0];
        }

        Cell attackingCell = attackerPlayer.getCell(attackerCordinates.getCol(), attackerCordinates.getRow());
        Cell targetCell = targetPlayer.getCell(targetCordinates.getCol(), targetCordinates.getRow());

        // make sure the attacking unit isnt on cold down
        // this attacker cell is on cold down and cannot attack send an error to
        //checks and makes sure the ships and all of its cells are not completetly destroyed
        if(!isAttackPossbile(attackerPlayer, targetPlayer, attackingCell, targetCell))
        {
            attackerPlayer.getConnection().sendToClient(ASBTP.ERROR);
            return;

        }
        
        
        // compose a list of all the cells that are going to be attacked based on the
        // type of the attacker
        ArrayList<Cell> targetCells = composeListOfAttackedCells(attackingCell.getCellType(), targetPlayer, targetCell);
        

        // add cold down for the attacking cell based on the type of the cell
        addColdDown(attackerPlayer, attackingCell);

        // destroy the attacked cells
        for (Cell c : targetCells) {
            c.setDestroyed(true);
        }

        // send this list of attacked cells to both the attacker player and the target
        // player to update their boards
        
        String json = gson.toJson(targetCells);
        
        attackerPlayer.getConnection().sendToClient(ASBTP.ATTACK_OK);
        attackerPlayer.getConnection().sendToClient(json);
        
        targetPlayer.getConnection().sendToClient(ASBTP.ATTACK);
        targetPlayer.getConnection().sendToClient(json);
        
        //if the attacker player has managed to destroy a ship then they wont lose their turn
        //and if they have managed to destroy a ship add money to their balance
        boolean hasDestroyedNonEmptyCell = false;
        for(Cell c : targetCells)
        {
            if(c.getCellType() != ASBTP.EMPTY)
            {
                addBalance(attackerPlayer, targetPlayer, c);
                targetPlayer.removeShipCell(1);
                hasDestroyedNonEmptyCell = true;
            }
        }

        changeTurn(!hasDestroyedNonEmptyCell);

    }




    private void addBalance(Player attackerPlayer, Player targetPlayer, Cell destroyedCell) {

        //destroying every kind of cells yields the minimum number of 5 coins
        //destroying a cavalry, fort, headquarters yields 10, 15, 20 respectively
        attackerPlayer.addBalance(5);

        //checks to see whther the whole ship has been destroyed or not
        Cell destroyedCellHeadShip = targetPlayer.getCell(destroyedCell.getHeadShipCordinates().getCol(), destroyedCell.getHeadShipCordinates().getRow());
        
        //just a flag not no add the bounty for this whole ship twice or more
        if(destroyedCellHeadShip.getCoodDown() > -1)
        {
            
            boolean hasDestroyedEntireShip = true;
            for(Point p : destroyedCellHeadShip.getChildShips())
            {
                Cell childCell = targetPlayer.getCell(p.getCol(), p.getRow());
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
                switch (destroyedCell.getCellType()) {
                    case ASBTP.CAVALRY:
                        attackerPlayer.addBalance(10);
                        
                        break;
                    case ASBTP.FORT:
                        attackerPlayer.addBalance(15);
                        
                        break;
                    case ASBTP.HEADQUARTERS:
                        attackerPlayer.addBalance(20);
                        
                    default:
                        break;
                }

                //just a flag not no add the bounty for this whole ship twice or more
                for(Point p : destroyedCellHeadShip.getChildShips())
                {
                    Cell childCell = targetPlayer.getCell(p.getCol(), p.getRow());
          
                    childCell.setCoolDown(-1);
                
                }
                destroyedCellHeadShip.setCoolDown(-1);

                //if the ship that has completely been destroyed is a fort then decrease the number of target player forts
                if(destroyedCellHeadShip.getCellType() == ASBTP.FORT)
                {
                    targetPlayer.addFort(-1);
                }
            }

        }


    }


    //substract one from all the non empty cells
    private void calculateColdDowns()
    {
        for(Player pl : players)
        {
            for(Cell c : pl.getBoard())
            {
                if(c.getCellType() != ASBTP.EMPTY && c.getCoodDown() > 0)
                {
                    c.setCoolDown(c.getCoodDown() - 1);
                }
            }
        }
    }

   


    //add cold down for the attacking cell and its head ship and the headship's children
    private void addColdDown(Player attackerPlayer, Cell attackingCell) {

        Cell attackerHeadShip = attackerPlayer.getCell(attackingCell.getHeadShipCordinates().getCol(), attackingCell.getHeadShipCordinates().getRow());
        attackerHeadShip.setCoolDown(attackerHeadShip.getCellType() * 2);

        for(Point p : attackerHeadShip.getChildShips())
        {
            Cell childCell = attackerPlayer.getCell(p.getCol(), p.getRow());
            childCell.setCoolDown(childCell.getCellType() * 2);

        }
    }


    //creates a list of attacked cells based on the attacking ships type
    private ArrayList<Cell> composeListOfAttackedCells(int attackingCellType, Player targetPlayer, Cell targetCell)
    {
        ArrayList<Cell> targetCells = new ArrayList<>();
        switch (attackingCellType) {
            case ASBTP.SOLDIER:
                // in case of soldier the only attacked cell is the target cell itself
                targetCells.add(targetCell);
                break;
            case ASBTP.CAVALRY:
                // in case of a calvary the attacked cells are the cell itself and the other
                // cell diagonally on the bottom right side of the targetcell
                targetCells.add(targetCell);
                targetCells.add(targetPlayer.getCell(targetCell.getCol() + 1, targetCell.getRow() + 1));
                break;
            case ASBTP.FORT:
                // in case of a fort the attacked cells are a 2 x 2 square with the target cell
                // on the top right of it
                targetCells.add(targetCell);
                targetCells.add(targetPlayer.getCell(targetCell.getCol(), targetCell.getRow() + 1));
                targetCells.add(targetPlayer.getCell(targetCell.getCol() + 1, targetCell.getRow()));
                targetCells.add(targetPlayer.getCell(targetCell.getCol() + 1, targetCell.getRow() + 1));
                break;
            case ASBTP.HEADQUARTERS:
                // in case of a fort the attacked cells are a the top left and right and bottom
                // left and right of the target cell
                targetCells.add(targetCell);
                targetCells.add(targetPlayer.getCell(targetCell.getCol() + 1, targetCell.getRow() + 1));
                targetCells.add(targetPlayer.getCell(targetCell.getCol() - 1, targetCell.getRow() + 1));
                targetCells.add(targetPlayer.getCell(targetCell.getCol() - 1, targetCell.getRow() - 1));
                targetCells.add(targetPlayer.getCell(targetCell.getCol() + 1, targetCell.getRow() - 1));
                break;
            default:
                break;
        }

        //remove the cell from the list if it had already been destroyed
        Iterator<Cell> itr = targetCells.iterator();
        while(itr.hasNext())
        {
            Cell c = itr.next();
            if(c.isDestroyed())
            {
                itr.remove();
            }
        }
        
        return targetCells;
    }


    //makes sure the attack is valid
    private boolean isAttackPossbile(Player attackerPlayer, Player targetPlayer, Cell attackingCell, Cell targetCell) {

        //make sure the attack is not out of the range of the size of the grid
        int col = targetCell.getCol();
        int row = targetCell.getRow();
        switch (attackingCell.getCellType()) {
            case ASBTP.SOLDIER:
                    break;

            case ASBTP.CAVALRY:
                if(row + 1 < gridSize && col + 1 < gridSize)
                {
                    break;
                }else
                {
                    return false;
                }

            case ASBTP.FORT:
                if(row + 1 < gridSize && col + 1 < gridSize)
                {
                    break;
                }else
                {
                    return false;
                }

            case ASBTP.HEADQUARTERS:
            if(row + 1 < gridSize && 
                col + 1 < gridSize &&
                row -1 >= 0 && 
                col - 1 >= 0)
            {
                break;
            }else
            {
                return false;
            }
                
            default:
                return false;
        }

        Cell attackerHeadShip = attackerPlayer.getCell(attackingCell.getHeadShipCordinates().getCol(), attackingCell.getHeadShipCordinates().getRow());
        //if the cold down of the head ship is -1 then it means that the whole hips has been desrroyed and it cannot attack
        //check for cool down of the attacking cell and its head ship and the headship's children
        //and make sure none of them is destroyed

        if(attackerHeadShip.getCoodDown() != 0)
        {
            return false;
        }

        for(Point p : attackerHeadShip.getChildShips())
        {
            Cell childCell = attackerPlayer.getCell(p.getCol(), p.getRow());
            if(childCell.getCoodDown() != 0)
            {
                return false;
            }

        }
        
        return true;
    }

    /**
     * @return the numberOfPlayers
     */
    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }/**
     * @param numberOfPlayers the numberOfPlayers to set
     */
    public void setNumberOfPlayers(int numberOfPlayers) {
        this.numberOfPlayers = numberOfPlayers;
    }
    /**
     * @return the gridSize
     */
    public int getGridSize() {
        return gridSize;
    }
    /**
     * @param gridSize the gridSize to set
     */
    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
    }


	/**
     * @param gameID the gameID to set
     */
    public void setGameID(long gameID) {
        this.gameID = gameID;
    }
    /**
     * @return the gameID
     */
    public long getGameID() {
        return gameID;
    }

}
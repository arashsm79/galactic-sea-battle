
/*

    Developed by @ArashSM79

*/
import java.util.ArrayList;

public class Player {


    //================================================================
    /////// Variable Declaration //////

    private Connection connection;

    private int gridSize;

    private int numberOfForts = 2;

    private String username;

    private ArrayList<Cell> board;

    private boolean turn = false;

    private int balance = 0;

    //if this reaches to zero; the player loses the game
    private int numberOfShipCells;

    /////// Variable Declaration END//////
    //================================================================


    Player(Connection connection, ArrayList<Cell> board)
    {
        this.connection = connection;
        this.gridSize = connection.getGridSize();
        this.board = board;
        this.username = connection.getUsername();
    }

    //================================================================

    //after each call send the state of turn to the client
    public void setTurn(boolean turn) {
        this.turn = turn;
        connection.sendToClient(ASBTP.CLIENT_TURN);
        if(turn)
        {
            connection.sendToClient(ASBTP.TRUE);
        } else
        {
            connection.sendToClient(ASBTP.FALSE);
        }

        //send the balance to the player
        connection.sendToClient(ASBTP.BALANCE);
        connection.sendToClient(Integer.toString(this.balance));
    }

    /**
     * @param numberOfShipCells the numberOfShipCells to set
     */
    public void setNumberOfShipCells(int numberOfShipCells) {
        this.numberOfShipCells = numberOfShipCells;
    }

    public void removeShipCell(int n)
    {
        this.numberOfShipCells -= n;
    }

    public void addShipCell(int n)
    {
        this.numberOfShipCells += n;
    }
    /**
     * @return the numberOfShipCells
     */
    public int getNumberOfShipCells() {
        return numberOfShipCells;
    }
    /**
     * @param turn the turn to set
     */
    

    /**
     * @return the balance
     */
    public int getBalance() {
        return balance;
    }
    /**
     * @param balance the balance to set
     */
    public void addBalance(int balance) {
        this.balance += balance;
    }
    /**
     * @return the turn
     */
    public boolean isTurn() {
        return turn;
    }
    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }
    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }
    /**
     * @param board the board to set
     */
    public void setBoard(ArrayList<Cell> board) {
        this.board = board;
    }/**
     * @param connection the connection to set
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
    }/**
     * @return the board
     */
    public ArrayList<Cell> getBoard() {
        return board;
    }/**
     * @return the connection
     */
    public Connection getConnection() {
        return connection;
    }

    public Cell getCell(int col, int row)
    {
        return this.board.get( col * gridSize + row);
    }

	public void addFort(int i) {
        this.numberOfForts += i;
    }
    /**
     * @return the numberOfForts
     */
    public int getNumberOfForts() {
        return numberOfForts;
    }
}
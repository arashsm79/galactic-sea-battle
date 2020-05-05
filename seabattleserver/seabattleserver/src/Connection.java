/*

    Developed by @ArashSM79

*/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Connection extends Thread {

    //================================================================

    /////// Variable Declaration //////

    public static int MAX_PLAYERS = 2;

    private Socket socket;
    private SeaBattleServer mainServer;

    private int gridSize;
    private String username;
    
    Gson gson = new Gson();

    // the game this connection is associated with
    private Game game;

    private BufferedReader in;
    private PrintWriter out;

    /////// Variable Declaration END //////


    //================================================================

    Connection(SeaBattleServer mainServer, Socket socket) {

        this.socket = socket;
        this.mainServer = mainServer;

    }

    //================================================================


    @Override
    public void run() {

        // get grid size username and the board
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            String input;

            // connection's main while loop
            // terminates if input == null (client disconnects)
            while ((input = in.readLine()) != null) {

                
                if (input.matches("^[A-Z1-9]{1,20}$")) {
                    switch (input) {

                        // join procedure
                        case ASBTP.JOIN:

                            // read the user name
                            input = in.readLine();
                            this.username = input;
                            System.out.println("Player: " + input + " has connected.");

                            // read the gridsize
                            input = in.readLine();
                            this.gridSize = Integer.parseInt(input);

                            // read the board in json
                            input = in.readLine();
                            // convert the received json board to an array of cells;
                            // ############
                            // parse json
                            ArrayList<Cell> joinBoard = jsonToBoard(input);

                            // join or create a game. if the game reaches enough players it sends a ready
                            // signal to both players
                            this.game = joinOrCreateGame(joinBoard);
                            break;

                        case ASBTP.ATTACK:

                            // read the attacker cordinates
                            input = in.readLine();
                            Point attackerCordinates = gson.fromJson(input, Point.class);

                            // read the cell that is going to be attacked if the power of the attacker is
                            // more than 1 we will also attack the surrounding cells
                            input = in.readLine();
                            Point targetCordinates = gson.fromJson(input, Point.class);

                            // this function checks whether the attacker is not on cold down and its a valid
                            // attack
                            // if its a valid attack it will send the resulf of the attack back to the
                            // connections
                            game.attack(this, attackerCordinates, targetCordinates);
                            break;

                        case ASBTP.BUY:

                            input = in.readLine();
                            Point buyingCell = gson.fromJson(input, Point.class);
                
                            String isHorizontal = in.readLine();

                            input = in.readLine();
                            int shipType = Integer.parseInt(input);

                            game.buy(this, buyingCell, shipType, isHorizontal);

                            break;

                        case ASBTP.MOVE: {

                            input = in.readLine();
                            Point sourceCell = gson.fromJson(input, Point.class);

                            input = in.readLine();
                            Point destCell = gson.fromJson(input, Point.class);

                            game.move(this, sourceCell, destCell);

                            break;
                        }

                        case ASBTP.LOSE_TURN:
                        {
                            game.changeTurn(true);
                            break;
                        }


                        case ASBTP.CHAT:
                        {
                            input = in.readLine();

                            game.chat(this.username, input);
                            break;
                        }

                        

                        default:

                            break;

                    }

                } else {
                    sendToClient(ASBTP.ERROR);
                }

            }

        } catch (Exception e) {


            closeSocket();

            // end of thread
            return;
        }

        
        closeSocket();
        // end of thread
        return;

    }


    //================================================================


    public void closeSocket() {

        try {
            if(this.socket != null && !this.socket.isClosed())
            {  
                System.out.println("Player: " + this.username + " has disconnected");
                if (game != null) {
                    game.connectionDisconnected(this);
                }
                this.socket.close();                
            }
        } catch (IOException e) {

            System.out.println("Something went wrong while closing the socket");
        }
    }
    public synchronized void sendToClient(String msg)
    {
        out.println(msg);
    }


    ArrayList<Cell> jsonToBoard(String json)
    {
        Type collectionType = new TypeToken<ArrayList<Cell>>(){}.getType();
        ArrayList<Cell> board = gson.fromJson(json, collectionType);
        return board;
    }


    //creates or joins a game
    Game joinOrCreateGame(ArrayList<Cell> board)
    {
        Game g = mainServer.searchQueuedGames(this.gridSize);

        if(g != null)
        {
            g.addPlayer(this, board);
            System.out.println("Player: " + this.username + " joined game with ID: " + g.getGameID());
            
            //if the game has reached two players simply remove it from the queued games
            if(g.getNumberOfPlayers() >= MAX_PLAYERS)
            {
                //sends a signal to all the players in the game that the game is about to start
                g.gameReadySignal();
                mainServer.removeQueuedGame(g);
            }
            return g;

        }

        //if no games with this gridsize were found create a new game
        Game newGame = new Game(this.gridSize);

        newGame.setGridSize(this.gridSize);
        newGame.addPlayer(this, board);
        newGame.setGameID(this.getId());
        this.game = newGame;
        mainServer.addQueuedGame(newGame);

        System.out.println("Game with ID: " + newGame.getGameID() + " created.");
        System.out.println("Player: " + this.username + " joined game with ID: " + newGame.getGameID());


        return newGame;
        
    }

    /**
     * @return the mainServer
     */
    public SeaBattleServer getMainServer() {
        return mainServer;
    }
    /**
     * @return the gridSize
     */
    public int getGridSize() {
        return gridSize;
    }/**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param game the game to set
     */
    public void setGame(Game game) {
        this.game = game;
    }

   
}
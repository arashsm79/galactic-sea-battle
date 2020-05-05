
/*

    Developed by @ArashSM79

*/
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;



public class SeaBattleServer implements Runnable {

    //================================================================

    public static int SERVER_PORT;

    private ServerSocket serverSocket;

    //two array lists that keep the track of all games and queued games
    private ArrayList<Game> games = new ArrayList<>();
    private ArrayList<Game> queuedGames = new ArrayList<>();

    //================================================================


    public static void main(String[] args) {
        SeaBattleServer sv = new SeaBattleServer();
        new Thread(sv).start();
    }

    //================================================================

    SeaBattleServer() {

        
    }
    //================================================================

    @Override
    public void run() {
        try {
            System.out.println("Arash's Galactic Sea Batlle Server");
            System.out.println("Type in the available port you want the server to run on: ( 0 for a random available port )");
            try {
                Scanner sc = new Scanner(System.in);
                SERVER_PORT = sc.nextInt();
                sc.close();
                
            } catch (Exception e) {

                System.out.println("An error occured.");

            }
            
            serverSocket = new ServerSocket(SERVER_PORT);
            SERVER_PORT = serverSocket.getLocalPort();

            System.out.println("Server running on: " + serverSocket.getLocalSocketAddress());
            System.out.println("Port: " + SERVER_PORT);

            while (true) {

                Socket socket = serverSocket.accept();

                //create a new connection
                new Connection(this, socket).start();
                
            }

        } catch (Exception e) {
            System.out.println("Something went wrong while creating the serversocket");

        } finally {
            try {
                if (serverSocket != null & !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {

                System.out.println("Something went wrong while closing the serversocket");

            }
       }

    }

    /**
     * @return the queuedGames
     */
    public ArrayList<Game> getQueuedGames() {
        return queuedGames;
    }

    public void addGame(Game game)
    {
        synchronized(this.games)
        {
            this.games.add(game);
        }
    }

    public void removeGame(Game toBeRemovedGame)
    {
        //first remove it from the queued games
        removeQueuedGame(toBeRemovedGame);

        synchronized(this.games)
        {

            Iterator<Game> itr = this.games.iterator();
            while(itr.hasNext())
            {
                Game g = itr.next();
                if(g.equals(toBeRemovedGame))
                {
                    System.out.println("Game with ID: " + g.getGameID() + " ended.");

                    itr.remove();
                }
            }
        }
    }

    public void addQueuedGame(Game game)
    {
        addGame(game);
        synchronized(this.queuedGames)
        {
            this.queuedGames.add(game);
        }
    }
    public void removeQueuedGame(Game toBeRemovedGame)
    {
        synchronized(this.queuedGames)
        {
            Iterator<Game> itr = this.queuedGames.iterator();
            while(itr.hasNext())
            {
                Game g = itr.next();
                if(g.equals(toBeRemovedGame))
                {
                    itr.remove();
                }
            }
            
               
        }
    }

    public Game searchQueuedGames(int gridSize)
    {
        synchronized(this.queuedGames)
        {
            for(Game g : this.getQueuedGames())
            {
                if(g.getGridSize() ==  gridSize)
                {
                    return g;
                }

            }
            return null;
        }
    }
}
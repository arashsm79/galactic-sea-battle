
/*

    Developed by @ArashSM79

*/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import javafx.concurrent.Service;
import javafx.concurrent.Task;


//The service that handles the requests and response communications to/from the server

public class Connection extends Service<HashMap<String, String>> {

    private String serverIP;
    private int serverPort;
    private Socket socket;

    private BufferedReader in;
    private PrintWriter out;

    //a reference to the startup controller and game controller for future callbacks
    private GameController gameController;
    private StartUpController startUpController;

    private String username;
    private int gridSize;
    private String boardJson;

    Connection(String ip, int port, String username, int gridSize, String boardJson, StartUpController startUpController) {

        this.username = username;
        this.serverIP = ip;
        this.serverPort = port;
        this.gridSize = gridSize;
        this.boardJson = boardJson;
        this.startUpController = startUpController;
    }

    //pretty straight forward
    //listens for responses from t he server and callsback to the controller
    
    @Override
    protected Task<HashMap<String, String>> createTask() {

        return new Task<HashMap<String, String>>() {

            @Override
            protected HashMap<String, String> call() throws Exception {

                HashMap<String, String> msg = new HashMap<>();
                try 
                {
                        

                    socket = new Socket(serverIP, serverPort);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    // joining a game procedure
                    out.println(ASBTP.JOIN);
                    out.println(username);
                    out.println(gridSize);
                    out.println(boardJson);

                    // read the response code
                    String responseCode = "";
                    String body = "";

                    //make sure the put messages are not null
                    msg.put(ASBTP.ACTION_CODE, responseCode);
                    msg.put(ASBTP.BODY, body);

                    while ((responseCode = in.readLine()) != null) {
                        switch (responseCode) {

                            case ASBTP.GAME_READY:
                                Thread.sleep(1000);
                                msg.put(ASBTP.ACTION_CODE, ASBTP.GAME_READY);
                                body = in.readLine();
                                msg.put(ASBTP.BODY, body);
                                startUpController.handleServerMessage(msg);
                                break;

                            case ASBTP.CLIENT_TURN: {
                                msg.put(ASBTP.ACTION_CODE, ASBTP.CLIENT_TURN);
                                body = in.readLine();
                                msg.put(ASBTP.BODY, body);
                                gameController.handleServerMessage(msg);
                                break;
                            }

                            case ASBTP.ATTACK: {
                                msg.put(ASBTP.ACTION_CODE, ASBTP.ATTACK);
                                body = in.readLine();
                                msg.put(ASBTP.BODY, body);
                                gameController.handleServerMessage(msg);
                                break;
                            }

                            case ASBTP.ATTACK_OK: {
                                msg.put(ASBTP.ACTION_CODE, ASBTP.ATTACK_OK);
                                body = in.readLine();
                                msg.put(ASBTP.BODY, body);
                                gameController.handleServerMessage(msg);
                                break;
                            }

                            case ASBTP.BALANCE: {
                                msg.put(ASBTP.ACTION_CODE, ASBTP.BALANCE);
                                body = in.readLine();
                                msg.put(ASBTP.BODY, body);
                                gameController.handleServerMessage(msg);
                                break;
                            }

                            case ASBTP.BUY: {

                                msg.put(ASBTP.ACTION_CODE, ASBTP.BUY);
                                body = in.readLine();
                                body += System.getProperty("line.separator");
                                body += in.readLine();
                                msg.put(ASBTP.BODY, body);
                                gameController.handleServerMessage(msg);
                                break;
                            }

                            case ASBTP.MOVE: {
                                msg.put(ASBTP.ACTION_CODE, ASBTP.MOVE);
                                body = in.readLine();
                                body += System.getProperty("line.separator");
                                body += in.readLine();
                                msg.put(ASBTP.BODY, body);
                                gameController.handleServerMessage(msg);

                                break;
                            }

                            case ASBTP.GAME_END: {
                                msg.put(ASBTP.ACTION_CODE, ASBTP.GAME_END);
                                body = in.readLine();
                                msg.put(ASBTP.BODY, body);
                                gameController.handleServerMessage(msg);
                                break;
                            }

                            case ASBTP.ERROR: {
                                msg.put(ASBTP.ACTION_CODE, ASBTP.ERROR);
                                gameController.handleServerMessage(msg);
                                break;
                            }

                            case ASBTP.CHAT: {
                                msg.put(ASBTP.ACTION_CODE, ASBTP.CHAT);
                                body = in.readLine();
                                body += System.getProperty("line.separator");
                                body += in.readLine();
                                msg.put(ASBTP.BODY, body);
                                gameController.handleServerMessage(msg);

                                break;
                            }

                            default:
                                break;
                        }

                        // System.out.println("Code received form server: " + responseCode);
                        // System.out.println("Body received form server: " + body);
                        if (isCancelled()) {
                            break;
                        }

                    }

                } catch (Exception e) {

                    msg.put(ASBTP.ACTION_CODE, ASBTP.GAME_END);
                    msg.put(ASBTP.BODY, ASBTP.CLIENT_DISCONNECTED);
                    gameController.handleServerMessage(msg);
                    closeSocket();
                    return null;

                }
                // if the server shuts down the game ends
                msg.put(ASBTP.ACTION_CODE, ASBTP.GAME_END);
                msg.put(ASBTP.BODY, ASBTP.CLIENT_DISCONNECTED);
                gameController.handleServerMessage(msg);
                closeSocket();
                return null;
            }

        };

    }

    public void closeSocket() {

        try {
            if(this.socket != null && !this.socket.isClosed())
            {  
                this.socket.close();                
                this.cancel();
            }
        } catch (IOException e) {

            System.out.println("Something went wrong while closing the socket");
        }
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param gameController the gameController to set
     */
    public void setGameController(GameController gameController) {
        this.gameController = gameController;
    }

    /**
     * @return the serverIP
     */
    public String getServerIP() {
        return serverIP;
    }

    /**
     * @return the serverPort
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * @param serverIP the serverIP to set
     */
    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    /**
     * @param serverPort the serverPort to set
     */
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void sendToServer(String msg) {
        out.println(msg);
    }

    public void disconnectFromServer() {
        try 
        {
            if(this.socket != null && !this.socket.isClosed())
            {  
                this.socket.close();
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

}
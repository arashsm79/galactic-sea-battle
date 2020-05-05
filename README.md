# Galactic Sea Battle - Battleship with multiplayer server written in Java using Javafx and Sockets

Sea Battle - Battleship with multiplayer game server written in Java using Javafx and Sockets

A variation of the classic Sea Battle game with some new additions like having different kinds of ships with different powers, moving ships accross the board and buying new ships.

The server is also written in java and uses scokets. it can handle multiple players and games at the same time.

The game is written in java and uses Javafx for it's GUI.

The game logic is also implemented in the server, meaning every request is checked and validated.

The server uses a HTTP like protocol to communicate with the clients. You can develop a client side appplication in another language and use the server as your game logic.

I've tried my best to leave a comment on most parts of the code.

# Dependencies:

-Gson

-Javafx

# Compiling


If you're facing gtk problems(it's a known bug with some versions of javafx), change to gtk 2. just add the following Vm argument after the java command:

java -Djdk.gtk.version=2 --module...

Here's an example on how to compile and run and javafx client:

javac --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml -d ./bin -cp src/lib/*.jar src/**/*.java

java -java --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml -cp bin:src/lib/gson-2.8.6.jar App
"PATH_TO_FX" : "/path/to/your/javafx-sdk-*-/lib"

Java Server:

javac -g src/**/*.java -d ./bin

java -cp "lib/gson-2.8.6.jar:bin" App


# Screenshots:
 ![Alt text](/screenshots/1.png?raw=true "Startup")
 ![Alt text](/screenshots/2.png?raw=true "Game")
 ![Alt text](/screenshots/3.png?raw=true "Game")
 ![Alt text](/screenshots/4.png?raw=true "Game")
 ![Alt text](/screenshots/5.png?raw=true "Game")
 ![Alt text](/screenshots/0.png?raw=true "Server")


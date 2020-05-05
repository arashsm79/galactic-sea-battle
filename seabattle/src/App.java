
/*

    Developed by @ArashSM79

*/
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.*;
import javafx.stage.*;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception{

        FXMLLoader loader = new FXMLLoader(getClass().getResource("resources/fxml/startUp.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, Color.LIGHTYELLOW);
        
        stage.setTitle("Galactic Sea Battle");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {

        launch();
    }

}
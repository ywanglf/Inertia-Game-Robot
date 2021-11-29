package hk.ust.cse.comp3021.pa3;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.jetbrains.annotations.NotNull;

/**
 * The main class handling command-line options parsing.
 */
public class Main extends Application {

    /**
     * Main entry-point.
     *
     * @param args Arguments from the command-line.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(@NotNull Stage primaryStage) {
        var game = new InertiaFxGame(primaryStage);
        game.run();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.exit(0);
            }
        });


    }
}

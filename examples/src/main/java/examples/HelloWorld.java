package examples;

import ch.bailu.gtk.gio.ApplicationFlags;
import ch.bailu.gtk.gtk.Application;
import ch.bailu.gtk.gtk.ApplicationWindow;
import ch.bailu.gtk.gtk.Button;

public class HelloWorld {

    public static void main (String[] argv) throws InterruptedException {
        // Create a new application

        System.loadLibrary("glue");

        var app = new Application("com.example.GtkApplication",
                                   ApplicationFlags.FLAGS_NONE);

        app.onActivate(() -> {
            // Create a new window
            var window = new ApplicationWindow(app);

            // Create a new button
            var button = Button.newWithLabelButton("Hello, World!");

            // When the button is clicked, close the window
            button.onClicked(() -> window.close());

            window.add(button);
            window.present();

        });

        app.run(argv.length, argv);
    }
}

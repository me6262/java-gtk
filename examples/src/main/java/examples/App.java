package examples;

import java.io.File;

import ch.bailu.gtk.GTK;
import ch.bailu.gtk.gio.ApplicationFlags;
import ch.bailu.gtk.gtk.Application;
import ch.bailu.gtk.gtk.ApplicationWindow;
import ch.bailu.gtk.gtk.Box;
import ch.bailu.gtk.gtk.Button;
import ch.bailu.gtk.gtk.Label;
import ch.bailu.gtk.gtk.Orientation;
import ch.bailu.gtk.gtk.ScrolledWindow;
import ch.bailu.gtk.gtk.Window;
import ch.bailu.gtk.type.Str;
import ch.bailu.gtk.type.Strs;
import examples.gtk4_demo.AppLauncher;
import examples.gtk4_demo.HeaderBarSample;
import examples.gtk4_demo.HelloWorldBoxed;
import examples.gtk4_demo.LinksSample;
import examples.gtk4_demo.PangoTextMask;
import examples.gtk4_demo.Picker;
import examples.gtk4_demo.Pixbufs;
import examples.gtk4_tutorial.BuilderExample;
import examples.gtk4_tutorial.CustomDrawing;
import examples.gtk4_tutorial.ExampleApplication;
import examples.test.MultiThreadingCallbacks;

public class App {

    public final static Str ID = new Str("org.gtk.example");
    private final static Str TITLE = new Str("java-gtk demo");
    public final static int WIDTH = 720 / 2;
    public final static int HEIGHT = 1440 / 2;

    public static void main (String[] args)  {

        var app = new Application(ID, ApplicationFlags.FLAGS_NONE);
        app.onActivate(()->{
            var demoList = new Box(Orientation.VERTICAL, 0);

            var scrolled = new ScrolledWindow();
            scrolled.setChild(demoList);

            var window = new ApplicationWindow(app);
            window.setTitle(TITLE);
            window.setDefaultSize(WIDTH, HEIGHT);
            window.setChild(scrolled);

            addSample(demoList, window, new ImageBridge());
            addSample(demoList, window, new AppLauncher());
            addSample(demoList, window, new Pixbufs());
            addSample(demoList, window, new Picker());
            addSample(demoList, window, new HeaderBarSample(app));
            addSample(demoList, window, new LinksSample());
            addSample(demoList, window, new PangoTextMask());
            addSample(demoList, window, new HelloWorldBoxed());
            addSample(demoList, window, new CustomDrawing());
            addSample(demoList, window, new BuilderExample(app));
            addSample(demoList, window, new HugeList());
            addSample(demoList, window, new ExampleApplication(app));
            addSample(demoList, window, new GlibSettings());
            addSample(demoList, window, new MultiThreadingCallbacks());


            window.show();
        });

        System.exit(app.run(args.length, new Strs(args)));
    }

    private static void addSample(Box demoList, Window window, DemoInterface demo) {
        var entry = new Box(Orientation.HORIZONTAL,0);
        var label = new Label(demo.getTitle());
        var button = new Button();

        entry.setMarginEnd(10);
        entry.setMarginTop(10);
        entry.setMarginStart(10);
        entry.setMarginBottom(10);
        label.setHexpand(GTK.TRUE);
        label.setXalign(0);
        button.setLabel(new Str("Run"));
        button.onClicked(() -> runDemo(demo, window));
        entry.append(label);
        entry.append(button);
        demoList.append(entry);
    }


    private static void runDemo(DemoInterface demo, Window parent) {
        Window demoWindow =  demo.runDemo();
        demoWindow.setDisplay(parent.getDisplay());
        demoWindow.setTitle(demo.getTitle());
        demoWindow.setTransientFor(parent);
        demoWindow.setModal(GTK.TRUE);
        demoWindow.show();
    }

    public static File path(String path) {
        if (new File("examples").exists()) {
            return new File(path);
        }
        return new File("..", path);
    }
}

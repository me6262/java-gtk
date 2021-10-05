package examples.gtk3_demo;

import ch.bailu.gtk.GTK;
import ch.bailu.gtk.cairo.Context;
import ch.bailu.gtk.cairo.Pattern;
import ch.bailu.gtk.gio.ApplicationFlags;
import ch.bailu.gtk.gtk.Application;
import ch.bailu.gtk.gtk.ApplicationWindow;
import ch.bailu.gtk.gtk.Box;
import ch.bailu.gtk.gtk.DrawingArea;
import ch.bailu.gtk.gtk.GtkConstants;
import ch.bailu.gtk.gtk.Label;
import ch.bailu.gtk.gtk.Orientation;
import ch.bailu.gtk.gtk.Widget;
import ch.bailu.gtk.pango.AttrList;
import ch.bailu.gtk.pango.AttrShape;
import ch.bailu.gtk.pango.Attribute;
import ch.bailu.gtk.pango.FontMetrics;
import ch.bailu.gtk.pango.Language;
import ch.bailu.gtk.pango.Layout;
import ch.bailu.gtk.pango.Pango;
import ch.bailu.gtk.pango.PangoConstants;
import ch.bailu.gtk.pango.Rectangle;
import ch.bailu.gtk.pangocairo.Pangocairo;
import ch.bailu.gtk.type.Bytes;
import ch.bailu.gtk.type.Dbl;
import ch.bailu.gtk.type.Int;
import ch.bailu.gtk.type.Pointer;
import ch.bailu.gtk.type.Str;
import ch.bailu.gtk.type.Strs;

public class PangoRotatedText {
    private final static int RADIUS  = 150;
    private final static int N_WORDS = 5;
    private final static String FONT = "Serif 18";

    private final static String HEART = "♥";
    private final static String TEXT = "I ♥ GTK+";

    private final static Str SHEART = new Str(HEART);
    private final static Str STEXT = new Str(TEXT);
    private final Int width = new Int();
    private final Int height = new Int();

    public PangoRotatedText(String[] argv) {

        var app = new Application(new Str("org.gtk.example"), ApplicationFlags.FLAGS_NONE);
        app.onActivate(() -> doRotatedText(new ApplicationWindow(app)));
        app.run(argv.length, new Strs(argv));

    }

    final Dbl x = new Dbl();
    final Dbl y = new Dbl();

    private void fancyShapeRenderer(Context cr, AttrShape attr, int doPath) {
        cr.save();
        cr.getCurrentPoint(x, y);
        cr.translate(x.get(), y.get());

        cr.scale(attr.getFieldInkRect().getFieldWidth() / PangoConstants.SCALE, attr.getFieldInkRect().getFieldHeight() / PangoConstants.SCALE);

        cr.moveTo(.5, .0);
        cr.lineTo(.9, -.4);
        cr.curveTo(1.1, -.8, .5, -.9, .5, -.5);
        cr.curveTo(.5, -.9, -.1, -.8, .1, -.4);
        cr.closePath();

        if (doPath == GTK.FALSE) {
            cr.setSourceRgb(1., 0., 0.);
            cr.fill();
        }
        cr.restore();
    }

    AttrList createFancyAttrListForLayout(Layout layout) {
        /* Get font metrics and prepare fancy shape size */
        AttrList attrs = new AttrList();
        FontMetrics metrics = layout.getContext().getMetrics(layout.getFontDescription(), null);;
        int ascent =  metrics.getAscent();
        metrics.unref();

        Rectangle logical_rect = new Rectangle();
        logical_rect.setFieldX(0);
        logical_rect.setFieldWidth(ascent);
        logical_rect.setFieldY(-ascent);
        logical_rect.setFieldHeight(ascent);

        Rectangle ink_rect = new Rectangle();
        ink_rect.setFieldX(0);
        ink_rect.setFieldWidth(ascent);
        ink_rect.setFieldY(-ascent);
        ink_rect.setFieldHeight(ascent);

        /* Set fancy shape attributes for all hearts */
        int foundAt = TEXT.indexOf(HEART.charAt(0));

        while (foundAt > -1) {
            Attribute attr =  AttrShape.newWithData(ink_rect, logical_rect, null , null, null);
            attr.setFieldStartIndex(foundAt);
            attr.setFieldEndIndex(foundAt+3);
            attrs.insert(attr);
            foundAt = TEXT.indexOf(HEART.charAt(0), foundAt+1);
        }
        return attrs;
    }


    private int rotatedTextDraw(Widget widget, Context cr) {
        /* Create a cairo context and set up a transformation matrix so that the user
         * space coordinates for the centered square where we draw are [-RADIUS, RADIUS],
         * [-RADIUS, RADIUS].
         * We first center, then change the scale. */

        int width = widget.getAllocatedWidth();
        int height = widget.getAllocatedHeight();

        double device_radius = Math.min(width, height) / 2d;

        cr.translate(device_radius + (width - 2 * device_radius) / 2,
                device_radius + (height - 2 * device_radius) / 2);

        cr.scale(device_radius / RADIUS, device_radius / RADIUS);

        /* Create and a subtle gradient source and use it. */
        Pattern pattern = new Pattern(-RADIUS, -RADIUS, RADIUS, RADIUS);
        pattern.addColorStopRgb(0., .5, .0, .0);
        pattern.addColorStopRgb(1., .0, .0, .5);
        cr.setSource(pattern);

        /* Create a PangoContext and set up our shape renderer */
        ch.bailu.gtk.pango.Context context = widget.createPangoContext();
        //Pangocairo.contextSetShapeRenderer(context, (cr1, attr, do_path, data) -> fancyShapeRenderer(cr1, attr, do_path), null, null);

        /* Create a PangoLayout, set the text, font, and attributes */
        Layout layout = new Layout(context);
        layout.setText(STEXT, -1);
        var desc = Pango.fontDescriptionFromString(new Str(FONT));
        layout.setFontDescription(desc);
        var attrs = createFancyAttrListForLayout(layout);
        //layout.setAttributes(attrs);

        /* Draw the layout N_WORDS times in a circle */
        for (int i = 0; i< N_WORDS; i++) {

            /* Inform Pango to re-layout the text with the new transformation matrix */
            Pangocairo.updateLayout(cr, layout);
            layout.getPixelSize(this.width, this.height);

            cr.moveTo(-this.width.get() / 2, -RADIUS * .9);
            System.out.println(i);

            Pangocairo.showLayout(cr, layout);

            /* Rotate for the next turn */
            cr.rotate(Math.PI*2 / N_WORDS);
        }

        /* free the objects we created */
        desc.free();
        layout.unref();
        context.unref();
        pattern.destroy();
        attrs.unref();

        return GTK.FALSE;
    }


    private void doRotatedText(ApplicationWindow window) {
        window.setTitle(new Str("Rotated Text"));
        window.setDefaultSize(4 * RADIUS, 2 * RADIUS);

        var box = new Box(Orientation.HORIZONTAL, 0);
        box.setHomogeneous(GTK.TRUE);
        window.add(box);

        /* Add a drawing area */
        var drawing_area = new DrawingArea();
        box.add(drawing_area);
        drawing_area.getStyleContext().addClass(new Str(GtkConstants.STYLE_CLASS_VIEW));
        drawing_area.onDraw(cr -> rotatedTextDraw(drawing_area, cr));

        /* And a label */
        var label = new Label(STEXT);
        box.add(label);
        label.setAngle(45d);

        /* Set up fancy stuff on the label */
        var layout = label.getLayout();


        Pangocairo.contextSetShapeRenderer(
               layout.getContext(),
                (cr, attr, do_path, data) -> fancyShapeRenderer(cr, attr, do_path),
                null,
                null);

        var attrs = createFancyAttrListForLayout(layout);
        label.setAttributes(attrs);
        attrs.unref();

        window.showAll();
    }

}

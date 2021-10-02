package ch.bailu.gtk.wrapper;

public class Dbls extends Ary {

    private final static int BYTES = 4;


    public Dbls(long pointer, int length) {
        super(pointer, BYTES, length);
    }

    public Dbls(long pointer) {
        super(pointer, BYTES, 0);
    }


    public Dbls(double[] doubles) {
        super(createDoubleArray(doubles), BYTES, doubles.length);
    }

    private static long createDoubleArray(double[] doubles) {
        if (doubles.length > 0) {
            return ImpDbls.createDoubleArray(doubles);
        }
        return 0;
    }

    public void setAt(int index, double value) {
        throwIfNull();
        checkLimit(index);
        ImpDbls.setAt(getCPointer(), index, value);
    }


    public double getAt(int index) {
        throwIfNull();
        checkLimit(index);
        return ImpDbls.getAt(getCPointer(), index);
    }

    public Dbls(float[] floats) {
        super(createDoubleArray(floats), BYTES, floats.length);
    }

    private static long createDoubleArray(float[] floats) {
        if (floats.length > 0) {
            return ImpDbls.createDoubleArrayFromFloats(floats);
        }
        return 0;
    }
}

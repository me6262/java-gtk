package ch.bailu.gtk.lib.handler;

import java.util.HashMap;

import ch.bailu.gtk.lib.util.SizeLog;
import ch.bailu.gtk.type.Int;
import ch.bailu.gtk.type.Pointer;

public class ClassHandler {
    private final static HashMap<String, ClassHandler> map = new HashMap<>();
    private static final SizeLog sizeLog = new SizeLog(ClassHandler.class.getSimpleName());


    private final String name;
    private final Pointer instance;

    public ClassHandler(String key) {
        instance = new Int();
        name = key;
    }

    public Pointer getInstance() {
        return instance;
    }

    public static ClassHandler get(String key) {
        synchronized (map) {
            if (map.containsKey(key)) {
                return map.get(key);
            } else {
                var result = new ClassHandler(key);
                map.put(key, result);
                sizeLog.log(map.size());
                return result;
            }
        }
    }
}

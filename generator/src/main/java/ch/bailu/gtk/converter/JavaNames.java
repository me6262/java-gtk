package ch.bailu.gtk.converter;

public class JavaNames {

    public static String toJavaMethodName(String name) {
        if (name.length()<3) {
            return name;
        }

        StringBuilder result = new StringBuilder();

        String[] names = name.split("_");

        result.append(names[0]);

        for (int i = 1; i<names.length; i++) {
            toFirstUpper(result, names[i]);
        }
        return fixToken(result.toString());
    }

    private static void toFirstUpper(StringBuilder result, String s) {
        if (s.length() > 0) {
            result.append(Character.toUpperCase(s.charAt(0)));

            if (s.length() > 1) {
                result.append(s.substring(1));
            }
        } else {
            result.append(s);
        }
    }


    public static String toJavaSignalName(String name) {
        StringBuilder result = new StringBuilder();
        result.append("on");

        String[] names = name.split("-");

        for (int i = 0; i<names.length; i++) {
            toFirstUpper(result, names[i]);
        }
        return fixToken(result.toString());

    }

    public static String fixToken(String token) {
        return ReservedTokenTable.instance().convert(token);
    }
}

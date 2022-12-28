package Utilities;

public class Rand {

    public static int randInt(int min, int max) {
        return (int) (Math.random() * (max - min + 1) + min);
    }

    public static double randDouble(double min, double max) {
        return (Math.random() * (max - min + 1) + min);
    }

    public static String randString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }

    public static String randString(int min, int max) {
        return randString(randInt(min, max));
    }
    
}

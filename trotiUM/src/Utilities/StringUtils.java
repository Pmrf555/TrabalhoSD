package Utilities;

public class StringUtils {
    public static void main(String[] args) {
      // Test the padding function with some examples
      System.out.println(padString("hello", 8));
      System.out.println(padString("hello", 10));
      System.out.println(padString("hello", 3));
      System.out.println(padString("hello", 15, '*'));
    }

    public static boolean isNumeric(String strNum) {
      if (strNum == null) {
          return false;
      }
      try {
          Double.parseDouble(strNum);
      } catch (NumberFormatException nfe) {
          return false;
      }
      return true;
  }
    
    public static String padString(String str, int targetLength) {
      // Padding character is a space by default
      return padString(str, targetLength, ' ');
    }
    
    public static String padString(String str, int targetLength, char paddingChar) {
      // Calculate the amount of padding needed
      int padding = targetLength - str.length();
      
      // If the string is already at least as long as the target length, return it as is
      if (padding <= 0) {
        return str;
      }
      
      // Divide the padding equally on both sides of the string
      int leftPadding = padding / 2;
      int rightPadding = padding - leftPadding;
      
      // Create a string builder to hold the padded string
      StringBuilder paddedString = new StringBuilder();
      
      // Add the left padding to the string builder
      for (int i = 0; i < leftPadding; i++) {
        paddedString.append(paddingChar);
      }
      
      // Add the original string to the string builder
      paddedString.append(str);
      
      // Add the right padding to the string builder
      for (int i = 0; i < rightPadding; i++) {
        paddedString.append(paddingChar);
      }
      
      // Return the padded string
      return paddedString.toString();
    }
  }
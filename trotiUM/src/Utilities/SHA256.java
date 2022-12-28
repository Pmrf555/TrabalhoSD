package Utilities;
/** SHA256.java
 * This class is used to encrypt a string using the SHA-256 algorithm.
 * 
 * @author: Miguel Gomes
 * @version: 1.0
 */


import java.security.MessageDigest;

public class SHA256 {

    public static String getSha256(String value) {
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(value.getBytes());
            return bytesToHex(md.digest());
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }
    private static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    public static void main(String[] args) {
        System.out.println(getSha256("Password123"));
    }
}

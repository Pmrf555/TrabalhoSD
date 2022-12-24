import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import Utilities.Pair;
import Utilities.Rand;
import Utilities.StringPad;

public class User implements Serializable {
    private static AtomicInteger idCounter = new AtomicInteger(0);
    private static final long serialVersionUID = 1L;
    private Integer id;
    private String username;
    private String password;
    private Pair<Integer, Integer> position;
    private Double balance;
    private List<Scooter.Invoice> invoices;

    public static class InvalidUser extends Exception {
        public InvalidUser(String message){
            super(message);
        }
    
    }
    public static class InvalidPassword extends Exception {
        public InvalidPassword(String message){
            super(message);
        }
    
    }

    public User(Integer id, String username, String password, Pair<Integer, Integer> position, Double balance, List<Scooter.Invoice> invoices){
        this.id = id;
        this.username = username;
        this.password = password;
        this.position = position;
        this.balance = balance;
        this.invoices = invoices;
    }

    public User(String username, String password, Pair<Integer, Integer> position, Double balance){
        this(idCounter.getAndIncrement(), username, password, position, balance,new ArrayList<Scooter.Invoice>());
    }

    public User(String username, String password, Double balance){
        this(username, password, new Pair<Integer, Integer>(Rand.randInt(0, 19), Rand.randInt(0, 19)), balance);
    }

    public User(String username, String password){
        this(username, password, 0.0);
    }

    public User(){
        this("","");
    }
    
    public Integer getId(){
        return this.id;
    }

    public void setId(Integer id){
        this.id = id;
    }

    public String getUsername(){
        return this.username;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public String getPassword(){
        return this.password;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public Pair<Integer, Integer> getPosition(){
        return this.position;
    }

    public void setPosition(Pair<Integer, Integer> position){
        this.position = position;
    }

    public Double getBalance(){
        return this.balance;
    }

    public void setBalance(Double balance){
        this.balance = balance;
    }

    public List<Scooter.Invoice> getInvoices(){
        return this.invoices;
    }

    public void setInvoices(List<Scooter.Invoice> invoices){
        this.invoices = invoices;
    }

    public String toString(){
        return "USER(Username: " + this.username + " | Password: " + this.password + " | Position: " + this.position.toString() + " | Balance: " + this.balance + ")";
    }
    

    public boolean checkCredentials(User user){
        return this.username.equals(user.getUsername()) && this.password.equals(user.getPassword());
    }

    public boolean update(User user){
        if (this.checkCredentials(user)){
            this.position = user.getPosition();
            this.balance = user.getBalance();
            return true;
        }
        return false;
    }

    public String server_log_center(){
        return StringPad.padString(new String("User{" + this.username + ", Pos(" + this.position.getL()+ ", " + this.position.getR() + "), Balance(" + this.balance + ")}"), 50);
    }

    public String serverLogRight(){
        return new String("User{" + this.username + ", Pos(" + this.position.getL()+ ", " + this.position.getR() + "), Balance(" + this.balance + ")}");
    }


    public boolean equals(Object obj){
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof User)) return false;
        User user = (User) obj;
        return this.username.equals(user.getUsername()) && this.password.equals(user.getPassword()) && this.position.equals(user.getPosition()) && this.balance.equals(user.getBalance());
    }

    public static void main(String[] args) {
        User user = new User("Migs", "1234", 10.00);
        System.out.println(user);
    }
}

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import Utilities.Pair;
import Utilities.Rand;
import Utilities.StringUtils;

public class User implements Serializable {
    private static AtomicInteger idCounter = new AtomicInteger(0);
    private static final long serialVersionUID = 1L;
    private Integer id;
    private String username;
    private String password;
    private Pair<Integer, Integer> position;
    private Boolean subscribed;
    private Double balance;
    private Integer rewards;
    private Integer discountUses = 0;
    private Double discount = 1.0;
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

    public User(Integer id, String username, String password, Pair<Integer, Integer> position, Boolean subscribed, Double balance, Integer rewards, List<Scooter.Invoice> invoices){
        this.id = id;
        this.username = username;
        this.password = password;
        this.position = position;
        this.subscribed = subscribed;
        this.balance = balance;
        this.rewards = rewards;
        this.discountUses = Rand.randInt(0, 2 + invoices.size()/2);
        this.invoices = invoices;

    }

    public User(String username, String password, Pair<Integer, Integer> position, Double balance){
        this(idCounter.getAndIncrement(), username, password, position, false, balance,0,new ArrayList<Scooter.Invoice>());
    }

    public User(String username, String password, Double balance){
        this(username, password, new Pair<Integer, Integer>(Rand.randInt(0, Server.GRID_DIMENSION-1), Rand.randInt(0, Server.GRID_DIMENSION)), balance);
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

    public Boolean Subscribed(){
        return this.subscribed;
    }

    public void setSubscribed(Boolean subscribed){
        this.subscribed = subscribed;
    }

    public Double getBalance(){
        return this.balance;
    }

    public void setBalance(Double balance){
        this.balance = balance;
    }

    public Integer getRewards(){
        return this.rewards;
    }

    public void setRewards(Integer rewards){
        this.rewards = rewards;
    }

    public Integer getDiscountUses(){
        return this.discountUses;
    }

    public void setDiscountUses(Integer discountUses){
        this.discountUses = discountUses;
    }

    public Double getDiscount(){
        if (this.discountUses > 0){
            return this.discount;
        }
        else
            this.calculateRewards();
        return this.discount;
    }

    public void setDiscount(Double discount){
        this.discount = discount;
    }

    public void calculateRewards(){
        if (this.rewards > 100){
            this.rewards = 90;
        }
        if (this.rewards < 0){
            this.rewards = 0;
        }
        Integer reward = Rand.randInt(this.rewards*-1, this.rewards);
        if (Math.abs(reward) < this.rewards - Rand.randInt(0, this.rewards )){
            this.discountUses = Rand.randInt(0, 2 + this.invoices.size()/2);
            System.out.println(Math.abs(reward));
            this.discount = 1 - Double.parseDouble(new String( "0." + Math.abs(reward)));
        }
        this.discount = 1.0;
    }

    public List<Scooter.Invoice> getInvoices(){
        return this.invoices;
    }

    public void setInvoices(List<Scooter.Invoice> invoices){
        this.invoices = invoices;
    }

    public void addInvoice(Scooter.Invoice invoice){
        if (this.invoices == null){
            this.invoices = new ArrayList<Scooter.Invoice>();
        }
        this.invoices.add(invoice);
    }

    public void removeInvoice(Scooter.Invoice invoice){
        if (this.invoices == null){
            return;
        }
        this.invoices.remove(invoice);
    }

    public void addBalance(Double balance){
        this.balance += balance;
    }

    public void removeBalance(Double balance){
        this.balance -= balance;
    }

    public void addReward(Integer reward){
        this.rewards += reward;
        this.calculateRewards();
    }

    public void removeReward(Integer reward){
        this.rewards -= reward;
    }

    public String toString(){
        return "USER(Username: " + this.username + " | Balance: " + this.balance + " | Trips: " + this.invoices.size() + ")";
    }
    

    public boolean checkCredentials(User user){
        return this.username.equals(user.getUsername()) && this.password.equals(user.getPassword());
    }

    public boolean update(User user){
        if (this.checkCredentials(user)){
            this.balance = user.getBalance();
            this.rewards = user.getRewards();
            this.subscribed = user.Subscribed();
            this.discountUses = user.getDiscountUses();
            this.discount = user.getDiscount();

            for (Scooter.Invoice invoice : this.invoices){
                if (invoice.getStatus().equals("PENDING")){
                    if(this.balance>=invoice.getPrice()){
                        this.balance -= invoice.getPrice();
                        invoice.setStatus("PAID");
                    }
                }
            }
            return true;
        }
        return false;
    }

    public String server_log_center(){
        return StringUtils.padString(new String("User{" + this.username + ", Pos(" + this.position.getL()+ ", " + this.position.getR() + "), Balance(" + this.balance + ")}"), 50);
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


    public static Double calculateRewards(Integer rewards){
        if (rewards > 100){
            rewards = 90;
        }
        if (rewards < 0){
            rewards = 0;
        }
        Integer reward = Rand.randInt(rewards*-1, rewards);
        if (Math.abs(reward) < rewards - Rand.randInt(0, rewards )){
            System.out.println(Math.abs(reward));
            return 1 - Double.parseDouble(new String( "0." + Math.abs(reward)));
        }
        return 1.0;
    }
    public static void main(String[] args) {
        System.out.println(User.calculateRewards(5) + "\n");
        System.out.println(User.calculateRewards(12) + "\n");
        System.out.println(User.calculateRewards(50) + "\n");
        System.out.println(User.calculateRewards(1) + "\n");
        System.out.println(User.calculateRewards(100) + "\n");
        System.out.println(User.calculateRewards(1000) + "\n");
    }
}

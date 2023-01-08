import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDate;
import java.io.Serializable;

import Utilities.Matrix;
import Utilities.Pair;
import Utilities.SHA256;

public class Scooter implements Serializable {
    public static final Double PRICE_PER_KM = 0.1;
    public static final Double PRICE_PER_MIN = 0.01;
    private static final long serialVersionUID = 1L;
    private static AtomicInteger idCounter = new AtomicInteger(0);
    private Integer id;
    private String reservationCode;
    private Long start;
    private Integer x = 0;
    private Integer y = 0;
    private Boolean beingUsed = false; // false if free, true if being used
    private String lastUser = "";

    public static class InvalidScooter extends Exception {
        public InvalidScooter() {
            // create an exception with a default message
            super("Invalid scooter");
        }
        public InvalidScooter(String message) {
            // create an exception with a specified message
            super(message);
        }
    }

    public static class InvalidReservationCode extends Exception{
        public InvalidReservationCode(String message){
            super(message);
        }
    }
    

    public static class Invoice implements Serializable {
        private static enum Status {NULL,PENDING, PAID, CANCELLED};

        private static final long serialVersionUID = 1L;
        private static AtomicInteger idCounter = new AtomicInteger(0);
        private Integer id;
        private Integer userId;
        private Integer scooterId;
        private double price;
        private LocalDate date;
        private Pair<Integer, Integer> from;
        private Pair<Integer, Integer> to;
        private Status status = Status.NULL;



        public Invoice(Integer id, Integer userId, Integer scooterId, double price, LocalDate date, Pair<Integer, Integer> from, Pair<Integer, Integer> to, String status){
            if (id>idCounter.get()) idCounter.set(id);
            this.id = id;
            this.userId = userId;
            this.scooterId = scooterId;
            this.price = price;
            this.date = date;
            this.from = from;
            this.to = to;
            this.status = Status.valueOf(status);
        }

        public Invoice(Integer userId, Integer scooterId, double price, LocalDate date, Pair<Integer, Integer> from, Pair<Integer, Integer> to, String status){
            this(idCounter.getAndIncrement(), userId, scooterId, price, date, from, to, status);
        }

        public Invoice(Integer user, Integer scooter, double Price, Pair<Integer, Integer> from, Pair<Integer, Integer> to, String status){
            this(user, scooter, Price, LocalDate.now(), from, to, status);
        }

        public Integer getId(){
            return this.id;
        }

        public void setId(Integer id){
            this.id = id;
        }

        public Integer getUser(){
            return this.userId;
        }

        public void setUser(Integer userId){
            this.userId = userId;
        }

        public Integer getScooter(){
            return this.scooterId;
        }

        public void setScooter(Integer scooterId){
            this.scooterId = scooterId;
        }

        public double getPrice(){
            return this.price;
        }

        public void setPrice(double price){
            this.price = price;
        }

        public LocalDate getDate(){
            return this.date;
        }

        public void setDate(LocalDate date){
            this.date = date;
        }

        public Pair<Integer, Integer> From(){
            return this.from;
        }

        public void setFrom(Pair<Integer, Integer> from){
            this.from = from;
        }

        public Pair<Integer, Integer> To(){
            return this.to;
        }

        public void setTo(Pair<Integer, Integer> to){
            this.to = to;
        }

        public String getStatus(){
            return this.status.toString();
        }

        public void setStatus(String status){
            this.status = Status.valueOf(status);
        }

        public String toString(){
            return "Invoice " + this.id + ":\n\tUser: " + this.userId + "\n\tScooter: " + this.scooterId + "\n\tPrice: " + this.price + "\n\tDate: " + this.date + "\n\tFrom: " + this.from + "\n\tTo: " + this.to + "\n\tStatus: " + this.status;
        }
    }

    public Scooter(Integer x, Integer y){
        this();
        this.x = x;
        this.y = y;
    }

    public Scooter(Integer id, String reservationCode){
        this.id = id;
        this.reservationCode = reservationCode;
    }

    public Scooter(String reservationCode){
        this(idCounter.getAndIncrement(), reservationCode);
    }

    public Scooter(){
        this(Scooter.generateRandomReservationCode());
    }

    public Integer getId(){
        return this.id;
    }

    public void setId(Integer id){
        this.id = id;
    }

    public String getReservationCode(){
        return this.reservationCode;
    }

    public void setReservationCode(String reservationCode){
        this.reservationCode = reservationCode;
    }

    public Pair<Integer,Integer> getPos(){
        return new Pair<Integer,Integer>(this.x, this.y);
    }

    public void setPos(Pair<Integer,Integer> pos){
        this.x = pos.getL();
        this.y = pos.getR();
    }

    public void setPos(Integer x, Integer y){
        this.x = x;
        this.y = y;
    }

    public Boolean isBeingUsed(){
        return this.beingUsed;
    }

    public void setBeingUsed(Boolean beingUsed){
        this.beingUsed = beingUsed;
    }

    public String getLastUser(){
        return this.lastUser;
    }

    public void setLastUser(String lastUser){
        this.lastUser = lastUser;
    }

    public static String generateRandomReservationCode(){
        Integer leftLimit = 48; // numeral '0'
        Integer rightLimit = 122; // letter 'z'
        Random random = new Random();
        Integer targetStringLength = random.ints(1, 5).findFirst().getAsInt();
    
        String generatedString = random.ints(leftLimit, rightLimit + 1).filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97)).limit(targetStringLength).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();

        return SHA256.getSha256(generatedString);
    }

    public String aquire(){
        this.beingUsed = true;
        this.start = System.currentTimeMillis();
        return this.reservationCode;
    }

    public boolean release(String reservationCode) throws Scooter.InvalidReservationCode{
        if (this.reservationCode.equals(reservationCode)){
            this.beingUsed = false;
            return true;
        }
        else if (!this.reservationCode.equals(reservationCode)){
            throw new Scooter.InvalidReservationCode("The code "+ reservationCode +" is not correct.");
        }
        return false;
    }

    public Scooter.Invoice generateInvoice(User user, Pair<Integer, Integer> to){
        Scooter.Invoice.Status paid = Scooter.Invoice.Status.NULL;
        Double price = Scooter.PRICE_PER_KM * Matrix.manhattan(this.x, this.y, to.getL(), to.getR());
        float took = ((System.currentTimeMillis()) - this.start)/1000;
        price = (Scooter.PRICE_PER_MIN * took) + (price);
        if(user.getDiscountUses() > 0 && user.Subscribed())
            price *= user.getDiscount();
            user.setDiscountUses(user.getDiscountUses() - 1);
        if (user.getBalance() < price)
            paid = Scooter.Invoice.Status.PENDING;
        else{
            user.removeBalance(price);
            paid = Scooter.Invoice.Status.PAID;
        }
        return new Scooter.Invoice(user.getId(), this.id, price, new Pair<Integer,Integer>(this.x,this.y), to, paid.toString());
    }

    public String toString(){
        return "Scooter{" + this.id + ", " + this.reservationCode + ", " + this.x + ", " + this.y + ", " + this.beingUsed + ", " + this.lastUser + "}";
    }

    public static void main(String[] args) {
        System.out.println(Scooter.generateRandomReservationCode());

        for(int i = 4; i >= 0; i--){
            System.out.println(i-Server.RADIUS);
        }
    }
    
}

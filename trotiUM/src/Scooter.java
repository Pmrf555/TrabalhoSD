import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;
import java.time.LocalDate;
import java.io.Serializable;

import Utilities.Pair;
import Utilities.SHA256;

public class Scooter implements Serializable {
    public static final String NULL_POSITION = "NULL NULL NULL NULL NULL ";
    public static final String Scooter_Icon = "._\\.";
    private static final long serialVersionUID = 1L;
    private static AtomicInteger idCounter = new AtomicInteger(0);
    private Integer id;
    private String reservationCode;
    private Integer x = 0;
    private Integer y = 0;
    private Boolean beingUsed = false; // false if free, true if being used
    private Lock lock= new ReentrantLock();


    public class Invoice implements Serializable {
        private static final long serialVersionUID = 1L;
        private static AtomicInteger idCounter = new AtomicInteger(0);
        private Integer id;
        private Integer userId;
        private Integer scooterId;
        private double price;
        private LocalDate date;
        private Pair<Integer, Integer> from;
        private Pair<Integer, Integer> to;

        public Invoice(Integer id, Integer userId, Integer scooterId, double price, LocalDate date, Pair<Integer, Integer> from, Pair<Integer, Integer> to){
            if (id>idCounter.get()) idCounter.set(id);
            this.id = id;
            this.userId = userId;
            this.scooterId = scooterId;
            this.price = price;
            this.date = date;
            this.from = from;
            this.to = to;
        }

        public Invoice(Integer userId, Integer scooterId, double price, LocalDate date, Pair<Integer, Integer> from, Pair<Integer, Integer> to){
            this(idCounter.getAndIncrement(), userId, scooterId, price, date, from, to);
        }

        public Invoice(Integer user, Integer scooter, double Price, Pair<Integer, Integer> from, Pair<Integer, Integer> to){
            this(user, scooter, Price, LocalDate.now(), from, to);
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

        public Pair<Integer, Integer> getFrom(){
            return this.from;
        }

        public void setFrom(Pair<Integer, Integer> from){
            this.from = from;
        }

        public Pair<Integer, Integer> getTo(){
            return this.to;
        }

        public void setTo(Pair<Integer, Integer> to){
            this.to = to;
        }

        public String toString(){
            return "Invoice " + this.id + ":\n\tUser: " + this.userId + "\n\tScooter: " + this.scooterId + "\n\tPrice: " + this.price + "\n\tDate: " + this.date + "\n\tFrom: " + this.from + "\n\tTo: " + this.to;
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

    public static String generateRandomReservationCode(){
        Integer leftLimit = 48; // numeral '0'
        Integer rightLimit = 122; // letter 'z'
        Random random = new Random();
        Integer targetStringLength = random.ints(1, 5).findFirst().getAsInt();
    
        String generatedString = random.ints(leftLimit, rightLimit + 1).filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97)).limit(targetStringLength).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();

        return SHA256.getSha256(generatedString);
    }

    public void aquire(){
        this.beingUsed = true;
        this.lock.lock();
    }

    public void release(){
        this.beingUsed = false;
        this.lock.unlock();
    }

    public String toString(){
        return "Scooter";
    }

    public static void main(String[] args) {
        System.out.println(Scooter.generateRandomReservationCode());
    }
    
}

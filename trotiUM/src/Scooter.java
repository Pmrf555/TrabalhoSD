import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;

import Utilities.Pair;
import Utilities.SHA256;

public class Scooter {
    private static AtomicInteger idCounter = new AtomicInteger(0);
    private Integer id;
    private String reservationCode;
    private Integer x = 0;
    private Integer y = 0;
    private Boolean beingUsed = false; // false if free, true if being used
    private Lock lock= new ReentrantLock();

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

    public static void main(String[] args) {
        System.out.println(Scooter.generateRandomReservationCode());
    }
    
}

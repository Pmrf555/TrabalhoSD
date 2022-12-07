import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class trotinete {
    int codReserva;
    int estado = 0; //0 se estiver livre e 1 se estiver a ser usada.
    Lock trotinete= new ReentrantLock();
    public void reserva(){
        estado = 1;
        this.trotinete.lock();
    }

    public void larga(){
        estado = 0;
        this.trotinete.unlock();
    }
}

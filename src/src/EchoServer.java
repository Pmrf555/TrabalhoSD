import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


class NewServer extends Thread{
    Socket socket;
    EstadoProjeto estadoProjeto;

    NewServer(Socket s,EstadoProjeto r){
        this.socket = s;
        this.estadoProjeto = r;
    }
    public void run() {
        try {
            int dadosCorretos = 0;
            BufferedReader in = null;

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            PrintWriter out = new PrintWriter(socket.getOutputStream());
            String username,password;

            while (dadosCorretos == 0) {
                out.println("Insira o seu username:\n");out.flush();
                username = in.readLine();
                out.println("Insira a sua password:\n");out.flush();
                password = in.readLine();

                if(estadoProjeto.dadosInicioSessao.get(username).equals(password)) dadosCorretos = 1;
            }

            String line;
            while ((line = in.readLine()) != null) {
                out.flush();
            }
            socket.shutdownOutput();
            socket.shutdownInput();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
class EstadoProjeto {
    int N = 20;
    int D = 2;
    Lock l = new ReentrantLock();
    int mapa[][] = new int[N][N];
    HashMap<String,String> dadosInicioSessao = new HashMap<>();

    int existemTrotinetesProximas(int x1,int y1,int x2,int y2){
        int result;
        if((Math.abs(x1-x2) + Math.abs(y1-y2))<=D) result = 1;
        else result = 0;
        return result;
    }

    void listaLocaisComTrotinetes(int posx,int posy){}
    void listaRecompensas(){}

}
public class EchoServer {
    public static void main(String[] args) {
        try {
            ServerSocket ss = new ServerSocket(12345);
            EstadoProjeto r = new EstadoProjeto();
            while (true) {
                Thread t = new Thread(new NewServer(ss.accept(),r));
                t.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
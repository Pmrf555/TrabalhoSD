import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class calcRecompensas extends Thread{
    EstadoProjeto estadoProjeto;

    calcRecompensas(EstadoProjeto estadoProjeto){
        this.estadoProjeto = estadoProjeto;
    }

    public void run(){
    }

}

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
            int tipo;
            Thread recompensas = new Thread(calcRecompensas(this.estadoProjeto));
            while (dadosCorretos == 0) {
                out.println("1-iniciar sessão\n2-criar conta");
                tipo=in.read();
                if (tipo==1){ // caso de iniciar sessão
                    out.println("Insira o seu username:\n");out.flush();
                    username = in.readLine();
                    out.println("Insira a sua password:\n");out.flush();
                    password = in.readLine();
                    if(estadoProjeto.dadosInicioSessao.get(username).equals(password)) dadosCorretos = 1;
                    else{
                        System.out.println("Dados incorretos.");
                }
                }
                else{ // caso de criar conta
                    out.println("Insira o seu username:\n");out.flush();
                    username = in.readLine();
                    out.println("Insira a sua password:\n");out.flush();
                    password = in.readLine();
                    estadoProjeto.dadosInicioSessao.put(username, password);
                }
                
            }

            String line;
            out.write("Indique o que pretende fazer:\n1- Listar trotinetes livre.\n2- Listar recompensas próximas.\n3-Reservar trotinete.\n4- Estacionar trotinete.\n5- Notificar quando tiver recompensas próximas.");
            out.flush();
            
            while ((line = in.readLine()) != null) {
                switch (line){
                    case "1":
                        estadoProjeto.existemTrotinetesProximas(1, 1, out); break;
                    case "2":
                        estadoProjeto.listaRecompensas(1, 1, out); break; 
                }
                

                System.out.println(line);
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
    int codReserva = 0;
    ArrayList<trotinete> listaTrotinetes;
    Lock lock = new ReentrantLock();
    ArrayList<trotinete> mapa[][]; // cada posição tem a lista de trotinetes que se encontra lá
    int mapaRecompensas[][] = new int[N][N]; // o inteiro em cada posição representa o valor da recompensa
    HashMap<String,String> dadosInicioSessao = new HashMap<>();

    for(int i=0;i<10;i++){
        listaTrotinetes.get(i).codReserva = i;
    }
    void existemTrotinetesProximas(int x1,int y1,PrintWriter out){//envia para o cliente as posições das trotinetes
        int number1 = 0,number2 = 0;
        try{
            lock.lock();
        
        while(number1<N){
            while(number2<N){
                if((Math.abs(x1-number1 + Math.abs(y1-number2))<=D) && mapa[number1][number2].size() >= 1) {
                    out.write(number1);out.flush();
                    out.write(number2);out.flush();
                    
                }
                number2++;
            }
            number2=0;
            number1++;
        }
        }finally{
            lock.unlock();
        }
    }

    void listaRecompensas(int x1,int y1,PrintWriter out){//envia para o cliente as posições das recompensas e o seu valor
        int number1 = 0,number2 = 0;
        try{
            lock.lock();
        
        while(number1<N){
            while(number2<N){
                if((Math.abs(x1-number1 + Math.abs(y1-number2))<=D) && mapaRecompensas[number1][number2] >= 1) {
                    out.write(number1);out.flush();
                    out.write(number2);out.flush();
                    out.write(mapaRecompensas[number1][number2]);out.flush();
                }
                number2++;
            }
            number2=0;
            number1++;
        }
    }finally{
        lock.unlock();
    }
    }
    int[] trotineteMaisProx(int x,int y){//retorna uma lista com o x e o y da trotinete mais próxima
        int number1 = 0,number2 = 0,flag = 0;
        int[] res = new int[2];
        while(number1<N && flag == 1){
            while(number2<N && flag == 1){
                if((Math.abs(x-number1 + Math.abs(y-number2))<=D) && mapa[number1][number2].size() >= 1) {
                    res[0] = number1;
                    res[1] = number2;
                    flag = 1;
                }
                number2++;
            }
            number2=0;
            number1++;
        }
        return res;
    }
    void reservaTrotinete(int x,int y,PrintWriter out){
        try{
            lock.lock();
        
        int[] pos = trotineteMaisProx(x, y);
        if (mapa[pos[0]][pos[1]].get(0).estado == 0) {
            mapa[pos[0]][pos[1]].get(0).reserva();
            mapa[pos[0]][pos[1]].remove(0);
            out.write("O código de reserva é" +  mapa[pos[0]][pos[1]].get(0).codReserva);out.flush();
        }
        else{
            out.write("Código de insucesso" + 0);out.flush();
        }
    }finally{
        lock.unlock();
    }
    }

    void estacionaTrotinete(int codRes,int x,int y,PrintWriter out){
        trotinete p = listaTrotinetes.get(codRes);
        p.larga();
        try{
            lock.lock();
            mapa[x][y].add(p);

        }finally{
            lock.unlock();
        }

    }


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
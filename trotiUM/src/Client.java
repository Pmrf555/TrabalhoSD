
import java.io.IOException;
import java.net.Socket;
import java.util.Random;
import java.io.Console;

import Connection.Demultiplexer;
import Connection.Message;
import Utilities.Pair;
import Utilities.SHA256;

public class Client {
    private static final Integer clientID = new Random().ints(1, 654321).findFirst().getAsInt();
    private User user;
    private Socket socket;
    private Demultiplexer connection;

    public Client(User user, Socket socket){
        this.user = user;
        this.socket = socket;
        try {
            this.connection = new Demultiplexer(socket);
        } catch (IOException e) {
            this.connection = null;
            System.err.println("Error creating connection");
        }
    }

    public Client(Socket socket){
        this(null, socket);
    }

    public User getUser(){
        return this.user;
    }

    public void setUser(User user){
        this.user = user;
    }

    public Socket getSocket(){
        return this.socket;
    }

    public void setSocket(Socket socket){
        this.socket = socket;
    }

    public Demultiplexer getConnection(){
        return this.connection;
    }

    public void setConnection(Demultiplexer connection){
        this.connection = connection;
    }

    private String sendKILL(){
        Pair<String,Object> response;
        Object data;
        //String command;
        try {
            this.connection.send(Client.clientID, Message.KILL,this.user);
        } catch (IOException e1) {
            return "Failed to send KILL";
        }
        try{
            response = this.connection.receive(Client.clientID);
            data = response.getR();
        }catch (Exception e){
            return "Failed to recive confirmation";
        }
        if(data.getClass() == String.class){
            return (String)data;
        }
        else{
            return "Failed to recive confirmation";
        }
    }

    public boolean register(String username, String password) throws User.InvalidUser{
        Pair<String,Object> response;
        Object data;
        //String command;
        try {
            this.connection.send(Client.clientID, Message.REGISTER,(username + "," + password));
        } catch (IOException e1) {
            return false;
        }
        try{
            response = this.connection.receive(Client.clientID);
            data = response.getR();
        }catch (Exception e){
            return false;
        }
        if(data.getClass() == Boolean.class){ 
            if((Boolean)data){
                this.user = new User(username, password);
                return true;
            }
            else{
                return false;
            }
        }
        else if(data.getClass() == User.InvalidUser.class){
            throw (User.InvalidUser)data;
        }
        else{
            return false;
        }
    }

    public boolean login(String username, String password) throws User.InvalidUser,User.InvalidPassword{
        Pair<String,Object> response;
        Object data;
        //String command;
        try {
            this.connection.send(Client.clientID, Message.LOGIN, (username + "," + password));
            response = this.connection.receive(Client.clientID);
            data = response.getR();
        }catch (Exception e){
            return false;
        }
        if(data.getClass() == Boolean.class && (Boolean)data){
                this.user = new User(username, password);
                return true;
        }
        else if(data.getClass() == User.InvalidUser.class){
            throw (User.InvalidUser)data;
        }
        else if(data.getClass() == User.InvalidPassword.class){
            throw (User.InvalidPassword)data;
        }
        else{
            return false;
        }
    }

    public static void main(String[] args) {
        Console console = System.console();
        try {
            Socket socket = new Socket("localhost",12345);
            Client client = new Client(socket);
            client.connection.start();
            
            Thread t = new Thread(() -> {
                String username = console.readLine("Username: ");
                String password = new String(console.readPassword("Password: "));
                console.printf("Logging in ...\n", username);

                boolean logedin = false;
                boolean register = false;
                try {
                    logedin = client.login(username, SHA256.getSha256(password));
                } catch (User.InvalidUser e) {
                    console.printf("Error: %s\n", e.getMessage());
                    register = Boolean.parseBoolean(console.readLine("Do you want to register? (true/FALSE): "));
                } catch (User.InvalidPassword e){
                    console.printf("Error: %s\n", e.getMessage());
                }

                if (register) {
                    try {
                        logedin = client.register(username, SHA256.getSha256(password));
                    } catch (User.InvalidUser e) {
                        console.printf("Error: %s\n", e.getMessage());
                    }
                }

                if(!logedin){
                    System.exit(-1);
                }
                console.printf("Logged in as %s\n", client.getUser().getUsername());

                boolean killServer = false;
                killServer = Boolean.parseBoolean(console.readLine("Do you want to kill the server? (true/FALSE): "));

                if (killServer)
                    console.printf("%s\n",client.sendKILL()); // admin only command
                System.exit(0);
            });
            t.start();
            t.join();
        } catch (Exception e) {
            console.printf("Error: %s\n", e.getMessage());
        }
    }
}
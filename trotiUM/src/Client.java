
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.io.Console;

import Connection.Demultiplexer;
import Connection.Message;
import Utilities.Matrix;
import Utilities.Pair;
import Utilities.SHA256;
import Utilities.StringPad;
import Utilities.Rand;


public class Client {
    private Integer clientID = Rand.randInt(0, 65535);
    private Lock lock = new ReentrantLock();
    private User user;
    private Socket socket;
    private Demultiplexer connection;
    private boolean running;

    public class ClientView{
        public static final Console console = System.console();
        public static final Integer windowLength = 100;
        public static final Integer windowWidth = 100;
        public static final String LOGIN_MENU = "1 - Login\n2 - Register\n0 - Exit\n";
        public static final String MAIN_MENU = "1 - Scooters\n2 - Profile\n3 - Kill Server\n9 - Logout\nQ - Exit\n";
        public static final String USER_MENU = "1 - View Profile\n2 - Add Funds\n3 - Update Position\n7 - Logout\n0 - Back | Q - Exit\n";
        public static final String SCOOTER_MENU = "1 - Nearby Scooters\n2 - Reserve Scooter\n3 - Park Scooter\n4 - Subscribe\n5 - Unsubscribe\n6 - Get User\n9 - Logout\n0 - Back | Q - Exit\n";

        public static void clear(){
            console.printf("\033[H\033[2J");
            console.flush();
        }

        public static void MainMenu(){
            clear();
            console.printf(MAIN_MENU);
        }

        public static void UserMenu(){
            clear();
            console.printf(USER_MENU);
        }

        public static void Login(){
            clear();
            console.printf(LOGIN_MENU);
        }

        public static void ScootersMenu(){
            clear();
            console.printf(SCOOTER_MENU);
        }

        public static String askForString(String message){
            return console.readLine(message);
        }

        public static String askForPassword(String message){
            return new String(console.readPassword(message));
        }

        public static Integer askForInteger(String message){
            return Integer.parseInt(console.readLine(message));
        }

        public static void showUser(User user){
            console.printf("Username: %s \n", user.getUsername());
            console.printf("\tBalance: %f \n", user.getBalance());
            console.printf("\tPosition: (%d, %d) \n", user.getPosition().getL(), user.getPosition().getR());
            console.printf("\tTrips made: %d \n", user.getInvoices().size());
        }

        public static void showScooters(Matrix<List<Scooter>> scooters){
            Integer size = scooters.dimension();
            String matrix = "";
            for(int i = size-1; i >= 0; i--){
                for(int j = 0; j < size; j++){
                    List<Scooter> list = scooters.get(i,j);
                    String mein = "";
                    String meout = "";
                    if (i == 2 && j == 2){mein = "\033[0;31m";meout = "\033[0m";} // Put user location in red
                    matrix += " |" + StringPad.padString(new String( " " + mein + list.size() + " Scooters " + meout), 12) + "|";
                }
                matrix += "\n";
            }
            console.printf(matrix);
            Client.ClientView.askForString("Press enter to continue...");
        }
    }

    public Client(User user, Socket socket, boolean running){
        this.user = user;
        this.socket = socket;
        this.running = running;
        try {
            this.connection = new Demultiplexer(socket);
        } catch (IOException e) {
            this.connection = null;
            System.err.println("Error creating connection");
        }
    }

    public Client(Socket socket){
        this(null, socket, true);
    }

    public void updateUser(){
        this.lock.lock();
        Pair<String,Object> response;
        Object data;
        //String command;
        try {
            this.connection.send(this.clientID, Message.UPDATE_PROFILE,this.user);
            response = this.connection.receive(this.clientID);
            data = response.getR();
            if(data.getClass() == User.class){
                this.user = (User)data;
            }
            else{
                System.err.println("Failed to recive confirmation");
            }
        }catch (Exception e){
            System.err.println("Failed to recive confirmation");
        }finally{
            this.lock.unlock();
        }
    }


    public void updateLocation(){
        Pair<Integer,Integer> newPos = new Pair<Integer,Integer>(Rand.randInt(0, 40), Rand.randInt(0, 40));
        this.lock.lock();
        try{
            this.user.setPosition(newPos);
        }
        finally{
            this.lock.unlock();
        }        
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

    private Pair<String,Boolean> sendKILL(){
        Pair<String,Object> response;
        Object data;
        //String command;
        try {
            this.connection.send(this.clientID, Message.KILL,this.user);
        } catch (IOException e1) {
            return new Pair<String,Boolean>("Failed to send KILL",false);
        }
        try{
            response = this.connection.receive(this.clientID);
            data = response.getR();
        }catch (Exception e){
            return new Pair<String,Boolean>("Failed to recive confirmation",false);
        }
        if(data.getClass() == String.class){
            return new Pair<String,Boolean>((String)data,response.getL().equals(Message.OK));
        }
        else{
            return new Pair<String,Boolean>("Failed to recive confirmation",false);
        }
    }

    public boolean register(String username, String password) throws User.InvalidUser{
        Pair<String,Object> response;
        Object data;
        //String command;
        try {
            this.connection.send(this.clientID, Message.REGISTER,(username + "," + password));
        } catch (IOException e1) {
            return false;
        }
        try{
            response = this.connection.receive(this.clientID);
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
            this.connection.send(this.clientID, Message.LOGIN, (username + "," + password));
            response = this.connection.receive(this.clientID);
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

    public void userKill(){
        Pair<String,Boolean> response = this.sendKILL();
        if(response.getR()){
            Client.ClientView.askForString(response.getL() + " and shuting down. Press enter to continue.");
            this.running = false;
        }
        else{
            Client.ClientView.askForString(response.getL() + " Press enter to continue.");
        }
    }

    public void logout(){
        this.user = null;
        this.clientID = Rand.randInt(0, 65535);
    }


    public void userLogin(){
        String username = Client.ClientView.askForString("Username: ");
        String password = SHA256.getSha256(Client.ClientView.askForPassword("Password: "));
        try{
            if(this.login(username, password)){
                updateUser();
                //Client.ClientView.showUser(this.user);
            }
            else{
                Client.ClientView.askForString("Login failed. Press enter to continue.");
            }
        }catch (User.InvalidUser e){
            Client.ClientView.askForString("Invalid username. Press enter to continue.");
        }catch (User.InvalidPassword e){
            Client.ClientView.askForString("Invalid password. Press enter to continue.");
        }
        

    }

    public void userRegister(){
        String username = Client.ClientView.askForString("Username: ");
        String password = SHA256.getSha256(Client.ClientView.askForPassword("Password: "));
        try{
            if(this.register(username, password)){
                //Client.ClientView.showUser(this.user);
            }
            else{
                Client.ClientView.askForString("Register failed. Press enter to continue.");
            }
        }catch (User.InvalidUser e){
            Client.ClientView.askForString("Invalid username. Press enter to continue.");
        }
    }

    @SuppressWarnings("unchecked")
    public void listScooters(){
        Pair<String,Object> response;
        Object data;
        //String command;
        try {
            this.connection.send(this.clientID, Message.LIST_SCOOTERS,this.user);
            response = this.connection.receive(this.clientID);
            data = response.getR();
            if(data.getClass() == Matrix.class){
                Client.ClientView.showScooters((Matrix<List<Scooter>>)data);
            }
            else{
                Client.ClientView.askForString("Failed to recive scooters. Press enter to continue.");
            }
        }catch (Exception e){
            return;
        }
    }

    public void scooterMenu(){
        boolean back = true;
        while (back && this.user != null){
            //1 - List Scooters
            //2 - Reserve Scooter
            //3 - Park Scooter
            //4 - Subscribe
            //5 - Unsubscribe
            //9 - Logout
            //0 - Back | Q - Exit";
            Client.ClientView.ScootersMenu();
            String option = Client.ClientView.askForString("Option: ");
            switch(option){
                case "1":
                    // List Nearby Scooters
                    this.listScooters();
                    break;
                case "2":
                    // Reserve Scooter
                    Client.ClientView.askForString("Not Implemented yet. Press enter to continue.");
                    break;
                case "3":
                    // Park Scooter
                    Client.ClientView.askForString("Not Implemented yet. Press enter to continue.");
                    break;
                case "4":
                    // Subscribe Rewards
                    Client.ClientView.askForString("Not Implemented yet. Press enter to continue.");
                    break;
                case "5":
                    // Unsubscribe Rewards
                    Client.ClientView.askForString("Not Implemented yet. Press enter to continue.");
                    break;
                case "9":
                    // User Logout
                    this.logout();
                    back = false;
                    break;
                case "0":
                    // Back a menu
                    back = false;
                    break;
                    // Exit
                case "Q", "q":
                    this.running = false;
                    back = false;
                    break;
                default:
                    return;
            }

        }
    }

    public void userMenu(){
        boolean back = true;
        while(back && this.user != null){
            Client.ClientView.UserMenu();
            String option = Client.ClientView.askForString("Option: ");
            switch(option){
                case "1":
                    this.viewProfile();
                    break;
                case "7":
                    this.logout();
                    back = false;
                    break;
                case "0":
                    back = false;
                    break;
                case "Q", "q":
                    this.running = false;
                    break;
                default:
                    return;
            }
        }
    }

    public void viewProfile(){
        Pair<String,Object> response;
        Object data;
        //String command;
        try {
            this.connection.send(this.clientID, Message.VIEW_PROFILE,this.user);
            response = this.connection.receive(this.clientID);
            data = response.getR();
        }catch (Exception e){
            return;
        }
        if(data.getClass() == User.class){
            this.user = (User)data;
            Client.ClientView.showUser(this.user);
            Client.ClientView.askForString("Press enter to continue ...");
        }
        else{
            return;
        }
    }

    public void main_menu(){
        boolean back = true;
        while (this.running && back && this.user != null){
            Client.ClientView.MainMenu();
            String option = Client.ClientView.askForString("Option: ");
            switch(option){
                case "1":
                    this.scooterMenu();
                    break;
                case "2":
                    this.userMenu();
                    break;
                case "3":
                    this.userKill();
                    break;
                case "9":
                    this.logout();
                    break;
                case "Q", "q":
                    this.running = false;
                    break;
                default:
                    
                    break;
            }
        }
    }

    public void login_menu(){
        while (this.user == null && this.running){
            Client.ClientView.Login();
            String option = Client.ClientView.askForString("Option: ");
            switch(option){
                case "1":
                    this.userLogin();
                    break;
                case "2":
                    this.userRegister();
                    break;
                case "0":
                    this.running = false;
                    break;
                default:
                    break;
            }
            if (this.user != null){
                this.main_menu();
            }
            if(!this.running){
                System.exit(0);
            }
        }
    }

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost",12345);
            Client client = new Client(socket);
            client.connection.start();
            
            Thread t = new Thread(() -> {
                client.login_menu();
            });
            t.start();
            t.join();
        } catch (Exception e) {
            Client.ClientView.console.printf("Error: %s\n", e.getMessage());
        }
    }
}
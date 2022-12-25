
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
import Utilities.StringUtils;
import Utilities.Rand;


public class Client {
    private Integer clientID = Rand.randInt(0, 65535);
    private Lock lock = new ReentrantLock();
    private User user;
    private Integer scooter;
    private Socket socket;
    private Demultiplexer connection;
    private boolean running;

    public class ClientView{
        public static final Console console = System.console();
        public static final Integer windowLength = 100;
        public static final Integer windowWidth = 100;
        public static final String LOGIN_MENU = "1 - Login\n2 - Register\nQ - Exit\n";
        public static final String MAIN_MENU = "1 - Scooters\n2 - Profile\n3 - Kill Server\nL - Logout\nQ - Exit\n";
        public static final String USER_MENU = "1 - View Profile\n2 - Add Funds\n3 - Update Position\nL - Logout\nB - Back | Q - Exit\n";
        public static final String SCOOTER_MENU = "1 - Nearby Scooters\n2 - Reserve Scooter\n3 - Park Scooter\n4 - Subscribe\n5 - Unsubscribe\nL - Logout\nB - Back | Q - Exit\n";

        public static void clear(){
            console.printf("\033[H\033[2J");
            console.flush();
        }

        public static void MainMenu(){
            console.printf(MAIN_MENU);
        }

        public static void UserMenu(){
            console.printf(USER_MENU);
        }

        public static void Login(){
            console.printf(LOGIN_MENU);
        }

        public static void ScootersMenu(){
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
        public static Double askForDouble(String message){
            String number = console.readLine(message);
            try{
                return Double.parseDouble(number);
            }
            catch (NumberFormatException e){
                if (StringUtils.isNumeric(number)){
                    return Double.parseDouble(number);
                }
                if (number.contains(","))
                    return Double.parseDouble(number.replace(',', '.'));
                else
                    return Double.parseDouble(number+".0");
            }
        }

        public static Pair<Integer, Integer> askForPair(String message){
            String[] pos = console.readLine(message).strip().split(",");
            return new Pair<Integer, Integer>(Integer.parseInt(pos[0]), Integer.parseInt(pos[1]));
        }

        public static void showUser(User user){
            console.printf("Username: %s \n", user.getUsername());
            console.printf("\tBalance: %f \n", user.getBalance());
            console.printf("\tPosition: (%d, %d) \n", user.getPosition().getL(), user.getPosition().getR());
            console.printf("\tTrips made: %d \n", user.getInvoices().size());
            console.printf("\tSubscribed: %s \n", user.Subscribed() ? "Yes" : "No");
            if (user.getDiscountUses() > 0){
                console.printf("\tDiscount: %f \n", user.getDiscount());
                console.printf("\tDiscount uses: %d \n", user.getDiscountUses());
            }
            if(user.getInvoices().size() > 0){
                console.printf("\tInvoices: \n");
                for (Scooter.Invoice i : user.getInvoices()){
                    console.printf("\tInvoice: %d \n", i.getId());
                    console.printf("\t\tScooter: %d \n", i.getScooter());
                    console.printf("\t\tStart: (%d, %d) \n", i.From().getL(), i.From().getR());
                    console.printf("\t\tEnd: (%d, %d) \n", i.To().getL(), i.To().getR());
                    console.printf("\t\tCost: %f \n", i.getPrice());
                    console.printf("\t\tStatus: %s \n", i.getStatus().toString());
                }
            }

        }

        public static void showScootersInRange(Matrix<List<Scooter>> scooters){
            String matrix = "";
            for (int x = (scooters.dimension()-1); x >= 0; x--){
                for (int pos = 0; pos < 5; pos ++){
                    for (int y = 0; y < scooters.dimension(); y++){
                        String mein = "";
                        String meout = "";
                        if (x == 2 && y == 2){mein = "\033[0;31m";meout = "\033[0m";} // Put user location in red
                        List<Scooter> list = scooters.get(x,y);
                        if (list != null){
                            Scooter s = null;
                            try{
                                s = list.get(pos);
                            }catch(Exception e){
                                s = null;
                            }
                            if (s != null){
                                matrix += " |" + mein + StringUtils.padString(" Scooter" + StringUtils.padString(""+s.getId(),5), 18) + meout + "| ";
                            }
                            else
                                matrix += " |" + mein + StringUtils.padString(new String( " NULL "), 18) + meout + "| ";
                        }
                        else
                            matrix += " |" + mein + StringUtils.padString(new String( " NULL "), 18) + meout + "| ";
                    }
                    matrix += "\n";
                }
                matrix += "\n";
            }
            console.printf(matrix);
            Client.ClientView.askForString("\033[0;31m* - User Location\033[0m\nPress enter to continue...");
        }

        public static void showRewardsInRange(Matrix<Integer> rewards){
            String matrix = "";
            for (int x = (rewards.dimension()-1); x >= 0; x--){
                for (int y = (rewards.dimension()-1); y >= 0; y--){
                    String mein = "";
                    String meout = "";
                    if (x == 2 && y == 2){mein = "\033[0;31m";meout = "\033[0m";} // Put user location in red
                    matrix += " |" + mein + StringUtils.padString(" Reward" + StringUtils.padString(""+rewards.get(x,y),4), 12) + meout + "| ";
                }
                matrix += "\n";
            }
            console.printf(matrix);
        }
    }

    public Client(User user, Socket socket, boolean running){
        this.user = user;
        this.socket = socket;
        this.running = running;
        this.scooter = null;
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



    public void setUserToServer(String message){
        this.lock.lock();
        Pair<String,Object> response;
        Object data;
        //String command;
        try {
            this.connection.send(this.clientID, message,this.user);
            response = this.connection.receive(this.clientID);
            data = response.getR();
            if(data.getClass() == Boolean.class && (Boolean)data){
                System.err.println("User updated");
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

    public void randomLocation(){
        Pair<Integer,Integer> newPos = new Pair<Integer,Integer>(Rand.randInt(0, 40), Rand.randInt(0, 40));
        this.lock.lock();
        try{
            this.user.setPosition(newPos);
        }
        finally{
            this.lock.unlock();
        }        
    }

    public Integer  getScooter(){
        return this.scooter;
    }

    public void setScooter(Integer  scooter){
        this.scooter = scooter;
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
                this.setUserToServer(Message.UPDATE_POSTITION);
                this.getUserFromServer();
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
                this.setUserToServer(Message.SET_PROFILE);
                this.getUserFromServer();
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
                Client.ClientView.showScootersInRange((Matrix<List<Scooter>>)data);
                this.setUserToServer(Message.SET_PROFILE);
            }
            else{
                Client.ClientView.askForString("Failed to recive scooters. Press enter to continue.");
            }
        }catch (Exception e){
            return;
        }
    }

    public void make_trip(){
        String scooterID = Client.ClientView.askForString("Scooter ID: ");
        Pair<String,Object> response;
        Object data;
        //String command;
        try {
            this.connection.send(this.clientID, Message.RESERVE_SCOOTER,(this.user.getUsername() + "," + this.user.getPassword() + "," + scooterID));
            response = this.connection.receive(this.clientID);
            data = response.getR();
            String command = response.getL();
            if (command.equals(Message.OK)){
                if(data.getClass() == String.class){
                    Client.ClientView.askForString("Scooter Reservation Code: " + (String)data + "\nScooter reserved. Press enter to continue.");
                    this.setScooter(Integer.parseInt(scooterID));
                    this.setUserToServer(Message.SET_PROFILE);
                }
            }
            else{
                Exception e = (Exception)data;
                Client.ClientView.askForString(e.getMessage());
            }
        }catch (Exception e){
            Client.ClientView.askForString("Failed to reserve scooter. Press enter to continue.");
        }
    }

    @SuppressWarnings("unchecked")
    public void rewards(Pair<Integer,Integer> userDestination){
        Pair<String,Object> response;
        Object data;
        //String command;
        try {
            this.connection.send(this.clientID, Message.GET_REWARDS,(userDestination));
            response = this.connection.receive(this.clientID);
            data = response.getR();
            String command = response.getL();
            if (command.equals(Message.OK)){
                if(data.getClass() == Matrix.class){
                    Client.ClientView.showRewardsInRange((Matrix<Integer>)data);
                }
            }
            else{
                Exception e = (Exception)data;
                Client.ClientView.askForString(e.getMessage());
            }
        }catch (Exception e){
            Client.ClientView.askForString("Failed to get rewards. Press enter to continue.");
        }
    }

    public void addFunds(){
        Double funds = Client.ClientView.askForDouble("How much money do you want to add? ");
        Client.ClientView.console.printf(this.user.toString());
        this.user.addBalance(funds);
        Client.ClientView.console.printf(this.user.toString());
        this.setUserToServer(Message.SET_PROFILE);
        Client.ClientView.askForString("Funds added. Press enter to continue.");
    }

    public void updatePosition(){
        Pair<Integer,Integer> userDestination = Client.ClientView.askForPair("Where are you? (x,y): ");
        this.user.setPosition(userDestination);
        this.setUserToServer(Message.SET_PROFILE);
        Client.ClientView.askForString("Position updated. Press enter to continue.");
    }

    public void parkScooter(){
        Pair<Integer,Integer> userDestination = Client.ClientView.askForPair("Where do you want to park the scooter? (x,y): ");
        String scooterCode = Client.ClientView.askForString("Scooter Reservation Code: ");
        Pair<String,Object> response;
        Object data;
        String command;
        boolean park = false;
        try {
            while(!park){
                this.rewards(userDestination);
                if (!Client.ClientView.askForString("\033[0;31m* - User Destination\033[0m\nDo you want to park the scooter in \033[0;31m(" + userDestination.getL() + ", " + userDestination.getR() + ")\033[0m? (y/N): ").equals("y")){
                    userDestination = Client.ClientView.askForPair("Where do you want to park the scooter? (x,y): ");
                }
                else{
                    park = true;
                }
            }
            this.connection.send(this.clientID, Message.PARK_SCOOTER,(this.user.getUsername() + "," + scooterCode  + "," + userDestination.getL() + "," + userDestination.getR()));
            response = this.connection.receive(this.clientID);
            data = response.getR();
            command = response.getL();
            if (command.equals(Message.OK)){
                if(data.getClass() == Boolean.class && (boolean)data){
                    Client.ClientView.askForString("Scooter Parked. Press enter to continue.");
                    this.setScooter(0);
                    this.getUserFromServer();
                }
            }
            else{
                Exception e = (Exception)data;
                Client.ClientView.askForString(e.getMessage());
            }
        }catch (Exception e){
            Client.ClientView.askForString("Failed to park scooter. Press enter to continue.");
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
            //L - Logout
            //B - Back | Q - Exit";
            Client.ClientView.ScootersMenu();
            String option = Client.ClientView.askForString("Option: ");
            switch(option){
                case "1":
                    // List Nearby Scooters
                    this.listScooters();
                    break;
                case "2":
                    // Reserve Scooter
                    this.make_trip();
                    break;
                case "3":
                    // Park Scooter
                    this.parkScooter();
                    break;
                case "4":
                    // Subscribe Rewards
                    Client.ClientView.askForString("Not Implemented yet. Press enter to continue.");
                    break;
                case "5":
                    // Unsubscribe Rewards
                    Client.ClientView.askForString("Not Implemented yet. Press enter to continue.");
                    break;
                case "l","L":
                    // User Logout
                    this.logout();
                    back = false;
                    break;
                case "b","B":
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
            /*
            1 - View Profile
            2 - Add Funds
            3 - Update Position
            L - Logout
            B - Back | Q - Exit
             */
            String option = Client.ClientView.askForString("Option: ");
            switch(option){
                case "1":
                    // View Profile
                    this.viewProfile();
                    break;
                case "2":
                    // Add Funds
                    this.addFunds();
                    break;
                case "3":
                    // Update Position
                    this.updatePosition();
                    break;
                case "l", "L":
                    // User Logout
                    this.logout();
                    back = false;
                    break;
                case "b", "B":
                    // Back a menu
                    back = false;
                    break;
                case "Q", "q":
                    // Exit Program
                    this.running = false;
                    back = false;
                    break;
                default:
                    return;
            }
        }
    }

    public void getUserFromServer(){
        Pair<String,Object> response;
        Object data;
        //String command;
        try {
            this.connection.send(this.clientID, Message.GET_PROFILE,this.user);
            response = this.connection.receive(this.clientID);
            data = response.getR();
        }catch (Exception e){
            System.out.println(e);
            return;
        }
        if(data.getClass() == User.class){
            this.user = (User)data;
        }
        else{
            return;
        }

    }

    public void viewProfile(){
        this.getUserFromServer();
        Client.ClientView.showUser(this.user);
        Client.ClientView.askForString("Press enter to continue ...");
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
                case "l","L":
                    this.logout();
                    break;
                case "Q", "q":
                    this.running = false;
                    back = false;
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
                case "q", "Q":
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
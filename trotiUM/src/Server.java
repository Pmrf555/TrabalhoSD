import java.io.Console;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Connection.TaggedConnection;
import Utilities.Log;
import Utilities.Matrix;
import Utilities.Pair;
import Utilities.SHA256;
import Utilities.StringUtils;
import Connection.Message;

class Server{
    private final static int WORKERS_PER_CONNECTION = 3;
    private final static int PORT = 12345;
    public final static int GRID_DIMENSION = 20;
    public final static int MAX_SCOOTERS = 800;
    public final static int RADIUS = 2;
    private final static User admin = new User("admin", SHA256.getSha256("admin"));
    private HashMap<String,String> tokens = new HashMap<String,String>();
    private HashMap<String,Integer> codeId = new HashMap<String,Integer>();
    private ArrayList<User> users = new ArrayList<User>();
    private Lock lock = new ReentrantLock();
    private State state = new State(GRID_DIMENSION, RADIUS, MAX_SCOOTERS);

    public class ServerView{
        public static final Console console = System.console();

        public static void printUserlist(List<User> users){
            console.printf(StringUtils.padString(" USER LIST ", 80, '-') + "\n");
            for (User u : users){
                console.printf(u.serverLogRight() + "\n");
            }
            console.printf(StringUtils.padString("", 80, '-') + "\n");
        }
    }

    public Server(){
        users.add(admin);
    }

    private boolean isAdmin(User user){
        return user.checkCredentials(Server.admin);
    }

    private boolean validUser (User user){
        lock.lock();
        try{
            for (User u : users){
                if (u.checkCredentials(user)) return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    private User getUser(User user) throws User.InvalidUser{
        User out = null;
        lock.lock();
        try{
            for (User u : users){
                if (u.checkCredentials(user)) out = u;
            }
            if (out == null) throw new User.InvalidUser("User does not exist");
            return out;
        } finally {
            lock.unlock();
        }
    }
    
    private User getUserByUsername(String username) throws User.InvalidUser{
        User out = null;
        lock.lock();
        try{
            for (User u : users){
                if (u.getUsername().equals(username)) out = u;
            }
            if (out == null) throw new User.InvalidUser("User does not exist");
            return out;
        } finally {
            lock.unlock();
        }
    }

    private Boolean updateUser(User user) throws User.InvalidUser{
        lock.lock();
        try{
            for (User u : users){
                if (u.checkCredentials(user)){
                    u.setPosition(user.getPosition());
                    u.update(user);
                    Log.all(Log.INFO,"User " + user.getUsername() + " got updated");
                    return true;
                }
            }
            throw new User.InvalidUser("User does not exist");
        } finally {
            lock.unlock();
        }
    }

    private Matrix<Integer> dropRewardsInRadius(Pair<Integer,Integer> destination) {
        return state.getDropRewardsInRadius(destination.getL(), destination.getR()); 
    }

    private Matrix<Integer> pickupRewardsInRadius(Pair<Integer,Integer> destination) {
        return state.getPickupRewardsInRadius(destination.getL(), destination.getR()); 
    }

    private Boolean parkScooter(String username, String scooterReservationCode, Integer x, Integer y) throws User.InvalidUser, Scooter.InvalidReservationCode, Scooter.InvalidScooter{
        User user = getUserByUsername(username);
        if ((tokens.get(username) == null) || (!tokens.get(username).equals(scooterReservationCode))) throw new Scooter.InvalidScooter("Invalid Scooter");

        try{
            Integer scooter_id = codeId.get(scooterReservationCode);
            if (scooter_id == null) throw new Scooter.InvalidReservationCode("Invalid Reservation Code");
            user = this.state.parkScooter(user, scooterReservationCode, x, y, state.getById(scooter_id));
            user.setPosition(new Pair<Integer,Integer>(x,y));
            this.tokens.remove(user.getUsername());
            this.codeId.remove(scooterReservationCode);
            Log.all(Log.INFO,"User " + user.getUsername() + " parked the scooter " + scooter_id + " at " + x + "," + y);
            return true;
        }
        catch(Exception e){
            Log.all(Log.ERROR,e.getMessage());
            throw e;
        }
    }
    private Matrix<List<Scooter>> scootersInRadius(User user) throws User.InvalidUser{
        if (this.validUser(user)){
            Pair<Integer, Integer> pos = user.getPosition();
            Matrix<List<Scooter>> radius = state.getScootersInRadius(pos.getL(), pos.getR());
            Log.all(Log.INFO,"User " + user.getUsername() + " requested scooters in radius");
            return radius;
        }
        else{
            throw new User.InvalidUser("Permission Denied");
        }   
    }

    private String reserveScooter(String username, String password, String idScooter) throws User.InvalidUser, Scooter.InvalidScooter, State.RadiusTooFar{
        User user = new User(username, password);
        Pair<String,Scooter> response = null;
        if (this.validUser(user)){
            try{
                user = this.getUser(user);
                response = state.reserveScooter(user, Integer.parseInt(idScooter));
                Scooter scooter = response.getR();
                String code = response.getL();
                this.tokens.put(user.getUsername(), code);
                Log.all(Log.INFO,"User " + user.getUsername() + " Reservation: Scooter " + scooter.getId());
                tokens.put(user.getUsername(), code);
                codeId.put(code, scooter.getId());
                return code;
            }catch(Exception e){
                Log.all(Log.ERROR,e.getMessage());
                throw e;
            }
        }
        else{
            throw new User.InvalidUser("Permission Denied");
        }
    }

    private boolean KILL(User user) throws User.InvalidUser{
        if (isAdmin(user)){
            Log.all(Log.EVENT,"User " + user.getUsername() + " requested server shutdown");
            return true;
        }
        else{
            Log.all(Log.EVENT,"User " + user.getUsername() + " attempted to shutdown server");
            throw new User.InvalidUser("Permission Denied");
        }
    }

    private void interrupt(){
        Log.all(Log.DEBUG,"Server shutdown, User dump:");
        for (User user : users){
            Log.all(Log.DEBUG,user.toString());
        }
        Log.all(Log.INFO,"User dump complete, BYE!");
        System.exit(0);
    }

    public boolean register(String username, String password) throws User.InvalidUser{
        lock.lock();
        try{
            for (User user : users){
                if (user.getUsername().equals(username)) {
                    throw new User.InvalidUser("Username already taken");}
            }
            users.add(new User(username, password));
            Log.all(Log.INFO,"User " + username + " registered");
        } finally {
            lock.unlock();
        }
        return true;
    }

    public boolean login(String username, String password) throws User.InvalidUser, User.InvalidPassword{
        lock.lock();
        boolean success = false;
        try{
            for (User user : users){
                if (user.getUsername().equals(username)) {
                    if (user.getPassword().equals(password)){
                        success = true;
                        Log.all(Log.INFO,"User " + username + " logged in");
                        break;
                    }
                    else {
                        Log.all(Log.INFO,"User " + username + " tried to log in with wrong password");
                        throw new User.InvalidPassword("Wrong password");
                    }
                }
            }
        } finally {
            lock.unlock();
        }
        if (!success){
            Log.all(Log.INFO,"User " + username + " tried to log but username was not found in the database");
            throw new User.InvalidUser("Username not found");
        }
        return success;
    }
    @SuppressWarnings("unchecked")
    public void start() throws Exception {
        ServerSocket ss = new ServerSocket(Server.PORT);

        while(true) {
            Socket s = ss.accept();
            TaggedConnection c = new TaggedConnection(s);

            Runnable worker = () -> {
                try (c) {
                    Boolean running = true;
                    int tag = -1;
                    Object data = null;
                    String command = null;
                    while (running) {
                        tag = 0;
                        data = null;
                        command = null;
                        TaggedConnection.TaggedMessage frame = c.read();
                        tag = frame.getTag();
                        data = frame.getData();
                        command = frame.getCommand();
                        Log.all(Log.EVENT,"Received command: " + command + " from the tag " + tag);
                        switch (command) {
                            case Message.REGISTER:
                                if (data.getClass() == String.class && ((String) data).length() != 0){
                                    String dataString = (String) data;
                                    String[] dataParts = dataString.split(",");
                                    if (dataParts.length >= 2){
                                        String username = dataParts[0];
                                        String password = dataParts[1];
                                        boolean success = false;
                                        try {
                                            success = register(username, password);
                                        } catch (User.InvalidUser e) {
                                            c.write(tag, Message.ERROR, e);
                                        }
                                        if (success) c.write(tag, Message.OK, (Boolean)true);
                                    }
                                }
                                break;
                    
                            case Message.LOGIN:
                                if (data.getClass() == String.class && ((String) data).length() != 0){
                                    String dataString = (String) data;
                                    String[] dataParts = dataString.split(",");
                                    if (dataParts.length >= 2){
                                        String username = dataParts[0];
                                        String password = dataParts[1];
                                        boolean success = false;
                                        try {
                                            success = login(username, password);
                                        } catch (Exception e) {
                                            c.write(tag, Message.ERROR, e);
                                        }
                                        if (success) c.write(tag, Message.OK, (Boolean)true);
                                    }
                                }
                                break;
                            
                            case Message.LIST_SCOOTERS:
                                if (data.getClass() == User.class){
                                    User dataUser = (User) data;
                                    try {
                                        Matrix<List<Scooter>> scooters = scootersInRadius(dataUser);
                                        c.write(tag, Message.OK, scooters);
                                    } catch (User.InvalidUser e) {
                                        c.write(tag, Message.ERROR, e);
                                    }
                                }
                                break;

                            case Message.RESERVE_SCOOTER:
                                if (data.getClass() == String.class){
                                    String[] dataString = ((String) data).split(",");
                                    try {
                                        String code = reserveScooter(dataString[0], dataString[1], dataString[2]);
                                            c.write(tag, Message.OK, code);
                                    }
                                    catch (Exception e) {
                                        c.write(tag, Message.ERROR, e);
                                    }
                                }
                                break;

                            case Message.PARK_SCOOTER:
                                if (data.getClass() == String.class){
                                    String[] dataString = ((String) data).split(",");
                                    try {
                                        parkScooter(dataString[0], dataString[1], Integer.parseInt(dataString[2]),Integer.parseInt(dataString[3]));
                                        c.write(tag, Message.OK, (Boolean)true);
                                    }
                                    catch (Exception e) {
                                        c.write(tag, Message.ERROR, e);
                                    }
                                }
                                break;
                                
                            case Message.UPDATE_POSTITION:
                                if(data.getClass() == User.class){
                                    User dataUser = ((User) data);
                                    try {
                                        this.updateUser(dataUser);
                                        c.write(tag, Message.OK, (Boolean)true);
                                    }
                                    catch (Exception e) {
                                        c.write(tag, Message.ERROR, e);
                                    }
                                }
                                break;

                            case Message.GET_PICKUP_REWARDS:
                                if (data.getClass() == Pair.class){
                                    Pair<Integer, Integer> dataPair = (Pair<Integer, Integer>) data;
                                    try {
                                        Matrix<Integer> rewards = pickupRewardsInRadius(dataPair);
                                        Log.all(Log.INFO,"Sent the Pickup rewards matrix in range from (" + dataPair.getL() + ", " + dataPair.getR() + ")");
                                        c.write(tag, Message.OK, rewards);
                                    } catch (Exception e) {
                                        c.write(tag, Message.ERROR, e);
                                    }
                                }
                                break;

                            case Message.GET_DROP_REWARDS:
                                if (data.getClass() == Pair.class){
                                    Pair<Integer, Integer> dataPair = (Pair<Integer, Integer>) data;
                                    try {
                                        Matrix<Integer> rewards = dropRewardsInRadius(dataPair);
                                        Log.all(Log.INFO,"Sent the drop rewards matrix in range from (" + dataPair.getL() + ", " + dataPair.getR() + ")");
                                        c.write(tag, Message.OK, rewards);
                                    } catch (Exception e) {
                                        c.write(tag, Message.ERROR, e);
                                    }
                                }
                                break;

                            case Message.KILL:
                                if (data.getClass() == User.class){
                                    User dataUser = (User) data;
                                    try {
                                        if (KILL(dataUser)){
                                            c.write(tag, Message.OK, "Killing server");
                                            ss.close();
                                            this.interrupt();
                                        }
                                    } catch (User.InvalidUser e) {
                                        c.write(tag, Message.ERROR, e);
                                    }
                                }
                                break;
                            
                            case Message.GET_PROFILE:
                                if (data.getClass() == User.class){
                                    User dataUser = (User) data;
                                    try {
                                        User user = getUser(dataUser);
                                        c.write(tag, Message.OK, user);
                                    } catch (User.InvalidUser e) {
                                        c.write(tag, Message.ERROR, e);
                                    }
                                }
                                break;

                            case Message.SET_PROFILE:
                                if (data.getClass() == User.class){
                                    User dataUser = (User) data;
                                    try {
                                        Boolean success = updateUser(dataUser);
                                        c.write(tag, Message.OK, success);
                                    } catch (User.InvalidUser e) {
                                        c.write(tag, Message.ERROR, e);
                                    }
                                }
                                break;
                            default: // Invalid command
                                c.write(tag, command, "Invalid command");
                                break;

                        }
                        //Server.ServerView.printUserlist(users);
                    }
                } catch (Exception ignored) { }
            };

            for (int i = 0; i < WORKERS_PER_CONNECTION; ++i)
                new Thread(worker).start();
        }

    }

    public static void main(String[] args) throws Exception {
        Log.line("\n" + StringUtils.padString(" New Server Instance ", 108, '=')+ "\n");
        Log.all(Log.INFO,"Server started");
        Server server = new Server();
        try {
            server.start();
            while(true) {
            Thread.sleep(1000);
            }
        }catch (Exception e){
            Log.all(Log.ERROR, e.getMessage());
        }
    }
}

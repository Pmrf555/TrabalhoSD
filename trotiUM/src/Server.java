import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Connection.TaggedConnection;
import Utilities.Log;
import Utilities.SHA256;
import Connection.Message;

class Server{
    private final static int WORKERS_PER_CONNECTION = 3;
    private final static int PORT = 12345;
    private final static User admin = new User("admin", SHA256.getSha256("admin"));
    private ArrayList<User> users = new ArrayList<User>();
    private Lock lock = new ReentrantLock();
    private State state = new State();

    public Server(){
        users.add(admin);
    }

    private boolean isAdmin(User user){
        return user.checkCredentials(Server.admin);
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

    public void start() throws Exception {
        Log.all(Log.INFO,"Server started");
        ServerSocket ss = new ServerSocket(Server.PORT);

        while(true) {
            Socket s = ss.accept();
            TaggedConnection c = new TaggedConnection(s);

            Runnable worker = () -> {
                try (c) {
                    for (;;) {
                        TaggedConnection.TaggedMessage frame = c.read();
                        int tag = frame.getTag();
                        Object data = frame.getData();
                        String command = frame.getCommand();
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
                                            c.write(tag, Message.ERROR, e.getMessage());
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
                                break;

                            case Message.RESERVE_SCOOTER:
                                break;

                            case Message.PARK_SCOOTER:
                                break;

                            case Message.SUBSCRIBE:
                                break;

                            case Message.UNSUBSCRIBE:
                                break;

                            case Message.GET_USER:
                                break;

                            case Message.KILL:
                                if (data.getClass() == User.class){
                                    User dataUser = (User) data;
                                    try {
                                        if (KILL(dataUser)){
                                            c.write(tag, Message.OK, "Killing server");
                                            ss.close();
                                            System.exit(0);
                                        }
                                    } catch (User.InvalidUser e) {
                                        c.write(tag, Message.ERROR, e.getMessage());
                                    }
                                }

                            default: // Invalid command
                                c.write(tag, command, "Invalid command");
                                break;

                        }
                        System.out.println(users);
                    }
                } catch (Exception ignored) { }
            };

            for (int i = 0; i < WORKERS_PER_CONNECTION; ++i)
                new Thread(worker).start();
        }

    }

    public static void main(String[] args) throws Exception {
        new Server().start();
        while(true) {
            Thread.sleep(1000);
        }
    }
}

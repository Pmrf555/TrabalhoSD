package Connection;

import java.io.Serializable;

public class Message<T> implements Serializable{

    public static final String OK = "OK";
    public static final String ERROR = "ERROR";
    public static final String REGISTER = "REGISTER";
    public static final String LOGIN = "LOGIN";
    public static final String LIST_SCOOTERS = "LIST_SCOOTERS";
    public static final String RESERVE_SCOOTER = "RESERVE_SCOOTER";
    public static final String PARK_SCOOTER = "PARK_SCOOTER";
    public static final String SUBSCRIBE = "SUBSCRIBE";
    public static final String UNSUBSCRIBE = "UNSUBSCRIBE";
    public static final String KILL = "KILL";
    public static final String GET_PROFILE = "GET_PROFILE";
    public static final String SET_PROFILE = "SET_PROFILE";
    public static final String GET_REWARDS = "GET_REWARDS";
    public static final String UPDATE_POSTITION = "UPDATE_POSITION";
    

    private static final long serialVersionUID = 1L;

    private T data;

    public Message(T data){
        this.data = data;

    }

    public T getData(){
        return this.data;
    }

    public void setData(T data){
        this.data = data;
    }

    @Override
    public String toString(){
        return "Message [data=" + this.data + "]";
    }


    
}

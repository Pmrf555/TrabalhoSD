package Connection;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.*;

public class TaggedConnection implements AutoCloseable {
    private ObjectInputStream dataIn;
    private ObjectOutputStream dataOut;
    private final Lock readLock = new ReentrantLock();
    private final Lock writeLock = new ReentrantLock();

    public class TaggedMessage {
        private int tag;
        private String command;
        private Object message;

        public TaggedMessage(int tag, String command, Object message){
            this.tag = tag;
            this.command = command;
            this.message = message;
        }

        public int getTag(){
            return this.tag;
        }

        public void setTag(int tag){
            this.tag = tag;
        }

        public String getCommand(){
            return this.command;
        }

        public void setCommand(String command){
            this.command = command;
        }

        public Object getData(){
            return this.message;
        }

        public void setData(Object message){
            this.message = message;
        }
    }

    public TaggedConnection(Socket socket) throws IOException {
        this.dataOut = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.dataOut.flush();
        this.dataIn = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
    }

    public TaggedMessage read() throws IOException, ClassNotFoundException {
        this.readLock.lock();
        int tag;
        String command;
        Object data;
        try {
            tag = this.dataIn.readInt();
            command = this.dataIn.readUTF();
            data = this.dataIn.readObject();
        } finally {
            this.readLock.unlock();
        }
        return new TaggedMessage(tag, command, data);
    }

    public void write(int tag, String command, Object message) throws IOException {
        this.writeLock.lock();
        try {
            this.dataOut.writeInt(tag);
            this.dataOut.writeUTF(command);
            this.dataOut.writeObject(message);
            this.dataOut.flush();
            this.dataOut.reset();   // reset the stream to avoid problems with the next write
        } finally {
            this.writeLock.unlock();
        }
    }

    public void write(TaggedMessage data) throws IOException {
        this.writeLock.lock();
        try {
            this.dataOut.writeInt(data.tag);
            this.dataOut.writeUTF(data.command);
            this.dataOut.writeObject(data.message);
            this.dataOut.flush();
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public void close() throws Exception {
        this.dataIn.close();
        this.dataOut.close();
    }
    
    
}

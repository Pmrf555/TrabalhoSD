package Connection;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import Utilities.Pair;

public class Demultiplexer implements AutoCloseable {

    private TaggedConnection tc;
    private ReentrantLock l = new ReentrantLock();
    private Map<Integer, FrameValue> map = new HashMap<>();
    private Exception exception = null;

    private class FrameValue {
        int waiters = 0;
        Queue<Pair<String,Object>> queue = new ArrayDeque<>();
        Condition c = l.newCondition();

        public FrameValue() {

        }
    }


    public Demultiplexer(Socket socket) throws IOException {
        this.tc = new TaggedConnection(socket);
    }

    public Demultiplexer(TaggedConnection conn) {
        this.tc = conn;
    }

    public void start() {
        new Thread(() -> {
            try {
                while (true) {
                    TaggedConnection.TaggedMessage message = tc.read();
                    l.lock();
                    try {
                        FrameValue fv = map.get(message.getTag());
                        if (fv == null) {
                            fv = new FrameValue();
                            map.put(message.getTag(), fv);
                        }
                        fv.queue.add(new Pair<String,Object>(message.getCommand(),message.getData()));
                        fv.c.signal();
                    }
                    finally {
                        l.unlock();
                    }
                }
            }
            catch (ClassNotFoundException c) {
                exception = c;
            }
            catch (IOException e) {
                exception = e;
            }
        }).start();
    }

    public void send(TaggedConnection.TaggedMessage message) throws IOException {
        tc.write(message);
    }

    public void send(int tag, String command, Object data) throws IOException {
        tc.write(tag, command, data);
    }

    public Pair<String,Object> receive(int tag) throws Exception {
        l.lock();
        FrameValue fv;
        try {
            fv = map.get(tag);
            if (fv == null) {
                fv = new FrameValue();
                map.put(tag, fv);
            }
            fv.waiters++;
            while(true) {
                if(! fv.queue.isEmpty()) {
                    fv.waiters--;
                    Pair<String,Object> reply = fv.queue.poll();
                    if (fv.waiters == 0 && fv.queue.isEmpty())
                        map.remove(tag);
                    return reply;
                }
                if (exception != null) {
                    throw exception;
                }
                fv.c.await();
            }
        }
        finally {
            l.unlock();
        }
    }

    @Override
    public void close() throws Exception {
        tc.close();
    }
}

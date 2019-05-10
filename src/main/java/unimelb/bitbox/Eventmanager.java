package unimelb.bitbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Eventmanager extends Thread{
    public BlockingQueue<String> newevent;
    public BlockingQueue<String> eventqueue1;
    public ArrayList<BlockingQueue<String>> serverqueue;

    public Eventmanager(int maximumIncommingConnections)
    {
        this.newevent = new LinkedBlockingQueue<>();
        this.eventqueue1 = new LinkedBlockingQueue<>();
        this.serverqueue = new ArrayList<BlockingQueue<String>>( maximumIncommingConnections);
    }

    public void run() {
        try {
            while (true)
            {
                String message = newevent.take();
                eventqueue1.offer(message);
                for (BlockingQueue<String> a : serverqueue)
                {
                    if (a != null) {
                        a.offer(message);
                    }else
                    {
                        break;
                    }
                }
            }
        }catch (Exception e) {
//            e.printStackTrace();
        }
    }
    public BlockingQueue<String> geteventqueue ()
    {
        return this.eventqueue1;
    }


}

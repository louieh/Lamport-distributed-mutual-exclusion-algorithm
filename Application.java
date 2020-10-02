import java.io.*;
import java.lang.Thread;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.*;


public class Application {
    Node node;
    private boolean hasRequested;
    private long responseStart;
    private long responseEnd;

    public Application(Node node) {
        this.node = node;
        this.hasRequested = false;
    }

    private synchronized void execute_critical_section() {
        System.out.println("executing crit");
        try {
            Thread.sleep(this.node.csExecutionTime);
        } catch (InterruptedException ex) {
        }
    }

    public void cs_enter() {
        while (true) {
            if (this.hasRequested) {
                Request peekRequest = this.node.q.peek();
                if (peekRequest.nodeID == this.node.nodeID && this.node.replyRecevied.get() == this.node.nodeNum - 1) {
//                    long startTime = System.currentTimeMillis();
                    SimpleDateFormat df_start = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                    String start = df_start.format(new Date());
                    this.node.ifInCS = true;
                    this.node.broadcast("TEST");
                    execute_critical_section();
                    this.node.ifInCS = false;
//                    long endtTime = System.currentTimeMillis();
                    SimpleDateFormat df_end = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                    String end = df_end.format(new Date());
                    this.node.outputFile("test", start, end);
                    cs_leave();
                    this.hasRequested = false;
                    this.node.replyRecevied.set(0);
                    break;
                }
            } else {
                int clock_now = this.node.scalar_clock.get();
                this.node.q.offer(new Request(clock_now, this.node.nodeID));
                this.node.broadcast("REQUEST", clock_now);
                this.hasRequested = true;
                responseStart = System.currentTimeMillis();
            }
        }
    }

    public void cs_leave() {
        // remove request from q
        this.node.q.poll();
        responseEnd = System.currentTimeMillis();
        this.node.outputFile("ResponseTime", responseStart, responseEnd);
        // broadcast remove message to all process to remove the request
        this.node.broadcast("RELEASE", 0);
    }

}
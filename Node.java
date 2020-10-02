import javax.swing.text.html.HTMLDocument;
import java.io.*;
import java.lang.Thread;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.PriorityBlockingQueue;


class Request implements Comparable<Request> {
    public final int timestamp;
    public final int nodeID;

    public Request(int timestamp, int nodeID) {
        this.timestamp = timestamp;
        this.nodeID = nodeID;
    }

    @Override
    public int compareTo(Request request) {
        return Integer.compare(this.timestamp, request.timestamp);
    }

    public int get_timestamp() {
        return this.timestamp;
    }
}

public class Node implements Runnable {
    public int nodeNum; // number of node
    public int nodeID; // node index
    public String hostName; // hostname dc01
    public int interRequestDelay;
    public int csExecutionTime;
    public int numRequests;
    public int port;
    public int request_sent;
    public AtomicInteger scalar_clock = new AtomicInteger(0);
    public AtomicInteger replyRecevied = new AtomicInteger(0);
    public AtomicInteger termination = new AtomicInteger(0);
    public AtomicInteger messageCounter = new AtomicInteger(0);
    public long theFirstRequestTime = 0;
    public ArrayList<HashMap<String, String>> NodeInfoList;
    public ArrayList<Integer> outgoingNodeList;
    public Application application;
    Comparator<Request> requestComparator = Comparator.comparing(Request::get_timestamp);
    public PriorityBlockingQueue<Request> q;
    public boolean ifInCS;
    public String[] replyTest;

    public Node() throws IOException {
        ConfigReader config = new ConfigReader();
        this.nodeNum = config.nodeNum;
        this.nodeID = config.myNodeIndex;
        this.hostName = config.myHostName;
        this.interRequestDelay = config.interRequestDelay;
        this.csExecutionTime = config.csExecutionTime;
        this.numRequests = config.numRequests;
        this.port = config.getPort();
        this.NodeInfoList = config.NodeInfoList;
        this.outgoingNodeList = config.outgoingNodeList;
        this.application = new Application(this);
        this.q = new PriorityBlockingQueue<>(this.numRequests, requestComparator);
        this.ifInCS = false;
        this.replyTest = new String[this.nodeNum];
    }


    public void broadcast(String msgType, int clock) {
        for (int outgoingNode : this.outgoingNodeList) {
            String broadcast_hostname = this.NodeInfoList.get(outgoingNode).get("hostname");
            int broadcast_port = Integer.parseInt(this.NodeInfoList.get(outgoingNode).get("port"));
            Message Msg = new Message.MessageBuilder()
                    .from(this.nodeID)
                    .to(outgoingNode)
                    .type(msgType)
                    .clock(clock)
                    .build();
            if (msgType.equals("REQUEST") || msgType.equals("RELEASE")) this.messageCounter.addAndGet(1);
            Msg.sendMsg(Msg, broadcast_hostname + ".utdallas.edu", broadcast_port);
        }
    }

    public void broadcast(String msgType) {
        for (int outgoingNode : this.outgoingNodeList) {
            String broadcast_hostname = this.NodeInfoList.get(outgoingNode).get("hostname");
            int broadcast_port = Integer.parseInt(this.NodeInfoList.get(outgoingNode).get("port"));
            Message Msg = new Message.MessageBuilder()
                    .from(this.nodeID)
                    .to(outgoingNode)
                    .type(msgType)
                    .build();
            Msg.sendMsg(Msg, broadcast_hostname + ".utdallas.edu", broadcast_port);
        }
    }

    public void listen() throws IOException {
        NodeListener listener = new NodeListener(this, this.port);
        Thread listener_thread = new Thread(listener);
        listener_thread.start();
    }

    public void appMsg() {
        AppMsg appMsg = new AppMsg(this);
        Thread appMsg_thread = new Thread(appMsg);
        appMsg_thread.start();
    }


    public void outputFile(String filename, long startTime, long endTime) {
        try {
            File file = new File(filename + this.nodeID + ".out");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file.getName(), true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            // bufferedWriter.write(startTime + "---" + endTime + ": " + (endTime - startTime));
            bufferedWriter.write(Objects.toString(endTime - startTime));
            bufferedWriter.write("\n");
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void outputFile(String filename, String startTime, String endTime) {
        try {
            File file = new File(filename + this.nodeID + ".out");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file.getName(), true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(startTime + "---" + endTime);
            bufferedWriter.write("\n");
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void outputMsgCount(int msgCount) {
        try {
            File file = new File("MessageComlexity" + this.nodeID + ".out");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file.getName(), true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write("msgCount: " + msgCount);
            bufferedWriter.write("\n");
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void receiveMsg(Message msg) throws InterruptedException {
        switch (msg.getType()) {
            case "APPLICATION":
                System.out.println("------ application message from " + msg.getSender() + " I have sent request: " + this.request_sent);
                this.scalar_clock.set(Math.max(msg.getClock(), this.scalar_clock.get()) + 1);
                System.out.println("scalar clock now is: " + this.scalar_clock);
                break;
            case "REPLY":
                System.out.println("------ reply message from " + msg.getSender() + " I have sent request: " + this.request_sent);
                this.replyRecevied.addAndGet(1);
                break;
            case "REQUEST":
                System.out.println("------ request message from " + msg.getSender() + " I have sent request: " + this.request_sent);
                this.q.offer(new Request(msg.getClock(), msg.getSender()));
                String hostname = this.NodeInfoList.get(msg.getSender()).get("hostname");
                int port = Integer.parseInt(this.NodeInfoList.get(msg.getSender()).get("port"));
                Message Msg = new Message.MessageBuilder()
                        .from(this.nodeID)
                        .to(msg.getSender())
                        .type("REPLY")
                        .build();
                this.messageCounter.addAndGet(1);
                Msg.sendMsg(Msg, hostname + ".utdallas.edu", port);
                break;
            case "RELEASE":
                System.out.println("------ release message from " + msg.getSender() + " I have sent request: " + this.request_sent);
                // remove request from q based on node id ignore the clock
                q.removeIf(r -> r.nodeID == msg.getSender());
                break;
            case "TERMINATION":
                System.out.println("------ termination message from " + msg.getSender());
                this.termination.addAndGet(1);
                break;
            case "TEST":
                System.out.println("------ test message from " + msg.getSender());
                String test_hostname = this.NodeInfoList.get(msg.getSender()).get("hostname");
                int test_port = Integer.parseInt(this.NodeInfoList.get(msg.getSender()).get("port"));
                Message testMsg = new Message.MessageBuilder()
                        .from(this.nodeID)
                        .to(msg.getSender())
                        .type("REPLYTEST")
                        .content(Boolean.toString(this.ifInCS))
                        .build();
                testMsg.sendMsg(testMsg, test_hostname + ".utdallas.edu", test_port);
                break;
            case "REPLYTEST":
                System.out.println("------ reply test message from " + msg.getSender());
                String reply = msg.getContent();
                this.replyTest[msg.getSender()] = reply;
                int count = 0;
                for (int i = 0; i < this.replyTest.length; i++) {
                    if (this.replyTest[i] != null) count++;
                }
                if (count == this.nodeNum - 1) {
                    System.out.println("=-=-=-===-=-=-=-=-print result of test reply-=-=-=-=-=");
                    for (String test : this.replyTest) {
                        if (test != null) System.out.println(test);
                    }
                    this.replyTest = new String[this.nodeNum];
                }

        }
    }

    @Override
    public void run() {
        try {
            listen();
            appMsg();
            // 调整进程运行开始时间，方式两个进行时间戳相同
            Thread.sleep(5000);
            while (true) {
                Random rand = new Random();
                // int decider = (int) (10 * Math.random()) % 2;
                int decider = rand.nextInt(101 - 1) + 1;
                if (this.request_sent == this.numRequests) {
                    System.out.println("I'm done: " + this.nodeID);
                    long theLastRequestTime = System.currentTimeMillis();
                    outputFile("Throughput", this.theFirstRequestTime, theLastRequestTime);
                    this.termination.addAndGet(1);
                    broadcast("TERMINATION");
                    return;
                } else {
                    if (this.theFirstRequestTime == 0) this.theFirstRequestTime = System.currentTimeMillis();
                    this.application.cs_enter();
                    this.request_sent++;
                }
//                else if (decider >= 1 && decider <= 90) {
//                    this.scalar_clock.addAndGet(1);
//                    broadcast("APPLICATION", this.scalar_clock.get());
//                } else {
//                    this.application.cs_enter();
//                    this.request_sent++;
//                }
                Thread.sleep(this.interRequestDelay);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Node node = new Node();
        Thread node_thread = new Thread(node);
        System.out.println("node thread started ...");
        node_thread.start();

//        String[] test = new String[2];
//        test[0] = "a";
//        test[1] = "b";
//        System.out.println(test.toString());
//        for (String a : test) {
//            System.out.println(a);
//        }
//        test = new String[2];
//        System.out.println(test.length);
//        for (String b : test) {
//            System.out.println(b);
//        }


//        Comparator<Request> requestComparator = Comparator.comparing(Request::get_timestamp);
//        PriorityBlockingQueue<Request> q = new PriorityBlockingQueue<>(50, requestComparator);
//
//        Request r1 = new Request(9, 1);
//        Request r2 = new Request(4, 1);
//        Request r3 = new Request(2, 1);
//        Request r4 = new Request(90, 1);
//        Request r5 = new Request(1, 1);
//        Request r6 = new Request(56, 1);
//
//        q.offer(r1);
//        q.offer(r2);
//        q.offer(r3);
//        q.offer(r4);
//        q.offer(r5);
//        q.offer(r6);
//
//        q.removeIf(r -> r.timestamp == 4);
//
////        Iterator q_iterator = q.iterator();
////        while (q_iterator.hasNext()) {
////            Request n = (Request) q_iterator.next();
////            System.out.println("ts: " + n.timestamp + " id: " + n.nodeID);
////
////       }
//        try {
//            while (true) {
//                Request request = q.take();
//                System.out.println("ts: " + request.timestamp + " id: " + request.nodeID);
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

    }

}

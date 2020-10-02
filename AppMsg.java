
public class AppMsg implements Runnable {
    Node node;
    int count;

    public AppMsg(Node node) {
        this.node = node;
    }

    @Override
    public void run() {
        while (true) {
            if (this.node.termination.get() == this.node.nodeNum) {
                if (count == 1) return;
                count++;
            }
            this.node.scalar_clock.addAndGet(1);
            this.node.broadcast("APPLICATION", this.node.scalar_clock.get());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

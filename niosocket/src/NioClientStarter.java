import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class NioClientStarter {
    public static void main(String[] args){
        Thread client1 = new Thread(new NioClient("localhost",8088, "order time"));
        Thread client2 = new Thread(new NioClient("localhost",8088, "order name"));
        client1.start();
        client2.start();
        try{
            client1.join();
            client2.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        System.out.println("all clients have closed.");
    }

    static class NioClient implements Runnable{

        private String host;

        private int port;

        private Selector selector;

        private volatile boolean stop = false;

        private String order;

        NioClient(String host, int port, String order){
            this.order = order;
            this.host = host == null?"localhost":host;
            this.port = port;
            try{
                selector = Selector.open();
            }catch (IOException e){
                e.printStackTrace();
                System.exit(1);
            }
        }

        @Override
        public void run() {
            try{
                doConnect();
            }catch (IOException e){
                e.printStackTrace();
                System.exit(1);
            }
            while (!stop){
                try{
                    selector.select(1000);
                    Set<SelectionKey> keys = selector.selectedKeys();
                    SelectionKey key;
                    Iterator<SelectionKey> iterator = keys.iterator();
                    while(iterator.hasNext()){
                        key = iterator.next();
                        iterator.remove();
                        try{
                            handleInput(key);
                        }catch (IOException e){
                            if(key != null){
                                key.cancel();
                                if(key.channel() != null)
                                    key.channel().close();
                            }
                        }
                    }
                }catch (IOException e){
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            if(selector != null) {
                try {
                    Thread.sleep(10*1000); // 10s
                    selector.close();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleInput(SelectionKey key) throws IOException {
            if(key.isValid()){
                SocketChannel sc = (SocketChannel) key.channel();
                if(key.isConnectable()){
                    if(sc.finishConnect()){
                        System.out.println("client has connected to server.");
                        sc.register(selector, SelectionKey.OP_READ);
                        writeRequest(sc);
                    }else System.exit(1);
                }
                if(key.isReadable()){
                    ByteBuffer byteBuffer = ByteBuffer.allocate(512);
                    int readBytes = sc.read(byteBuffer);
                    if(readBytes > 0){
                        byteBuffer.flip();
                        byte[] bytes = new byte[byteBuffer.remaining()];
                        byteBuffer.get(bytes);
                        String ans = new String(bytes, StandardCharsets.UTF_8);
                        System.out.println("client get answer: "+ans);
                        this.stop = true;
                    }else if(readBytes < 0){
                        key.cancel();
                        sc.close();
                    }
                }
            }
        }

        private void doConnect() throws IOException {
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            if(sc.connect(new InetSocketAddress(host,port))) {
                System.out.println("client has connected to server.");
                sc.register(selector, SelectionKey.OP_READ);
                writeRequest(sc);
            }
            else {
                System.out.println("client try to connect to server.");
                sc.register(selector, SelectionKey.OP_CONNECT);
            }
        }

        private void writeRequest(SocketChannel sc) throws IOException {
            byte[] bytes = order.getBytes();
            ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
            byteBuffer.put(bytes);
            byteBuffer.flip();
            sc.write(byteBuffer);
            if(!byteBuffer.hasRemaining())
                System.out.println("send message to server succeed.");
        }
    }
}

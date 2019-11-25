import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class NioServerStarter {
    public static void main(String[] args){
        int port = 8088;
        // re-use-port option
        NioServer nioServer1 = new NioServer(port, "NIO-Server-1");
        NioServer nioServer2 = new NioServer(port, "NIO-Server-2");
        new Thread(nioServer1).start();
        new Thread(nioServer2).start();
        try {
            Thread.sleep(30*1000); // 30s
        }catch (InterruptedException e){
            e.printStackTrace();
        }finally {
            System.out.println("close server manually.");
            nioServer1.setStop();
            nioServer2.setStop();
        }
    }

    static class NioServer implements Runnable{

        private String name;

        private Selector selector;

        private volatile boolean stop = false;

        NioServer (int port, String name){
            this.name = name;
            try{
                selector = Selector.open();
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(false);
                // re-use-port option
                serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEPORT, true);
                serverSocketChannel.bind(new InetSocketAddress(port),1024);
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                System.out.println("server [" +name+"] is open on port: "+port+" waiting for connection...");
            }catch (IOException e){
                e.printStackTrace();
                System.exit(1);
            }
        }

        void setStop(){
            this.stop = true;
        }

        @Override
        public void run() {
            while(!stop){
                try{
                    selector.select(1000);
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    SelectionKey key;
                    while(iterator.hasNext()){
                        key = iterator.next();
                        iterator.remove();
                        try{
                            handleInput(key);
                        }catch (Exception e){
                            if(key != null){
                                key.cancel();
                                if(key.channel() != null)
                                    key.channel().close();
                            }
                        }
                    }
                }catch (Throwable e){
                    e.printStackTrace();
                }
            }
            if(selector != null){
                try {
                    selector.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }

        private void handleInput (SelectionKey key) throws IOException {
            if(key.isValid()){
                if(key.isAcceptable()){
                    SocketChannel sc = ((ServerSocketChannel)key.channel()).accept();
                    sc.configureBlocking(false);
                    sc.register(selector,SelectionKey.OP_READ);
                }
                if(key.isReadable()){
                    SocketChannel sc = (SocketChannel) key.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int readBytes = sc.read(byteBuffer);
                    if(readBytes > 0){
                        byteBuffer.flip();
                        byte[] bytes = new byte[byteBuffer.remaining()];
                        byteBuffer.get(bytes);
                        String str = new String(bytes, StandardCharsets.UTF_8);
                        System.out.println("server ["+name+"] received order: "+str);
                        String ans;
                        switch (str){
                            case "order time" :
                                ans = new Date().toString();
                                break;
                            case "order name" :
                                ans = "chorifa";
                                break;
                            default:
                                ans = "unsupported order";
                        }
                        writeResponse(sc,ans);
                    }else if(readBytes < 0){
                        key.cancel();
                        //TODO if not close . CLOSE_WAIT
                        sc.close();  // what if not close?
                    }// 0 忽略
                }
            }
        }

        private void writeResponse(SocketChannel sc, String ans) throws IOException {
            if(ans != null && ans.length()>0){
                byte[] bytes = ans.getBytes();
                ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
                byteBuffer.put(bytes);
                byteBuffer.flip();
                sc.write(byteBuffer);
            }
        }
    }
}

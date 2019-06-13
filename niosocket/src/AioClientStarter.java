import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public class AioClientStarter {

    public static void main(String[] args){
        new AioClient("localhost",8088, "order time").doConnect();
        new AioClient("localhost",8088, "order name").doConnect();
        System.out.println("all client has started async.");
    }

    // connect 要求result泛型为Void,Void表示只能为null
    static class AioClient implements CompletionHandler<Void,AioClient>{

        private AsynchronousSocketChannel channel;

        private String host;

        private String order;

        private int port;

        private CountDownLatch countDownLatch;

        AioClient(String host, int port, String order){
            this.host = host;
            this.port = port;
            this.order = order;
            try{
                channel = AsynchronousSocketChannel.open();
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        void doConnect(){
            countDownLatch = new CountDownLatch(1);
            System.out.println("client try to async connect.");
            channel.connect(new InetSocketAddress(host,port),this,this);
            try{
                countDownLatch.await();
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            try{
                channel.close();
                System.out.println("client closed.");
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void completed(Void result, AioClient attachment) {
            System.out.println("client connect completed.");
            byte[] req = order.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
            writeBuffer.put(req);
            writeBuffer.flip();
            System.out.println("client try to async write order...");
            channel.write(writeBuffer, writeBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    System.out.println("client write complete.");
                    if(attachment.hasRemaining()){ // continue to write
                        channel.write(attachment,attachment,this);
                    }else{ // read ans
                        ByteBuffer readBuffer = ByteBuffer.allocate(512);
                        channel.read(readBuffer, readBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                            @Override
                            public void completed(Integer result, ByteBuffer attachment) {
                                System.out.println("client read completed.");
                                attachment.flip();
                                byte[] bytes = new byte[attachment.remaining()];
                                attachment.get(bytes);
                                String ans = new String(bytes, StandardCharsets.UTF_8);
                                System.out.println("client received answer: "+ans);
                                countDownLatch.countDown();
                            }

                            @Override
                            public void failed(Throwable exc, ByteBuffer attachment) {
                                try{
                                    channel.close();
                                    countDownLatch.countDown();
                                }catch (IOException e){
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    try{
                        channel.close();
                        countDownLatch.countDown();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void failed(Throwable exc, AioClient attachment) {
            try{
                channel.close();
                countDownLatch.countDown();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

}

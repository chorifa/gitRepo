import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class AioServerStarter {

    public static void main(String[] args){
        AioServer server = new AioServer(8088);
        server.doAccept();
        System.out.println("try to start async server...");
        try {
            Thread.sleep(10000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        server.close();
        System.out.println("server is closed...");
    }

    static class AioServer{
        AsynchronousServerSocketChannel asynchronousServerSocketChannel;

        AioServer(int port){
            try{
                asynchronousServerSocketChannel = AsynchronousServerSocketChannel.open();
                asynchronousServerSocketChannel.bind(new InetSocketAddress(port));
                System.out.println("server start in port: "+port);
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        void doAccept(){
            System.out.println("try to async accept connection.");
            asynchronousServerSocketChannel.accept(this, new AfterAccept());
        }

        void doAccept(AfterAccept afterAccept){
            System.out.println("try to async accept connection.");
            asynchronousServerSocketChannel.accept(this, afterAccept);
        }

        void close() {
            if(asynchronousServerSocketChannel != null) {
                try {
                    asynchronousServerSocketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // accept方法要求泛型为<AsynchronousSocketChannel,AioServer>
    static class AfterAccept implements CompletionHandler<AsynchronousSocketChannel,AioServer> {

        @Override
        public void completed(AsynchronousSocketChannel result, AioServer attachment) {
            System.out.println("accept connection completed.");
            // continue to accept connection request.
            attachment.doAccept(this);

            ByteBuffer byteBuffer = ByteBuffer.allocate(512);
            System.out.println("try to async read request.");
            result.read(byteBuffer,byteBuffer,new AfterReader(result));
        }

        @Override
        public void failed(Throwable exc, AioServer attachment) {
            exc.printStackTrace();
        }
    }

    // read方法需要返回Integer
    static class AfterReader implements CompletionHandler<Integer,ByteBuffer>{

        private AsynchronousSocketChannel asynchronousSocketChannel;

        AfterReader(AsynchronousSocketChannel channel){
            asynchronousSocketChannel = channel;
        }

        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            System.out.println("read request completed.");
            attachment.flip();
            byte[] bytes = new byte[attachment.remaining()];
            attachment.get(bytes);
            String req = new String(bytes, StandardCharsets.UTF_8);
            System.out.println("server received order: "+req);
            String ans;
            switch (req){
                case "order time" :
                    ans = new Date().toString();
                    break;
                case "order name" :
                    ans = "chorifa";
                    break;
                default:
                    ans = "unsupported order";
            }
            writeResponse(ans);
        }

        private void writeResponse(String ans) {
            if(ans != null && ans.length()>0){
                byte[] bytes = ans.getBytes();
                ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
                byteBuffer.put(bytes);
                byteBuffer.flip();
                System.out.println("try to async write ans: "+ ans);
                asynchronousSocketChannel.write(byteBuffer, byteBuffer, new AfterWrite(asynchronousSocketChannel));
            }
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
            try{
                this.asynchronousSocketChannel.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    static class AfterWrite implements CompletionHandler<Integer, ByteBuffer>{

        private AsynchronousSocketChannel asynchronousSocketChannel;

        AfterWrite(AsynchronousSocketChannel channel){
            asynchronousSocketChannel = channel;
        }

        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            System.out.println("async write completed.");
            if(attachment.hasRemaining())
                asynchronousSocketChannel.write(attachment,attachment,this); // async write
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
            try{
                this.asynchronousSocketChannel.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}

package socketdemo;

/**
 * Hello world!
 *
 */
public class App {
    public static void main( String[] args ) throws Exception {
        System.out.println( "Hello World!" );
        final NettyNioServer nioServer = new NettyNioServer();
        Thread server = new Thread(()->{
            try{
                System.out.println("server init.");
                nioServer.init(8088);
            }catch (Exception e){
                e.printStackTrace();
            }
        });
        server.start();
        Thread.sleep(1000);
        NettyNioClient client = new NettyNioClient();
        Thread client1 = new Thread(()->{
            try{
                System.out.println("client connect...");
                client.connect(8088,"localhost","order time");
            }catch (Exception e){
                e.printStackTrace();
            }
        });
        Thread client2 = new Thread(()->{
            try{
                System.out.println("client connect...");
                client.connect(8088,"localhost","order name");
            }catch (Exception e){
                e.printStackTrace();
            }
        });
        try{
            client1.start();
            client2.start();
            client1.join();
            client2.join();
            server.join();
            Thread.sleep(60*1000);
        }finally {
            client.getGroup().shutdownGracefully();
            nioServer.shutdown();
        }
    }
}

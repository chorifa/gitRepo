package socketdemo;

/**
 * Hello world!
 *
 */
public class App {
    public static void main( String[] args ) throws Exception {
        System.out.println( "Hello World!" );
        Thread server = new Thread(()->{
            try{
                System.out.println("server init.");
                new NettyNioServer().init(8088);
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
        client1.start();
        client2.start();
        client1.join();
        client2.join();
        server.join();
    }
}

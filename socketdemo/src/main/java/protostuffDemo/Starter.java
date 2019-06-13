package protostuffDemo;

public class Starter {
    public static void main( String[] args ) throws Exception {
        System.out.println( "Hello World!" );
        Thread server = new Thread(()->{
            try{
                System.out.println("server init.");
                new Server().init(8088);
            }catch (Exception e){
                e.printStackTrace();
            }
        });
        server.start();
        Thread.sleep(1000);
        Thread client1 = new Thread(()->{
            try{
                System.out.println("client connect...");
                new Client().connect(8088,"localhost");
            }catch (Exception e){
                e.printStackTrace();
            }
        });
        Thread client2 = new Thread(()->{
            try{
                System.out.println("client connect...");
                new Client().connect(8088,"localhost");
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

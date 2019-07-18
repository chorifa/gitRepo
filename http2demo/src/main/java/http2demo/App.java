package http2demo;

import http2demo.server.Http2Server;

import java.util.concurrent.TimeUnit;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );

        Http2Server server = new Http2Server();

        try {
            server.start();
            TimeUnit.MINUTES.sleep(1);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            server.stop();
        }

    }
}

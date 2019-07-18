package http2demo.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public final class Http2Server {

    static final boolean SSL = true;

    static final int PORT = SSL? 8443 : 8080;

    private Thread thread;

    public void start(){

        thread = new Thread(()->{

            final SslContext sslCtx;
            EventLoopGroup bossGroup = null;
            EventLoopGroup workGroup = null;

            try {
                if (SSL) {
                    // 使用JDK原生实现，JDK9后(含)提供支持
                    SslProvider provider = SslProvider.JDK;

                    SelfSignedCertificate ssc = new SelfSignedCertificate();
                    sslCtx = SslContextBuilder.forServer(ssc.certificate(),ssc.privateKey())
                            .sslProvider(provider)
                            .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                            .applicationProtocolConfig(new ApplicationProtocolConfig(
                                    ApplicationProtocolConfig.Protocol.ALPN,
                                    ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                    ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                    ApplicationProtocolNames.HTTP_2,
                                    ApplicationProtocolNames.HTTP_1_1))
                            .build();
                }else sslCtx = null;

                bossGroup = new NioEventLoopGroup();
                workGroup = new NioEventLoopGroup();

                ServerBootstrap sbs = new ServerBootstrap();
                sbs.option(ChannelOption.SO_BACKLOG,1024);
                sbs.group(bossGroup,workGroup).channel(NioServerSocketChannel.class)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(new Http2ServerInitializer(sslCtx));

                Channel ch = sbs.bind(PORT).sync().channel();

                System.err.println("Open your HTTP/2-enabled web browser and navigate to " +
                        (SSL? "https" : "http") + "://127.0.0.1:" + PORT + '/');

                ch.closeFuture().sync();

            }catch (Exception e){
                if(e instanceof InterruptedException)
                    System.out.println("interrupted >>> shut down...");
                else
                    System.err.println("other exception!!!");
            }finally {
                if(bossGroup != null)
                    bossGroup.shutdownGracefully();
                if(workGroup != null)
                    workGroup.shutdownGracefully();
            }

        });

        thread.start();

    }

    public void stop(){
        if(thread != null && thread.isAlive())
            thread.interrupt();
    }

}

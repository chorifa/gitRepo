package http2demo.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

import java.util.concurrent.TimeUnit;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class Http2Client {

    static final boolean SSL = false;
    static final String HOST = "localhost";
    static final int PORT = SSL?8443:8080;
    static final String URL = "/get";
    static final String URL2 = "/post";
    static final String URL2DATA = "test data!";

    public static void main(String[] args) throws Exception{
        final SslContext sslCtx;
        if(SSL){
            SslProvider provider = SslProvider.JDK;
            sslCtx = SslContextBuilder.forClient()
                    .sslProvider(provider)
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            ApplicationProtocolConfig.Protocol.ALPN,
                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2,
                            ApplicationProtocolNames.HTTP_1_1))
                    .build();
        }else sslCtx = null;

        EventLoopGroup workGroup = new NioEventLoopGroup();
        Http2ClientInitializer initializer = new Http2ClientInitializer(sslCtx,Integer.MAX_VALUE);

        try{
            Bootstrap bs = new Bootstrap();
            bs.group(workGroup);
            bs.channel(NioSocketChannel.class);
            bs.option(ChannelOption.SO_KEEPALIVE,true);
            bs.remoteAddress(HOST,PORT);
            bs.handler(initializer);

            Channel channel = bs.connect().sync().channel();
            System.out.println("Connected to [" + HOST + ':' + PORT + ']');

            Http2SettingsHandler http2SettingsHandler = initializer.settingsHandler();
            http2SettingsHandler.awaitSettings(5, TimeUnit.SECONDS);

            HttpResponseHandler responseHandler = initializer.responseHandler();
            int streamId = 3;
            HttpScheme scheme = SSL ? HttpScheme.HTTPS : HttpScheme.HTTP;
            AsciiString hostName = new AsciiString(HOST + ':' + PORT);
            System.err.println("Sending request(s)...");

            // send get
            FullHttpRequest getReq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,URL);
            getReq.headers().setInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), streamId);
            getReq.headers().add(HttpHeaderNames.HOST,hostName);
            getReq.headers().add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), scheme.name());
            getReq.headers().add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
            getReq.headers().add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.DEFLATE);
            responseHandler.put(streamId,channel.write(getReq),channel.newPromise());

            streamId += 4;
            FullHttpRequest postReq = new DefaultFullHttpRequest(HTTP_1_1, POST, URL2,
                    wrappedBuffer(URL2DATA.getBytes(CharsetUtil.UTF_8)));
            postReq.headers().add(HttpHeaderNames.HOST, hostName);
            postReq.headers().add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), scheme.name());
            postReq.headers().add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
            postReq.headers().add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.DEFLATE);
            postReq.headers().setInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), streamId);
            responseHandler.put(streamId,channel.write(postReq),channel.newPromise());

            channel.flush();
            responseHandler.awaitAllResponses(5,TimeUnit.SECONDS);
            System.out.println("Finished HTTP/2 request");

            channel.closeFuture().sync();
        }finally {
            workGroup.shutdownGracefully();
        }

    }

}

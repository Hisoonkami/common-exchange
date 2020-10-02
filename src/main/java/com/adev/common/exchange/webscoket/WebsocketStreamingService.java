package com.adev.common.exchange.webscoket;

import com.adev.common.exchange.ProductSubscription;
import com.adev.common.exchange.exception.NotConnectedException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.IdleStateHandler;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class WebsocketStreamingService<T> implements WebSocketClientHandler.WebSocketMessageHandler {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(60);

    private static final Duration DEFAULT_RETRY_DURATION = Duration.ofSeconds(10);

    private static final Duration LAST_RETRY_DURATION = Duration.ofSeconds(10);

    private boolean retrying = false;

    /**
     * //开始连接就失败，重试重连次数最大重试5次
     */
    private static int RETRY_COUNT_MAX = 5;

    /**
     * //重连记数
     */
    private static AtomicInteger retry_counter = new AtomicInteger(0);

    public class Subscription {
        final ObservableEmitter<T> emitter;

        final String channelName;

        final Object[] args;

        public Subscription(ObservableEmitter<T> emitter, String channelName, Object[] args) {
            this.emitter = emitter;
            this.channelName = channelName;
            this.args = args;
        }
    }

    private final int maxFramePayloadLength;

    private final URI uri;

    private boolean isManualDisconnect = false;

    private Channel webSocketChannel;

    private Duration retryDuration;

    private Duration connectionTimeout;

    private final NioEventLoopGroup eventLoopGroup;

    protected Map<String, Subscription> channels = new ConcurrentHashMap<>();

    private boolean compressedMessages = false;

    private String currentCollect = null;

    public WebsocketStreamingService(String apiUrl) {
        this(apiUrl, 65536);
    }

    public WebsocketStreamingService(String apiUrl, int maxFramePayloadLength) {
        this(apiUrl, maxFramePayloadLength, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_RETRY_DURATION);
    }

    public WebsocketStreamingService(String apiUrl, int maxFramePayloadLength, Duration connectionTimeout,
                                 Duration retryDuration) {
        try {
            this.maxFramePayloadLength = maxFramePayloadLength;
            this.retryDuration = retryDuration;
            this.connectionTimeout = connectionTimeout;
            this.uri = new URI(apiUrl);
            this.eventLoopGroup = new NioEventLoopGroup();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Error parsing URI " + apiUrl, e);
        }
    }

    public Completable connect(ProductSubscription... args) {

        if(args != null && args.length > 0){

            ProductSubscription productSubscription = args[0];
            currentCollect = productSubscription.getCurrentCollect();
        }

        return Completable.create(completable -> {
            try {
                String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();

                String host = uri.getHost();
                if (host == null) {
                    throw new IllegalArgumentException("Host cannot be null.");
                }

                final int port;
                if (uri.getPort() == -1) {
                    if ("ws".equalsIgnoreCase(scheme)) {
                        port = 80;
                    } else if ("wss".equalsIgnoreCase(scheme)) {
                        port = 443;
                    } else {
                        port = -1;
                    }
                } else {
                    port = uri.getPort();
                }

                if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
                    throw new IllegalArgumentException("Only WS(S) is supported.");
                }


                if(currentCollect != null && CollectionCache.collectStatusMap.get(currentCollect) != null && CollectionCache.collectStatusMap.get(currentCollect).equals(CollectionCache.ConnectionState.CONNECTED)){
                    return;
                }

                LOG.info("Connecting to {}://{}:{}{}", scheme, host, port, uri.getPath());

                final boolean ssl = "wss".equalsIgnoreCase(scheme);
                final SslContext sslCtx;
                if (ssl) {
                    sslCtx = SslContextBuilder.forClient().build();
                } else {
                    sslCtx = null;
                }

                HttpHeaders httpHeaders = new DefaultHttpHeaders();
                addHeader(httpHeaders);

                String subprotocol = getSubprotocol();

                final WebSocketClientHandler handler =
                        getWebSocketClientHandler(WebSocketClientHandshakerFactory.newHandshaker(uri,
                                WebSocketVersion.V13, subprotocol, true, httpHeaders, maxFramePayloadLength), this, currentCollect);

                Bootstrap b = new Bootstrap();
                b.group(eventLoopGroup)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                                java.lang.Math.toIntExact(connectionTimeout.toMillis()))
                        //当缓存不够用时isWriteable返回false，这里设置大一点
                        .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(64 * 1024, 192 * 1024))
                        .option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.TCP_NODELAY, true)
                        .channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast("ping", new IdleStateHandler(3 * 60, 5, 60 * 60, TimeUnit.SECONDS));
                        if (sslCtx != null) {
                            p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                        }

                        WebSocketClientExtensionHandler clientExtensionHandler =
                                getWebSocketClientExtensionHandler();
                        List<ChannelHandler> handlers = new ArrayList<>(4);
                        handlers.add(new HttpClientCodec());
                        if (compressedMessages) {
                            handlers.add(WebSocketClientCompressionHandler.INSTANCE);
                        }

                        handlers.add(new HttpObjectAggregator(8192));

                        if (clientExtensionHandler != null) {
                            handlers.add(clientExtensionHandler);
                        }

                        handlers.add(handler);
                        p.addLast(handlers.toArray(new ChannelHandler[handlers.size()]));
                    }
                });

                b.connect(uri.getHost(), port).addListener((ChannelFuture future) -> {
                    webSocketChannel = future.channel();

                    if (future.isSuccess()) {
                        handler.handshakeFuture().addListener(f -> {
                            if (f.isSuccess()) {
                                completable.onComplete();
                            } else {
                                completable.onError(f.cause());
                            }
                        });
                    } else {
                        if (retry_counter.getAndIncrement() < RETRY_COUNT_MAX) {
                            final EventLoop loop = webSocketChannel.eventLoop();
                            loop.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    LOG.info("{} Server can't connect, start to reconnect.", exName());

                                    if(currentCollect != null && CollectionCache.collectStatusMap.get(currentCollect) != null && CollectionCache.collectStatusMap.get(currentCollect).equals(CollectionCache.ConnectionState.CONNECTING)){
                                        return;
                                    }

                                    if(currentCollect != null){

                                        CollectionCache.collectStatusMap.put(currentCollect, CollectionCache.ConnectionState.CLOSED_ON_ERROR);
                                    }

                                }
                            }, retryDuration.toMillis(), TimeUnit.MILLISECONDS);
                        } else {
                            completable.onError(future.cause());
                        }

                    }

                });
            } catch (Exception throwable) {
                completable.onError(throwable);
            }
        });
    }

    public Completable disconnect() {
        isManualDisconnect = true;
        return Completable.create(completable -> {
            if (webSocketChannel.isOpen()) {
                CloseWebSocketFrame closeFrame = new CloseWebSocketFrame();
                webSocketChannel.writeAndFlush(closeFrame).addListener(future -> {
                    channels = new ConcurrentHashMap<>();
                    completable.onComplete();
                });
            }
        });
    }

    protected abstract String getChannelNameFromMessage(T message) throws IOException;

    public abstract String getSubscribeMessage(String channelName, Object... args) throws IOException;

    public abstract String getUnsubscribeMessage(String channelName) throws IOException;

    public String getSubscriptionUniqueId(String channelName, Object... args) {
        return channelName;
    }

    /**
     * Handler that receives incoming messages.
     *
     * @param message Content of the message from the server.
     */
    public abstract void messageHandler(String message);

    @Override
    public void onMessage(String message) {
        messageHandler(message);
    }

    @Override
    public boolean isDeflate() {
        return false;
    }

    @Override
    public boolean isGzip() {
        return false;
    }

    public void sendMessage(String message) {
        LOG.debug("Sending message: {} to WebSocket[{}]", message, uri.getHost());

        if (webSocketChannel == null || !webSocketChannel.isOpen()) {
            webSocketChannel.close();
            webSocketChannel.disconnect();
            LOG.warn("WebSocket[{}] is not open! Call connect first. {} not sent", uri.getHost(), message);
            return;
        }

        if (!webSocketChannel.isWritable()) {
            LOG.warn("Cannot send data to WebSocket[{}] because WebSocketChannel is not writable.", uri.getHost());
            return;
        }

        if (message != null) {
            WebSocketFrame frame = new TextWebSocketFrame(message);
            webSocketChannel.writeAndFlush(frame);
        }
    }

    public Observable<T> subscribeChannel(String channelName, Object... args) {
        final String channelId = getSubscriptionUniqueId(channelName, args);
        LOG.info("Subscribing to WebSocket[{}], channelId={}", uri.getHost(), channelId);

        return Observable.<T> create(e -> {
            if (webSocketChannel == null || !webSocketChannel.isOpen()) {
                e.onError(new NotConnectedException());
            }

            if (!channels.containsKey(channelId)) {
                Subscription newSubscription = new Subscription(e, channelName, args);
                channels.put(channelId, newSubscription);
                try {

                    String message = getSubscribeMessage(channelName, args);
                    if(StringUtils.isNotEmpty(message)){

                        sendMessage(message);
                    }
                } catch (IOException throwable) {
                    e.onError(throwable);
                }
            }
        }).doOnDispose(() -> {
            if (channels.containsKey(channelId)) {
                String message = getUnsubscribeMessage(channelId);
                if(StringUtils.isNotEmpty(message)){

                    sendMessage(message);
                }
                channels.remove(channelId);
            }
        }).share();
    }

    public void resubscribeChannels() {
        for (String channelId : channels.keySet()) {
            try {
                Subscription subscription = channels.get(channelId);
                String msg = getSubscribeMessage(subscription.channelName, subscription.args);

                if(StringUtils.isNotEmpty(msg)){

                    sendMessage(msg);
                    LOG.info("Resubscribing WebSocket[{}], send message: {}", uri.getHost(), msg);
                }
            } catch (IOException e) {
                LOG.error("Failed to reconnect WebSocket[{}] msg={}", uri.getHost(), channelId);
            }
        }
    }

    protected String getChannel(T message) {
        String channel;
        try {
            channel = getChannelNameFromMessage(message);
        } catch (IOException e) {
//            LOG.error("Cannot parse channel from WebSocket[{}] message: {}", uri.getHost(), message);
            return "";
        }
        return channel;
    }

    protected void handleMessage(T message) {
        String channel = getChannel(message);
        handleChannelMessage(channel, message);
    }

    protected void handleError(T message, Throwable t) {
        String channel = getChannel(message);
        handleChannelError(channel, t);
    }

    protected void handleChannelMessage(String channel, T message) {
        if(StringUtils.isEmpty(channel)){
            return;
        }

        Subscription subscription = channels.get(channel);
        if (subscription == null) {
            LOG.debug("No subscriber for channel[{}] {}.", uri.getHost(), channel);
            return;
        }
        ObservableEmitter<T> emitter = subscription.emitter;
        if (emitter == null) {
            LOG.debug("No emitter for channel[{}] {}.", uri.getHost(), channel);
            return;
        }else{

            if(message != null){

                emitter.onNext(message);
            }
        }

    }

    protected void handleChannelError(String channel, Throwable t) {
        if (!channel.contains(channel)) {
            LOG.error("WebSocket[{}], Unexpected channel's error: {}, {}.", uri.getHost(), channel, t);
            return;
        }
        ObservableEmitter<T> emitter = channels.get(channel).emitter;
        if (emitter == null) {
            LOG.debug("No subscriber for channel[{}] {}.", uri.getHost(), channel);
            return;
        }else{

            emitter.onError(t);
        }
    }

    protected void addHeader(HttpHeaders httpHeaders) {

    }

    protected String getSubprotocol() {
        return null;
    }

    protected WebSocketClientExtensionHandler getWebSocketClientExtensionHandler() {
        return WebSocketClientCompressionHandler.INSTANCE;
    }

    protected WebSocketClientHandler getWebSocketClientHandler(WebSocketClientHandshaker handshaker,
                                                               WebSocketClientHandler.WebSocketMessageHandler handler, String currentCollect) {
        return new NettyWebSocketClientHandler(handshaker, handler, currentCollect);
    }

    protected class NettyWebSocketClientHandler extends WebSocketClientHandler {
        protected NettyWebSocketClientHandler(WebSocketClientHandshaker handshaker, WebSocketMessageHandler handler, String currentCollect) {
            super(handshaker, handler, currentCollect);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (isManualDisconnect) {
                isManualDisconnect = false;
            } else {
                if(retrying){
                    return;
                }
                retrying = true;

                final EventLoop eventLoop = ctx.channel().eventLoop();
                eventLoop.schedule(() -> {

                    LOG.info("Reopening WebSocket[{}] because it was closed.", uri.getHost());
                    final Completable c = connect().doOnComplete(() -> {
                        LOG.info("Resubscribing channels to WebSocket[{}]", uri.getHost());
                        resubscribeChannels();
                    });

                    c.subscribe();

                    retrying = false;

                }, LAST_RETRY_DURATION.getSeconds(), TimeUnit.SECONDS);

                super.channelInactive(ctx);
            }
        }
    }

    public boolean isSocketOpen() {
        return webSocketChannel.isOpen();
    }

    public void useCompressedMessages(boolean compressedMessages) {
        this.compressedMessages = compressedMessages;
    }
}

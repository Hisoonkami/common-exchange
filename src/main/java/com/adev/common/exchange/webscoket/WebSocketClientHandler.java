package com.adev.common.exchange.webscoket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.zip.Inflater;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketClientHandler.class);

    public interface WebSocketMessageHandler {
        public void onMessage(String message);
        String exName();
        boolean isDeflate();
        boolean isGzip();
        boolean ping();
    }

    private final WebSocketClientHandshaker handshaker;

    private final WebSocketMessageHandler handler;

    private final String currentCollect;

    private ChannelPromise handshakeFuture;

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker, WebSocketMessageHandler handler, String currentCollect) {
        this.handshaker = handshaker;
        this.handler = handler;
        this.currentCollect = currentCollect;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOG.info("{}, WebSocket Client disconnected!", handler.exName());

//        try {
//			Thread.sleep(20000L);
//		} catch (Exception e) {
//		}
//        CollectionCache.collectStatusMap.put(handshaker.uri().toString(), false);
        if(currentCollect != null){

            CollectionCache.collectStatusMap.put(currentCollect, CollectionCache.ConnectionState.DELAY_CONNECT);
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();

        if (!handshaker.isHandshakeComplete()) {
            try {
                handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                LOG.info("{}, WebSocket Client connected!", handler.exName());
                handshakeFuture.setSuccess();

                if(currentCollect != null){

                    CollectionCache.collectStatusMap.put(currentCollect, CollectionCache.ConnectionState.CONNECTED);
                }

            } catch (WebSocketHandshakeException e) {
                LOG.error("{}, WebSocket Client failed to connect. {}", handler.exName(), e.getMessage());
                handshakeFuture.setFailure(e);
            }
            return;
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException("Unexpected FullHttpResponse (getStatus=" + response.status() + ", content="
                    + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            handler.onMessage(textFrame.text());
        } else if (frame instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame binaryFrame = (BinaryWebSocketFrame) frame;

            String s = null;
            if(handler.isDeflate()){
                s = uncompress(binaryFrame.content());
            }else if(handler.isGzip()){
                s = decodeByteBuff(binaryFrame.content());
            }else{
                ByteBuf buf = binaryFrame.content();
                byte[] temp = new byte[buf.readableBytes()];
                buf.readBytes(temp);
                s = new String(temp, "UTF-8");
            }

            if(s != null){
                handler.onMessage(s);
            }

        } else if (frame instanceof PingWebSocketFrame) {
            LOG.debug("{}, WebSocket Client received PingWebSocketFrame.", handler.exName());
            ctx.channel().write(
                    new PongWebSocketFrame(frame.content().retain()));
            LOG.debug("{}, WebSocket Client send PongWebSocketFrame.", handler.exName());

        } else if (frame instanceof PongWebSocketFrame) {
            LOG.debug("{}, WebSocket Client received pong.", handler.exName());

        } else if (frame instanceof CloseWebSocketFrame) {
            LOG.debug("{}, WebSocket Client received closing.", handler.exName());
            ch.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.debug("{}, exceptionCaught:", handler.exName(), cause);
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);

        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.READER_IDLE)) {
                LOG.debug("{}, Long time no server data has been received. 3 minute.", handler.exName());
            } else if (event.state().equals(IdleState.WRITER_IDLE)) {

//        		LOG.debug("{}, Not sending data to the server for a long time. 5 seconds.", handler.exName());
                //发送心跳包
//        		LOG.debug("{}, sending ping to server.", handler.exName());
                //ctx.writeAndFlush("{\"ping\":\"123456\"}");

                if(!handler.ping()){

                    ctx.writeAndFlush(new PingWebSocketFrame());
                }

            } else if (event.state().equals(IdleState.ALL_IDLE)) {
                LOG.debug("{}, have not received message and sending message for a long time.", handler.exName());
            }
        }
    }

    public String decodeByteBuff(ByteBuf buf) throws Exception {
        /* 使用了该解压的交易所： */
        byte[] temp = new byte[buf.readableBytes()];
        buf.readBytes(temp);

        try {
            temp = GZipUtils.decompress(temp);
        } catch (Exception e) {
            LOG.debug("{}, is not GZIP data, {}", handler.exName(), e.getMessage());
        }
        String str = new String(temp, "UTF-8");

        return str;
    }

    public String uncompress(ByteBuf byteBuf){
        /* 使用了该解压的交易所：okex */
        byte [] temp = new byte[byteBuf.readableBytes()];
        ByteBufInputStream bis = new ByteBufInputStream(byteBuf);
        StringBuilder appender = new StringBuilder();
        try {
            bis.read(temp);
            bis.close();
            Inflater infl = new Inflater(true);
            infl.setInput(temp,0,temp.length);
            byte [] result = new byte[1024];
            while (!infl.finished()){
                int length = infl.inflate(result);
                appender.append(new String(result,0,length,"UTF-8"));
            }
            infl.end();
        } catch (Exception e) {
            LOG.debug("{}, is not Inflater data, {}", handler.exName(), e.getMessage());
        }
        return appender.toString();
    }
}

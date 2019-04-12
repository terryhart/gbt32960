package com.ime.gbt32960.protocol;

import com.ime.gbt32960.codec.FrameHeader;
import com.ime.gbt32960.codec.GBT32960Message;
import com.ime.gbt32960.codec.ResponseMessage;
import com.ime.gbt32960.codec.ResponseTag;
import com.ime.iov.gbt32960.PlatformMessage;
import com.ime.iov.gbt32960.TerminalClockCorrect;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.Getter;

import java.time.Instant;

import static com.ime.gbt32960.codec.GBT32960Message.emptyResponse;
/**
 * @author Qingxi
 */

@ChannelHandler.Sharable
public class ProtocolHandler extends ChannelDuplexHandler {

    @Getter
    private static final ProtocolHandler instance = new ProtocolHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        GBT32960Message message = (GBT32960Message) msg;
        FrameHeader header = message.getHeader();
        switch (header.getRequestType()) {
            case LOGIN:
                emptyResponse(ctx, header, ResponseTag.SUCCESS, Instant.now().getEpochSecond());
                break;
            case HEARTBEAT:
                emptyResponse(ctx, header, ResponseTag.SUCCESS);
                break;
            case CLOCK_CORRECT:
                PlatformMessage correctMsg = PlatformMessage.newBuilder()
                        .setClockCorrect(TerminalClockCorrect.newBuilder()
                                .setSystemTime(Instant.now().getEpochSecond()).build()).build();
                ctx.write(new ResponseMessage(header.getVin(), correctMsg));
                break;
                default:
                    ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception{
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                ctx.close();
            }
        }
    }

}

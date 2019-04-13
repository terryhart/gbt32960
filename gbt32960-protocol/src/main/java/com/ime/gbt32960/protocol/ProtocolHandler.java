package com.ime.gbt32960.protocol;

import com.ime.gbt32960.codec.*;
import com.ime.iov.gbt32960.LoginRequest;
import com.ime.iov.gbt32960.PlatformMessage;
import com.ime.iov.gbt32960.ProtoResponse;
import com.ime.iov.gbt32960.TerminalClockCorrect;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZonedDateTime;

import static com.ime.gbt32960.codec.GBT32960Message.ZONE_UTC8;
import static com.ime.gbt32960.codec.GBT32960Message.emptyResponse;
/**
 * @author Qingxi
 */
@Slf4j
@ChannelHandler.Sharable
public class ProtocolHandler extends ChannelDuplexHandler {

    @Getter
    private static final ProtocolHandler instance = new ProtocolHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        GBT32960Message message = (GBT32960Message) msg;
        FrameHeader header = message.getHeader();
        switch (header.getRequestType()) {
            /*
            想要测试 登入不成功的情况，只需将 case LOGIN 这段代码注释掉
             */
            case LOGIN:
                LoginRequest login = (LoginRequest) message.getPayload();
                loginResponse(ctx, header, ResponseTag.SUCCESS, login);
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

    /**
     * 返回 登录成功应答
     * @param ctx ChannelHandlerContext
     * @param header 头部
     * @param tag 登录成功标志
     * @param loginRequest 登入消息
     */
    private void loginResponse(ChannelHandlerContext ctx, FrameHeader header, ResponseTag tag, LoginRequest loginRequest) {
        PlatformMessage message = PlatformMessage.newBuilder()
                .setProtoResponse(ProtoResponse.newBuilder()
                        .setMesssageType(RequestType.LOGIN.getValue())
                        .setResult(tag.getValue())
                        .setLogin(loginRequest)
                        .build())
                .build();
        log.info("返回登入成功消息: \n数据采集时间:{}\n{}", ZonedDateTime.ofInstant(Instant.ofEpochSecond(message.getProtoResponse().getLogin().getRecordTime()), ZONE_UTC8),message);
        log.info("{} 登入成功!", header.getVin());
        ctx.writeAndFlush(new ResponseMessage(header.getVin(), message));
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

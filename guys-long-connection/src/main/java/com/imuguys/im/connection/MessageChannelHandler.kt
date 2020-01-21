package com.imuguys.im.connection

import android.util.Log
import com.imuguys.im.connection.message.SocketJsonMessage
import com.imuguys.im.connection.utils.Gsons
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.util.concurrent.ConcurrentHashMap

/**
 * Json处理
 * 处理收到的Json，通知MessageHandler
 */
class MessageChannelHandler : SimpleChannelInboundHandler<SocketJsonMessage>() {

    companion object {
        private const val TAG = "MessageChannelHandler"
    }

    var mLongConnectionContext: LongConnectionContext? = null
    private val mSocketMessageHandlers = ConcurrentHashMap<String, SocketMessageListener<Any>>()

    /**
     * 连接断开，可能是由于服务端主动断开
     */
    override fun channelInactive(ctx: ChannelHandlerContext?) {
        mLongConnectionContext?.onRemoteDisconnectSubject!!.onNext(false)
        super.channelInactive(ctx)
    }

    /**
     * 收到读事件后的处理
     */
    override fun channelRead0(ctx: ChannelHandlerContext?, socketJsonMessage: SocketJsonMessage) {
        val innerMessage =
            Gsons.GUYS_GSON.fromJson(
                socketJsonMessage.classBytes,
                Class.forName(socketJsonMessage.classType)
            )
        val targetSocketJsonMessage = mSocketMessageHandlers[socketJsonMessage.classType]
        if (targetSocketJsonMessage != null) {
            targetSocketJsonMessage.handleMessage(innerMessage)
        } else {
            Log.w(TAG, "no compatible listener for message type: " + socketJsonMessage.classType)
        }
    }

    /**
     * 添加消息观察者
     */
    @Suppress("UNCHECKED_CAST")
    fun <Message> addMessageListener(
        messageClassName: String,
        messageListener: SocketMessageListener<Message>
    ) {
        mSocketMessageHandlers[messageClassName] = messageListener as SocketMessageListener<Any>
    }
}

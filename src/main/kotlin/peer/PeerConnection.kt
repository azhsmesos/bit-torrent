package peer


import ParserException
import metainfo.MetaInfo
import skip
import toArray
import toInt
import java.io.DataInputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.net.SocketFactory

/**
 * @Author: zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2023/12/3 12:49
 */
class PeerConnection(peer: Peer) {

    private val socket: Socket
    private val inputStream: DataInputStream
    private var status: PeerStatus = PeerStatus(interested = false, choked = true)
    private var requestInFlight = 0

    init {
        socket = SocketFactory.getDefault().createSocket()
        socket.connect(peer.socket)
        inputStream = DataInputStream(socket.getInputStream())
    }

    fun handshake(metaInfo: MetaInfo): ByteArray {
        // tcp 握手
        // peerID = 00112233445566778899
        val handshake = byteArrayOf(19) + "BitTorrent protocol".toByteArray() +
                ByteArray(8) + metaInfo.infoHash + "00112233445566778899".toByteArray()
        socket.run { getOutputStream().write(handshake) }
        val response = ByteArray(8)
        inputStream.readFully(response)
        return response.skip(48)
    }

    fun initConnection() {
        if (!status.interested) {
            waitFor(PeerMessageType.BITFIELD)
            sendMessage(PeerMessage(PeerMessageType.INTERESTED, ByteArray(0)))
        }
        if (status.choked) {
            waitFor(PeerMessageType.UN_CHOKE)
        }
    }

    private fun waitFor(type: PeerMessageType): PeerMessage {
        var message = readMessage()
        while (message.type != type) {
            message = readMessage()
        }
        return message
    }

    private fun readMessage(): PeerMessage {
        val bytes = ByteArray(4)
        // 读取消息长度前缀
        inputStream.readFully(bytes)
        val length = bytes.toInt()
        if (length == 0) {
            return PeerMessage(PeerMessageType.KEEP_ALIVE, ByteArray(0))
        }
        // 读取消息id
        val messageType = inputStream.readByte().toInt()
        val type = PeerMessageType.valueOf(messageType)
            ?: throw ParserException("Unknown message type received $messageType")
        val payload = if (length > 1) {
            val ret = ByteArray(length - 1)
            inputStream.readFully(ret)
            ret
        } else {
            ByteArray(0)
        }
        when (type) {
            PeerMessageType.CHOKE -> status = status.markAsChoked()
            PeerMessageType.UN_CHOKE -> status.markAsUnChoked()
            PeerMessageType.INTERESTED -> status.markAsInterested()
            PeerMessageType.NOT_INTERESTED -> status.markAsNotInterested()
            else -> null
        }
        return PeerMessage(type, payload)
    }

    private fun sendMessage(message: PeerMessage) {
        val length = 4 + 1 + message.payload.size
        val buffer = ByteBuffer.allocate(length)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(length)
            .put(message.payload)
        socket.getOutputStream().write(buffer.toArray())
    }

    fun download(getNextItem: () -> PeerPartialPieceRequest?, saveResponse: (PeerPartialPieceResponse) -> Boolean) {
        var item = getNextItem()
        while (item != null) {
            while (requestInFlight < 5 && item != null) {
                sendRequest(item)
                item = getNextItem()
            }
            if (requestInFlight > 0) {
                readPiece(saveResponse)
            }
        }
        while (requestInFlight != 0) {
            readPiece(saveResponse)
        }
    }

    private fun sendRequest(item: PeerPartialPieceRequest) {
        // todo 这儿
    }

    private fun readPiece(saveResponse: (PeerPartialPieceResponse) -> Boolean) {

    }

    private enum class PeerMessageType(val protocolType: Int) {
        KEEP_ALIVE(-1),
        CHOKE(0),
        UN_CHOKE(1),
        INTERESTED(2),
        NOT_INTERESTED(3),
        HAVE(4),
        BITFIELD(5),
        REQUEST(6),
        PIECE(7),
        CANCEL(8);

        companion object {
            @OptIn(ExperimentalStdlibApi::class)
            fun valueOf(value: Int) = entries.firstOrNull { it.protocolType == value }
        }
    }

    private data class PeerMessage(val type: PeerMessageType, val payload: ByteArray)

    private data class PeerStatus(val interested: Boolean, val choked: Boolean) {
        fun markAsInterested() = PeerStatus(true, choked)
        fun markAsNotInterested() = PeerStatus(false, choked)
        fun markAsChoked() = PeerStatus(interested, true)
        fun markAsUnChoked() = PeerStatus(interested, false)
    }

}
package peer

import metainfo.MetaInfo
import skip
import java.io.DataInputStream
import java.net.Socket
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



    private data class PeerStatus(val interested: Boolean, val choked: Boolean) {
        fun markAsInterested() = PeerStatus(true, choked)
        fun markAsNotInterested() = PeerStatus(false, choked)
        fun markAsChoked() = PeerStatus(interested, true)
        fun markAsUnchoked() = PeerStatus(interested, false)
    }

}
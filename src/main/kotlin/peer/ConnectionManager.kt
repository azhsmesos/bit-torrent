package peer

import metainfo.MetaInfo
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2023-12-05
 */

private const val MAX_PARTIAL_PIECE_LENGTH = 16384 // 2^14

class ConnectionManager(val metaInfo: MetaInfo, peers: List<Peer>) {
    val connections: Map<Int, PeerConnection>
    val workQueue = mutableListOf<PeerPartialPieceRequest>()
    val receivedPiece = mutableListOf<PeerPartialPieceResponse>()
    val lock = Any()

    init {
        connections = peers.mapIndexed { index, peer ->
            index to PeerConnection(peer)
        }.toMap()
    }

    fun connect() {
        connections.values.forEach {
            it.handshake(metaInfo)
            it.initConnection()
        }
    }

    fun requestPiece(pieceNumber: Int) {
        workQueue.addAll(getRequest(pieceNumber))
    }

    fun download() {
        val peers = connections.values
        val threads = peers.map {
            thread(start = true) {
                it.download({ getNextItem() }) { downloaded ->
                    synchronized(receivedPiece) {
                        receivedPiece.add(downloaded)
                    }
                }
            }
        }
        while (threads.any {
            it.isAlive
            }) {
            // 防止cpu空轮训
            TimeUnit.MICROSECONDS.sleep(1)
        }
    }

    fun getPiece(pieceNumber: Int): ByteArray {
        val ret = ByteArray(getLengthOfPiece(pieceNumber)?:0)
        receivedPiece.filter {
            it.pieceIndex == pieceNumber
        }.sortedBy {
            it.begin
        }.forEach {
            System.arraycopy(it.bytes, 0, ret, it.begin, it.bytes.size)
        }
        return ret
    }

    private fun getNextItem(): PeerPartialPieceRequest? = synchronized(lock) {
        workQueue.removeFirstOrNull()
    }

    private fun getRequest(pieceIndex: Int): List<PeerPartialPieceRequest> {
        val pieceLength = getLengthOfPiece(pieceIndex) ?: return emptyList()
        val fullPartialPieces = pieceLength / MAX_PARTIAL_PIECE_LENGTH
        val remainder = pieceIndex % MAX_PARTIAL_PIECE_LENGTH
        val ret = mutableListOf<PeerPartialPieceRequest>()
        for (i in 0 until fullPartialPieces) {
            ret.add(PeerPartialPieceRequest(pieceIndex, i * MAX_PARTIAL_PIECE_LENGTH, MAX_PARTIAL_PIECE_LENGTH))
        }
        if (remainder != 0) {
            ret.add(PeerPartialPieceRequest(pieceIndex, fullPartialPieces * MAX_PARTIAL_PIECE_LENGTH, remainder))
        }
        return ret
    }

    private fun getLengthOfPiece(pieceNumber: Int): Int? = if (isLastPiece(pieceNumber)) {
        val remainder = metaInfo.info.fileLength?.rem(metaInfo.info.pieceLength)
        if (remainder == 0L) {
            metaInfo.info.pieceLength
        }
        remainder?.toInt()
    } else {
        metaInfo.info.pieceLength
    }

    private fun isLastPiece(pieceNumber: Int) = pieceNumber == metaInfo.info.pieces.size - 1
}
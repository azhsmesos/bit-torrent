package peer

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2023-12-05
 */

data class PeerPartialPieceRequest(val pieceIndex: Int, val begin: Int, val length: Int)
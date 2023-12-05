@file:OptIn(ExperimentalStdlibApi::class)
import bencode.BencodeParser.parseNext
import metainfo.MetaInfo
import metainfo.MetaInfoParser
import peer.ConnectionManager
import peer.Peer
import peer.PeerConnection
import peer.TrackerManager
import java.io.StringReader

fun main(args: Array<String>) {


}


fun decode(bencodeValue: String): String {
    val decode = parseNext(bencodeValue).first
    return decode.toJSON()
}

// 由于bt种子文件格式的区别，导致不一定可以获取正确数据
// 有些格式是 announce 和 info 两块
// 但是info中信息有区别
fun metaInfo(filename: String): MetaInfo {
    return MetaInfoParser.parse(filename)
}

// 获取peer地址，有两个key
// interval 表示请求的频率
// peers表示客户端可连接的list，每个peer用6个字节，前4个字节是ip地址，后2个是端口
fun sendPeers(filename: String): List<Peer> {
    val metaInfo = MetaInfoParser.parse(filename)
    return TrackerManager.sendPeers(metaInfo)
}

fun handShake(filename: String, peer: String): String {
    val metaInfo = MetaInfoParser.parse(filename)
    val conn = PeerConnection(Peer(peer))
    return bytesToHex(conn.handshake(metaInfo))
}

/**
 * 交换多个对等消息下载文件
 * 对等消息长度前缀4字节，消息ID1字节，有效负载（可变大小）组成
 *
 */
fun downloadPiece(outputFilename: String, metaInfoFilename: String, pieceNumber: Int) {
    val metaInfo = MetaInfoParser.parse(metaInfoFilename)
    val peers = TrackerManager.sendPeers(metaInfo)
    val connectionManager = ConnectionManager(metaInfo, peers)
    connectionManager.connect()
    connectionManager.requestPiece(pieceNumber)
    connectionManager.download()
}
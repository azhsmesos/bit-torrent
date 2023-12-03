package peer

import ParserException
import asType
import bencode.BencodeParser
import bencode.DictBencodeValue
import bencode.StringBencodeValue
import metainfo.MetaInfo
import org.apache.commons.codec.net.URLCodec
import java.net.HttpURLConnection
import java.net.URL
import java.util.BitSet

/**
 * @Author: zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2023/12/2 20:23
 */
// 客户端唯一标识
const val PEER_ID = "00112233445566778899"
const val PORT = 6881
const val GET_REQUEST = "GET"
const val PEERS = "peers"


object TrackerManager {
    // 根据hash请求对应announce对应的服务器获取数据
    fun sendPeers(metaInfo: MetaInfo): List<Peer> {
        val encodeHash = String(URLCodec.encodeUrl(BitSet(256), metaInfo.infoHash))
        var fileLength = metaInfo.info.fileLength
        if (fileLength == null) {
            // 如果存在files存在但是fileLength不存在场景，当前先不支持，后续改造
            fileLength = 0;
        }
        // 这儿有个bug，URL不支持UDP协议，需要适配
        val url =
            URL("${metaInfo.announce}?info_hash=$encodeHash&peer_id=$PEER_ID&port=$PORT&uploaded=0&downloaded=0&left=${fileLength}&compact=1")
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod = GET_REQUEST
        val bencoded = con.inputStream.readBytes()
        val dict = BencodeParser.parseNext(bencoded).first.asType<DictBencodeValue>()
        val peers = dict[PEERS]?.asType<StringBencodeValue>()
            ?: throw ParserException("peers not present in tracker response")
        // 返回list，按照6个元素进行分割
        // 每个peer使用 6 个字节表示。前 4 个字节是对等体的 IP 地址，后 2 个字节是对等体的端口号。
        return peers.value.toList().chunked(6) {
            Peer(it.toByteArray())
        }
    }
}
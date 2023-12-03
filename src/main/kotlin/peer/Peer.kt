package peer

import skip
import toShort
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * @Author: zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2023/12/2 20:28
 */
data class Peer (val socket: InetSocketAddress) {
    constructor(inetAddress: InetAddress, port: Int) : this(InetSocketAddress(inetAddress, port))

    // and 0xffff 是为了确保结果是无符号的 16 位整数。
    // 4个字节是因为当前基于ipv4模型完成的
    constructor(byteArray: ByteArray) : this(
        InetAddress.getByAddress(byteArray.copyOf(4)),
        byteArray.skip(4).toShort().toInt() and 0xffff
    )

    constructor(peer: String) : this(
        InetAddress.getByName(peer.substringBefore(':')), peer.substringAfter(':').toInt()
    )
}
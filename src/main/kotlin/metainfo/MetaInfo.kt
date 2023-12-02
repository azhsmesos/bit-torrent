package metainfo

import com.google.gson.Gson

/**
 * @Author: zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2023/12/2 18:40
 */
data class MetaInfo(
    val announce: String,
    val infoHash: ByteArray,
    val info: Info  // info 序列化需要设计
) {

    val announceList: List<String>? = null
    val comment: String? = null
    val createdBy: String? = null
    val creationDate: Long? = null

    // 需要改改
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is MetaInfo) {
            return false
        }
        if (announce != other.announce) {
            return false
        }
        if (!infoHash.contentEquals(other.infoHash)) {
            return false
        }
        return info == other.info
    }

    override fun hashCode(): Int {
        var result = announce.hashCode()
        result = 31 * result + infoHash.contentHashCode()
        result = 31 * result + info.hashCode()
        return result
    }
}
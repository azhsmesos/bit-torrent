package metainfo

/**
 * @Author: zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2023/12/2 19:47
 */
data class Info (
    val fileName: String,
    val pieceLength: Int,
    val pieces: List<ByteArray>
) {
    var fileLength: Long? = null
    var files: String? = null
}
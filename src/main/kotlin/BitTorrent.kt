import bencode.BencodeParser.parseNext
import metainfo.MetaInfo
import metainfo.MetaInfoParser

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
import bencode.BencodeParser.parseNext
import bencode.DictBencodeValue

fun main(args: Array<String>) {


}


fun decode(bencodeValue: String): String {
    val decode = parseNext(bencodeValue).first
    return decode.toJSON()
}
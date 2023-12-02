import bencode.BencodeValue
import kotlin.reflect.KClass

/**
 * @Author: zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2023/12/2 13:55
 */


open class ParserException(message: String) : Exception(message)

class InvalidDictionaryKeyException(bencodeValue: BencodeValue) :
    ParserException("Invalid dictionary key type. Expected String, got $bencodeValue")

class InvalidMetaInfoFileException(bencodeValue: BencodeValue) :
    ParserException("Invalid metainfo file. Expected root to be dictionary, got $bencodeValue")

class MismatchedTypeException(expected: KClass<out BencodeValue>, got: BencodeValue) : ParserException("Expected type ${expected.simpleName}, got $got")
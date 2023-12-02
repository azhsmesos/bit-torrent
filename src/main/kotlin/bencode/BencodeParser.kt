package bencode

import InvalidDictionaryKeyException
import ParserException
import bencode.BencodeParser.ToBencode.decodeDictionary
import bencode.BencodeParser.ToBencode.decodeInteger
import bencode.BencodeParser.ToBencode.decodeList
import bencode.BencodeParser.ToBencode.decodeString
import bencode.BencodeParser.ToObject.parseDict
import bencode.BencodeParser.ToObject.parseInteger
import bencode.BencodeParser.ToObject.parseList
import bencode.BencodeParser.ToObject.parseString
import java.nio.charset.Charset
import skip
import toBigInteger
import java.math.BigInteger.TEN
import java.math.BigInteger.ZERO

/**
 * @Author: zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2023/12/2 13:09
 */

const val INTEGER_TAG = 'i'
const val LIST_TAG = 'l'
const val DICTIONARY_TAG = 'd'
const val END_STRING_TAG = ':'.code.toByte()
const val END_TAG_TAG = 'e'.code.toByte()
const val NEGATIVE_TAG = '-'.code.toByte()

object BencodeParser {

    fun toBencode(value: BencodeValue): ByteArray {
        return when (value) {
            is StringBencodeValue -> decodeString(value)
            is IntegerBencodeValue -> decodeInteger(value)
            is ListBencodeValue -> decodeList(value)
            is DictBencodeValue -> decodeDictionary(value)
        }
    }

    fun parseNext(bencodeValue: String): Pair<BencodeValue, ByteArray> =
        parseNext(bencodeValue.toByteArray())

    fun parseNext(bencodeValue: ByteArray) : Pair<BencodeValue, ByteArray> {
        return when (bencodeValue[0].toInt().toChar()) {
            in '1'..'9' -> parseString(bencodeValue)
            INTEGER_TAG -> parseInteger(bencodeValue)
            LIST_TAG -> parseList(bencodeValue)
            DICTIONARY_TAG -> parseDict(bencodeValue)
            else -> throw ParserException("Unknown type ${bencodeValue[0]}")
        }
    }

    object ToBencode {
        fun decodeString(value: StringBencodeValue) =
            value.value.size.toString().toByteArray() + END_STRING_TAG + value.value

        fun decodeInteger(value: IntegerBencodeValue) =
            byteArrayOf(INTEGER_TAG.code.toByte()) + value.value.toString(10).toByteArray() + END_TAG_TAG

        fun decodeList(value: ListBencodeValue) = byteArrayOf(LIST_TAG.code.toByte()) +
                value.values.map {
                    toBencode(it)
                }.reduce { acc, bytes ->
                    acc + bytes
                } + END_TAG_TAG

        fun decodeDictionary(value: DictBencodeValue) = byteArrayOf(DICTIONARY_TAG.code.toByte()) +
                value.values.map {
                    toBencode(value)
                }.reduce { acc, bytes ->
                    acc + bytes
                } + END_TAG_TAG
    }

    object ToObject {
        // For example, the string is encoded as .<length>:<contents>"hello""5:hello"
        fun parseString(bencodeValue: ByteArray): Pair<StringBencodeValue, ByteArray> {
            val index = bencodeValue.indexOfFirst {
                it == END_STRING_TAG
            }
            val len = bencodeValue.copyOf(index)
                .toString(Charset.defaultCharset())
                .toInt(10)
            val totalLen = index + 1 + len
            val value = bencodeValue.copyOfRange(index + 1, totalLen).toBencodeValue()
            return value to bencodeValue.skip(totalLen)
        }

        // Integers are encoded as i<number>e. For example,
        // 52 is encoded as i52e and -52 is encoded as i-52e.
        fun parseInteger(bencodeValue: ByteArray): Pair<IntegerBencodeValue, ByteArray> {
            // Handling negative numbers
            val value = bencodeValue.asSequence()
                .drop(1)
                .takeWhile {
                    it != END_TAG_TAG
                }.map {
                    when (it) {
                        NEGATIVE_TAG -> ZERO
                        else -> it.toBigInteger()
                    }
                }.reduce { acc, bigInteger ->
                    acc.multiply(TEN).add(bigInteger)
                }
            if (bencodeValue[1] == NEGATIVE_TAG) {
                return value.negate().toBencodeValue() to bencodeValue.skip(value.toString(10).length + 3)
            }
            return value.toBencodeValue() to bencodeValue.skip(value.toString(10).length + 2)
        }

        // For example, ["hello", 52] would be encoded as l5:helloi52ee.
        // Note that there are no separators between the elements.
        fun parseList(bencodeValue: ByteArray): Pair<ListBencodeValue, ByteArray> {
            val values = mutableListOf<BencodeValue>()
            var currentBencodeValue = bencodeValue.skip(1)
            while (currentBencodeValue[0] != END_TAG_TAG) {
                val (valueToAdd, newBencodeValue) = parseNext(currentBencodeValue)
                values.add(valueToAdd)
                currentBencodeValue = newBencodeValue
            }
            return values.toBencodeValue() to currentBencodeValue.skip(1)
        }

        // For example, {"hello": 52, "foo":"bar"}
        // would be encoded as: d3:foo3:bar5:helloi52ee
        // (note that the keys were reordered).
        fun parseDict(bencodeValue: ByteArray): Pair<BencodeValue, ByteArray> {
            val values = mutableMapOf<ByteArray, BencodeValue>()
            var currentBencodeValue = bencodeValue.skip(1)
            while (currentBencodeValue[0] != END_TAG_TAG) {
                val (key, newBencodeValue) = parseNext(currentBencodeValue)
                if (key !is StringBencodeValue) {
                    throw InvalidDictionaryKeyException(key)
                }
                val (value, newBencodeValue2) = parseNext(newBencodeValue)
                values[key.value] = value
                currentBencodeValue = newBencodeValue2
            }
            return values.toBencodeValue() to currentBencodeValue.skip(1)
        }
    }
}
package bencode

import com.google.gson.Gson
import java.math.BigInteger
import java.nio.charset.Charset

/**
 * @Author: zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2023/12/2 13:14
 */

val gson = Gson()

sealed interface BencodeValue {
    fun toJSON(): String
}

class StringBencodeValue(val value: ByteArray): BencodeValue {
    override fun toJSON(): String = gson.toJson(asString())
    fun asString(): String = value.toString(Charset.defaultCharset())
}

class IntegerBencodeValue(val value: BigInteger): BencodeValue {
    override fun toJSON(): String = gson.toJson(value)
    fun asInt(): Int = value.toInt()
    fun asLong(): Long = value.toLong()
}

class ListBencodeValue(val values: List<BencodeValue>): BencodeValue {
    override fun toJSON(): String = '[' + values.joinToString(",") { it.toJSON() } + ']'
}

class DictBencodeValue(val values: Map<ByteArray, BencodeValue>): BencodeValue {
    override fun toJSON(): String =
        '{' + values.map { "\"${it.key.toString(Charset.defaultCharset())}" +
                "\":${it.value.toJSON()}"
        }.joinToString(",")+ '}'

    operator fun get(key: ByteArray): BencodeValue?? = values
        .entries
        .firstOrNull {
            it.key.contentEquals(key)
        }?.value

    operator fun get(key: String): BencodeValue? = get(key.toByteArray())
}

fun ByteArray.toBencodeValue() = StringBencodeValue(this)
fun String.toBencodeValue() = StringBencodeValue(this.toByteArray())
fun BigInteger.toBencodeValue() = IntegerBencodeValue(this)
fun Int.toBencodeValue() = IntegerBencodeValue(this.toBigInteger())
fun List<BencodeValue>.toBencodeValue() = ListBencodeValue(this)
fun Map<ByteArray, BencodeValue>.toBencodeValue() = DictBencodeValue(this)
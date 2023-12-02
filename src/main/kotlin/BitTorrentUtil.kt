import bencode.BencodeValue
import com.google.gson.Gson
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @Author: zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2023/12/2 13:50
 */

fun Byte.toBigInteger() = this.toInt().toChar().toString().toBigInteger()

fun ByteArray.skip(num: Int) = this.copyOfRange(num, this.size)

fun ByteArray.toShort(order: ByteOrder = ByteOrder.BIG_ENDIAN): Short = ByteBuffer.wrap(this).order(order).getShort()

fun ByteArray.toInt(order: ByteOrder = ByteOrder.BIG_ENDIAN): Int = ByteBuffer.wrap(this).order(order).getInt()

fun ByteBuffer.toArray(): ByteArray {
    val buf = this.duplicate().rewind() as ByteBuffer
    val ret = ByteArray(buf.remaining())
    buf.get(ret)
    return ret
}

inline fun <reified T: BencodeValue> BencodeValue.asType(): T = if (this !is T) {
    throw MismatchedTypeException(T::class, this)
} else {
    this
}

private val gson = Gson()

fun toJSON(obj: Any): String {
    return gson.toJson(obj)
}



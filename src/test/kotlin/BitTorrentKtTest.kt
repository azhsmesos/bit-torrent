import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * @Author: zhaozhenhang <zhaozhenhang></zhaozhenhang>@kuaishou.com>
 * Created on 2023/12/2 14:39
 */
class BitTorrentKtTest {

    @Test
    fun decodeTest() {
        val s1 = "i52e"
        val msg = decode(s1)
        assertEquals("52", msg)

        val s2 = "i-52e"
        val msg2 = decode(s2)
        assertEquals(msg2, "-52")

        val s3 = "5:hello"
        val msg3 = decode(s3)
        assertEquals(msg3, "\"hello\"")

        val s4 = "l5:helloi52ee"
        val msg4 = decode(s4)
        assertEquals(msg4, "[\"hello\",52]")

        val s5 = "d3:foo3:bar5:helloi52ee"
        val msg5 = decode(s5)
        assertEquals(msg5,"{\"foo\":\"bar\",\"hello\":52}")
    }

    // bt 种子 https://www.dyg5.com/file-49231.html
    @Test
    fun metaInfoTest() {
        val info = metaInfo("src/main/resources/test.torrent")
        println("info: " + toJSON(info))
    }
}
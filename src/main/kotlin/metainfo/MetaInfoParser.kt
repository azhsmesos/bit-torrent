package metainfo

import InvalidMetaInfoFileException
import ParserException
import asType
import bencode.BencodeParser
import bencode.BencodeParser.parseNext
import bencode.DictBencodeValue
import bencode.IntegerBencodeValue
import bencode.StringBencodeValue
import java.io.File
import java.security.MessageDigest

/**
 * @Author: zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2023/12/2 18:40
 */

const val ANNOUNCE = "announce"
const val INFO = "info"
const val PIECES = "pieces"
const val FILE_LENGTH = "length"
const val FILES = "files"
const val FILENAME = "name"
const val PIECE_LENGTH = "piece length"
const val SHA_1 = "SHA-1"

object MetaInfoParser {

    fun parse(fileName: String): MetaInfo {
        val parseDict = parseNext(File(fileName).readBytes()).first
        if (parseDict !is DictBencodeValue) {
            throw InvalidMetaInfoFileException(parseDict)
        }
        val announce = parseDict[ANNOUNCE]?.asType<StringBencodeValue>()
            ?: throw ParserException("announce not found inside metainfo")
        val infoDict = parseDict[INFO]?.asType<DictBencodeValue>()
            ?: throw ParserException("info not found inside metainfo")
        val pieces = infoDict[PIECES]?.asType<StringBencodeValue>()
            ?: throw ParserException("pieces not found inside info")
        val fileLength = infoDict[FILE_LENGTH]?.asType<IntegerBencodeValue>()
        val files = infoDict[FILES]
        if (fileLength == null && files == null) {
            throw ParserException("fileLength and files not found inside info")
        }
        val filename = infoDict[FILENAME]?.asType<StringBencodeValue>()
            ?: throw ParserException("filename not found inside info")
        val pieceLength = infoDict[PIECE_LENGTH]?.asType<IntegerBencodeValue>()
            ?: throw ParserException("piece length not found inside info")
        val infoHash = MessageDigest.getInstance(SHA_1).digest(BencodeParser.toBencode(infoDict))
        val pieceList = pieces.value
            .toList()
            .chunked(20) {
                it.toByteArray()
            }
        val info = Info(filename.asString(), pieceLength.asInt(), pieceList)
        info.fileLength = fileLength?.asLong()
        info.files = files?.toJSON()
        return MetaInfo(
            announce.asString(),
            infoHash,
            info
        )
    }
}
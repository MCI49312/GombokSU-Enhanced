package mci.github.gomboksu.ui.util

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.android.apksig.ApkSigner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.random.Random

object ApkSpoofer {
    private const val TAG = "ApkSpoofer"
    private val PACKAGE_ID_REGEX = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")

    fun isValidPackageId(pkg: String): Boolean {
        return pkg.length in 5..120 && PACKAGE_ID_REGEX.matches(pkg)
    }

    fun generateRandomPackageId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz"
        fun word(len: Int) = (1..len).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "${word(5)}.${word(6)}.${word(6)}"
    }

    suspend fun buildCustomSpoofedApk(
        context: Context,
        newPackageId: String,
        newAppName: String,
        onStatus: (String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanPkg = newPackageId.trim()
            val cleanName = newAppName.trim()
            require(isValidPackageId(cleanPkg)) {
                "Invalid package ID. Use lowercase letters, digits, underscores, and at least one dot (e.g. com.my.notes)."
            }
            require(cleanName.isNotEmpty() && cleanName.length <= 64) {
                "App name must be between 1 and 64 characters."
            }

            val oldPackageId = context.packageName
            val sourceApk = File(context.applicationInfo.sourceDir)
            require(sourceApk.exists() && sourceApk.canRead()) {
                "Cannot read installed APK at ${sourceApk.absolutePath}"
            }

            val workDir = File(context.cacheDir, "custom_spoof").apply {
                deleteRecursively()
                mkdirs()
            }
            val unsignedApk = File(workDir, "unsigned.apk")
            val signedApk = File(workDir, "${cleanPkg.replace('.', '_')}_spoofed.apk")

            onStatus("Repackaging APK & patching AndroidManifest.xml…")
            repackageApk(
                sourceApk = sourceApk,
                outputApk = unsignedApk,
                oldPackageId = oldPackageId,
                newPackageId = cleanPkg,
                newAppName = cleanName
            )

            onStatus("Signing APK with GombokSU v2 certificate (0x3e6)…")
            signApkV2WithGombokKey(context, unsignedApk, signedApk)
            unsignedApk.delete()

            require(signedApk.exists() && signedApk.length() > 1024L) {
                "Failed to produce signed APK"
            }
            signedApk
        }
    }

    suspend fun installCustomSpoofedApk(
        context: Context,
        signedApk: File,
        newPackageId: String,
        uninstallOld: Boolean,
        onStatus: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val oldPackageId = context.packageName
            if (rootAvailable()) {
                onStatus("Installing $newPackageId via root…")
                val tmpPath = "/data/local/tmp/${signedApk.name.replace("'", "")}"
                val samePkg = oldPackageId == newPackageId
                if (samePkg) {
                    val detached = """
                        cp '${signedApk.absolutePath}' '$tmpPath' && \
                        chmod 644 '$tmpPath' && \
                        nohup sh -c "pm install -r -d '$tmpPath' && rm -f '$tmpPath' && sleep 1 && monkey -p '$newPackageId' -c android.intent.category.LAUNCHER 1" >/dev/null 2>&1 &
                    """.trimIndent().replace("\n", " ")
                    val ok = withNewRootShell(globalMnt = true) {
                        newJob().add(detached).exec().isSuccess
                    }
                    if (ok) {
                        return@runCatching "Installing via root… app will relaunch shortly."
                    }
                } else {
                    val installCmd = "cp '${signedApk.absolutePath}' '$tmpPath' && chmod 644 '$tmpPath' && pm install -r -d '$tmpPath'; rc=\$?; rm -f '$tmpPath'; exit \$rc"
                    val res = withNewRootShell(globalMnt = true) {
                        newJob().add(installCmd).exec()
                    }
                    if (res.isSuccess) {
                        if (uninstallOld) {
                            onStatus("Launching $newPackageId and removing old package…")
                            val switchCmd = "nohup sh -c \"monkey -p '$newPackageId' -c android.intent.category.LAUNCHER 1; sleep 2; pm uninstall '$oldPackageId'\" >/dev/null 2>&1 &"
                            withNewRootShell(globalMnt = true) {
                                newJob().add(switchCmd).exec()
                            }
                            return@runCatching "Installed $newPackageId and scheduled uninstall of $oldPackageId."
                        } else {
                            withNewRootShell(globalMnt = true) {
                                newJob().add("monkey -p '$newPackageId' -c android.intent.category.LAUNCHER 1").exec()
                            }
                            return@runCatching "Successfully installed $newPackageId!"
                        }
                    } else {
                        Log.w(TAG, "Root pm install failed: ${res.err.joinToString("\n")}")
                    }
                }
            }

            // Fallback: standard Android PackageInstaller UI
            onStatus("Opening Android Package Installer…")
            withContext(Dispatchers.Main) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    signedApk
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            "Opened system installer for $newPackageId."
        }
    }

    suspend fun copyApkToDownloads(signedApk: File): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs()
            val outFile = File(downloadsDir, signedApk.name)
            signedApk.copyTo(outFile, overwrite = true)
            outFile
        }
    }

    private class CountingOutputStream(out: OutputStream) : FilterOutputStream(out) {
        var count: Long = 0L
            private set

        override fun write(b: Int) {
            out.write(b)
            count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            count += len
        }
    }

    private fun repackageApk(
        sourceApk: File,
        outputApk: File,
        oldPackageId: String,
        newPackageId: String,
        newAppName: String
    ) {
        ZipFile(sourceApk).use { zip ->
            val countingOut = CountingOutputStream(FileOutputStream(outputApk))
            ZipOutputStream(countingOut).use { zos ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name

                    // Skip old signature files so ApkSigner creates a clean v2 signature block
                    if (name.startsWith("META-INF/") && (
                            name.endsWith(".SF", ignoreCase = true) ||
                            name.endsWith(".RSA", ignoreCase = true) ||
                            name.endsWith(".DSA", ignoreCase = true) ||
                            name.endsWith(".EC", ignoreCase = true) ||
                            name.equals("META-INF/MANIFEST.MF", ignoreCase = true)
                        )
                    ) {
                        continue
                    }

                    val rawBytes = zip.getInputStream(entry).use { it.readBytes() }
                    val finalBytes = when {
                        name == "AndroidManifest.xml" -> {
                            patchAxml(
                                data = rawBytes,
                                oldPackageId = oldPackageId,
                                newPackageId = newPackageId,
                                newAppName = newAppName,
                                isManifest = true
                            )
                        }
                        name.startsWith("res/") && name.endsWith(".xml") && isAxml(rawBytes) -> {
                            runCatching {
                                patchAxml(
                                    data = rawBytes,
                                    oldPackageId = oldPackageId,
                                    newPackageId = newPackageId,
                                    newAppName = newAppName,
                                    isManifest = false
                                )
                            }.getOrDefault(rawBytes)
                        }
                        else -> rawBytes
                    }

                    val outEntry = ZipEntry(name)
                    outEntry.time = entry.time

                    val keepStored = entry.method == ZipEntry.STORED || name == "resources.arsc"
                    if (keepStored) {
                        outEntry.method = ZipEntry.STORED
                        outEntry.size = finalBytes.size.toLong()
                        outEntry.compressedSize = finalBytes.size.toLong()
                        val crc = CRC32().apply { update(finalBytes) }
                        outEntry.crc = crc.value

                        // Ensure 4-byte alignment (4096-byte for uncompressed .so) as required by Android 11+
                        val alignment = if (name.endsWith(".so")) 4096 else 4
                        val nameBytesLen = name.toByteArray(Charsets.UTF_8).size
                        val headerEnd = countingOut.count + 30L + nameBytesLen
                        val remainder = (headerEnd % alignment).toInt()
                        if (remainder != 0) {
                            outEntry.extra = ByteArray(alignment - remainder)
                        }
                    } else {
                        outEntry.method = ZipEntry.DEFLATED
                    }

                    zos.putNextEntry(outEntry)
                    zos.write(finalBytes)
                    zos.closeEntry()
                }
            }
        }
    }

    private fun signApkV2WithGombokKey(context: Context, inputApk: File, outputApk: File) {
        val ks = KeyStore.getInstance("PKCS12")
        val pass = "gomboksu123".toCharArray()
        context.assets.open("gomboksu.p12").use { stream ->
            ks.load(stream, pass)
        }
        val alias = ks.aliases().nextElement()
        val privateKey = ks.getKey(alias, pass) as PrivateKey
        val certs = ks.getCertificateChain(alias).map { it as X509Certificate }

        val signerConfig = ApkSigner.SignerConfig.Builder("gomboksu", privateKey, certs).build()
        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(inputApk)
            .setOutputApk(outputApk)
            .setMinSdkVersion(26)
            .setV1SigningEnabled(false)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(false)
            .setV4SigningEnabled(false)
            .build()
            .sign()
    }

    // --- Pure Kotlin Binary AXML (AndroidManifest.xml / shortcuts.xml) Patcher ---

    private fun isAxml(data: ByteArray): Boolean {
        return data.size >= 16 && readU16(data, 0) == 0x0003 && readU16(data, 8) == 0x0001
    }

    private fun patchAxml(
        data: ByteArray,
        oldPackageId: String,
        newPackageId: String,
        newAppName: String,
        isManifest: Boolean
    ): ByteArray {
        require(isAxml(data)) { "Not a valid Android binary XML file" }

        val spOffset = 8
        val spHeaderSize = readU16(data, spOffset + 2)
        val spChunkSize = readU32(data, spOffset + 4)
        val oldStringCount = readU32(data, spOffset + 8)
        val styleCount = readU32(data, spOffset + 12)
        val oldFlags = readU32(data, spOffset + 16)
        val isUtf8 = (oldFlags and 0x100) != 0
        val oldStringsStart = readU32(data, spOffset + 20)
        val oldStylesStart = readU32(data, spOffset + 24)

        val oldOffsets = IntArray(oldStringCount) { i ->
            readU32(data, spOffset + spHeaderSize + i * 4)
        }
        val styleOffsetsBytes = if (styleCount > 0) {
            val start = spOffset + spHeaderSize + oldStringCount * 4
            data.copyOfRange(start, start + styleCount * 4)
        } else ByteArray(0)

        val stringsAbsStart = spOffset + oldStringsStart
        val stringsAbsEnd = if (oldStylesStart > 0) spOffset + oldStylesStart else spOffset + spChunkSize
        val existingStringsData = data.copyOfRange(stringsAbsStart, stringsAbsEnd)
        val existingStylesData = if (oldStylesStart > 0) {
            data.copyOfRange(spOffset + oldStylesStart, spOffset + spChunkSize)
        } else ByteArray(0)

        val existingStrings = List(oldStringCount) { i ->
            decodePoolString(existingStringsData, oldOffsets[i], isUtf8)
        }

        val remaining = data.copyOfRange(spOffset + spChunkSize, data.size)

        // Collect new strings to append
        val addedStringToIndex = LinkedHashMap<String, Int>()
        fun internNew(value: String): Int {
            return addedStringToIndex.getOrPut(value) {
                oldStringCount + addedStringToIndex.size
            }
        }

        val newPkgIdx = internNew(newPackageId)
        val newLabelIdx = if (isManifest) internNew(newAppName) else -1

        // Walk RES_XML_START_ELEMENT chunks and patch target attributes
        var modifiedAny = false
        var pos = 0
        while (pos + 8 <= remaining.size) {
            val chunkType = readU16(remaining, pos)
            val chunkSize = readU32(remaining, pos + 4)
            if (chunkSize < 8 || pos + chunkSize > remaining.size) break

            if (chunkType == 0x0102 && chunkSize >= 36) {
                val elemNameIdx = readI32(remaining, pos + 20)
                val elemName = existingStrings.getOrNull(elemNameIdx)
                val attrStart = readU16(remaining, pos + 24)
                val attrSize = readU16(remaining, pos + 26)
                val attrCount = readU16(remaining, pos + 28)

                // Check element's android:name first (to skip FlashAnyKernel alias label if needed)
                var elemAndroidName: String? = null
                for (a in 0 until attrCount) {
                    val aOff = pos + 16 + attrStart + a * attrSize
                    if (aOff + 20 > pos + chunkSize) break
                    val aName = existingStrings.getOrNull(readI32(remaining, aOff + 4))
                    if (aName == "name") {
                        val dType = remaining[aOff + 15].toInt() and 0xFF
                        val dVal = readI32(remaining, aOff + 16)
                        if (dType == 0x03) {
                            elemAndroidName = existingStrings.getOrNull(dVal)
                        }
                    }
                }

                val isComponentElem = elemName == "application" ||
                    elemName == "activity" ||
                    elemName == "activity-alias" ||
                    elemName == "service" ||
                    elemName == "receiver" ||
                    elemName == "provider"

                for (a in 0 until attrCount) {
                    val aOff = pos + 16 + attrStart + a * attrSize
                    if (aOff + 20 > pos + chunkSize) break
                    val aName = existingStrings.getOrNull(readI32(remaining, aOff + 4))
                    val dType = remaining[aOff + 15].toInt() and 0xFF
                    val dVal = readI32(remaining, aOff + 16)

                    when {
                        isManifest && elemName == "manifest" && aName == "package" -> {
                            writeStringAttr(remaining, aOff, newPkgIdx)
                            modifiedAny = true
                        }
                        isManifest && elemName == "provider" && aName == "authorities" && dType == 0x03 -> {
                            val currentVal = existingStrings.getOrNull(dVal)
                            if (currentVal != null) {
                                val updatedAuth = currentVal.split(";").joinToString(";") { part ->
                                    when {
                                        part == oldPackageId -> newPackageId
                                        part.startsWith("$oldPackageId.") -> newPackageId + part.removePrefix(oldPackageId)
                                        else -> part
                                    }
                                }
                                if (updatedAuth != currentVal) {
                                    writeStringAttr(remaining, aOff, internNew(updatedAuth))
                                    modifiedAny = true
                                }
                            }
                        }
                        isManifest && isComponentElem && (aName == "name" || aName == "targetActivity") && dType == 0x03 -> {
                            val currentVal = existingStrings.getOrNull(dVal)
                            if (currentVal != null && currentVal.startsWith(".")) {
                                // Ensure relative class names remain anchored to original DEX package
                                writeStringAttr(remaining, aOff, internNew(oldPackageId + currentVal))
                                modifiedAny = true
                            }
                        }
                        isManifest && (elemName == "application" || (elemName == "activity-alias" && elemAndroidName?.endsWith(".FlashAnyKernel") != true)) && aName == "label" -> {
                            writeStringAttr(remaining, aOff, newLabelIdx)
                            modifiedAny = true
                        }
                        isManifest && (elemName == "permission" || elemName == "uses-permission") && aName == "name" && dType == 0x03 -> {
                            val currentVal = existingStrings.getOrNull(dVal)
                            if (currentVal != null && currentVal.startsWith("$oldPackageId.")) {
                                val updatedPerm = newPackageId + currentVal.removePrefix(oldPackageId)
                                writeStringAttr(remaining, aOff, internNew(updatedPerm))
                                modifiedAny = true
                            }
                        }
                        isManifest && isComponentElem && aName == "permission" && dType == 0x03 -> {
                            val currentVal = existingStrings.getOrNull(dVal)
                            if (currentVal != null && currentVal.startsWith("$oldPackageId.")) {
                                val updatedPerm = newPackageId + currentVal.removePrefix(oldPackageId)
                                writeStringAttr(remaining, aOff, internNew(updatedPerm))
                                modifiedAny = true
                            }
                        }
                        !isManifest && elemName == "intent" && aName == "targetPackage" && dType == 0x03 -> {
                            val currentVal = existingStrings.getOrNull(dVal)
                            if (currentVal == oldPackageId) {
                                writeStringAttr(remaining, aOff, newPkgIdx)
                                modifiedAny = true
                            }
                        }
                    }
                }
            }
            pos += chunkSize
        }

        if (!modifiedAny) return data

        // Build new strings data buffer
        val newStringsStream = ByteArrayOutputStream(existingStringsData.size + 512)
        newStringsStream.write(existingStringsData)

        val newOffsets = IntArray(oldStringCount + addedStringToIndex.size)
        System.arraycopy(oldOffsets, 0, newOffsets, 0, oldStringCount)

        var nextIdx = oldStringCount
        for (str in addedStringToIndex.keys) {
            newOffsets[nextIdx++] = newStringsStream.size()
            newStringsStream.write(encodePoolString(str, isUtf8))
        }

        // Pad strings section to 4-byte boundary
        while (newStringsStream.size() % 4 != 0) {
            newStringsStream.write(0)
        }
        val finalStringsData = newStringsStream.toByteArray()

        val newStringCount = newOffsets.size
        val newFlags = oldFlags and 1.inv() // Clear SORTED_FLAG since we appended strings
        val newStringsStart = spHeaderSize + 4 * (newStringCount + styleCount)
        val newStylesStart = if (oldStylesStart > 0) newStringsStart + finalStringsData.size else 0
        val newSpChunkSize = newStringsStart + finalStringsData.size + existingStylesData.size
        val newFileSize = 8 + newSpChunkSize + remaining.size

        val out = ByteBuffer.allocate(newFileSize).order(ByteOrder.LITTLE_ENDIAN)
        // Root XML header
        out.putShort(0x0003)
        out.putShort(0x0008)
        out.putInt(newFileSize)

        // String pool header (28 bytes)
        out.putShort(0x0001)
        out.putShort(spHeaderSize.toShort())
        out.putInt(newSpChunkSize)
        out.putInt(newStringCount)
        out.putInt(styleCount)
        out.putInt(newFlags)
        out.putInt(newStringsStart)
        out.putInt(newStylesStart)

        for (off in newOffsets) {
            out.putInt(off)
        }
        if (styleOffsetsBytes.isNotEmpty()) {
            out.put(styleOffsetsBytes)
        }
        out.put(finalStringsData)
        if (existingStylesData.isNotEmpty()) {
            out.put(existingStylesData)
        }
        out.put(remaining)

        return out.array()
    }

    private fun writeStringAttr(buf: ByteArray, attrOffset: Int, stringIndex: Int) {
        writeI32(buf, attrOffset + 8, stringIndex) // rawValue
        buf[attrOffset + 12] = 8                   // Res_value.size low byte
        buf[attrOffset + 13] = 0                   // Res_value.size high byte
        buf[attrOffset + 14] = 0                   // Res_value.res0
        buf[attrOffset + 15] = 0x03                // TYPE_STRING
        writeI32(buf, attrOffset + 16, stringIndex) // Res_value.data
    }

    private fun decodePoolString(buf: ByteArray, offset: Int, isUtf8: Boolean): String {
        if (offset < 0 || offset >= buf.size) return ""
        return if (isUtf8) {
            var p = offset
            val first = buf[p++].toInt() and 0xFF
            if ((first and 0x80) != 0) p++ // skip 2nd byte of utf16 len
            val byteLenFirst = buf[p++].toInt() and 0xFF
            val byteLen = if ((byteLenFirst and 0x80) != 0) {
                ((byteLenFirst and 0x7F) shl 8) or (buf[p++].toInt() and 0xFF)
            } else {
                byteLenFirst
            }
            if (p + byteLen > buf.size) "" else String(buf, p, byteLen, Charsets.UTF_8)
        } else {
            var p = offset
            val first = readU16(buf, p)
            p += 2
            val charLen = if ((first and 0x8000) != 0) {
                val second = readU16(buf, p)
                p += 2
                ((first and 0x7FFF) shl 16) or second
            } else {
                first
            }
            val byteLen = charLen * 2
            if (p + byteLen > buf.size) "" else String(buf, p, byteLen, Charsets.UTF_16LE)
        }
    }

    private fun encodePoolString(str: String, isUtf8: Boolean): ByteArray {
        val baos = ByteArrayOutputStream()
        if (isUtf8) {
            val utf16Len = str.length
            val utf8Bytes = str.toByteArray(Charsets.UTF_8)
            val utf8Len = utf8Bytes.size
            if (utf16Len >= 0x80) {
                baos.write(((utf16Len shr 8) and 0x7F) or 0x80)
                baos.write(utf16Len and 0xFF)
            } else {
                baos.write(utf16Len)
            }
            if (utf8Len >= 0x80) {
                baos.write(((utf8Len shr 8) and 0x7F) or 0x80)
                baos.write(utf8Len and 0xFF)
            } else {
                baos.write(utf8Len)
            }
            baos.write(utf8Bytes)
            baos.write(0)
        } else {
            val charLen = str.length
            if (charLen >= 0x8000) {
                val high = ((charLen shr 16) and 0x7FFF) or 0x8000
                val low = charLen and 0xFFFF
                baos.write(high and 0xFF)
                baos.write((high shr 8) and 0xFF)
                baos.write(low and 0xFF)
                baos.write((low shr 8) and 0xFF)
            } else {
                baos.write(charLen and 0xFF)
                baos.write((charLen shr 8) and 0xFF)
            }
            baos.write(str.toByteArray(Charsets.UTF_16LE))
            baos.write(0)
            baos.write(0)
        }
        return baos.toByteArray()
    }

    private fun readU16(b: ByteArray, off: Int): Int {
        return (b[off].toInt() and 0xFF) or ((b[off + 1].toInt() and 0xFF) shl 8)
    }

    private fun readU32(b: ByteArray, off: Int): Int {
        return (b[off].toInt() and 0xFF) or
            ((b[off + 1].toInt() and 0xFF) shl 8) or
            ((b[off + 2].toInt() and 0xFF) shl 16) or
            ((b[off + 3].toInt() and 0xFF) shl 24)
    }

    private fun readI32(b: ByteArray, off: Int): Int = readU32(b, off)

    private fun writeI32(b: ByteArray, off: Int, value: Int) {
        b[off] = (value and 0xFF).toByte()
        b[off + 1] = ((value shr 8) and 0xFF).toByte()
        b[off + 2] = ((value shr 16) and 0xFF).toByte()
        b[off + 3] = ((value shr 24) and 0xFF).toByte()
    }
}

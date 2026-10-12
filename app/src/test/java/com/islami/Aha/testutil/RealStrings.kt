package com.islami.Aha.testutil

import android.content.Context
import com.islami.Aha.R
import io.mockk.every
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Teks asli dari res/values/strings.xml untuk tes unit JVM (tanpa Robolectric), supaya tes
 * membandingkan teks yang benar-benar dilihat pengguna, bukan "" dari Context tiruan.
 */
object RealStrings {
    private val byName: Map<String, String> by lazy {
        val file = listOf("src/main/res/values/strings.xml", "app/src/main/res/values/strings.xml")
            .map(::File).first { it.exists() }
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        (0 until nodes.length).associate { i ->
            val el = nodes.item(i) as org.w3c.dom.Element
            el.getAttribute("name") to unescape(el.textContent)
        }
    }

    private val nameById: Map<Int, String> by lazy {
        R.string::class.java.fields.associate { it.getInt(null) to it.name }
    }

    fun get(id: Int): String {
        val name = nameById[id] ?: error("ID string tidak dikenal: $id")
        return byName[name] ?: error("String '$name' tidak ada di strings.xml")
    }

    fun format(id: Int, vararg args: Any?): String = String.format(get(id), *args)

    /** Pasang ke Context tiruan: getString(id) dan getString(id, args...) memakai teks asli. */
    fun installOn(context: Context) {
        every { context.getString(any<Int>()) } answers { get(firstArg()) }
        every { context.getString(any<Int>(), *anyVararg()) } answers {
            @Suppress("UNCHECKED_CAST")
            val formatArgs = (args[1] as Array<Any?>)
            format(firstArg(), *formatArgs)
        }
    }

    private fun unescape(raw: String): String = raw.trim()
        .removeSurrounding("\"")
        .replace("\\'", "'")
        .replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\@", "@")
        .replace("\\?", "?")
}

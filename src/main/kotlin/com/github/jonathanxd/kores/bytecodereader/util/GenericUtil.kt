/*
 *      Kores-BytecodeReader - Translates JVM Bytecode to Kores Structure <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2018 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/) <jonathan.scripter@programmer.net>
 *      Copyright (c) contributors
 *
 *
 *      Permission is hereby granted, free of charge, to any person obtaining a copy
 *      of this software and associated documentation files (the "Software"), to deal
 *      in the Software without restriction, including without limitation the rights
 *      to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *      copies of the Software, and to permit persons to whom the Software is
 *      furnished to do so, subject to the following conditions:
 *
 *      The above copyright notice and this permission notice shall be included in
 *      all copies or substantial portions of the Software.
 *
 *      THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *      IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *      FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *      AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *      LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *      OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *      THE SOFTWARE.
 */
package com.github.jonathanxd.kores.bytecodereader.util

import com.github.jonathanxd.kores.generic.GenericSignature
import com.github.jonathanxd.kores.type.Generic
import com.github.jonathanxd.kores.type.GenericType
import com.github.jonathanxd.kores.type.koresType
import com.github.jonathanxd.kores.util.TypeResolver
import com.github.jonathanxd.kores.util.genericSignatureToDescriptor
import com.github.jonathanxd.kores.util.resolveUnknown
import java.text.CharacterIterator
import java.text.StringCharacterIterator

object GenericUtil {
    fun parseFull(typeResolver: TypeResolver, signature: String?): Signature {
        if (signature == null || signature.isEmpty()) {
            return Signature(GenericSignature.empty(), null, emptyArray())
        }


        val genericSignature =
            if (signature.startsWith("<")) GenericUtil.parse(typeResolver, signature)
            else GenericSignature.empty()
        var superType: GenericType? = null
        val interfaces = mutableListOf<GenericType>()

        val str =
            StringBuilder(
                if (genericSignature.isNotEmpty) genericSignature.genericSignatureToDescriptor()
                else ""
            )

        while (signature.length > str.length && signature.startsWith(str)) {
            val sub = signature.substring(str.length)

            val sign = parse(typeResolver, sub)

            if (sign.types.size != 1)
                break

            val type = sign.types[0]

            if (superType == null)
                superType = type
            else
                interfaces.add(type)

            str.append(type.javaSpecName)
        }

        return Signature(genericSignature, superType, interfaces.toTypedArray())
    }

    fun parse(typeResolver: TypeResolver, str: String?): GenericSignature {
        if (str == null || str.isEmpty()) {
            return GenericSignature.empty()
        } else {

            val stringCharacterIterator = StringCharacterIterator(str)

            return if (str.startsWith("<")) {
                parse(stringCharacterIterator, typeResolver)
            } else {
                GenericSignature.create(parseTypeOrVar(stringCharacterIterator, typeResolver)!!)
            }

        }
    }

    fun parse(signature: CharacterIterator, typeResolver: TypeResolver): GenericSignature {
        val stringBuilder = StringBuilder()

        while (signature.current() != ':') {
            if (signature.current() == CharacterIterator.DONE) {
                return GenericSignature.empty()
            }
            if (signature.current() != '<') {
                stringBuilder.append(signature.current())
            }

            signature.next()
        }

        signature.next()

        val x = Generic.type(stringBuilder.toString())

        val generic = parseTypeOrVar(signature, typeResolver)
        return GenericSignature.create(x.`extends$`(generic!!))
    }

    fun parseTypeOrVar(signature: CharacterIterator, typeResolver: TypeResolver): Generic? {
        if (signature.current() == ':') {
            signature.next()
        }

        if (signature.current() == CharacterIterator.DONE) {
            return null
        }

        return parseNameType(signature, typeResolver)
    }

    fun parseNameType(signature: CharacterIterator, typeResolver: TypeResolver): Generic? {
        val current = signature.current()

        if (current == 'L') {
            return parseJavaClass(signature, typeResolver)
        } else if (current == 'T') {
            return parseVar(signature)
        }

        return null
    }

    fun parseJavaClass(signature: CharacterIterator, typeResolver: TypeResolver): Generic? {
        val sb = StringBuilder()

        signature.next()

        if (signature.current() == CharacterIterator.DONE) {
            return null
        }

        var generic: Generic? = null

        while (signature.current() != ';' && signature.current() != '>' && signature.current() != CharacterIterator.DONE) {

            if (signature.current() == '<') {
                val name = sb.toString()

                generic = Generic.type(typeResolver.resolveUnknown(name).koresType)

                signature.next()

                do {
                    val bound = parseTypeOrVar(signature, typeResolver)!!
                    generic = generic!!.of(bound)
                } while (signature.current() != '>')

            }

            sb.append(signature.current())

            signature.next()
        }

        if (signature.current() == CharacterIterator.DONE)
            return null
        signature.next()

        if (generic != null) {
            return generic
        } else {
            return Generic.type(typeResolver.resolveUnknown(sb.toString()).koresType)
        }
    }

    fun parseVar(signature: CharacterIterator): Generic? {
        val sb = StringBuilder()

        signature.next()
        while (signature.current() != ';' && signature.current() != '>' && signature.current() != CharacterIterator.DONE) {
            sb.append(signature.current())
            signature.next()
        }

        if (signature.current() == CharacterIterator.DONE) {
            return null
        }

        if (signature.current() == ';') {
            signature.next()
        }

        return Generic.type(sb.toString())
    }
}
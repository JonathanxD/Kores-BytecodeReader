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
package com.github.jonathanxd.kores.bytecodereader.env

import com.github.jonathanxd.kores.Instruction
import com.github.jonathanxd.kores.base.Access
import com.github.jonathanxd.kores.base.VariableAccess
import org.objectweb.asm.tree.LabelNode
import java.lang.reflect.Type

class EmulatedFrame {

    /**
     * Local variable table
     */
    val localVariableTable = LocalVariableTable()

    /**
     * List with variable names.
     */
    val varList = VariableList { localVariableTable }

    /**
     * Last label
     */
    lateinit var lastLabel: LabelNode
        private set

    /**
     * Operand Stack
     */
    var operandStack = StackManager<Instruction>(null)
        private set

    fun enterStack() {
        operandStack = StackManager(operandStack)
    }

    fun exitStack() {
        this.operandStack = this.operandStack.parent
                ?: throw IllegalStateException("Cannot exit root stack")
    }

    /**
     * Pushes the values to [.localVariableTable].
     *
     * Example of instructions that store value into Variable Table: istore, astore
     *
     * @param values     Part
     * @param startIndex Start Slot index
     */
    fun storeValues(values: List<VariableAccess>, startIndex: Int) {
        for (i in values.indices) {
            this.store(values[i], startIndex + i)
        }
    }

    /**
     * Push the value to [localVariableTable].
     *
     * Example of instructions that store value into Variable Table: istore, astore
     *
     * @param part  Part
     * @param index Slot index
     */
    fun storeAccess(part: Access, index: Int) {
        this.localVariableTable.store(part, index)
    }

    /**
     * Push the value to [localVariableTable].
     *
     * Example of instructions that store value into Variable Table: istore, astore
     *
     * @param part  Part
     * @param index Slot index
     */
    fun store(part: VariableAccess, index: Int) {
        this.localVariableTable.store(part, index)
    }

    /**
     * Pop the top value of the [operandStack] and push into [localVariableTable].
     *
     * @param index Slot index.
     */
    fun storeFromStack(index: Int) {
        this.store(this.operandStack.pop() as VariableAccess, index)
    }

    /**
     * Gets the variable from [localVariableTable].
     *
     * @param index Slot index
     * @return The Variable.
     */
    fun load(index: Int): Instruction {
        return this.localVariableTable[index]
    }

    /**
     * Gets the variable from [localVariableTable] and push to [operandStack].
     *
     * @param index Slot index
     */
    fun loadToStack(index: Int) {
        this.operandStack.push(this.load(index))
    }

    fun storeInfo(
        slot: Int,
        variableType: Type,
        variableName: String,
        start: LabelNode?,
        end: LabelNode?
    ) {
        this.localVariableTable.storeVariableInfo(slot, variableType, variableName, start, end)
    }

    fun getInfo(slot: Int): LocalVariableTable.VariableInfo? {
        return this.localVariableTable.getInfo(slot)
    }

    fun visitLabel(label: LabelNode) {
        this.lastLabel = label
        this.localVariableTable.visitLabel(label)
    }
}

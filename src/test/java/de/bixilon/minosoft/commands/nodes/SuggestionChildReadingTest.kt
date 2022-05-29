/*
 * Minosoft
 * Copyright (C) 2020-2022 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.commands.nodes

import de.bixilon.minosoft.commands.errors.DeadEndError
import de.bixilon.minosoft.commands.errors.literal.InvalidLiteralArgumentError
import de.bixilon.minosoft.commands.errors.literal.TrailingTextArgument
import de.bixilon.minosoft.commands.errors.reader.ExpectedWhitespaceError
import de.bixilon.minosoft.commands.parser.brigadier.string.StringParseError
import de.bixilon.minosoft.commands.parser.brigadier.string.StringParser
import de.bixilon.minosoft.commands.stack.CommandStack
import de.bixilon.minosoft.commands.util.CommandReader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue


internal class SuggestionChildReadingTest {

    private fun createCommand(): CommandNode {
        return RootNode()
            .addChild(
                LiteralNode("1_literal")
                    .addChild(LiteralNode("1_literal_2"))
                    .addChild(LiteralNode("2_literal_2"))
            )
            .addChild(LiteralNode("2_literal"))
            .addChild(
                LiteralNode("3_literal", executable = true)
                    .addChild(ArgumentNode("arg", StringParser(StringParser.StringModes.SINGLE)))
            )
            .addChild(
                LiteralNode("1_execute")
                    .addChild(ArgumentNode("args", StringParser(StringParser.StringModes.GREEDY), executable = true))
            )
    }

    @Test
    fun testCreation() {
        assertTrue(createCommand() is RootNode)
    }

    @Test
    fun testValid() {
        assertEquals(createCommand().getSuggestions(CommandReader("1_execute test"), CommandStack()), emptyList())
    }

    @Test
    fun testValid2() {
        assertEquals(createCommand().getSuggestions(CommandReader("1_execute this is a really long valid greedy read string..."), CommandStack()), emptyList())
    }

    @Test
    fun testInvalidLiteralValid() {
        assertThrows<InvalidLiteralArgumentError> { (createCommand().getSuggestions(CommandReader("3_404_not_found"), CommandStack())) }
    }

    @Test
    fun testBlankStringArgument() {
        assertThrows<StringParseError> { (createCommand().getSuggestions(CommandReader("1_execute "), CommandStack())) }
    }

    @Test
    fun testNoStringArgument() {
        assertThrows<ExpectedWhitespaceError> { (createCommand().getSuggestions(CommandReader("1_execute"), CommandStack())) }
    }

    @Test
    fun testPrefixSuggestions() {
        assertEquals(createCommand().getSuggestions(CommandReader("1_"), CommandStack()), listOf("1_literal", "1_execute"))
    }

    @Test
    fun testEmptySuggestions() {
        assertEquals(createCommand().getSuggestions(CommandReader(""), CommandStack()), listOf("1_literal", "2_literal", "3_literal", "1_execute"))
    }

    @Test
    fun testWhitespaceSuggestions() {
        assertEquals(createCommand().getSuggestions(CommandReader(" "), CommandStack()), listOf("1_literal", "2_literal", "3_literal", "1_execute"))
    }

    @Test
    fun testTrailingData() {
        assertThrows<TrailingTextArgument> { (createCommand().getSuggestions(CommandReader("2_literal test"), CommandStack())) }
    }

    @Test
    fun test2InvalidLiteral() {
        assertThrows<InvalidLiteralArgumentError> { (createCommand().getSuggestions(CommandReader("1_literal test"), CommandStack())) }
    }

    @Test
    fun test2EmptyLevelSuggestions() {
        assertEquals(createCommand().getSuggestions(CommandReader("1_literal"), CommandStack()), listOf("1_literal_2", "2_literal_2"))
    }

    @Test
    fun test2TrailingData() {
        assertThrows<TrailingTextArgument> { (createCommand().getSuggestions(CommandReader("2_literal 1_literal_2 test"), CommandStack())) }
    }

    @Test
    fun checkDeadEnd() {
        assertThrows<DeadEndError> { createCommand().getSuggestions(CommandReader("2_literal"), CommandStack()) }
    }

    @Test
    fun testTrailingWhitespace() {
        assertThrows<DeadEndError> { createCommand().getSuggestions(CommandReader("2_literal "), CommandStack()) }
    }

    @Test
    fun test2TrailingWhitespace() {
        assertThrows<DeadEndError> { createCommand().getSuggestions(CommandReader("1_literal 1_literal_2 "), CommandStack()) }
    }

    @Test
    fun testEmptyNode() {
        assertThrows<DeadEndError> { RootNode().getSuggestions(CommandReader(""), CommandStack()) }
    }

    @Test
    fun testTrailingTextEmptyNode() {
        assertThrows<TrailingTextArgument> { RootNode().getSuggestions(CommandReader("trailing"), CommandStack()) }
    }

    @Test
    fun testEmptyWhitespaceSuggestions() {
        assertThrows<DeadEndError> { RootNode().getSuggestions(CommandReader(" "), CommandStack()) }
    }

    @Test
    fun testOptionalArguments() {
        assertDoesNotThrow { createCommand().getSuggestions(CommandReader("3_literal"), CommandStack()) }
    }

    @Test
    fun testStringOptionalArguments() {
        assertDoesNotThrow { createCommand().getSuggestions(CommandReader("3_literal string"), CommandStack()) }
    }
}

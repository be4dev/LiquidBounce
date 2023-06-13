/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_COMMIT
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_CREATOR
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_VERSION
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.ServerUtils
import net.ccbluex.liquidbounce.utils.extensions.getPing
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.Minecraft
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.text.DecimalFormat
import java.text.SimpleDateFormat

/**
 * CustomHUD text element
 *
 * Allows to draw custom text
 */
@ElementInfo(name = "Text")
class Text(x: Double = 10.0, y: Double = 10.0, scale: Float = 1F, side: Side = Side.default()) : Element(x, y, scale, side) {

    companion object {

        val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")
        val HOUR_FORMAT = SimpleDateFormat("HH:mm")

        val DECIMAL_FORMAT = DecimalFormat("0.00")

        /**
         * Create default element
         */
        fun defaultClient(): Text {
            val text = Text(x = 2.0, y = 2.0, scale = 2F)

            text.displayString = "%clientName%"
            text.shadow = true
            text.font = Fonts.font40
            text.color = Color(0, 111, 255)

            return text
        }

    }

    private var displayString by TextValue("DisplayText", "")

    private val rainbow by BoolValue("Rainbow", false)
    private val rainbowX by FloatValue("Rainbow-X", -1000F, -2000F..2000F) { rainbow }
    private val rainbowY by FloatValue("Rainbow-Y", -1000F, -2000F..2000F) { rainbow }

    private var red by IntegerValue("Red", 255, 0..255) { !rainbow }
    private var green by IntegerValue("Green", 255, 0..255) { !rainbow }
    private var blue by IntegerValue("Blue", 255, 0..255) { !rainbow }

    private var shadow by BoolValue("Shadow", true)
    private var font by FontValue("Font", Fonts.font40)

    private var editMode = false
    private var editTicks = 0
    private var prevClick = 0L

    private var displayText = display

    private val display: String
        get() {
            val textContent = if (displayString.isEmpty() && !editMode)
                "Text Element"
            else
                displayString


            return multiReplace(textContent)
        }

    private var color: Color
        get() = Color(red, green, blue)
        set(value) {
            red = value.red
            green = value.green
            blue = value.blue
        }

    private fun getReplacement(str: String): String? {
        val thePlayer = mc.thePlayer

        if (thePlayer != null) {
            when (str.lowercase()) {
                "x" -> return DECIMAL_FORMAT.format(thePlayer.posX)
                "y" -> return DECIMAL_FORMAT.format(thePlayer.posY)
                "z" -> return DECIMAL_FORMAT.format(thePlayer.posZ)
                "xdp" -> return thePlayer.posX.toString()
                "ydp" -> return thePlayer.posY.toString()
                "zdp" -> return thePlayer.posZ.toString()
                "velocity" -> return DECIMAL_FORMAT.format(speed)
                "ping" -> return thePlayer.getPing().toString()
                "health" -> return DECIMAL_FORMAT.format(thePlayer.health)
                "maxhealth" -> return DECIMAL_FORMAT.format(thePlayer.maxHealth)
                "food" -> return thePlayer.foodStats.foodLevel.toString()
            }
        }

        return when (str.lowercase()) {
            "username" -> mc.session.username
            "clientname" -> CLIENT_NAME
            "clientversion" -> CLIENT_VERSION
            "clientcommit" -> CLIENT_COMMIT
            "clientcreator" -> CLIENT_CREATOR
            "fps" -> Minecraft.getDebugFPS().toString()
            "date" -> DATE_FORMAT.format(System.currentTimeMillis())
            "time" -> HOUR_FORMAT.format(System.currentTimeMillis())
            "serverip" -> ServerUtils.remoteIp
            "cps", "lcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.LEFT).toString()
            "mcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.MIDDLE).toString()
            "rcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.RIGHT).toString()
            else -> null // Null = don't replace
        }
    }

    private fun multiReplace(str: String): String {
        var lastPercent = -1
        val result = StringBuilder()
        for (i in str.indices) {
            if (str[i] == '%') {
                if (lastPercent != -1) {
                    if (lastPercent + 1 != i) {
                        val replacement = getReplacement(str.substring(lastPercent + 1, i))

                        if (replacement != null) {
                            result.append(replacement)
                            lastPercent = -1
                            continue
                        }
                    }
                    result.append(str, lastPercent, i)
                }
                lastPercent = i
            } else if (lastPercent == -1) {
                result.append(str[i])
            }
        }

        if (lastPercent != -1) {
            result.append(str, lastPercent, str.length)
        }

        return result.toString()
    }

    /**
     * Draw element
     */
    override fun drawElement(): Border {
        val rainbow = rainbow

        RainbowFontShader.begin(rainbow, if (rainbowX == 0f) 0f else 1f / rainbowX, if (rainbowY == 0f) 0f else 1f / rainbowY, System.currentTimeMillis() % 10000 / 10000F).use {
            font.drawString(displayText, 0F, 0F, if (rainbow)
                0 else color.rgb, shadow)

            if (editMode && mc.currentScreen is GuiHudDesigner && editTicks <= 40)
                font.drawString("_", font.getStringWidth(displayText) + 2F,
                        0F, if (rainbow) ColorUtils.rainbow(400000000L).rgb else color.rgb, shadow)
        }

        if (editMode && mc.currentScreen !is GuiHudDesigner) {
            editMode = false
            updateElement()
        }

        return Border(-2F, -2F, font.getStringWidth(displayText) + 2F, font.FONT_HEIGHT.toFloat())
    }

    override fun updateElement() {
        editTicks += 5
        if (editTicks > 80) editTicks = 0

        displayText = if (editMode) displayString else display
    }

    override fun handleMouseClick(x: Double, y: Double, mouseButton: Int) {
        if (isInBorder(x, y) && mouseButton == 0) {
            if (System.currentTimeMillis() - prevClick <= 250L)
                editMode = true

            prevClick = System.currentTimeMillis()
        } else {
            editMode = false
        }
    }

    override fun handleKey(c: Char, keyCode: Int) {
        if (editMode && mc.currentScreen is GuiHudDesigner) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (displayString.isNotEmpty())
                    displayString = displayString.dropLast(1)

                updateElement()
                return
            }

            if (ColorUtils.isAllowedCharacter(c) || c == '§')
                displayString += c

            updateElement()
        }
    }
}
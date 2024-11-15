/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.nocheatplus

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket

/**
 * @anticheat NoCheatPlus
 * @anticheatVersion 3.16.1-SNAPSHOT-sMD5NET-b115s
 * @testedOn eu.loyisa.cn
 */

internal object NoCheatPlusBow : Choice("NoCheatPlusBow") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleLongJump.mode

    private var receivedArrows = 0
    private var shotArrows = 0

    private val rotations = tree(RotationsConfigurable(this))
    private val bowCharge by int("BowCharge", 4, 3..20)
    private val arrowsToReceive by int("ArrowsToReceive", 8, 0..20)
    private val additionalArrowsToShoot by int("AdditionalArrowsToShoot", 8, 0..20)
    private val workingTime by int("WorkingTime", 20, 0..100)
    private val boostSpeed by float("BoostSpeed", 2.5f, 0f..20f)
    private val fallDistance by float("FallDistanceToJump", 0.42f, 0f..2f)

    private var stopMovement = false
    private var timeLeft = 0

    val movementInputHandler = handler<MovementInputEvent> {
        if (stopMovement) {
            it.directionalInput = DirectionalInput.NONE
        }
    }

    val tickJumpHandler = repeatable {
        if (timeLeft > 0) {
            return@repeatable
        }

        if (receivedArrows >= arrowsToReceive) {
            mc.options.useKey.isPressed = false
            if (player.isUsingItem) {
                interaction.stopUsingItem(player)
            }

            timeLeft = workingTime
            shotArrows = 0
            receivedArrows = 0
            stopMovement = false
            waitTicks(5)
            player.jump()
            player.strafe(speed = boostSpeed.toDouble())
            return@repeatable
        }


        if (shotArrows <= arrowsToReceive + additionalArrowsToShoot) {
            RotationManager.aimAt(
                Rotation(player.yaw, -90f),
                configurable = rotations,
                priority = Priority.IMPORTANT_FOR_USAGE_2,
                provider = ModuleLongJump
            )

            // Stops moving
            stopMovement = true

            // Shoots arrow
            mc.options.useKey.isPressed = true
            if (player.itemUseTime >= bowCharge) {
                interaction.stopUsingItem(player)
                shotArrows++
            }
        }
    }

    val handleMovementInput = handler<MovementInputEvent> {
        if (timeLeft <= 0) {
            return@handler
        }

        timeLeft--
        if (player.fallDistance >= fallDistance) {
            player.jump()
            player.fallDistance = 0f
        }
    }

    val velocityHandler = handler<PacketEvent> {
        val packet = it.packet

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id && shotArrows > 0.0) {
            receivedArrows++
        }
    }

    override fun enable() {
        shotArrows = 0
        receivedArrows = 0
        stopMovement = false
        timeLeft = 0
    }
}

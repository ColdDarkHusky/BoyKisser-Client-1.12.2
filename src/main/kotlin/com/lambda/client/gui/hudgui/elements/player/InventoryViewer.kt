package com.lambda.client.gui.hudgui.elements.player

import com.lambda.client.event.SafeClientEvent
import com.lambda.client.event.events.PacketEvent
import com.lambda.client.gui.hudgui.HudElement
import com.lambda.client.manager.managers.CachedContainerManager
import com.lambda.client.util.color.ColorHolder
import com.lambda.client.util.graphics.GlStateUtils
import com.lambda.client.util.graphics.RenderUtils2D
import com.lambda.client.util.graphics.VertexHelper
import com.lambda.client.util.items.storageSlots
import com.lambda.client.util.math.Vec2d
import com.lambda.client.util.threads.runSafe
import com.lambda.client.util.threads.safeListener
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.network.play.server.SPacketOpenWindow
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.TextComponentTranslation
import org.lwjgl.opengl.GL11.*

internal object InventoryViewer : HudElement(
    name = "InventoryViewer",
    category = Category.PLAYER,
    description = "Items in Inventory"
) {
    private val enderChest by setting("Inventory", SlotType.PLAYER)
    private val mcTexture by setting("Minecraft Texture", false)
    private val showIcon by setting("Show Icon", false, { !mcTexture })
    private val iconScale by setting("Icon Scale", 0.5f, 0.1f..1.0f, 0.1f, { !mcTexture && showIcon })
    private val background by setting("Background", true, { !mcTexture })
    private val backgroundColor by setting("Background Color", ColorHolder(0, 0, 0, 150), visibility = { !mcTexture && background })
    private val outline by setting("Outline", true, visibility = { !mcTexture })
    private val outlineColor by setting("Outline Color", ColorHolder(255, 255, 255, 150), visibility = { !mcTexture && outline })
    private val outlineThickness by setting("Outline Thickness", 1.0f, 0.5f..5.0f, 0.5f, { !mcTexture && outline })
    private val containerTexture = ResourceLocation("textures/gui/container/inventory.png")
    private val lambdaIcon = ResourceLocation("lambda/lambda_icon.png")

    override val hudWidth: Float = 162.0f
    override val hudHeight: Float = 54.0f

    override fun renderHud(vertexHelper: VertexHelper) {
        super.renderHud(vertexHelper)
        runSafe {
            drawFrame(vertexHelper)
            drawFrameTexture()
            drawItems()
        }
    }

    private fun drawFrame(vertexHelper: VertexHelper) {
        if (!mcTexture) {
            if (background) {
                RenderUtils2D.drawRectFilled(vertexHelper, posEnd = Vec2d(hudWidth.toDouble(), hudHeight.toDouble()), color = backgroundColor)
            }
            if (outline) {
                RenderUtils2D.drawRectOutline(vertexHelper, posEnd = Vec2d(hudWidth.toDouble(), hudHeight.toDouble()), lineWidth = outlineThickness, color = outlineColor)
            }
        }
    }

    private fun drawFrameTexture() {
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer
        GlStateUtils.texture2d(true)

        if (mcTexture) {
            mc.renderEngine.bindTexture(containerTexture)
            buffer.begin(GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_TEX)
            buffer.pos(0.0, 0.0, 0.0).tex(0.02734375, 0.32421875).endVertex() // (7 / 256), (83 / 256)
            buffer.pos(0.0, hudHeight.toDouble(), 0.0).tex(0.02734375, 0.53125).endVertex() // (7 / 256), (136 / 256)
            buffer.pos(hudWidth.toDouble(), 0.0, 0.0).tex(0.65625, 0.32421875).endVertex() // (168 / 256), (83 / 256)
            buffer.pos(hudWidth.toDouble(), hudHeight.toDouble(), 0.0).tex(0.65625, 0.53125).endVertex() // (168 / 256), (136 / 256)
            tessellator.draw()
        } else if (showIcon) {
            mc.renderEngine.bindTexture(lambdaIcon)
            GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)

            val center = Vec2d(hudWidth / 2.0, hudHeight / 2.0)
            val halfWidth = iconScale * 50.0
            val halfHeight = iconScale * 50.0

            buffer.begin(GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_TEX)
            buffer.pos(center.x - halfWidth, center.y - halfHeight, 0.0).tex(0.0, 0.0).endVertex()
            buffer.pos(center.x - halfWidth, center.y + halfHeight, 0.0).tex(0.0, 1.0).endVertex()
            buffer.pos(center.x + halfWidth, center.y - halfHeight, 0.0).tex(1.0, 0.0).endVertex()
            buffer.pos(center.x + halfWidth, center.y + halfHeight, 0.0).tex(1.0, 1.0).endVertex()
            tessellator.draw()

            GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        }
    }


    init {
        safeListener<PacketEvent.Receive> {
            if (it.packet !is SPacketOpenWindow) return@safeListener
            if (it.packet.guiId != "minecraft:container") return@safeListener
            val title = it.packet.windowTitle
            if (title !is TextComponentTranslation) return@safeListener
            if (title.key != "container.enderchest") return@safeListener

        }
    }

    private fun SafeClientEvent.drawItems() {
        if (enderChest == SlotType.ENDER_CHEST) {
            CachedContainerManager.getEnderChestInventory().forEachIndexed { index, stack ->
                if (stack.isEmpty) return@forEachIndexed
                val slotX = index % 9 * (hudWidth / 9.0) + 1
                val slotY = index / 9 * (hudWidth / 9.0) + 1
                RenderUtils2D.drawItem(stack, slotX.toInt(), slotY.toInt())
            }
        } else {
            for ((index, slot) in player.storageSlots.withIndex()) {
                val itemStack = slot.stack
                if (itemStack.isEmpty) continue

                val slotX = index % 9 * (hudWidth / 9.0) + 1
                val slotY = index / 9 * (hudWidth / 9.0) + 1

                RenderUtils2D.drawItem(itemStack, slotX.toInt(), slotY.toInt())
            }
        }
    }

    private enum class SlotType {
        PLAYER, ENDER_CHEST
    }
}
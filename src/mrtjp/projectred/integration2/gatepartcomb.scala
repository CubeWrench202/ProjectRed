/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.integration2

import java.util.Random

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import mrtjp.projectred.api.IScrewdriver
import mrtjp.projectred.core.{TFaceOrient, Configurator}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.EnumSkyBlock

class ComboGatePart extends RedstoneGatePart
{
    /**
     * Mapped inputs and outputs of the gate.
     * OOOO IIII
     * High nybble is output.
     * Low nybble is input
     */
    private var gateState:Byte = 0

    def state = gateState&0xFF
    def setState(s:Int)
    {
        gateState = s.asInstanceOf[Byte]
    }

    override def getLogic[T] = ComboGateLogic.instances(subID).asInstanceOf[T]
    def getLogicCombo = getLogic[ComboGateLogic]

    override def save(tag:NBTTagCompound)
    {
        super.save(tag)
        tag.setByte("state", gateState)
    }

    override def load(tag:NBTTagCompound)
    {
        super.load(tag)
        gateState = tag.getByte("state")
    }

    override def writeDesc(packet:MCDataOutput)
    {
        super.writeDesc(packet)
        packet.writeByte(gateState)
    }

    override def readDesc(packet:MCDataInput)
    {
        super.readDesc(packet)
        gateState = packet.readByte
    }

    override def read(packet:MCDataInput, key:Int) = key match
    {
        case 10 =>
            gateState = packet.readByte()
            if (Configurator.staticGates) tile.markRender()
        case _ => super.read(packet, key)
    }

    override def onWorldJoin()
    {
        super.onWorldJoin()
        if (getLogic == null) tile.remPart(this)
    }

    override def getType = "pr_sgate"

    def sendStateUpdate()
    {
        getWriteStreamOf(10).writeByte(gateState)
    }

    def onInputChange()
    {
        tile.markDirty()
        sendStateUpdate()
    }

    def onOutputChange(mask:Int)
    {
        tile.markDirty()
        sendStateUpdate()
        tile.internalPartChange(this)
        notifyExternals(toAbsoluteMask(mask))
    }
}

object ComboGateLogic
{
    val advanceDead = Array(1, 2, 4, 0, 5, 6, 3)

    val instances = new Array[ComboGateLogic](GateDefinition.values.length)
    initialize()

    def initialize()
    {
        import mrtjp.projectred.integration2.{GateDefinition => defs}

        instances(defs.OR.ordinal) = OR
        instances(defs.NOR.ordinal) = NOR
        instances(defs.NOT.ordinal) = NOT
        instances(defs.AND.ordinal) = AND
        instances(defs.NAND.ordinal) = NAND
        instances(defs.XOR.ordinal) = XOR
        instances(defs.XNOR.ordinal) = XNOR
        instances(defs.Buffer.ordinal) = Buffer
        instances(defs.Multiplexer.ordinal) = Multiplexer
        instances(defs.Pulse.ordinal) = Pulse
        instances(defs.Repeater.ordinal) = Repeater
        instances(defs.Randomizer.ordinal) = Randomizer

        instances(defs.TransparentLatch.ordinal) = TransparentLatch
        instances(defs.LightSensor.ordinal) = LightSensor
        instances(defs.RainSensor.ordinal) = RainSensor

        instances(defs.ANDCell.ordinal) = ANDCell
    }
}

class ComboGateLogic extends RedstoneGateLogic[ComboGatePart]
{
    override def getOutput(gate:ComboGatePart, r:Int) =
        if ((gate.state&0x10<<r) != 0) 15 else 0

    override def cycleShape(gate:ComboGatePart) =
    {
        val oldShape = gate.shape
        val newShape = cycleShape(oldShape)
        if (newShape != oldShape)
        {
            gate.setShape(newShape)
            true
        }
        else false
    }

    def cycleShape(shape:Int) =
    {
        var shape1 = shape
        import java.lang.Integer.{bitCount, numberOfTrailingZeros => trail}
        do shape1 = ComboGateLogic.advanceDead(shape1)
        while (bitCount(shape1) > maxDeadSides && 32-trail(shape1) <= maxDeadSides)
        shape1
    }

    def deadSides = 0
    def maxDeadSides = deadSides-1

    def getDelay(shape:Int) = 2

    def feedbackMask(shape:Int) = 0

    override def onChange(gate:ComboGatePart)
    {
        val iMask = inputMask(gate.shape)
        val oMask = outputMask(gate.shape)
        val fMask = feedbackMask(gate.shape)
        val oldInput = gate.state&0xF
        val newInput = getInput(gate, iMask|fMask)
        if (oldInput != newInput)
        {
            gate.setState(gate.state&0xF0|newInput)
            gate.onInputChange()
        }

        val newOutput = calcOutput(gate, gate.state&iMask)&oMask
        if (newOutput != (gate.state>>4)) gate.scheduleTick(getDelay(gate.shape))
    }

    override def scheduledTick(gate:ComboGatePart)
    {
        val iMask = inputMask(gate.shape)
        val oMask = outputMask(gate.shape)
        val oldOutput = gate.state>>4
        val newOutput = calcOutput(gate, gate.state&iMask)&oMask
        if (oldOutput != newOutput)
        {
            gate.setState(gate.state&0xF|newOutput<<4)
            gate.onOutputChange(oMask)
        }
        onChange(gate)
    }

    override def setup(gate:ComboGatePart)
    {
        val iMask = inputMask(gate.shape)
        val oMask = outputMask(gate.shape)
        val output = calcOutput(gate, getInput(gate, iMask))&oMask
        if (output != 0)
        {
            gate.setState(output<<4)
            gate.onOutputChange(output) //use output for change mask because nothing is going low
        }
    }
}

object OR extends ComboGateLogic
{
    override def deadSides = 3

    override def inputMask(shape:Int) = ~shape<<1&0xE

    override def calcOutput(gate:ComboGatePart, input:Int) = if (input != 0) 1 else 0
}

object NOR extends ComboGateLogic
{
    override def feedbackMask(shape:Int) = 1

    override def inputMask(shape:Int) = ~shape<<1&0xE

    override def deadSides = 3

    override def calcOutput(gate:ComboGatePart, input:Int) = if (input == 0) 1 else 0
}

object NOT extends ComboGateLogic
{
    override def feedbackMask(shape:Int) = outputMask(shape)

    override def outputMask(shape:Int) =
    {
        val m = (shape&1)<<1|(shape&2)>>1|(shape&4)<<1
        ~m&0xB
    }

    override def inputMask(shape:Int) = 4

    override def deadSides = 3

    override def calcOutput(gate:ComboGatePart, input:Int) = if (input == 0) 0xB else 0
}

object AND extends ComboGateLogic
{
    override def inputMask(shape:Int) = ~shape<<1&0xE

    override def deadSides = 3

    override def calcOutput(gate:ComboGatePart, input:Int) = if (input == inputMask(gate.shape)) 1 else 0
}

object NAND extends ComboGateLogic
{
    override def inputMask(shape:Int) = ~shape<<1&0xE

    override def deadSides = 3

    override def calcOutput(gate:ComboGatePart, input:Int) = if (input == inputMask(gate.shape)) 0 else 1
}

object XOR extends ComboGateLogic
{
    override def inputMask(shape:Int) = 10

    override def calcOutput(gate:ComboGatePart, input:Int) =
    {
        val side1 = (input&1<<1) != 0
        val side2 = (input&1<<3) != 0
        if (side1 != side2) 1 else 0
    }
}

object XNOR extends ComboGateLogic
{
    override def inputMask(shape:Int) = 10

    override def calcOutput(gate:ComboGatePart, input:Int) =
    {
        val side1 = (input&1<<1) != 0
        val side2 = (input&1<<3) != 0
        if (side1 == side2) 1 else 0
    }
}

object Buffer extends ComboGateLogic
{
    override def feedbackMask(shape:Int) = outputMask(shape)

    override def outputMask(shape:Int) =
    {
        val m = (shape&1)<<1|(shape&2)<<2|(shape&8)<<4
        ~m&0xB
    }

    override def inputMask(shape:Int) = 4

    override def deadSides = 2

    override def calcOutput(gate:ComboGatePart, input:Int) = if (input != 0) 0xB else 0
}

object Multiplexer extends ComboGateLogic
{
    override def outputMask(shape:Int) = 1

    override def inputMask(shape:Int) = 0xE

    override def calcOutput(gate:ComboGatePart, input:Int) = if ((input&1<<2) != 0) (input>>3)&1 else (input>>1)&1
}

object Pulse extends ComboGateLogic
{
    override def calcOutput(gate:ComboGatePart, input:Int) = 0

    override def inputMask(shape:Int) = 4

    override def onChange(gate:ComboGatePart) =
    {
        val oldInput = gate.state&0xF
        val newInput = getInput(gate, 4)

        if (oldInput != newInput)
        {
            gate.setState(gate.state&0xF0|newInput)
            gate.onInputChange()
            if (newInput != 0 && (gate.state&0xF0) == 0)
            {
                gate.setState(gate.state&0xF|0x10)
                gate.scheduleTick(2)
                gate.onOutputChange(1)
            }
        }
    }
}

object Repeater extends ComboGateLogic
{
    val delays = Array(2, 4, 6, 8, 16, 32, 64, 128, 256)

    override def calcOutput(gate:ComboGatePart, input:Int) = if (input == 0) 0 else 1

    override def inputMask(shape:Int) = 4

    override def onChange(gate:ComboGatePart) = if (gate.schedTime < 0) super.onChange(gate)

    override def getDelay(shape:Int) = delays(shape)

    override def cycleShape(shape:Int) = (shape+1)%delays.length

    override def activate(gate:ComboGatePart, player:EntityPlayer, held:ItemStack, hit:MovingObjectPosition)=
    {
        if (held == null || !held.getItem.isInstanceOf[IScrewdriver])
        {
            if (!gate.world.isRemote) gate.configure()
            true
        }
        else false
    }
}

object Randomizer extends ComboGateLogic
{
    val rand = new Random

    override def calcOutput(gate:ComboGatePart, input:Int) =
        if (input == 0) gate.state>>4 else TFaceOrient.shiftMask(rand.nextInt(8), 3)

    override def outputMask(shape:Int) = 0xB

    override def inputMask(shape:Int) = 4

    override def onChange(gate:ComboGatePart)
    {
        super.onChange(gate)
        if ((gate.state&4) != 0) gate.scheduleTick(2)
    }
}

object TransparentLatch extends ComboGateLogic
{
    override def cycleShape(shape:Int) = shape^1

    override def inputMask(shape:Int) = if (shape == 0) 0xC else 6

    override def outputMask(shape:Int) = if (shape == 0) 3 else 9

    override def calcOutput(gate:ComboGatePart, input:Int) =
    {
        if ((input&4) == 0) gate.state>>4
        else if ((input&0xA) == 0) 0 else 0xF
    }
}

object LightSensor extends ComboGateLogic
{
    override def getOutput(gate:ComboGatePart, r:Int) = if (r == 2) gate.state>>4 else 0

    override def inputMask(shape:Int) = 0

    override def outputMask(shape:Int) = 4

    override def feedbackMask(shape:Int) = 4

    override def cycleShape(shape:Int) = (shape+1)%3

    override def setup(gate:ComboGatePart){ onTick(gate) }

    //TODO unnecessary, all calls to this overridden
    override def calcOutput(gate:ComboGatePart, input:Int) = gate.state>>4

    override def onTick(gate:ComboGatePart)
    {
        if (gate.world.isRemote) return

        def sky = gate.world.getSavedLightValue(EnumSkyBlock.Sky, gate.x, gate.y, gate.z)-gate.world.skylightSubtracted
        def block = gate.world.getSavedLightValue(EnumSkyBlock.Block, gate.x, gate.y, gate.z)

        val shape = gate.shape
        val newOutput = shape match
        {
            case 1 => sky
            case 2 => block
            case _ => Math.max(sky, block)
        }

        if (newOutput != (gate.state>>4))
        {
            gate.setState(newOutput<<4|gate.state&0xF)
            gate.onOutputChange(4)
        }
    }

    override def onChange(gate:ComboGatePart)
    {
        val oldInput = gate.state&0xF
        val newInput = getInput(gate, 4)
        if (oldInput != newInput)
        {
            gate.setState(gate.state&0xF0|newInput)
            gate.onInputChange()
        }
    }

    override def lightLevel = 0
}

object RainSensor extends ComboGateLogic
{
    override def inputMask(shape:Int) = 0

    override def outputMask(shape:Int) = 4

    override def feedbackMask(shape:Int) = 4

    override def onTick(gate:ComboGatePart)
    {
        if (gate.world.isRemote) return

        val newOutput = if (gate.world.isRaining && gate.world.canBlockSeeTheSky(gate.x, gate.y+1, gate.z)) 4 else 0
        val oldOutput = gate.state>>4
        if (newOutput != oldOutput)
        {
            gate.setState(newOutput<<4|gate.state&0xF)
            gate.onOutputChange(4)
        }
    }

    override def lightLevel = 0
}

object ANDCell extends ComboGateLogic
{
    //TODO
}
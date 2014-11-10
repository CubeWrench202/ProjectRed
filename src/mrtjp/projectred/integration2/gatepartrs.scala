/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.integration2

import java.util.Random

import codechicken.lib.vec.BlockCoord
import codechicken.multipart.IRandomDisplayTick
import codechicken.multipart.handler.MultipartProxy
import mrtjp.projectred.api.IConnectable
import mrtjp.projectred.transmission.{IRedwireEmitter, TFaceRSAcquisitions}

abstract class RedstoneGatePart extends GatePart with TFaceRSAcquisitions with IRandomDisplayTick
{
    def getLogicRS = getLogic[RedstoneGateLogic[RedstoneGatePart]]

    override def strongPowerLevel(side:Int):Int =
    {
        if ((side&6) == (this.side&6)) return 0
        val ir = toInternal(absoluteRot(side))
        if ((getLogicRS.outputMask(shape)&1<<ir) != 0) getLogicRS.getOutput(this, ir) else 0
    }

    override def weakPowerLevel(side:Int) = strongPowerLevel(side)

    override def canConnectRedstone(side:Int) =
    {
        if ((side&6) == (this.side&6)) false
        else getLogicRS.canConnect(this, toInternal(absoluteRot(side)))
    }

    override def notifyExternals(mask:Int)
    {
        var smask = 0
        val block = MultipartProxy.block
        val pos = new BlockCoord
        val pos2 = new BlockCoord

        for (r <- 0 until 4) if ((mask&1<<r) != 0)
        {
            val absSide = absoluteDir(r)
            pos.set(x, y, z).offset(absSide)

            world.notifyBlockOfNeighborChange(pos.x, pos.y, pos.z, block)
            for (s <- 0 until 6) if (s != (absSide^1) && (smask&1<<s) == 0)
            {
                pos2.set(pos).offset(s)
                world.notifyBlockOfNeighborChange(pos2.x, pos2.y, pos2.z, block)
            }
            smask |= 1<<absSide
        }
    }

    def getRedstoneInput(r:Int) =
    {
        val ar = toAbsolute(r)
        if (maskConnectsCorner(ar)) calcCornerSignal(ar)
        else if (maskConnectsStraight(ar)) calcStraightSignal(ar)
        else if (maskConnectsInside(ar)) calcInternalSignal(ar)
        else calcMaxSignal(ar, getLogicRS.requireStrongInput(r))
    }

    override def resolveSignal(part:Any, r:Int) = part match
    {
        case re:IRedwireEmitter => re.getRedwireSignal(r)
        case _ => 0
    }

    override def randomDisplayTick(rand:Random)
    {
        RenderGate.spawnParticles(this, rand)
    }
}

abstract class RedstoneGateLogic[T <: RedstoneGatePart] extends GateLogic[T]
{
    override def canConnecetTo(gate:T, part:IConnectable, r:Int) = part match
    {
        case re:IRedwireEmitter => canConnect(gate, r)
        case _ => false
    }

    def canConnect(gate:T, r:Int):Boolean = canConnect(gate.shape, r)
    def canConnect(shape:Int, r:Int):Boolean = ((inputMask(shape)|outputMask(shape))&1<<r) != 0

    def inputMask(shape:Int) = 0xE
    def outputMask(shape:Int) = 1

    def getOutput(gate:T, r:Int):Int

    def getInput(gate:T, mask:Int) =
    {
        var input = 0
        for (r <- 0 until 4) if ((mask&1<<r) != 0 && gate.getRedstoneInput(r) > 0)
            input |= 1<<r
        input
    }

    def calcOutput(gate:T, input:Int) = 0

    def requireStrongInput(r:Int) = false
}
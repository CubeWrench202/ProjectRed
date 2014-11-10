/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.integration2

import codechicken.lib.packet.PacketCustom
import codechicken.multipart.MultiPartRegistry
import codechicken.multipart.MultiPartRegistry.IPartFactory
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.gui.GuiHandler
import mrtjp.projectred.ProjectRedIntegration
import mrtjp.projectred.ProjectRedIntegration._
import mrtjp.projectred.core.IProxy
import net.minecraftforge.client.MinecraftForgeClient

class IntegrationProxy_server extends IProxy with IPartFactory
{
    override def preinit()
    {
        PacketCustom.assignHandler(IntegrationSPH.channel, IntegrationSPH) //TODO
    }

    override def init()
    {
        MultiPartRegistry.registerParts(this, Array[String](
            "pr_sgate", "pr_igate", "pr_agate",
            "pr_bgate", "pr_tgate", "pr_rgate"
        ))

        itemPartGate2 = new ItemPartGate

        //IntegrationRecipes.initRecipes() //TODO
    }

    override def postinit(){}

    override def createPart(name:String, client:Boolean) = name match
    {
        case "pr_sgate" => new ComboGatePart
        case "pr_igate" => new SequentialGatePart
        case "pr_agate" => null
        case "pr_bgate" => new BundledGatePart
        case "pr_tgate" => new SequentialGatePartT
        case "pr_rgate" => null
        case _ => null
    }

    override def version = "@VERSION@"
    override def build = "@BUILD_NUMBER@"
}

class IntegrationProxy_client extends IntegrationProxy_server
{
    val timerGui = 10
    val counterGui = 11

    @SideOnly(Side.CLIENT)
    override def preinit()
    {
        super.preinit()
        PacketCustom.assignHandler(IntegrationCPH.channel, IntegrationCPH)
    }

    @SideOnly(Side.CLIENT)
    override def init()
    {
        super.init()
        MinecraftForgeClient.registerItemRenderer(ProjectRedIntegration.itemPartGate2, GateItemRenderer)

        GuiHandler.register(GuiTimer, timerGui)
        GuiHandler.register(GuiCounter, counterGui)
    }
}

object IntegrationProxy extends IntegrationProxy_client
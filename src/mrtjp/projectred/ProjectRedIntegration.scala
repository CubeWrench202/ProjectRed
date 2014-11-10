package mrtjp.projectred

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}
import mrtjp.projectred.integration.{EnumGate, IntegrationProxy, ItemPartGate}
import mrtjp.projectred.integration2.GateDefinition
import net.minecraft.creativetab.CreativeTabs

@Mod(modid = "ProjRed|Integration", useMetadata = true, modLanguage = "scala")
object ProjectRedIntegration
{
    /** Multipart items **/
    var itemPartGate:ItemPartGate = null
    var itemPartGate2:integration2.ItemPartGate = null

    var tabIntegration = new CreativeTabs("int")
    {
        override def getIconItemStack = EnumGate.Timer.makeStack
        override def getTabIconItem = getIconItemStack.getItem
    }

    var tabIntegration2 = new CreativeTabs("int2")
    {
        override def getIconItemStack = GateDefinition.OR.makeStack
        override def getTabIconItem = getIconItemStack.getItem
    }

    @Mod.EventHandler
    def preInit(event:FMLPreInitializationEvent)
    {
        IntegrationProxy.versionCheck()
        IntegrationProxy.preinit()

        integration2.IntegrationProxy.versionCheck()
        integration2.IntegrationProxy.preinit()
    }

    @Mod.EventHandler
    def init(event:FMLInitializationEvent)
    {
        IntegrationProxy.init()
        integration2.IntegrationProxy.init()
    }

    @Mod.EventHandler
    def postInit(event:FMLPostInitializationEvent)
    {
        IntegrationProxy.postinit()
        integration2.IntegrationProxy.postinit()
    }
}
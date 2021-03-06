package tw.oresplus;

import java.io.File;
import java.util.HashMap;
import java.util.Map;



//import buildcraft.api.gates.ActionManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.oredict.OreDictionary;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLModIdMappingEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.VillagerRegistry;
import tw.oresplus.api.Ores;
import tw.oresplus.blocks.BlockCore;
import tw.oresplus.blocks.BlockOre;
import tw.oresplus.blocks.BlockManager;
import tw.oresplus.blocks.TileEntityCracker;
import tw.oresplus.blocks.TileEntityGrinder;
import tw.oresplus.core.BucketHandler;
import tw.oresplus.core.GuiHandler;
import tw.oresplus.core.IMCHandler;
import tw.oresplus.core.IProxy;
import tw.oresplus.core.ItemMapHelper;
import tw.oresplus.core.OreEventHandler;
import tw.oresplus.core.OreLog;
import tw.oresplus.core.References;
import tw.oresplus.core.TickHandler;
import tw.oresplus.core.config.ConfigCore;
import tw.oresplus.core.config.ConfigMain;
import tw.oresplus.core.helpers.AppEngHelper;
import tw.oresplus.core.helpers.BCHelper;
import tw.oresplus.core.helpers.Helpers;
import tw.oresplus.fluids.FluidManager;
import tw.oresplus.gases.GasManager;
import tw.oresplus.items.ItemCore;
import tw.oresplus.items.ItemManager;
import tw.oresplus.items.Items;
import tw.oresplus.network.NetHandler;
import tw.oresplus.ores.MetallicOres;
import tw.oresplus.ores.OreManager;
import tw.oresplus.recipes.RecipeManager;
//import tw.oresplus.triggers.OresTrigger;
//import tw.oresplus.triggers.TriggerProvider;
import tw.oresplus.worldgen.IOreGenerator;
import tw.oresplus.worldgen.OreChestLoot;
import tw.oresplus.worldgen.OreGenClass;
import tw.oresplus.worldgen.OreGenerators;
import tw.oresplus.worldgen.OreGeneratorsEnd;
import tw.oresplus.worldgen.OreGeneratorsNether;
import tw.oresplus.worldgen.VillagerTradeHandler;
import tw.oresplus.worldgen.WorldGenCore;
import tw.oresplus.worldgen.WorldGenOre;

@Mod(modid = References.MOD_ID, name = References.MOD_NAME, version = References.MOD_VERSION, dependencies="required-after:Forge@10.13.0.1180;after:TConstruct")
public class OresPlus {
	
	@SidedProxy(clientSide="tw.oresplus.client.ClientProxy", serverSide="tw.oresplus.core.ServerProxy") 
	public static IProxy proxy;
	
	@Instance(References.MOD_ID)
	public static OresPlus instance;

    public static OreLog log;
    
    public static ConfigMain config = new ConfigMain();
    
    public static String regenKeyOre = "DEFAULT";
    public static String regenKeyOil = "DEFAULT";
    public static String regenKeyRubberTree = "DEFAULT";
    public static String regenKeyBeehives = "DEFAULT";

    public static boolean angryPigmen = true;
    public static boolean debugMode = false;
    public static boolean difficultAlloys = false;
    public static boolean easyMineralSmelt = true;
    public static boolean iridiumPlateRecipe = true;
    public static boolean logRegenerations = false;
    
    public static WorldGenCore worldGen = new WorldGenCore();
    public static OreEventHandler eventHandler = new OreEventHandler();
    public static BucketHandler bucketHandler = new BucketHandler();
    public static TickHandler tickHandler = new TickHandler();
    public static IMCHandler imcHandler = new IMCHandler();
    public static ItemMapHelper itemMapHelper;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    	log.init();
    	
		RecipeManager.init();

		ConfigCore.setBaseDir(event.getModConfigurationDirectory());
		config.init(event);
		
		config.load();
		
	    angryPigmen = config.getBoolean("angryPigmen", angryPigmen, "set to false to prevent zombie pigmen from attacking when mining nether ores");
	    debugMode = config.getBoolean("debugMode", debugMode, "set to true to enable finer debug logging");
	    difficultAlloys = config.getBoolean("difficultAlloys", difficultAlloys, "enable true to set brass & bronze alloy recipes to output only 2 dusts");
	    easyMineralSmelt = config.getBoolean("easyMineralSmelt", easyMineralSmelt, "disable to prevent Mineral ores smelting directly into metal");
	    iridiumPlateRecipe = config.getBoolean("iridiumPlateRecipe", iridiumPlateRecipe, "enables an ore dictionary-enabled recipe for iridium plate");
	    logRegenerations = config.getBoolean("logRegenerations", logRegenerations, "enable to log all regenerations that occur");

	    regenKeyOre = config.getString(ConfigCore.CAT_REGEN, "regenKey", regenKeyOre, "change this to regenerate ores");
	    regenKeyOil = config.getString(ConfigCore.CAT_REGEN, "regenKeyOil", regenKeyOil, "change this to regenerate buildcraft oil wells");
	    regenKeyRubberTree = config.getString(ConfigCore.CAT_REGEN, "regenKeyRubberTree", regenKeyRubberTree, "change this to regenerate IC2 rubber trees");
	    regenKeyBeehives = config.getString(ConfigCore.CAT_REGEN, "regenKeyBeehives", regenKeyBeehives, "change this to regenerate Forestry beehives");
		
		Ores.manager = new OreManager();
		
    	BlockManager.init();
    	ItemManager.init();
    	FluidManager.init();
    	GasManager.init();

    	// register aluminium->aluminum ore dictionary 
    	OreDictionary.registerOre("oreAluminum", MetallicOres.Aluminium.ore.source);
    	OreDictionary.registerOre("oreNetherAluminum", MetallicOres.Aluminium.netherOre.source);
    	OreDictionary.registerOre("blockAluminum", MetallicOres.Aluminium.block.source);
    	OreDictionary.registerOre("ingotAluminum", MetallicOres.Aluminium.ingot.source);
    	OreDictionary.registerOre("nuggetAluminum", MetallicOres.Aluminium.nugget.source);
    	OreDictionary.registerOre("crushedAluminum", MetallicOres.Aluminium.crushedOre.source);
    	OreDictionary.registerOre("crushedPurifiedAluminum", MetallicOres.Aluminium.purifiedCrushedOre.source);
    	OreDictionary.registerOre("dustAluminum", MetallicOres.Aluminium.dust.source);
    	OreDictionary.registerOre("dustTinyAluminum", MetallicOres.Aluminium.tinyDust.source);
    	OreDictionary.registerOre("clusterAluminum", MetallicOres.Aluminium.cluster.source);
    	OreDictionary.registerOre("dustDirtyAluminum", MetallicOres.Aluminium.dirtyDust.source);
    	OreDictionary.registerOre("clumpAluminum", MetallicOres.Aluminium.clump.source);
    	OreDictionary.registerOre("shardAluminum", MetallicOres.Aluminium.shard.source);
    	OreDictionary.registerOre("crystalAluminum", MetallicOres.Aluminium.crystal.source);
    	// register mercury-> quicksilver ore dictionary
    	OreDictionary.registerOre("quicksilver", Items.itemMercury.item.source);
    	
    	//Register Ore Generators
    	log.info("Registering Ore Generators");
    	for (OreGenerators oreGen : OreGenerators.values()) {
    		oreGen.registerGenerator();
    	}
    	for (OreGeneratorsNether oreGen : OreGeneratorsNether.values()) {
    		oreGen.registerGenerator();
    	}
    	for (OreGeneratorsEnd oreGen : OreGeneratorsEnd.values()) {
    		oreGen.registerGenerator();
    	}
    	
		config.save();
		
    	for (Helpers helper : Helpers.values()) {
    		helper.preInit();
    	}
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event) {
    	RecipeManager.initRecipes();
    	
    	GameRegistry.registerTileEntity(TileEntityGrinder.class, "TileEntityGrinder");
    	GameRegistry.registerTileEntity(TileEntityCracker.class, "TileEntityCracker");
    	
    	MinecraftForge.EVENT_BUS.register(eventHandler);
    	MinecraftForge.EVENT_BUS.register(bucketHandler);
    	MinecraftForge.ORE_GEN_BUS.register(eventHandler);
    	FMLCommonHandler.instance().bus().register(tickHandler);
    	
    	NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());
    	
    	itemMapHelper = new ItemMapHelper();
    	
    	//OresTrigger.registerTriggers();
    	//ActionManager.registerTriggerProvider(new TriggerProvider());
    	
    	VillagerRegistry.instance().registerVillageTradeHandler(VillagerTradeHandler.VILLAGER_BLACKSMITH, new VillagerTradeHandler(VillagerTradeHandler.VILLAGER_BLACKSMITH));

    	NetHandler.init();
    	
    	for (Helpers helper : Helpers.values()) {
    		helper.init();
    	}
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    	if (debugMode) {
    		Ores.grinderRecipes.debug();
    	}
    	
    	//RecipeManager.replaceRecipeResults();
    	
    	OreChestLoot.registerChestLoot();
    	
    	for (Helpers helper : Helpers.values()) {
    		helper.postInit();
    	}
    }
    
    @EventHandler
	public void recieveIMC(FMLInterModComms.IMCEvent event) {
    	imcHandler.recieveIMC(event);
    }
    
    @EventHandler
    public void handleMissingMaps(FMLMissingMappingsEvent event) {
    	itemMapHelper.handleMissingMaps(event);
    }
    
    @EventHandler
    public void handRemaps(FMLModIdMappingEvent event) {
    	BlockManager.handleRemaps(event);
    }
    
}

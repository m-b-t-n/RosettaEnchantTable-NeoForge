package net.minecraft.scalar.rosettaenchanttable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * エンチャントテーブルの文字を解読する。
 * というのは建前で、実際は付与されるエンチャントを強制的に決定してしまうツール。
 * マイクラ側の実装に強く依存する形なので公式の ver.up で無効化されやすい mod。
 * @author scalar
 */
@Mod(mod_RosettaEnchantTable.MODID)
public class mod_RosettaEnchantTable {
	public static final String MODID = "rosettaenchanttable"; 
    private static final Logger LOGGER = LogManager.getLogger();
	ItemStack beforeItem = null;
	boolean isUpdateEnchantLevel = false;
	boolean blClickBook = false;
	boolean blRightClickBook = false;
	int sendPacketCnt = 0;
	int nowRandomSeed = 0;
	int restTickPacketSend = 0;
	int[] nowLevel = {0, 0, 0};
	List<List<Component>> enchantResults = new ArrayList<>();
	static final String CHANNEL_NAME = "mod_rosenchtable";
	@ModProperty(defaultInt=0)
	public static int offsetX = 0;
	@ModProperty(defaultInt=0)
	public static int offsetY = 0;
	@ModProperty(defaultBoolean=false)
	public static boolean showAllResult = false;

	static mod_RosettaEnchantTable _mod;
	@SuppressWarnings("unused")
	private final IProxy _client;

	public mod_RosettaEnchantTable(IEventBus modEventBus, ModContainer modContainer) {
		_client = FMLEnvironment.dist == Dist.CLIENT ? (new ProxyClient(modEventBus)) : (new ProxyServer(modEventBus));
		_mod = this;
		Config.configure(this.getClass(), modEventBus, modContainer);

		// Register the setup method for modloading
		modEventBus.addListener(this::setup);
		// Register the enqueueIMC method for modloading
		//FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
		// Register the processIMC method for modloading
		//FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
		// Register the doClientStuff method for modloading
		modEventBus.addListener(this::doClientStuff);
		modEventBus.addListener(this::registerPackets);

		// Register ourselves for server, registry and other game events we are interested in
		//NeoForge.EVENT_BUS.register(this);
		//LOGGER.info("mod_RosettaEnchantTable constructor end.");
	}

	private void setup(final FMLCommonSetupEvent event) {
		// some preinit code
		//LOGGER.info("mod_RosettaEnchantTable::setup start");
		/*
		StringBuilder sb = new StringBuilder();
		for (Method m : GuiScreen.class.getDeclaredMethods()) {
			Class<?>[] p = m.getParameterTypes();
			sb.setLength(0);
			sb.append(m.getName()).append('\n');
			for (Class<?> o : p) {
				sb.append('\t').append(o.getName()).append('\n');
			}
			FMLLog.info(sb.toString());
		}
		for (Method m : GuiContainer.class.getDeclaredMethods()) {
			Class<?>[] p = m.getParameterTypes();
			sb.setLength(0);
			sb.append(m.getName()).append('\n');
			for (Class<?> o : p) {
				sb.append('\t').append(o.getName()).append('\n');
			}
			FMLLog.info(sb.toString());
		}
*/
		//LOGGER.info("mod_RosettaEnchantTable::setup end");
	}

	private void doClientStuff(final FMLClientSetupEvent event) {
		//LOGGER.info("mod_RosettaEnchantTable::doClientStuff start");
		// do something that can only be done on the client
		_client.initClient();
		//LOGGER.info("mod_RosettaEnchantTable::doClientStuff end");
	}

	public void registerPackets(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar("1");
		registrar.playToServer(
				Packet_mod_RosettaEnchantTable.TYPE,
				Packet_mod_RosettaEnchantTable.STREAM_CODEC,
				this::onServerPacket);
	}

	public void onServerPacket(final Packet_mod_RosettaEnchantTable p, final IPayloadContext ctx) {
		//ctx.setPacketHandled(true);
		Config.checkAndReload();
		ctx.enqueueWork(() -> {
			final var player = ctx.player();
			if (!(player.containerMenu instanceof EnchantmentMenu)) {
				LOGGER.info("open container is not EnchantmentContainer.");
				return;
			}
			final var ce = (EnchantmentMenu)player.containerMenu;
			if (p.sendPacketCnt >= 2) {
				LOGGER.debug("mod_RosettaEnchantTable.onServerPacket:click sound. {}, {}", player.position(), player.blockPosition());
				player.level().playSound(null, player.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 0.6F);
			}
			((DataSlot)findPrivateValue(ce.getClass(), ce, DataSlot.class).get(0)).set(p.randomSeed);
			List<Field> o = findFields(ce.getClass(), ce, RandomSource.class);
			if (null == o || o.isEmpty()) {
				LOGGER.info("not find Random class member.");
				return;
			}
			var r = RandomSource.create();
			r.setSeed(p.randomSeed);
			try {
				o.get(0).set(ce, r);
				LOGGER.info("set Random Seed=" + p.randomSeed);
			} catch (IllegalArgumentException e) {
				LOGGER.info("set Random Seed failed.[IllegalArgumentException]");
			} catch (IllegalAccessException e) {
				LOGGER.info("set Random Seed failed.[IllegalAccessException]");
			}
			if (p.isResetLevel) {
				getEnchantmentContainerTableInventory(ce).setChanged();
				LOGGER.info("reset Level Packet.");
			}
		});
	}

	public static Container getEnchantmentContainerTableInventory(final EnchantmentMenu ec) {
		final List<Object> l = findPrivateValue(ec.getClass(), ec, Container.class);
		return (Container)l.get(0);
	}

	public static List<Field> findFields(Class<?> insClass, Object instance, Class<?> targetClass) {
		List<Field> ret = new ArrayList<Field>();
		if (null == insClass && null != instance) {
			insClass = instance.getClass();
		}
		Field[] fs = insClass.getDeclaredFields();
		for (Field f : fs) {
			if (!f.getType().equals(targetClass)) {
				continue;
			}
			if (null == instance && !Modifier.isStatic(f.getModifiers())) {
				// instance が null の場合、static 宣言されているものに限定。
				continue;
			}
			f.setAccessible(true);	// private でも無視して取る。
			ret.add(f);
		}
		return ret;
	}
	public static List<Object> findPrivateValue(Class<?> insClass, Object instance, Class<?> targetClass) {
		List<Field> fs = findFields(insClass, instance, targetClass);
		List<Object> ret = new ArrayList<Object>();
		for (Field f : fs) {
			try {
				ret.add(f.get(instance));
			} catch (Exception e) {
			}
		}
		return ret;
	}
	public static Method getMethod(Class<?> targetClass, Class<?>... args) {
		for (Method m : targetClass.getDeclaredMethods()) {
			Class<?>[] p = m.getParameterTypes();
			if (p.length != args.length) {
				continue;
			}
			boolean bl = true;
			for (int i = 0; i < args.length; ++i) {
				if (p[i] != args[i]) {
					bl = false;
					break;
				}
			}
			if (!bl) {
				continue;
			}
			m.setAccessible(true);
			return m;
		}
		LOGGER.warn("no invoke method.");
		return null;
	}

	ItemStack enchantItem(Level world, ItemStack itemstack, int id, final EnchantmentMenu ce) {
		final var rand = RandomSource.create();
		rand.setSeed(nowRandomSeed + id);
		final boolean isBook = itemstack.is(Items.BOOK);
		Optional<HolderSet.Named<Enchantment>> optional = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);
		final List<EnchantmentInstance> list = optional.isEmpty() ? List.of()
				: EnchantmentHelper.selectEnchantment(rand, itemstack, ce.costs[id], optional.get().stream());
		if (isBook && list.size() > 1) {
			list.remove(rand.nextInt(list.size()));
			itemstack = itemstack.getItem().applyEnchantments(itemstack, list);
		}
		for (final var enchantmentdata : list) {
			if (isBook) {
			} else {
				itemstack.enchant(enchantmentdata.enchantment, enchantmentdata.level);
			}
		}
		return itemstack;
	}
}

package net.minecraft.scalar.rosettaenchanttable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class ProxyClient implements IProxy {
    private static final Logger LOGGER = LogManager.getLogger();

	public ProxyClient(IEventBus modEventBus) {
	}

	@Override
	public void initClient() {
		NeoForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public boolean onGuiMouseClick(ScreenEvent.MouseButtonPressed.Pre ev) {
		//LOGGER.info("onGuiMouseClick=({}, {}), {}", ev.getMouseX(), ev.getMouseY(), ev.getButton());
		if (!(ev.getScreen() instanceof EnchantmentScreen)) {
			return false;
		}
		this.isRightButtonPressed = ev.getButton() == 1;
		this.isLeftButtonPressed = ev.getButton() == 0;
		return false;
	}

	private boolean isRightButtonPressed = false;
	private boolean isLeftButtonPressed = false;
	@SubscribeEvent
	public void drawScreenEvent(ScreenEvent.Render.Post ev) {
		if (!(ev.getScreen() instanceof EnchantmentScreen)) {
			mod_RosettaEnchantTable._mod.sendPacketCnt = 0;
			return;
		}
		final EnchantmentScreen ge = (EnchantmentScreen)ev.getScreen();
		final boolean onBook = isMounseOnBookIcon(ge, (int)ev.getMouseX(), (int)ev.getMouseY());
		final boolean isLeftBtnPressed = this.isLeftButtonPressed;
		final boolean isRightBtnPressed = this.isRightButtonPressed;
		this.isRightButtonPressed = this.isLeftButtonPressed = false;
		do {
			if (!isLeftBtnPressed) {
				mod_RosettaEnchantTable._mod.blClickBook = false;
				break;
			}
			if (mod_RosettaEnchantTable._mod.blClickBook) {
				break;
			}
			if (onBook) {
				mod_RosettaEnchantTable._mod.blClickBook = true;
				mod_RosettaEnchantTable._mod.isUpdateEnchantLevel = false;
				mod_RosettaEnchantTable._mod.nowLevel[0] = -1;
//LOGGER.info("Enchant Click Pos=({}, {})", ev.getMouseX(), ev.getMouseY());
			}
		} while(false);
		// 本を右クリックした場合はレベル更新。
		if (isRightBtnPressed && onBook) {
			mod_RosettaEnchantTable._mod.blRightClickBook = true;
//LOGGER.info("right button down on book.");
		}
		if (!isRightBtnPressed) {
			if (onBook && mod_RosettaEnchantTable._mod.blRightClickBook) {
				// 右クリックした。
				Packet_mod_RosettaEnchantTable p = new Packet_mod_RosettaEnchantTable();
				p.isResetLevel = true;
				p.randomSeed = mod_RosettaEnchantTable._mod.nowRandomSeed = (int) System.currentTimeMillis();
				p.sendPacketCnt = 0;
				PacketDistributor.sendToServer(p);
				mod_RosettaEnchantTable._mod.isUpdateEnchantLevel = false;
			}
			mod_RosettaEnchantTable._mod.blRightClickBook = false;
		}
		if (mod_RosettaEnchantTable._mod.restTickPacketSend >= 1) {
			// パケットを送信。
			--mod_RosettaEnchantTable._mod.restTickPacketSend;
//FMLLog.info("packet send=" + restTickPacketSend);
			Packet_mod_RosettaEnchantTable p = new Packet_mod_RosettaEnchantTable();
			p.randomSeed = mod_RosettaEnchantTable._mod.nowRandomSeed;
			p.sendPacketCnt = 1 == mod_RosettaEnchantTable._mod.restTickPacketSend ? mod_RosettaEnchantTable._mod.sendPacketCnt : 0;
			PacketDistributor.sendToServer(p);
		}
		// コンテナを取り出す。
		final EnchantmentMenu ce = ge.getMenu();
		do {
			ItemStack is = mod_RosettaEnchantTable.getEnchantmentContainerTableInventory(ce).getItem(0);
			boolean isAttach = false;
			if (null == mod_RosettaEnchantTable._mod.beforeItem && null == is) {
				break;
			} else if (null == mod_RosettaEnchantTable._mod.beforeItem && null != is) {
				isAttach = true;
			} else  if (null != mod_RosettaEnchantTable._mod.beforeItem && null == is) {
				//LOGGER.info("EnchantTable Item Detach");
			} else if (ItemStack.isSameItem(mod_RosettaEnchantTable._mod.beforeItem, is)) {
				break;
			} else {
				mod_RosettaEnchantTable._mod.isUpdateEnchantLevel = false;
				isAttach = true;
			}
			if (isAttach) {
/*
StringBuilder sb = new StringBuilder();
sb.append("EnchantTable Item Attach=").append(is.toString()).append('\n')
.append("\t[").append(ce.enchantLevels[0]).append(',')
.append(ce.enchantLevels[1]).append(',')
.append(ce.enchantLevels[2]).append(']')
;
FMLLog.info(sb.toString());
*/
			}
			mod_RosettaEnchantTable._mod.beforeItem = is;
		} while (false);
		if (mod_RosettaEnchantTable._mod.isUpdateEnchantLevel) {
			if (ce.costs[0] <= 0
				|| !(ce.costs[0] == mod_RosettaEnchantTable._mod.nowLevel[0]
					&& ce.costs[1] == mod_RosettaEnchantTable._mod.nowLevel[1]
					&& ce.costs[2] == mod_RosettaEnchantTable._mod.nowLevel[2])
			) {
				mod_RosettaEnchantTable._mod.nowLevel[0] = ce.costs[0];
				mod_RosettaEnchantTable._mod.nowLevel[1] = ce.costs[1];
				mod_RosettaEnchantTable._mod.nowLevel[2] = ce.costs[2];
				mod_RosettaEnchantTable._mod.restTickPacketSend = 0;
				mod_RosettaEnchantTable._mod.isUpdateEnchantLevel = false;
			}
		} else {
			if (ce.costs[0] >= 1) {
				mod_RosettaEnchantTable._mod.nowRandomSeed = (int) System.currentTimeMillis();
				mod_RosettaEnchantTable._mod.isUpdateEnchantLevel = true;
				mod_RosettaEnchantTable._mod.restTickPacketSend = 5;
				mod_RosettaEnchantTable._mod.enchantResults.clear();
				++mod_RosettaEnchantTable._mod.sendPacketCnt;
/*
StringBuilder sb = new StringBuilder();
sb.append("EnchantTable Level Set=").append(beforeItem.toString()).append('\n')
.append("\t[").append(ce.enchantLevels[0]).append(',')
.append(ce.enchantLevels[1]).append(',')
.append(ce.enchantLevels[2]).append(']')
;
ModLoader.getLogger().info(sb.toString());
*/
			}
		}
		if (!mod_RosettaEnchantTable._mod.isUpdateEnchantLevel) {
			return;
		}
		if (mod_RosettaEnchantTable.showAllResult && onBook) {
			// 本の上では全レベルを表示。
			final List<Component> tooltips = new ArrayList<>();
			for (int n = 0; n < 3; ++n) {
				final var ret = getEnchantResult(ce, n);
				if (null != ret) {
					if (!tooltips.isEmpty()) {
						tooltips.add(MutableComponent.create(new LiteralContents("----")));
					}
					tooltips.addAll(ret);
				}
			}
			if (!tooltips.isEmpty()) {
				drawTooltip(ge, ev.getMouseX(), ev.getMouseY(), mod_RosettaEnchantTable.offsetX,
						-10 * tooltips.size() * 1 / 3 + mod_RosettaEnchantTable.offsetY, tooltips);
			}
			return;
		}
		// 描画。
		int n = getEnchantButtonOffset(ge, ev.getMouseX(), ev.getMouseY());
		if (n < 0) {
			return;
		}
//FMLLog.info("mod_RossetaEnchantTable.drawScreenEvent(post)=No.%d", n);
		final List<Component> tooltips = getEnchantResult(ce, n);
		// ツールチップとして表示。
		if (null != tooltips) {
			drawTooltip(ge, ev.getMouseX(), ev.getMouseY(),
					mod_RosettaEnchantTable.offsetX,  -12 - 10 * tooltips.size() + mod_RosettaEnchantTable.offsetY, tooltips);
		}
	}

	private List<Component> getEnchantResult(final EnchantmentMenu ce, int n) {
		if (n < mod_RosettaEnchantTable._mod.enchantResults.size()) {
			final List<Component> ret = mod_RosettaEnchantTable._mod.enchantResults.get(n);
			if (ret != null) {
				return ret;
			}
		}
		while (mod_RosettaEnchantTable._mod.enchantResults.size() <= n) {
			mod_RosettaEnchantTable._mod.enchantResults.add(null);
		}
		// ボタン上なので付与効果を計算。
		int lv = ce.costs[n];
		int effectAndLevel = ce.enchantClue[n];
		if (lv > 0 && effectAndLevel >= 0) {
		} else {
			return null;
		}
		ItemStack is = mod_RosettaEnchantTable._mod.beforeItem.copy();
		final Minecraft mc = Minecraft.getInstance();
		is = mod_RosettaEnchantTable._mod.enchantItem(mc.level, is, n, ce);
		if (null == is) {
			return null;
		}
		// アイテムに付与されるエンチャントの情報を取得。
		final List<Component> tooltips = is.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player
				, mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);
		// アイテム名をエンチャントレベルで上書きする。
		final String itemName = is.getHoverName().getString();
		final String lvStr = "Lv." + ce.costs[n];
		final List<Component> strToolTips = new ArrayList<>();
		for (int i = 0; i < tooltips.size(); ++i) {
			String tc = tooltips.get(i).getString();
			tc = tc.replace(itemName, lvStr);
			strToolTips.add(MutableComponent.create(new LiteralContents(tc)));
		}
		mod_RosettaEnchantTable._mod.enchantResults.set(n, strToolTips);
		return strToolTips;
	}

	public static void drawTooltip(EnchantmentScreen gui, int mouseX, int mouseY, int offsetX, int offsetY, List<Component> str) {
		if (null == str || str.isEmpty()) {
			return;
		}
		final var n = new ArrayList<FormattedCharSequence>();
		str.forEach(s -> {
			n.add(FormattedCharSequence.forward(s.getString(), Style.EMPTY));
		});
		gui.setTooltipForNextRenderPass(n, new TooltipPositioner(offsetX, offsetY), false);
	}

	private static Method isPointInRegionMethod = null;
	public static int getEnchantButtonOffset(EnchantmentScreen ge, int posX, int posY) {
		getIsPointInRegionMethod();
		if (null == isPointInRegionMethod) {
			return -1;
		}
		for (int i = 0; i < 3; ++i) {
			try {
				boolean bl = (Boolean)isPointInRegionMethod.invoke(ge, 60, 14 + 19 * i, 108, 17, posX, posY);
				if (bl) {
					return i;
				}
			} catch (Exception e) {
				LOGGER.error(e);
				return -1;
			}
		}
		return -1;
	}
	// 本アイコンの上にマウスカーソルがあるか？
	public static boolean isMounseOnBookIcon(EnchantmentScreen ge, int posX, int posY) {
		getIsPointInRegionMethod();
		if (null == isPointInRegionMethod) {
			return false;
		}
		try {
			return (Boolean)isPointInRegionMethod.invoke(ge, 20, 19, 40, 25, posX, posY);
		} catch (Exception e) {
			LOGGER.error(e);
		}
		return false;
	}
	private static void getIsPointInRegionMethod() {
		if (null != isPointInRegionMethod) {
			return;
		}
		isPointInRegionMethod = mod_RosettaEnchantTable.getMethod(AbstractContainerScreen.class
				, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Double.TYPE, Double.TYPE);
		if (null == isPointInRegionMethod) {
			LOGGER.info("[mod_RosettaEnchantTable] isPointInRegionMethod is null");
		}
	}
	
	private static class TooltipPositioner implements ClientTooltipPositioner {
		final int _offsetX;
		final int _offsetY;
		public TooltipPositioner(int offsetX, int offsetY) {
			this._offsetX = offsetX;
			this._offsetY = offsetY;
		}

		@Override
		public Vector2ic positionTooltip(int p_263026_, int p_262969_, int p_262971_, int p_263058_, int p_281643_,
				int p_282590_) {
			final var i = DefaultTooltipPositioner.INSTANCE.positionTooltip(p_263026_, p_262969_, p_262971_, p_263058_, p_281643_, p_282590_);
			final var ret = new Vector2i(this._offsetX, this._offsetY);
			ret.add(i, ret);
			return ret;
		}
		
	}
}

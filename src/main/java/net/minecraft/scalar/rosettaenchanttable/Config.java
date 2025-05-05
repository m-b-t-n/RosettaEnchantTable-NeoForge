package net.minecraft.scalar.rosettaenchanttable;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final Logger LOGGER = LogManager.getLogger();
	private static Class<?> _cls = null;
	private static List<AbstractMap.SimpleEntry<Field, ModConfigSpec.ConfigValue<?>>> mapConfigFields = new ArrayList<>();
    private static File _configFile = null;
	public static void configure(final Class<?> cls, final IEventBus modEventBus, final ModContainer modContainer) {
		_cls = cls;
		modEventBus.register(Config.class);
        modContainer.registerConfig(ModConfig.Type.COMMON, execConfigure());
	}

	@SubscribeEvent
	public static void onLoad(final ModConfigEvent.Loading configEvent) {
		LOGGER.info("{}::onLoad file:{}", ModConfig.class.getCanonicalName(), configEvent.getConfig().getFullPath());
		if (configEvent.getConfig().getFileName() != null) {
			_configFile = new File(configEvent.getConfig().getFileName());
		}
		setFieldsFromConfig();
		_changed = true;
	}

	private static volatile boolean _changed = false;
	private static volatile long _lastModified = -1;
	private static long _lastCheck = System.currentTimeMillis();
	private static final long _checkInterval = 500;
	@SubscribeEvent
	public static void onFileChange(final ModConfigEvent.Reloading configEvent) {
		onFileChange();
	}
	private static void onFileChange() {
		//LOGGER.info("{}::onFileChange file:{}", ModConfig.class.getCanonicalName(), null != _config ? _config.getNioPath() : "[unknown]");
		if (null != _configFile) {
			_lastModified = _configFile.lastModified();
		}
		setFieldsFromConfig();
		// 再読み込みしたことを通知。
		_changed = true;
		String s = String.format("%s config reload.", _cls.getName());
		if (FMLEnvironment.dist == Dist.CLIENT) {
			final net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
			if (null != minecraft && null != minecraft.gui && null != minecraft.gui.getChat()) {
				minecraft.gui.getChat().addMessage(MutableComponent.create(new LiteralContents(s)));
			}
		}
		LOGGER.info(s);
	}
	public static boolean checkAndReload() {
		// forge の実装だとコンフィグファイルの入っているディレクトリを監視している。
		// が、Windows では中のファイルが更新されてもそれを含んでいるディレクトリに対して変更が通知されない場合がある。
		// よって自前で監視する。
		do {
			if ((System.currentTimeMillis() - _lastCheck) < _checkInterval) {
				break;
			}
			_lastCheck = System.currentTimeMillis();
			if (null != _configFile) {
				final long l = _configFile.lastModified();
				if (_lastModified < 0) {
					_lastModified = l;
				}
				if (_lastModified == l) {
					break;
				}
				_lastModified = l;
				// ファイルが更新されている。
				//LOGGER.info("modified. {}", _config.getFile().getAbsolutePath());
				onFileChange();
			}
		} while(false);
		final boolean ret = _changed;
		_changed = false;
		return ret;
	}

	private Config() {
		//execConfigure();
	}
	private static ModConfigSpec execConfigure() {
		final var builder = new ModConfigSpec.Builder();
		builder.comment(_cls.getCanonicalName()).push("configuration");
		final Field[] fields = _cls.getFields();
		for (Field f : fields) {
			final ModProperty anno = f.getAnnotation(ModProperty.class);
			if (null == anno) {
				continue;
			}
			if (!Modifier.isStatic(f.getModifiers())) {
				continue;
			}
			final Class<?> type = f.getType();
			try {
				String comment = null;
				if (type.equals(Boolean.TYPE)) {
					comment = "Boolean: " + (f.getBoolean(null) ? "true" : "false");
				} else if (type.equals(Integer.TYPE)) {
					comment = "Integer: " + f.getInt(null);
				} else if (type.equals(Double.TYPE)) {
					comment = "Double: " + f.getDouble(null);
				} else if (type.equals(String.class)) {
					comment = "String: \"" + f.get(null) + '"';
				} else {
					LOGGER.printf(Level.INFO, "unrecognizable type: %s", type.getCanonicalName());
				}
				if (null != anno.comment() && anno.comment().length() >= 1) {
					if (null == comment) {
						comment = anno.comment();
					} else {
						comment += "\n" + anno.comment();
					}
				}
				if (null != comment) {
					builder.comment(comment);
				}
				builder.translation(f.getName());
				if (type.equals(Boolean.TYPE)) {
					mapConfigFields.add(new AbstractMap.SimpleEntry<Field, ModConfigSpec.ConfigValue<?>>(f, builder.define(f.getName(), anno.defaultBoolean())));
				} else if (type.equals(Integer.TYPE)) {
					mapConfigFields.add(new AbstractMap.SimpleEntry<Field, ModConfigSpec.ConfigValue<?>>(f, builder.define(f.getName(), anno.defaultInt())));
				} else if (type.equals(Double.TYPE)) {
					mapConfigFields.add(new AbstractMap.SimpleEntry<Field, ModConfigSpec.ConfigValue<?>>(f, builder.define(f.getName(), anno.defaultDouble())));
				} else if (type.equals(String.class)) {
					mapConfigFields.add(new AbstractMap.SimpleEntry<Field, ModConfigSpec.ConfigValue<?>>(f, builder.define(f.getName(), anno.defaultString())));
				} else {
					LOGGER.printf(Level.INFO, "unrecognizable type: %s", type.getCanonicalName());
				}
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			}
		}
		builder.pop();
		return builder.build();
	}
	@SuppressWarnings("unchecked")
	private static void setFieldsFromConfig() {
		mapConfigFields.forEach(i -> {
			try {
				final Field f = i.getKey();
				final ModConfigSpec.ConfigValue<?> val = i.getValue();
				final Class<?> type = f.getType();
				val.clearCache();
				//LOGGER.info("setFieldsFromConfig: {}", f.getName());
				if (type.equals(Boolean.TYPE)) {
					f.setBoolean(null, ((ModConfigSpec.BooleanValue) val).get());
				} else if (type.equals(Integer.TYPE)) {
					f.setInt(null, ((ModConfigSpec.ConfigValue<Integer>) val).get());
				} else if (type.equals(Double.TYPE)) {
					f.setDouble(null, ((ModConfigSpec.ConfigValue<Double>) val).get());
				} else if (type.equals(String.class)) {
					f.set(null, val.get().toString());
				} else {
					LOGGER.error("config param:{} not set", f.getName());
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				LOGGER.error("config read error", e);
			}
		});
	}
	@SuppressWarnings("unchecked")
	public static void saveFieldToConfigFile(String fieldName) {
		mapConfigFields.forEach(i -> {
			try {
				final Field f = i.getKey();
				if (!f.getName().equals(fieldName)) {
					return;
				}
				final ModConfigSpec.ConfigValue<?> val = i.getValue();
				final Class<?> type = f.getType();
				//LOGGER.info("setFieldsFromConfig: {}", f.getName());
				if (type.equals(Boolean.TYPE)) {
					((ModConfigSpec.BooleanValue) val).set(f.getBoolean(null));
				} else if (type.equals(Integer.TYPE)) {
					((ModConfigSpec.ConfigValue<Integer>) val).set(f.getInt(null));
				} else if (type.equals(Double.TYPE)) {
					((ModConfigSpec.ConfigValue<Double>) val).set(f.getDouble(null));
				} else if (type.equals(String.class)) {
					((ModConfigSpec.ConfigValue<String>) val).set(f.get(null).toString());
				} else {
					LOGGER.error("field {} is Unknown Type.", f.getName());
					return;
				}
				val.save();
			} catch (IllegalArgumentException | IllegalAccessException e) {
				LOGGER.error("config read error", e);
			}
		});
	}

	public static String getKeyboardKeyName(final int keyno) {
		Field[] fs = null;
		try {
			Class<?> cls = Class.forName("org.lwjgl.glfw.GLFW");
			fs = cls.getFields();
		} catch (ClassNotFoundException e1) {
			return "" + keyno;
		}
		if (null == fs) {
			return "" + keyno;
		}
		for (Field f : fs) {
			if (!Modifier.isStatic(f.getModifiers()) || !Integer.TYPE.equals(f.getType())) {
				continue;
			}
			String s = f.getName();
			if (0 != s.indexOf("GLFW_KEY_")) {
				continue;
			}
			try {
				if (keyno != f.getInt(null)) {
					continue;
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				continue;
			}
			// 先頭の GLFW_ を取る。
			s = s.substring(5);
			LOGGER.printf(Level.INFO, "getKeyboardKeyName=%s", s);
			return s;
		}
		return "" + keyno;
	}
	public static int getKeyboardNo(final String name) {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			return getKeyboardNoClient(name);
		}
		return -1;
	}
	private static int getKeyboardNoClient(String name) {
		Field[] fs = null;
		try {
			Class<?> cls = Class.forName("org.lwjgl.glfw.GLFW");
			fs = cls.getFields();
		} catch (ClassNotFoundException e1) {
			return -1;
		}
		if (null == fs) {
			return -1;
		}
		if ("KEY_COLON".equals(name)) {
			// なぜかコロンがアポストロフィに割り当てられているので対応。
			name = "KEY_APOSTROPHE";
		}
		int key_no = 0;
		for (Field f : fs) {
			if (!Modifier.isStatic(f.getModifiers())) {
				continue;
			}
			String s = f.getName();
			if (0 != s.indexOf("GLFW_KEY_")) {
				continue;
			}
			// 先頭の GLFW_ を取る。
			s = s.substring(5);
			if (s.equalsIgnoreCase(name)) {
				try {
					key_no = f.getInt(null);
				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				}
				break;
			}
		}
		LOGGER.printf(Level.INFO, "getKeyboardNo=%d", key_no);
		return key_no;
	}
}

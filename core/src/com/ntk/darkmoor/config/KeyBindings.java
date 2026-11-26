package com.ntk.darkmoor.config;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

/**
 * 按键绑定系统 — 把物理按键 (libGDX keycode) 映射到游戏动作 ({@link Action})。
 *
 * <p>支持一个动作绑定多个键（例如 {@code MoveForward} 同时绑 {@code W} 和 {@code UP}）。
 *
 * <p>持久化到 {@code settings.properties}，键名格式：
 * <pre>
 *   KeyBindings.MoveForward=51,19
 *   KeyBindings.TurnLeft=45
 * </pre>
 * 值为逗号分隔的 keycode 列表（libGDX {@link Keys} 常量值）。
 *
 * <p>设计文档：{@code docs/v0.5/09-input-system.md}
 *
 * @see KeyboardInputProcessor
 */
public class KeyBindings {

	/** 所有可绑定的游戏动作。 */
	public enum Action {
		MoveForward, MoveBackward, StrafeLeft, StrafeRight,
		TurnLeft, TurnRight,
		Camp, Inventory, Statistics,
		SelectHero1, SelectHero2, SelectHero3, SelectHero4, SelectHero5, SelectHero6,
		MainMenu, QuickSave, QuickLoad,
		ToggleMinimap, ToggleNavButtons
	}

	/** properties 文件中的键名前缀。 */
	private static final String PROP_PREFIX = "KeyBindings.";

	private final Map<Integer, Action> keyToAction;
	private final Map<Action, List<Integer>> actionToKeys;

	public KeyBindings() {
		keyToAction = new HashMap<Integer, Action>();
		actionToKeys = new EnumMap<Action, List<Integer>>(Action.class);
		for (Action a : Action.values()) {
			actionToKeys.put(a, new ArrayList<Integer>(2));
		}
	}

	/** 绑定一个键到某个动作。同动作可多键。 */
	public void bind(int keycode, Action action) {
		if (keycode < 0 || action == null) return;
		// 若该键已绑到其他动作，先解绑
		keyToAction.remove(keycode);
		keyToAction.put(keycode, action);
		List<Integer> keys = actionToKeys.get(action);
		if (!keys.contains(keycode)) keys.add(keycode);
	}

	/** 解绑某键。 */
	public void unbind(int keycode) {
		Action a = keyToAction.remove(keycode);
		if (a != null) actionToKeys.get(a).remove(Integer.valueOf(keycode));
	}

	/** 解绑某动作的所有键。 */
	public void unbindAction(Action action) {
		List<Integer> keys = actionToKeys.get(action);
		if (keys == null) return;
		for (Integer k : new ArrayList<Integer>(keys)) {
			keyToAction.remove(k);
		}
		keys.clear();
	}

	/** keyDown 时调用，返回该键对应的动作；无绑定返回 null。 */
	public Action lookup(int keycode) {
		return keyToAction.get(keycode);
	}

	/** 取某动作当前绑定的所有键（用于 UI 显示）。 */
	public List<Integer> keysFor(Action action) {
		List<Integer> keys = actionToKeys.get(action);
		return keys == null ? new ArrayList<Integer>() : new ArrayList<Integer>(keys);
	}

	/** 把 keycode 转成可读名（"W"、"UP"、"F5" 等）。 */
	public static String keyName(int keycode) {
		if (Gdx.input == null) return String.valueOf(keycode);
		return Keys.toString(keycode);
	}

	/** 是否没有任何绑定。 */
	public boolean isEmpty() {
		return keyToAction.isEmpty();
	}

	/** 保存到 Properties 对象（不写入磁盘，由 Settings 统一持久化）。 */
	public void saveTo(Properties props) {
		if (props == null) return;
		for (Action action : Action.values()) {
			List<Integer> keys = actionToKeys.get(action);
			String key = PROP_PREFIX + action.name();
			if (keys == null || keys.isEmpty()) {
				props.remove(key);
			} else {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < keys.size(); i++) {
					if (i > 0) sb.append(",");
					sb.append(keys.get(i));
				}
				props.setProperty(key, sb.toString());
			}
		}
	}

	/** 从 Properties 加载。若完全没有 KeyBindings.* 键，返回 null（调用方应 fallback 到 defaults()）。 */
	public static KeyBindings loadFrom(Properties props) {
		if (props == null) return null;
		KeyBindings kb = new KeyBindings();
		boolean any = false;
		for (Action action : Action.values()) {
			String key = PROP_PREFIX + action.name();
			String value = props.getProperty(key);
			if (value == null || value.isEmpty()) continue;
			any = true;
			for (String code : value.split(",")) {
				try {
					int keycode = Integer.parseInt(code.trim());
					kb.bind(keycode, action);
				} catch (NumberFormatException e) {
					// skip malformed entry
				}
			}
		}
		return any ? kb : null;
	}

	/** 默认 QWERTY 布局。 */
	public static KeyBindings defaults() {
		KeyBindings kb = new KeyBindings();
		kb.bind(Keys.W,     Action.MoveForward);
		kb.bind(Keys.UP,    Action.MoveForward);
		kb.bind(Keys.S,     Action.MoveBackward);
		kb.bind(Keys.DOWN,  Action.MoveBackward);
		kb.bind(Keys.A,     Action.StrafeLeft);
		kb.bind(Keys.LEFT,  Action.StrafeLeft);
		kb.bind(Keys.D,     Action.StrafeRight);
		kb.bind(Keys.RIGHT, Action.StrafeRight);
		kb.bind(Keys.Q,     Action.TurnLeft);
		kb.bind(Keys.E,     Action.TurnRight);
		kb.bind(Keys.C,     Action.Camp);
		kb.bind(Keys.I,     Action.Inventory);
		kb.bind(Keys.P,     Action.Statistics);
		kb.bind(Keys.NUM_1, Action.SelectHero1);
		kb.bind(Keys.NUM_2, Action.SelectHero2);
		kb.bind(Keys.NUM_3, Action.SelectHero3);
		kb.bind(Keys.NUM_4, Action.SelectHero4);
		kb.bind(Keys.NUM_5, Action.SelectHero5);
		kb.bind(Keys.NUM_6, Action.SelectHero6);
		kb.bind(Keys.ESCAPE, Action.MainMenu);
		kb.bind(Keys.F5,    Action.QuickSave);
		kb.bind(Keys.F9,    Action.QuickLoad);
		kb.bind(Keys.M,     Action.ToggleMinimap);
		kb.bind(Keys.N,     Action.ToggleNavButtons);
		return kb;
	}

	/** AZERTY 默认布局（供未来切换）。 */
	public static KeyBindings azerty() {
		KeyBindings kb = defaults();
		// AZERTY: WASD → ZQSD
		kb.unbindAction(Action.MoveForward);
		kb.unbindAction(Action.StrafeLeft);
		kb.unbindAction(Action.MoveBackward);
		kb.unbindAction(Action.StrafeRight);
		kb.unbindAction(Action.TurnLeft);
		kb.unbindAction(Action.TurnRight);
		kb.bind(Keys.Z, Action.MoveForward);
		kb.bind(Keys.UP, Action.MoveForward);
		kb.bind(Keys.Q, Action.StrafeLeft);
		kb.bind(Keys.LEFT, Action.StrafeLeft);
		kb.bind(Keys.S, Action.MoveBackward);
		kb.bind(Keys.DOWN, Action.MoveBackward);
		kb.bind(Keys.D, Action.StrafeRight);
		kb.bind(Keys.RIGHT, Action.StrafeRight);
		// AZERTY 的 A 键在 QWERTY 位置是 Q
		kb.bind(Keys.A, Action.TurnLeft);
		kb.bind(Keys.E, Action.TurnRight);
		return kb;
	}
}

package com.ntk.darkmoor.engine;

import java.util.ArrayDeque;

import com.badlogic.gdx.math.Rectangle;
import com.ntk.darkmoor.GameScreen;
import com.ntk.darkmoor.GameScreenBase;
import com.ntk.darkmoor.engine.gui.BaseWindow;
import com.ntk.darkmoor.engine.gui.campwindows.MainWindow;
import com.ntk.darkmoor.stub.Mouse;
import com.ntk.darkmoor.config.LanguagesManager;

/**
 * 营地对话框 — 玩家点击 CAMP 按钮或按 C 键时弹出。
 *
 * <p>管理一个 {@link ArrayDeque} 窗口栈：构造时压入 {@link MainWindow}，
 * 子窗口可通过 {@link #addWindow(BaseWindow)} 入栈，{@link #exit()} 清空栈并关闭对话框。
 *
 * <p>v0.5 重构：原 {@code buttons} 字段和鼠标轮询逻辑已移到 {@link BaseWindow}，
 * 由栈顶子窗口自己处理 ScreenButton 点击。
 *
 * <p>设计文档：{@code docs/v0.5/10-camp-system.md}
 */
public class CampDialog extends BaseWindow {

	private GameScreenBase game;
	private Rectangle rectangle;
	private ArrayDeque<BaseWindow> windows;

	public CampDialog(GameScreenBase game, GameScreenBase parent) {
		super(null, LanguagesManager.getString("titles", 1), parent);
		this.game = game;
		windows = new ArrayDeque<BaseWindow>();
		rectangle = new Rectangle(0, 0, 352, 288);
		addWindow(new MainWindow(this, parent));
		Mouse.setTile(0);
	}

	public void exit() {
		Team team = GameScreen.getTeam();

		windows.clear();

		// Restore item in hand cursor
		if (team.getItemInHand() != null)
			Mouse.setTile(team.getItemInHand().getTextureID());

		closeAndRemoveFromParent(this);
	}

	public void addWindow(BaseWindow window) {
		if (window == null)
			return;

		windows.push(window);
	}

	@Override
	public void update(float delta) {
		if (windows.size() > 0) {
			// Remove closing windows
			while (windows.size() > 0 && windows.peek().isClosing())
				windows.pop();

			if (windows.isEmpty())
				exit();
			else
				windows.peek().update(delta);
		}
		// 注：鼠标轮询由 BaseWindow.update() 处理，栈顶子窗口自己轮询自己的 screenButtons
	}

	public Rectangle getRectangle() {
		return rectangle;
	}

	public void setRectangle(Rectangle rectangle) {
		this.rectangle = rectangle;
	}

	public ArrayDeque<BaseWindow> getWindows() {
		return windows;
	}

	protected void setWindows(ArrayDeque<BaseWindow> windows) {
		this.windows = windows;
	}

	public GameScreenBase getGame() {
		return game;
	}

}

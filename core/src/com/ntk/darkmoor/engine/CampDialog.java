package com.ntk.darkmoor.engine;

import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.ntk.darkmoor.GameScreenBase;
import com.ntk.darkmoor.config.LanguagesManager;
import com.ntk.darkmoor.engine.gui.BaseWindow;
import com.ntk.darkmoor.engine.gui.campwindows.LoadGameWindow;
import com.ntk.darkmoor.stub.Mouse;

/**
 * 营地对话框 — 玩家点击"营地"按钮或按 C 键时弹出。
 *
 * <p>v0.5 第二轮重构：从旧 {@code BaseWindow}（352x288 空壳 + 损坏的
 * ScreenButton 子窗口栈）迁移为 {@link VisWindow}，菜单按钮内联渲染：
 * <ul>
 *   <li>标准标题栏"营地" + 右上角关闭按钮 + 可拖动</li>
 *   <li>6 个菜单项 + 退出，用 VisTextButton 直接列出（中文字体来自 UITheme）</li>
 *   <li>尺寸约 520x480，居中显示</li>
 *   <li>"游戏选项"打开 VisUI 版读取存档窗口（LoadGameWindow）</li>
 *   <li>其余菜单项（休息/记忆法术/祈求法术/抄写卷轴/偏好设置）在
 *       v0.5 显示"功能尚未实现"提示 —— 旧营地子窗口仍是损坏的
 *       BaseWindow（ScreenButton 未注册），打开只会得到空窗口</li>
 * </ul>
 *
 * <p>保留的旧 API：{@link #exit()}（LoadGameWindow 等通过
 * getCamp().exit() 关闭整个营地）。{@code windows} 子窗口栈已废弃，
 * {@link #addWindow(BaseWindow)} 保留为空操作以兼容旧调用。
 *
 * <p>设计文档：{@code docs/v0.5/10-camp-system.md}、{@code docs/v0.5/12-visui-migration-log.md} §12.9
 */
public class CampDialog extends VisWindow {

	private GameScreenBase game;

	public CampDialog(GameScreenBase game, GameScreenBase parent) {
		super(LanguagesManager.getString("titles", 1)); // "营地"
		this.game = game;
		setModal(true);
		setMovable(true);
		buildMenu(parent);
		pack();
		setSize(520, 480);
		centerWindow();
		Mouse.setTile(0);
	}

	/** 构建营地菜单：6 个功能项 + 退出。 */
	private void buildMenu(final GameScreenBase parent) {
		VisTable table = new VisTable();
		table.defaults().pad(8).left().growX();

		addMenuButton(table, LanguagesManager.getString("Camp", 1), new ChangeListener() { // 小队休息
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				showNotImplemented();
			}
		});
		addMenuButton(table, LanguagesManager.getString("Camp", 2), new ChangeListener() { // 记忆法术
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				showNotImplemented();
			}
		});
		addMenuButton(table, LanguagesManager.getString("Camp", 3), new ChangeListener() { // 祈求法术
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				showNotImplemented();
			}
		});
		addMenuButton(table, LanguagesManager.getString("Camp", 4), new ChangeListener() { // 抄写卷轴
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				showNotImplemented();
			}
		});
		addMenuButton(table, LanguagesManager.getString("Camp", 5), new ChangeListener() { // 偏好设置
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				showNotImplemented();
			}
		});
		addMenuButton(table, LanguagesManager.getString("Camp", 6), new ChangeListener() { // 游戏选项
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				// v0.5：游戏选项直接打开读取存档（VisUI 版）；保存/偏好等完整菜单 v0.6 恢复
				parent.getStage().addActor(new LoadGameWindow(CampDialog.this, parent));
				closeSelf();
			}
		});

		table.row().padTop(16);
		VisTextButton exitButton = new VisTextButton(LanguagesManager.getString("campui", 1)); // 退出
		exitButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				exit();
			}
		});
		table.add(exitButton).growX();

		add(table).grow().pad(12);
	}

	private void addMenuButton(VisTable table, String text, ChangeListener listener) {
		VisTextButton button = new VisTextButton(text);
		button.addListener(listener);
		table.add(button).growX();
		table.row();
	}

	/** 显示"功能尚未实现"提示（VisDialog，带确定按钮）。 */
	private void showNotImplemented() {
		VisDialog dialog = new VisDialog(LanguagesManager.getString("titles", 1)); // 标题"营地"
		dialog.text(LanguagesManager.getString("campui", 16)); // "该功能尚未实现。"
		dialog.button(LanguagesManager.getString("campui", 2), true); // "取消" 兼作确定/关闭
		dialog.pack();
		dialog.centerWindow();
		getStage().addActor(dialog);
	}

	/** 关闭营地对话框自身。 */
	private void closeSelf() {
		fadeOut();
		remove();
	}

	/**
	 * 退出营地。保留旧 API：子窗口（LoadGameWindow 等）通过
	 * getCamp().exit() 关闭整个营地。
	 */
	public void exit() {
		closeSelf();
	}

	public GameScreenBase getGame() {
		return game;
	}

	/** 旧 API 兼容：v0.5 起子窗口栈废弃，此方法为空操作。 */
	public void addWindow(BaseWindow window) {
		// no-op（旧窗口栈已由内联菜单取代）
	}
}

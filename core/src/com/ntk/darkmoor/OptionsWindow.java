package com.ntk.darkmoor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.ntk.darkmoor.config.LanguagesManager;
import com.ntk.darkmoor.config.Log;
import com.ntk.darkmoor.config.Settings;
import com.ntk.darkmoor.engine.CampDialog;

/**
 * 选项窗口 — 使用 VisUI 的 VisWindow。
 *
 * <p>v0.5 重构：从 BaseWindow（裸 scene2d Dialog）改为 VisWindow，获得：
 * <ul>
 *   <li>标准标题栏 + 右上角关闭按钮</li>
 *   <li>VisCheckBox 替代裸 CheckBox（样式更清晰）</li>
 *   <li>VisTextButton 替代裸 TextButton</li>
 *   <li>VisTable 自动布局</li>
 *   <li>可拖动标题栏</li>
 * </ul>
 *
 * <p>设计文档：{@code docs/v0.5/12-visui-migration-log.md}
 */
public class OptionsWindow extends VisWindow {

	private LanguagesManager stringTable;

	private VisCheckBox chkMusic, chkEffects, chkHpAsBar, chkFullScreen, chkLanguage;

	public OptionsWindow(CampDialog camp, GameScreenBase parent) {
		super(LanguagesManager.getString("titles", 3));
		Log.debug("[OptionMenu] loadContent()");
		stringTable = LanguagesManager.getInstance();
		buildUI(parent);
	}

	private void buildUI(final GameScreenBase parent) {
		// 允许拖动 + 关闭按钮
		setMovable(true);
		setModal(true);

		VisTable content = new VisTable();
		content.defaults().pad(8).left();
		content.columnDefaults(0).width(280);
		content.columnDefaults(1).width(280);

		// 音乐
		chkMusic = new VisCheckBox(LanguagesManager.getString("titles", 12));
		chkMusic.setChecked(Settings.getLastLoadedInstance().isMusic());
		chkMusic.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				Settings.getLastLoadedInstance().setMusic(chkMusic.isChecked());
				if (chkMusic.isChecked()) {
					parent.getGame().resumeMusic();
				} else {
					parent.getGame().pauseMusic();
				}
			}
		});
		content.add(chkMusic).left();
		content.add().expandX();
		content.row();

		// 音效
		chkEffects = new VisCheckBox(LanguagesManager.getString("titles", 13));
		chkEffects.setChecked(Settings.getLastLoadedInstance().isEffects());
		chkEffects.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				Settings.getLastLoadedInstance().setEffects(chkEffects.isChecked());
			}
		});
		content.add(chkEffects).left();
		content.add().expandX();
		content.row();

		// 全屏
		chkFullScreen = new VisCheckBox(LanguagesManager.getString("titles", 14));
		chkFullScreen.setChecked(Settings.getLastLoadedInstance().isFullScreen());
		chkFullScreen.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				Settings.getLastLoadedInstance().setFullScreen(chkFullScreen.isChecked());
			}
		});
		content.add(chkFullScreen).left();
		content.add().expandX();
		content.row();

		// 血条
		chkHpAsBar = new VisCheckBox(LanguagesManager.getString("titles", 15));
		chkHpAsBar.setChecked(Settings.getLastLoadedInstance().isHPAsBar());
		chkHpAsBar.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				Settings.getLastLoadedInstance().setHPAsBar(chkHpAsBar.isChecked());
			}
		});
		content.add(chkHpAsBar).left();
		content.add().expandX();
		content.row();

		// 语言切换
		chkLanguage = new VisCheckBox(
				String.format(LanguagesManager.getString("titles", 16), Settings.getLastLoadedInstance().getLanguage()));
		chkLanguage.setChecked(true);
		chkLanguage.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				// 循环切换语言
				String[] languages = stringTable.getAvailableLanguages();
				String current = Settings.getLastLoadedInstance().getLanguage();
				int index = 0;
				for (int i = 0; i < languages.length; i++) {
					if (languages[i].equalsIgnoreCase(current)) {
						index = i;
						break;
					}
				}
				String next = languages[(index + 1) % languages.length];
				Settings.getLastLoadedInstance().setLanguage(next);
				stringTable.setLanguage(next);
				chkLanguage.setText(String.format(LanguagesManager.getString("titles", 16), next));
			}
		});
		content.add(chkLanguage).left();
		content.add().expandX();
		content.row();

		// 操控按钮
		VisTextButton controlsBtn = new VisTextButton(LanguagesManager.getString("titles", 18));
		controlsBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				parent.getStage().addActor(new KeyConfigWindow(null, parent));
			}
		});
		content.add(controlsBtn).left().colspan(2).padTop(20);
		content.row();

		// 返回按钮
		VisTextButton backBtn = new VisTextButton(LanguagesManager.getString("titles", 17));
		backBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				fadeOut();
				remove();
			}
		});
		content.add(backBtn).left().colspan(2).padTop(10);
		content.row();

		add(content).grow().pad(10);
		pack();

		// 居中显示
		setSize(Math.min(Gdx.graphics.getWidth() - 80, 640),
				Math.min(Gdx.graphics.getHeight() - 80, getHeight() + 40));
		centerWindow();
	}
}

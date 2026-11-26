package com.ntk.darkmoor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.ntk.darkmoor.config.LanguagesManager;
import com.ntk.darkmoor.config.Log;
import com.ntk.darkmoor.config.Settings;
import com.ntk.darkmoor.engine.CampDialog;
import com.ntk.darkmoor.engine.gui.BaseWindow;

public class OptionsWindow extends BaseWindow {

	private LanguagesManager stringTable;

	private CheckBox chkMusic, chkEffects, chkHpAsBar, chkFullScreen, chkLanguage;

	public OptionsWindow(CampDialog camp, GameScreenBase parent) {
		super(camp, LanguagesManager.getString("titles", 3), parent);
		loadContent();
	}

	@Override
	public void loadContent() {
		Log.debug("[OptionMenu] loadContent()");

		super.loadContent();
		setupWindow();
		stringTable = LanguagesManager.getInstance(); // "main";
		pack();

	}

	@Override
	protected void setupButtons(Skin uiSkin) {
		super.setupButtons(uiSkin);

		int widthUnit = DarkmoorGame.DISPLAY_WIDTH / 5;
		int heightUnit = DarkmoorGame.DISPLAY_HEIGHT / 6;
		buttons = new TextButton[6];

		chkMusic = new CheckBox(LanguagesManager.getString("titles", 12), uiSkin, "transparent-font64");
		chkMusic.setChecked(Settings.getLastLoadedInstance().isMusic());
		chkMusic.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				Settings.getLastLoadedInstance().setMusic(!Settings.getLastLoadedInstance().isMusic());
				if (Settings.getLastLoadedInstance().isMusic()) {
					parent.getGame().resumeMusic();
				} else {
					parent.getGame().pauseMusic();
				}
				return true;
			}
		});

		chkEffects = new CheckBox(LanguagesManager.getString("titles", 13), uiSkin, "transparent-font64");
		chkEffects.setChecked(Settings.getLastLoadedInstance().isEffects());
		chkEffects.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				Settings.getLastLoadedInstance().setEffects(!Settings.getLastLoadedInstance().isEffects());
				return true;
			}
		});

		chkFullScreen = new CheckBox(LanguagesManager.getString("titles", 14), uiSkin, "transparent-font64");
		chkFullScreen.setChecked(Settings.getLastLoadedInstance().isFullScreen());
		chkFullScreen.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				Settings.getLastLoadedInstance().setFullScreen(!Settings.getLastLoadedInstance().isFullScreen());
				return true;
			}
		});

		chkHpAsBar = new CheckBox(LanguagesManager.getString("titles", 15), uiSkin, "transparent-font64");
		chkHpAsBar.setChecked(Settings.getLastLoadedInstance().isHPAsBar());
		chkHpAsBar.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				Settings.getLastLoadedInstance().setHPAsBar(!Settings.getLastLoadedInstance().isHPAsBar());
				return true;
			}
		});

		chkLanguage = new CheckBox(
				String.format(LanguagesManager.getString("titles", 16), Settings.getLastLoadedInstance().getLanguage()),
				uiSkin, "transparent-font64");
		chkLanguage.setChecked(true);
		chkLanguage.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				// Cycle through ALL languages advertised by the string table
				// (English -> Francais -> Chinese -> English -> ...).
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
				// Keep the manager in sync so LanguagesManager.getString(...) picks
				// up the new language immediately (not just after a restart).
				stringTable.setLanguage(next);
				chkLanguage.setText(String.format(LanguagesManager.getString("titles", 16), next));
				return true;
			}
		});

		buttons[5] = new TextButton(LanguagesManager.getString("titles", 17), uiSkin, "transparent-font64");
		buttons[5].setBounds(widthUnit * 3, heightUnit << 1, widthUnit, heightUnit);
		buttons[5].setTouchable(Touchable.enabled);
		buttons[5].addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				closeAndRemoveFromParent(OptionsWindow.this);

				return true;
			}
		});

		row().minHeight(150).minWidth(400).fillY().fillX().expandX().expandY();
		add(chkMusic);
		add(chkEffects);
		row().minHeight(150).minWidth(400).fillY().fillX().expandX().expandY();
		add(chkFullScreen);
		add(chkHpAsBar);
		row().minHeight(150).minWidth(400).fillY().fillX().expandX().expandY();
		add(chkLanguage);
		add(buttons[5]);
		row().minHeight(150).minWidth(400).fillY().fillX().expandX().expandY();
	}

	private void setupWindow() {
		// getButtonTable().add(new TextButton("X", uiSkin)).height(getPadTop());
		setPosition((Gdx.graphics.getWidth() >>2) - 75, (Gdx.graphics.getHeight()>>1) - 50);
		setWidth(Gdx.graphics.getWidth() >>1);
		setHeight(200);
		defaults().space(10);
		row().fill().expandX();

		// addAction(sequence(fadeIn(5000)));
	}

}

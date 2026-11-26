package com.ntk.darkmoor.engine.gui.campwindows;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.ntk.darkmoor.GameScreenBase;
import com.ntk.darkmoor.config.Settings;
import com.ntk.darkmoor.engine.CampDialog;
import com.ntk.darkmoor.engine.gui.BaseWindow;
import com.ntk.darkmoor.engine.gui.ScreenButton;
import com.ntk.darkmoor.config.LanguagesManager;

public class PreferencesWindow extends BaseWindow {

	public PreferencesWindow(CampDialog camp, GameScreenBase parent) {
		super(camp, LanguagesManager.getString("titles", 6), parent);

		ScreenButton button;

		button = new ScreenButton("", new Rectangle(16, 40, 320, 28));
		button.addListener(new EventListener() {

			@Override
			public boolean handle(Event event) {
				return musicSelected(event);
			}

		});
		if (Settings.getLastLoadedInstance().isMusic())
			button.setText(LanguagesManager.getString("campui", 10));
		else
			button.setText(LanguagesManager.getString("campui", 11));
		//getButtons().add(button);

		button = new ScreenButton("", new Rectangle(16, 74, 320, 28));
		button.addListener(new EventListener() {

			@Override
			public boolean handle(Event event) {
				return soundSelected(event);
			}

		});
		if (Settings.getLastLoadedInstance().isEffects())
			button.setText(LanguagesManager.getString("campui", 12));
		else
			button.setText(LanguagesManager.getString("campui", 13));
		//getButtons().add(button);

		button = new ScreenButton("", new Rectangle(16, 108, 320, 28));
		button.addListener(new EventListener() {

			@Override
			public boolean handle(Event event) {
				return barGraphsSelected(event);
			}

		});
		if (Settings.getLastLoadedInstance().isHPAsBar())
			button.setText(LanguagesManager.getString("campui", 14));
		else
			button.setText(LanguagesManager.getString("campui", 15));
		//getButtons().add(button);

		button = new ScreenButton(LanguagesManager.getString("campui", 1), new Rectangle(256, 244, 80, 28));
		button.addListener(new EventListener() {

			@Override
			public boolean handle(Event event) {
				return exitSelected(event);
			}

		});
		//getButtons().add(button);

	}

	protected boolean exitSelected(Event event) {
		Settings.save();
		setClosing(true);
		return true;
	}

	protected boolean musicSelected(Event event) {
		ScreenButton button = ((ScreenButton) event.getTarget());

		if (Settings.getLastLoadedInstance().isMusic()) {
			button.setText("Music is OFF");
		} else {
			button.setText("Music is ON");
		}

		// TODO: ntk: change here the music on/off state

		Settings.getLastLoadedInstance().setMusic(Settings.getLastLoadedInstance().isMusic());
		return true;
	}

	protected boolean soundSelected(Event event) {
		ScreenButton button = ((ScreenButton) event.getTarget());

		if (Settings.getLastLoadedInstance().isEffects()) {
			button.setText("Sound FX are OFF");
		} else {
			button.setText("Sound FX are ON");
		}

		// TODO: ntk: change here the sound on/off state

		Settings.getLastLoadedInstance().setEffects(Settings.getLastLoadedInstance().isEffects());
		return true;
	}

	protected boolean barGraphsSelected(Event event) {
		ScreenButton button = ((ScreenButton) event.getTarget());

		if (Settings.getLastLoadedInstance().isHPAsBar()) {
			button.setText("Bar Graphs are OFF");
		} else {
			button.setText("Bar Graphs are ON");
		}
		
		return true;
	}
}

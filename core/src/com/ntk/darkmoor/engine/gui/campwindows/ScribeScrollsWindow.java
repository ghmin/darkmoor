package com.ntk.darkmoor.engine.gui.campwindows;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.ntk.darkmoor.GameScreenBase;
import com.ntk.darkmoor.engine.CampDialog;
import com.ntk.darkmoor.engine.gui.BaseWindow;
import com.ntk.darkmoor.engine.gui.ScreenButton;
import com.ntk.darkmoor.config.LanguagesManager;

public class ScribeScrollsWindow extends BaseWindow {

	public ScribeScrollsWindow(CampDialog camp, GameScreenBase parent) {
		super(camp, LanguagesManager.getString("titles", 5), parent);
		
		ScreenButton button;
		
		button = new ScreenButton(LanguagesManager.getString("campui", 1), new Rectangle(256, 244, 80, 28));
		button.addListener(new EventListener() {

			@Override
			public boolean handle(Event event) {
				return exitSelected(event);
			}

		});
		//getButtons().add(button);
		
		//TODO: ntk: implement the scribe logic and GUI
	}

	protected boolean exitSelected(Event event) {
		setClosing(true);
		return true;
	}

}

package com.ntk.darkmoor.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.ntk.darkmoor.DarkmoorGame;

public class DesktopLauncher {
	public static void main (String[] arg) {
		// LWJGL3 backend (libGDX >= 1.11.0). The old `LwjglApplication` /
		// `LwjglApplicationConfiguration` classes from the LWJGL2 backend are gone.
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("The Legend of Darkmoor");
		config.setWindowedMode(DarkmoorGame.DISPLAY_WIDTH, DarkmoorGame.DISPLAY_HEIGHT);
		config.setForegroundFPS(60);
		config.useVsync(true);
		// `config.disableAudio(false)` - audio is enabled by default.
		new Lwjgl3Application(new DarkmoorGame(), config);
	}
}

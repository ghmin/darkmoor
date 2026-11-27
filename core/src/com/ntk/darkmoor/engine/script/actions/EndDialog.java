package com.ntk.darkmoor.engine.script.actions;

import java.io.IOException;

import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlWriter;
import com.ntk.darkmoor.DarkmoorGame;
import com.ntk.darkmoor.engine.ScriptedDialog;

public class EndDialog extends ActionBase {

	public static final String TAG = "EndDialog";

	public EndDialog() {
		name = TAG;
	}

	@Override
	public boolean run() {
		// 修正：EndDialog 在事件数据中不带 target，旧代码在此直接 return false
		// 导致弹窗永不退出。改为防御式获取并关闭当前事件弹窗。
		if (DarkmoorGame.getInstance() == null)
			return false;

		ScriptedDialog dialog = DarkmoorGame.getInstance().getGameScreen().getDialog();
		if (dialog != null)
			dialog.exit();

		return true;
	}

	@Override
	public boolean load(XmlReader.Element xml) {
		
		if (xml == null || !TAG.equals(xml.getName()))
			return false;
		
		return true;
	}

	@Override
	public boolean save(XmlWriter writer) throws IOException {
		if (writer == null)
			return false;

		writer.element(TAG).pop();

		return true;
	}
}

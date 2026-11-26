package com.ntk.darkmoor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.ntk.darkmoor.config.KeyBindings;
import com.ntk.darkmoor.config.KeyBindings.Action;
import com.ntk.darkmoor.config.LanguagesManager;
import com.ntk.darkmoor.config.Log;
import com.ntk.darkmoor.config.Settings;
import com.ntk.darkmoor.engine.CampDialog;
import com.ntk.darkmoor.engine.gui.BaseWindow;

/**
 * 操控按键配置窗口 — 从 OptionsWindow 进入，让玩家查看和修改按键绑定。
 *
 * <p>布局：
 * <pre>
 *   ┌─────────── 操控 ───────────┐
 *   │ [向前移动]    W / ↑   [修改]│
 *   │ [向后移动]    S / ↓   [修改]│
 *   │ ...                          │
 *   │ [恢复默认]      [返回]       │
 *   └──────────────────────────────┘
 * </pre>
 *
 * <p>点"修改"后，该行进入捕获模式，下一个 keyDown 事件即绑定到该动作。
 *
 * <p>设计文档：{@code docs/v0.5/09-input-system.md}
 */
public class KeyConfigWindow extends BaseWindow {

        private KeyBindings bindings;
        private Action capturingFor = null;
        private Label[] valueLabels;
        private TextButton[] changeButtons;

        public KeyConfigWindow(CampDialog camp, GameScreenBase parent) {
                super(camp, LanguagesManager.getString("controls", 1), parent);
                // 从 Settings 加载当前绑定（或默认）
                bindings = KeyBindings.loadFrom(Settings.getLastLoadedInstance().getProperties());
                if (bindings == null) {
                        bindings = KeyBindings.defaults();
                }
                loadContent();
        }

        @Override
        public void loadContent() {
                Log.debug("[KeyConfigWindow] loadContent()");
                super.loadContent();
                setupWindow();
                pack();
        }

        @Override
        protected void setupButtons(Skin uiSkin) {
                super.setupButtons(uiSkin);

                // 动作清单（按 UI 分组顺序）
                Action[] actions = {
                        Action.MoveForward, Action.MoveBackward, Action.StrafeLeft, Action.StrafeRight,
                        Action.TurnLeft, Action.TurnRight,
                        Action.Camp, Action.Inventory, Action.Statistics,
                        Action.SelectHero1, Action.SelectHero2, Action.SelectHero3,
                        Action.SelectHero4, Action.SelectHero5, Action.SelectHero6,
                        Action.MainMenu, Action.QuickSave, Action.QuickLoad,
                        Action.ToggleMinimap, Action.ToggleNavButtons
                };

                valueLabels = new Label[actions.length];
                changeButtons = new TextButton[actions.length];

                // 用 Table 布局：动作名 | 当前按键 | 修改按钮
                Table table = new Table(uiSkin);
                table.defaults().pad(4).minHeight(40).minWidth(180);

                for (int i = 0; i < actions.length; i++) {
                        final Action action = actions[i];
                        final int index = i;

                        // 动作名（i18n，controls stringtable id = action.ordinal() + 2）
                        Label nameLabel = new Label(actionLabel(action), uiSkin, "default");
                        table.add(nameLabel).left().expandX();

                        // 当前绑定的按键名
                        valueLabels[i] = new Label(keysDisplay(action), uiSkin, "default");
                        table.add(valueLabels[i]).center().expandX();

                        // 修改按钮
                        changeButtons[i] = new TextButton(
                                        LanguagesManager.getString("controls", 20), uiSkin, "default");
                        changeButtons[i].setTouchable(Touchable.enabled);
                        changeButtons[i].addListener(new InputListener() {
                                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                                        startCapture(action, index);
                                        return true;
                                }
                        });
                        table.add(changeButtons[i]).right();
                        table.row();
                }

                // 底部按钮：恢复默认 / 返回
                TextButton resetBtn = new TextButton(
                                LanguagesManager.getString("controls", 18), uiSkin, "default");
                resetBtn.setTouchable(Touchable.enabled);
                resetBtn.addListener(new InputListener() {
                        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                                bindings = KeyBindings.defaults();
                                refreshAllLabels();
                                return true;
                        }
                });

                TextButton backBtn = new TextButton(
                                LanguagesManager.getString("controls", 19), uiSkin, "default");
                backBtn.setTouchable(Touchable.enabled);
                backBtn.addListener(new InputListener() {
                        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                                saveAndClose();
                                return true;
                        }
                });

                table.row();
                table.add(resetBtn).colspan(1).center().padTop(20);
                table.add();
                table.add(backBtn).colspan(1).center().padTop(20);

                // 把 table 放到 ScrollPane 里（动作多时支持滚动）
                ScrollPane scroll = new ScrollPane(table, uiSkin);
                scroll.setScrollingDisabled(true, false);
                add(scroll).grow().pad(10);
        }

        private void setupWindow() {
                setPosition(50, 50);
                setWidth(Gdx.graphics.getWidth() - 100);
                setHeight(Gdx.graphics.getHeight() - 100);
                defaults().space(10);
        }

        /** 进入捕获模式：等玩家按下任意键，绑定到 action。 */
        private void startCapture(Action action, int index) {
                capturingFor = action;
                valueLabels[index].setText(LanguagesManager.getString("controls", 17));

                // 创建一次性 keyDown 监听器，捕获下一个按键
                final InputListener captureListener = new InputListener() {
                        public boolean keyDown(InputEvent event, int keycode) {
                                if (capturingFor == null) return false;
                                // ESC 取消捕获
                                if (keycode == Keys.ESCAPE) {
                                        capturingFor = null;
                                        refreshAllLabels();
                                        KeyConfigWindow.this.removeListener(this);
                                        return true;
                                }
                                // 绑定该键到当前动作
                                bindings.unbind(keycode); // 先从其他动作解绑
                                bindings.bind(keycode, capturingFor);
                                capturingFor = null;
                                refreshAllLabels();
                                KeyConfigWindow.this.removeListener(this);
                                return true;
                        }
                };
                addListener(captureListener);
        }

        /** 刷新所有动作的按键显示。 */
        private void refreshAllLabels() {
                Action[] all = Action.values();
                for (int i = 0; i < all.length && i < valueLabels.length; i++) {
                        if (valueLabels[i] != null) {
                                valueLabels[i].setText(keysDisplay(all[i]));
                        }
                }
        }

        /** 返回某动作当前绑定的按键的可读名（多个键用 / 分隔）。 */
        private String keysDisplay(Action action) {
                java.util.List<Integer> keys = bindings.keysFor(action);
                if (keys.isEmpty()) return "—";
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < keys.size(); i++) {
                        if (i > 0) sb.append(" / ");
                        sb.append(KeyBindings.keyName(keys.get(i)));
                }
                return sb.toString();
        }

        /** 返回动作的 i18n 显示名。 */
        private String actionLabel(Action action) {
                // controls stringtable 的 id 与 action 的对应关系：
                // id=2 MoveForward, id=3 MoveBackward, ..., id=16 ToggleNavButtons
                int id = action.ordinal() + 2;
                try {
                        return LanguagesManager.getString("controls", id);
                } catch (Exception e) {
                        return action.name();
                }
        }

        /** 保存绑定到 Settings 并关闭窗口。 */
        private void saveAndClose() {
                bindings.saveTo(Settings.getLastLoadedInstance().getProperties());
                try {
                        Settings.save();
                } catch (Exception e) {
                        Log.error("[KeyConfigWindow] 保存设置失败: %s", e.getMessage());
                }
                closeAndRemoveFromParent(this);
        }
}

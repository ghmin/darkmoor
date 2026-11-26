package com.ntk.darkmoor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.ntk.darkmoor.config.KeyBindings;
import com.ntk.darkmoor.config.KeyBindings.Action;
import com.ntk.darkmoor.config.LanguagesManager;
import com.ntk.darkmoor.config.Log;
import com.ntk.darkmoor.config.Settings;
import com.ntk.darkmoor.engine.CampDialog;

/**
 * 操控按键配置窗口 — 使用 VisUI 的 VisDialog + ButtonBar + VisScrollPane。
 *
 * <p>v0.5 重构：从 BaseWindow（裸 scene2d Dialog）改为 VisDialog，获得：
 * <ul>
 *   <li>标准标题栏 + 关闭按钮</li>
 *   <li>底部 ButtonBar（恢复默认 / 返回）</li>
 *   <li>VisScrollPane 支持内容超出屏幕时滚动</li>
 *   <li>VisLabel/VisTextButton 字体清晰</li>
 *   <li>VisTable 自动布局</li>
 * </ul>
 *
 * <p>设计文档：{@code docs/v0.5/12-visui-migration-log.md}
 */
public class KeyConfigWindow extends VisDialog {

        private KeyBindings bindings;
        private Action capturingFor = null;
        private VisLabel[] valueLabels;

        public KeyConfigWindow(CampDialog camp, GameScreenBase parent) {
                super(LanguagesManager.getString("controls", 1));
                bindings = KeyBindings.loadFrom(Settings.getLastLoadedInstance().getProperties());
                if (bindings == null) {
                        bindings = KeyBindings.defaults();
                }
                buildUI();
        }

        private void buildUI() {
                // 动作清单
                Action[] actions = {
                        Action.MoveForward, Action.MoveBackward, Action.StrafeLeft, Action.StrafeRight,
                        Action.TurnLeft, Action.TurnRight,
                        Action.Camp, Action.Inventory, Action.Statistics,
                        Action.SelectHero1, Action.SelectHero2, Action.SelectHero3,
                        Action.SelectHero4, Action.SelectHero5, Action.SelectHero6,
                        Action.MainMenu, Action.QuickSave, Action.QuickLoad,
                        Action.ToggleMinimap, Action.ToggleNavButtons
                };

                valueLabels = new VisLabel[actions.length];

                // 用 VisTable 布局：动作名 | 当前按键 | 修改按钮
                VisTable table = new VisTable();
                table.defaults().pad(6).left();
                table.columnDefaults(0).width(200);
                table.columnDefaults(1).width(200);
                table.columnDefaults(2).width(100);

                // 表头
                table.add(new VisLabel(LanguagesManager.getString("controls", 1))).padTop(10);
                table.add(new VisLabel("Keys"));
                table.add(new VisLabel(LanguagesManager.getString("controls", 20)));
                table.row().padTop(2);
                table.add().colspan(3).height(4); // 间隔
                table.row();

                for (int i = 0; i < actions.length; i++) {
                        final Action action = actions[i];
                        final int index = i;

                        // 动作名
                        table.add(new VisLabel(actionLabel(action))).left();

                        // 当前绑定的按键名
                        valueLabels[i] = new VisLabel(keysDisplay(action));
                        valueLabels[i].setAlignment(Align.center);
                        table.add(valueLabels[i]).center();

                        // 修改按钮
                        VisTextButton changeBtn = new VisTextButton(
                                        LanguagesManager.getString("controls", 20));
                        changeBtn.addListener(new InputListener() {
                                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                                        startCapture(action, index);
                                        return true;
                                }
                        });
                        table.add(changeBtn).right();
                        table.row();
                }

                // 把 table 放到 VisScrollPane 里
                VisScrollPane scroll = new VisScrollPane(table);
                scroll.setFadeScrollBars(false);
                scroll.setScrollingDisabled(true, false); // 只允许纵向滚动
                getContentTable().add(scroll).grow().pad(10).minHeight(400).maxHeight(500);

                // 底部按钮 — 用 VisTextButton 直接放，不用 ButtonBar（API 兼容性更稳）
                VisTable bottomBar = new VisTable();
                bottomBar.defaults().pad(6);

                VisTextButton resetBtn = new VisTextButton(LanguagesManager.getString("controls", 18));
                resetBtn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                                bindings = KeyBindings.defaults();
                                refreshAllLabels();
                        }
                });
                bottomBar.add(resetBtn).growX();

                VisTextButton backBtn = new VisTextButton(LanguagesManager.getString("controls", 19));
                backBtn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                                saveAndClose();
                        }
                });
                bottomBar.add(backBtn).growX();

                getButtonsTable().add(bottomBar).padTop(10).growX();
                pack();
                // 居中显示，尺寸适配屏幕
                setSize(Math.min(Gdx.graphics.getWidth() - 80, 600), Math.min(Gdx.graphics.getHeight() - 80, 600));
                centerWindow();
        }

        /** 进入捕获模式：等玩家按下任意键，绑定到 action。 */
        private void startCapture(Action action, int index) {
                capturingFor = action;
                valueLabels[index].setText(LanguagesManager.getString("controls", 17));

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
                                bindings.unbind(keycode);
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
                fadeOut();
                remove();
        }
}

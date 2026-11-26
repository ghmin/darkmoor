package com.ntk.darkmoor.engine.gui.campwindows;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.ntk.darkmoor.GameScreenBase;
import com.ntk.darkmoor.config.SaveGameSlot;
import com.ntk.darkmoor.config.Settings;
import com.ntk.darkmoor.engine.CampDialog;
import com.ntk.darkmoor.config.LanguagesManager;

/**
 * 读取存档窗口 — VisUI 版。
 *
 * <p>v0.5 第二轮重构：从旧 {@code BaseWindow}（透明文本按钮 + 硬编码
 * buttons[6] 索引 bug）迁移为 {@link VisWindow}：
 * <ul>
 *   <li>标准标题栏"读取游戏" + 右上角关闭按钮</li>
 *   <li>存档槽用 VisTextButton 列出（中文字体来自 UITheme，可读）</li>
 *   <li>底部"取消"按钮 + 居中显示</li>
 *   <li>保留旧 API：{@link #getSelectedSlot()}（MainMenu 使用）、
 *       构造签名 {@code (CampDialog, GameScreenBase)}（MainMenu/GameOptionsWindow 使用）</li>
 * </ul>
 *
 * <p>设计文档：{@code docs/v0.5/12-visui-migration-log.md} §12.9
 */
public class LoadGameWindow extends VisWindow {

        private int selectedSlot;
        /** 营地对话框引用（可能为 null — 主菜单打开时无营地）。 */
        private final CampDialog camp;
        /** 父屏幕（主菜单或游戏界面）。 */
        private final GameScreenBase parent;

        public LoadGameWindow(CampDialog camp, GameScreenBase parent) {
                super(LanguagesManager.getString("titles", 7)); // "读取游戏"
                this.camp = camp;
                this.parent = parent;
                selectedSlot = -1;

                setModal(true);
                setMovable(true);

                VisTable table = new VisTable();
                table.defaults().pad(6).growX();

                for (int id = 0; id < Settings.getLastLoadedInstance().getSaveSlots(); id++) {
                        final int slotNumber = id;
                        String label = String.format(LanguagesManager.getString("campui", 6), id + 1); // "空存档位 %d"

                        if (parent.getGame().getSavedGame() != null) {
                                SaveGameSlot slot = parent.getGame().getSavedGame().getSlot(id);
                                if (slot != null && !slot.isEmpty())
                                        label = slot.getName();
                        }

                        VisTextButton slotButton = new VisTextButton(label);
                        slotButton.addListener(new ChangeListener() {
                                @Override
                                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                                        slotSelected(slotNumber);
                                }
                        });
                        table.add(slotButton).growX();
                        table.row();
                }

                VisTextButton cancelButton = new VisTextButton(LanguagesManager.getString("campui", 2)); // "取消"
                cancelButton.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                                closeSelf();
                        }
                });
                table.add(cancelButton).growX().padTop(16);

                add(table).grow().pad(12);
                pack();
                setSize(Math.min(Gdx.graphics.getWidth() - 80, 520),
                                Math.min(Gdx.graphics.getHeight() - 80, getHeight()));
                centerWindow();
        }

        /** 关闭窗口自身。 */
        private void closeSelf() {
                fadeOut();
                remove();
        }

        /** 选中存档槽：读取游戏（若在游戏内），切到 GameScreen，关闭营地。 */
        protected void slotSelected(int slotNumber) {
                selectedSlot = slotNumber;
                closeSelf();

                // If ingame, then load the savegame
                if (parent.getGame().loadGameSlot(selectedSlot))
                        parent.setScreen(parent.getGame().getGameScreen());
                if (camp != null)
                        camp.exit();
        }

        public int getSelectedSlot() {
                return selectedSlot;
        }
}

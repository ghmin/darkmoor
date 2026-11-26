package com.ntk.darkmoor.engine.gui.campwindows;

import org.ntk.commons.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.ntk.darkmoor.GameScreen;
import com.ntk.darkmoor.GameScreenBase;
import com.ntk.darkmoor.config.SaveGameSlot;
import com.ntk.darkmoor.config.Settings;
import com.ntk.darkmoor.engine.CampDialog;
import com.ntk.darkmoor.config.LanguagesManager;

/**
 * 保存存档窗口 — VisUI 版。
 *
 * <p>v0.5 第二轮重构：从旧 {@code BaseWindow}（ScreenButton 从未注册、
 * 保存逻辑是复制粘贴残留的空操作）迁移为 {@link VisWindow}：
 * <ul>
 *   <li>标准标题栏"保存游戏" + 右上角关闭按钮</li>
 *   <li>存档槽用 VisTextButton 列出，点击即保存到对应槽位</li>
 *   <li>覆盖已有存档时弹出 VisDialog 确认（是 / 否）</li>
 *   <li><b>修复了原版 bug</b>：旧实现点击槽位后只关闭营地、从未调用保存；
 *       现在通过 {@code GameScreen.saveGameSlot(slot)} 真正保存</li>
 * </ul>
 *
 * <p>设计文档：{@code docs/v0.5/12-visui-migration-log.md} §12.9
 */
public class SaveGameWindow extends VisWindow {

        private int selectedSlot;
        /** 营地对话框引用（可能为 null）。 */
        private final CampDialog camp;
        /** 父屏幕（游戏界面）。 */
        private final GameScreenBase parent;

        public SaveGameWindow(CampDialog camp, GameScreenBase parent) {
                super(LanguagesManager.getString("titles", 8)); // "保存游戏"
                this.camp = camp;
                this.parent = parent;
                selectedSlot = -1;

                if (parent.getGame().getSavedGame() != null)
                        parent.getGame().getSavedGame().load();

                setModal(true);
                setMovable(true);

                VisTable table = new VisTable();
                table.defaults().pad(6).growX();

                for (int id = 0; id < Settings.getLastLoadedInstance().getSaveSlots(); id++) {
                        final int slotNumber = id;
                        String label = String.format(LanguagesManager.getString("campui", 6), id + 1); // "空存档位 %d"

                        if (parent.getGame().getSavedGame() != null) {
                                SaveGameSlot slot = parent.getGame().getSavedGame().getSlot(id);
                                if (slot != null && !StringUtils.isEmpty(slot.getName()))
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

        /** 选中槽位：空槽直接保存；已有存档先确认覆盖。 */
        protected void slotSelected(int slotNumber) {
                selectedSlot = slotNumber;

                boolean occupied = false;
                if (parent.getGame().getSavedGame() != null) {
                        SaveGameSlot slot = parent.getGame().getSavedGame().getSlot(slotNumber);
                        occupied = slot != null && !StringUtils.isEmpty(slot.getName());
                }

                if (occupied) {
                        // 已有存档 → VisDialog 确认覆盖；重写 result() 接收按钮结果
                        VisDialog confirm = new VisDialog(LanguagesManager.getString("titles", 8)) {
                                @Override
                                protected void result(Object object) {
                                        SaveGameWindow.this.onConfirm(Boolean.TRUE.equals(object));
                                }
                        };
                        confirm.text(LanguagesManager.getString("campui", 18)); // "确定要覆盖这个存档吗？"
                        confirm.button(LanguagesManager.getString("campui", 3), Boolean.TRUE); // "是"
                        confirm.button(LanguagesManager.getString("campui", 4), Boolean.FALSE); // "否"
                        confirm.pack();
                        confirm.centerWindow();
                        getStage().addActor(confirm);
                        return;
                }

                doSave();
        }

        /** 确认结果回调：isYes 为 true 时执行保存。 */
        private void onConfirm(boolean isYes) {
                if (isYes)
                        doSave();
        }

        /** 执行真正的保存：调用 GameScreen.saveGameSlot（游戏内），然后关闭窗口与营地。 */
        private void doSave() {
                if (parent instanceof GameScreen) {
                        try {
                                ((GameScreen) parent).saveGameSlot(selectedSlot);
                        } catch (Exception e) {
                                com.ntk.darkmoor.config.Log.error("[SaveGameWindow] save failed: %s",
                                                new Object[] { e.getMessage() });
                        }
                }
                closeSelf();
                if (camp != null)
                        camp.exit();
        }
}

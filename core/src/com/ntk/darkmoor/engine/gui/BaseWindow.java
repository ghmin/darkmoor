package com.ntk.darkmoor.engine.gui;

import java.util.ArrayList;

import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Disposable;
import com.ntk.darkmoor.GameScreenBase;
import com.ntk.darkmoor.engine.CampDialog;

public class BaseWindow extends Dialog implements Disposable {

        private CampDialog camp;
        private boolean closing;
        private MessageBox messageBox;
        protected TextButton[] buttons;

        /** v0.5 新增：本窗口自己的 ScreenButton 列表。
         * 子类（MainWindow 等）用 screenButtons.add(button) 注册按钮，
         * BaseWindow.update(delta) 会轮询鼠标点击并调用 onSelectEntry()。 */
        protected ArrayList<ScreenButton> screenButtons;

        protected GameScreenBase parent;

        public BaseWindow(CampDialog camp, String title, GameScreenBase parentScreen) {
                super(title, parentScreen.getSkin());
                this.camp = camp;
                closing = false;
                this.parent = parentScreen;
                this.screenButtons = new ArrayList<ScreenButton>();
        }

        public void close() {
                closing = true;
        }

        public void update(float delta) {
                if (messageBox != null && !messageBox.isClosing())
                        messageBox = null;

                if (messageBox != null)
                        messageBox.update(delta);
                else
                        updateButtons(delta, buttons);

                // v0.5 新增：轮询鼠标点击，让 ScreenButton 响应（模仿 MessageBox 的做法）
                pollScreenButtonClick();
        }

        /** 检测鼠标左键点击是否命中某个 ScreenButton，命中则触发 onSelectEntry()。
         * 用边沿检测避免按住时连续触发。 */
        private boolean lastMousePressed = false;
        private void pollScreenButtonClick() {
                if (screenButtons == null || screenButtons.isEmpty()) return;
                if (getStage() == null) return;

                boolean pressed = com.badlogic.gdx.Gdx.input.isButtonPressed(com.badlogic.gdx.Input.Buttons.LEFT);
                // 边沿检测：只在"刚按下"这一帧触发
                if (pressed && !lastMousePressed) {
                        com.badlogic.gdx.math.Vector2 mouse = new com.badlogic.gdx.math.Vector2(
                                        com.badlogic.gdx.Gdx.input.getX(), com.badlogic.gdx.Gdx.input.getY());
                        getStage().screenToStageCoordinates(mouse);
                        // stage 坐标 → 本窗口本地坐标
                        com.badlogic.gdx.math.Vector2 local = screenToLocalCoordinates(mouse);
                        for (ScreenButton btn : screenButtons) {
                                if (btn != null && btn.isVisible() && btn.getRectangle() != null
                                                && btn.getRectangle().contains(local.x, local.y)) {
                                        btn.onSelectEntry();
                                        break;
                                }
                        }
                }
                lastMousePressed = pressed;
        }

        protected void setupButtons(Skin uiSkin) {

        }

        private void updateButtons(float delta, TextButton[] buttons2) {
                // update message box
                if (messageBox != null) {
                        messageBox.update(delta);

                        return;
                }
        }
        

        protected void closeAndRemoveFromParent(BaseWindow window) {
                window.close();
                parent.getStage().getRoot().removeActor(window);
        }

        public CampDialog getCamp() {
                return camp;
        }

        /** v0.5 新增：返回本窗口的 ScreenButton 列表，子类用 getButtons().add(button) 注册。
         * BaseWindow.update(delta) 会轮询鼠标点击此列表中的按钮。 */
        public ArrayList<ScreenButton> getButtons() {
                if (screenButtons == null) {
                        screenButtons = new ArrayList<ScreenButton>();
                }
                return screenButtons;
        }

        public void loadContent() {
                setupButtons(parent.getSkin());
        }

        protected Image setupBackground() {
                return null;
        }

        public void unloadContent() {
                buttons = null;
        }


        protected void setCamp(CampDialog camp) {
                this.camp = camp;
        }

        public boolean isClosing() {
                return closing;
        }

        public void setClosing(boolean closing) {
                this.closing = closing;
        }

        public MessageBox getMessageBox() {
                return messageBox;
        }

        protected void setMessageBox(MessageBox messageBox) {
                this.messageBox = messageBox;
        }

        @Override
        public void dispose() {
                unloadContent();
                
        }

}

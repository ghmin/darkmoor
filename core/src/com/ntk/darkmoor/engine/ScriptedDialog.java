package com.ntk.darkmoor.engine;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.ntk.darkmoor.config.LanguagesManager;
import com.ntk.darkmoor.config.Log;
import com.ntk.darkmoor.engine.event.RandomEvent;
import com.ntk.darkmoor.engine.event.RandomEventBus;
import com.ntk.darkmoor.engine.script.gui.ScriptChoice;

/**
 * 随机事件弹窗 — 由 {@link RandomEventBus} 创建并显示，玩家阅读事件正文后
 * 点选选项（或"确定"关闭）。
 *
 * <p><b>v0.5 第三轮重构（修复"事件弹窗永不显示"）</b>：旧版继承
 * {@code DialogBase}（scene2d Dialog）却沿用原版 C# 的手绘管线
 * （{@code draw(SpriteBatch, float)} / {@code update(float)}）。这两个方法
 * 一个是错误签名（scene2d 的绘制入口是 {@code draw(Batch, float)}，永远不会
 * 被调用），另一个从未被 GameScreen 主循环调用——因此弹窗被 setDialog 后
 * 什么也不会发生。现改为 VisDialog（VisUI），走 stage 标准渲染，自动获得
 * 模态、中文标题栏、按钮布局；选项点击通过 scene2d result 机制回调总线。
 *
 * <p>关闭链路：{@code hide(Action)} → {@link RandomEventBus#onDialogClosed()}
 * → 展示队列中的下一个事件；脚本动作 EndDialog 调用 {@link #exit()} 走同一链路。
 */
public class ScriptedDialog extends VisDialog {

        private static final String TAG = "[ScriptedDialog]";

        /** 正文/配图区域的最大宽度。 */
        private static final float BODY_WIDTH = 520f;

        private final RandomEvent event;
        private Texture picture;

        public ScriptedDialog(RandomEvent event) {
                super(LanguagesManager.getString("events", 2)); // 标题"事件"
                if (event == null)
                        throw new IllegalArgumentException("RandomEvent is null");
                this.event = event;
                setModal(true);
                setMovable(true);

                VisTable content = new VisTable();

                // 事件配图（等比缩放，宽上限 BODY_WIDTH）
                String pictureName = event.getPictureName();
                if (pictureName != null && !pictureName.isEmpty()) {
                        try {
                                picture = new Texture("data/" + pictureName);
                        } catch (Throwable t) {
                                Log.error(TAG + " picture not found: %s", new Object[] { pictureName });
                                picture = null;
                        }
                        if (picture != null) {
                                Image image = new Image(picture);
                                float scale = Math.min(1f, BODY_WIDTH / picture.getWidth());
                                image.setSize(picture.getWidth() * scale, picture.getHeight() * scale);
                                content.add(image).row();
                        }
                }

                // 正文（兼容数据中的 <br /> 换行标记）
                String text = event.getText();
                if (text != null && !text.isEmpty()) {
                        text = text.replaceAll("(?i)<br\\s*/?>", "\n").trim();
                        VisLabel label = new VisLabel(text);
                        label.setWrap(true);
                        content.add(label).width(BODY_WIDTH).padTop(12).row();
                }

                getContentTable().add(content).pad(8);

                // 选项按钮：无可用选项时显示"确定"（纯信息弹窗）
                List<ScriptChoice> enabled = new ArrayList<ScriptChoice>();
                if (event.getChoices() != null)
                        for (ScriptChoice choice : event.getChoices())
                                if (choice != null && choice.isEnabled())
                                        enabled.add(choice);

                if (enabled.isEmpty()) {
                        button(LanguagesManager.getString("events", 3), null); // "确定"
                } else {
                        for (ScriptChoice choice : enabled)
                                button(choice.getName(), (Object) choice);
                }
        }

        @Override
        protected void result(Object object) {
                if (object instanceof ScriptChoice)
                        RandomEventBus.runChoice((ScriptChoice) object);
        }

        @Override
        public void hide(Action action) {
                super.hide(action);
                RandomEventBus.onDialogClosed();
        }

        /** 兼容 EndDialog / EndChoice 脚本动作的退出入口。 */
        public void exit() {
                hide();
        }

        public RandomEvent getEvent() {
                return event;
        }

        /** 释放配图纹理。由 GameScreen（setDialog 替换时 / unloadContent）调用。 */
        public void dispose() {
                if (picture != null)
                        picture.dispose();
                picture = null;
        }
}

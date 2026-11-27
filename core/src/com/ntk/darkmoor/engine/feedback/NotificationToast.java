package com.ntk.darkmoor.engine.feedback;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.kotcrab.vis.ui.widget.VisLabel;

/**
 * 通知横幅（toast） — 屏幕中部上方短暂显示一条消息后自动淡出。
 *
 * <p>用于两类反馈（docs/v0.5/14-move-blocked-feedback.md）：
 * <ul>
 *   <li>撞墙提示（红色底）：展示 game#1 "那个方向走不通！"</li>
 *   <li>事件短消息（深蓝底）：英雄喊话 / 旁白 / 发现提示</li>
 * </ul>
 *
 * <p>实现说明：1x1 白色纹理 + {@code setColor} 着色做背景；文字用
 * VisLabel（默认样式已被 UITheme 换成中文字体）。淡入-停留-淡出用
 * scene2d Action 序列驱动，重复调用 show() 会重置动画（快速连撞不叠加）。
 */
public class NotificationToast extends Group implements Disposable {

        private static final float LABEL_MAX_WIDTH = 640f;
        private static final float PAD_X = 28f;
        private static final float PAD_Y = 16f;

        private final Texture pixelTexture;
        private final Image background;
        private final VisLabel label;

        private final float stageWidth;
        private final float stageHeight;

        public NotificationToast(float stageWidth, float stageHeight) {
                this.stageWidth = stageWidth;
                this.stageHeight = stageHeight;

                setTransform(false);
                setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);

                Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
                pixmap.setColor(Color.WHITE);
                pixmap.fill();
                pixelTexture = new Texture(pixmap);
                pixmap.dispose();

                Drawable drawable = new TextureRegionDrawable(new TextureRegion(pixelTexture));
                background = new Image(drawable);
                addActor(background);

                label = new VisLabel("");
                label.setWrap(true);
                label.setAlignment(Align.center);
                addActor(label);

                setVisible(false);
        }

        /** 显示一条通知；若正在显示则重置内容与计时（节流，不叠加）。 */
        public void show(String text, Color backgroundColor) {
                if (text == null || text.isEmpty())
                        return;

                label.setText(text);
                label.setWidth(Math.min(LABEL_MAX_WIDTH, stageWidth * 0.5f));
                label.pack();

                float width = label.getWidth() + PAD_X * 2;
                float height = label.getHeight() + PAD_Y * 2;

                background.setColor(backgroundColor);
                background.setSize(width, height);
                label.setPosition(PAD_X, PAD_Y);
                setSize(width, height);
                // 屏幕水平居中，垂直位于迷宫视图下半部（约 54% 高度处）
                setPosition((stageWidth - width) / 2f, stageHeight * 0.54f);

                toFront();
                setVisible(true);
                getColor().a = 0f;
                clearActions();
                addAction(Actions.sequence(
                                Actions.fadeIn(0.12f),
                                Actions.delay(1.4f),
                                Actions.fadeOut(0.4f),
                                Actions.hide()));
        }

        @Override
        public void dispose() {
                if (pixelTexture != null) {
                        pixelTexture.dispose();
                }
        }
}

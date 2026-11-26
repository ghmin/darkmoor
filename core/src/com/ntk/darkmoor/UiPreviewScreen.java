package com.ntk.darkmoor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.ScreenUtils;
import com.ntk.darkmoor.config.Log;
import com.ntk.darkmoor.engine.CampDialog;
import com.ntk.darkmoor.engine.gui.campwindows.LoadGameWindow;

/**
 * UI 预览/测试屏幕 — 用于<b>视觉验证</b>所有 VisUI 弹窗的实际渲染效果。
 *
 * <p><b>动机</b>：v0.5 VisUI 迁移第一轮中，弹窗文本全部不可见
 * （根因：VisUI 默认皮肤是拉丁字体，无法渲染中文）。为避免再次
 * "改了但没看到效果"，本屏幕将主要弹窗同时铺开显示，并定时截图保存。
 *
 * <p><b>启动方式</b>：
 * <pre>
 *   java ... com.ntk.darkmoor.desktop.DesktopLauncher --ui-preview
 *   （或 gradle: ./gradlew :desktop:run --args='--ui-preview'）
 * </pre>
 *
 * <p><b>行为</b>：
 * <ul>
 *   <li>同时打开 4 个弹窗：选项 / 操控配置 / 营地 / 读取存档，四象限摆放</li>
 *   <li>第 30 帧保存 {@code ui-preview-1-initial.png}（初始状态）</li>
 *   <li>之后每 5 秒（300 帧）保存 {@code ui-preview-N.png}，可先操作再截图</li>
 *   <li>按 ESC 退出程序</li>
 * </ul>
 *
 * <p>截图保存在游戏工作目录（desktop 任务的 assetsDir）。
 */
public class UiPreviewScreen extends GameScreenBase {

        private int frameCount;
        private int shotIndex;

        public UiPreviewScreen(DarkmoorGame game) {
                super(game);
        }

        @Override
        public void loadContent() {
                super.loadContent();

                // 四象限铺开所有已迁移到 VisUI 的弹窗，方便一眼对比
                OptionsWindow options = new OptionsWindow(null, this);
                options.setPosition(30, 700);
                stage.addActor(options);

                KeyConfigWindow keyConfig = new KeyConfigWindow(null, this);
                keyConfig.setPosition(660, 620);
                stage.addActor(keyConfig);

                LoadGameWindow loadGame = new LoadGameWindow(null, this);
                loadGame.setPosition(30, 120);
                stage.addActor(loadGame);

                CampDialog camp = new CampDialog(this, this);
                camp.setPosition(700, 120);
                stage.addActor(camp);

                Log.debug("[UiPreviewScreen] All preview dialogs opened.");
        }

        @Override
        public void render(float delta) {
                super.render(delta);
                frameCount++;

                if (frameCount == 30) {
                        // 初始状态截图（所有弹窗刚打开、无交互）
                        snapshot("ui-preview-1-initial.png");
                } else if (frameCount % 300 == 0) {
                        // 每 5 秒一张，供交互后对比
                        shotIndex++;
                        snapshot("ui-preview-" + (shotIndex + 1) + ".png");
                }

                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                        Gdx.app.exit();
                }
        }

        /** 把当前帧保存为 PNG（工作目录）。 */
        private void snapshot(String fileName) {
                try {
                        Pixmap frame = ScreenUtils.getFrameBufferPixmap(0, 0,
                                        Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                        // 帧缓冲原点是左下角，PNG 需要上下翻转
                        Pixmap flipped = new Pixmap(frame.getWidth(), frame.getHeight(), frame.getFormat());
                        for (int y = 0; y < frame.getHeight(); y++) {
                                flipped.drawPixmap(frame, 0, y, frame.getWidth(), 1, 0, frame.getHeight() - 1 - y);
                        }
                        PixmapIO.writePNG(Gdx.files.local(fileName), flipped);
                        frame.dispose();
                        flipped.dispose();
                        Log.debug("[UiPreviewScreen] Screenshot saved: %s", new Object[] { fileName });
                } catch (Throwable t) {
                        Log.error("[UiPreviewScreen] Snapshot failed: %s", new Object[] { t.getMessage() });
                }
        }

        @Override
        public void unloadContent() {
                super.unloadContent();
        }
}

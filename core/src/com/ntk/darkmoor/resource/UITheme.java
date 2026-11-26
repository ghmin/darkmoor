package com.ntk.darkmoor.resource;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.ntk.darkmoor.config.Log;
import com.ntk.darkmoor.config.LanguagesManager;

/**
 * UI 主题初始化 — 在 VisUI 默认皮肤的基础上注入中文字体。
 *
 * <p><b>背景</b>：VisUI 1.5.9 的默认皮肤使用拉丁位图字体（vis-default.fnt），
 * <b>不含任何 CJK 字形</b>。直接 VisUI.load() 会让所有中文文本渲染为空白/方块。
 * 本类在 VisUI 加载后用 FreeType 从 NotoSansSC-Regular.ttf 生成包含
 * "StringTable.xml 全部字符" 的字体，并替换 VisUI 各组件样式的 font 字段。
 *
 * <p><b>为什么直接改样式对象而不是 skin.add()</b>：libGDX Skin 的样式在
 * JSON 解析时就解析了字体引用，之后 skin.add("default-font", ...) 不会更新
 * 已解析的样式。因此这里直接对样式对象的 font/titleFont 字段赋值，确保
 * 本方法之后创建的所有组件都使用中文字体。
 *
 * <p>设计文档：{@code docs/v0.5/12-visui-migration-log.md} §12.9
 */
public class UITheme {

        /** 对话框正文/按钮字体（stage 被纵向压缩 0.5625 后视觉高度约 22px） */
        private static BitmapFont bodyFont;
        /** 窗口标题字体 */
        private static BitmapFont titleFont;

        /** 加载 VisUI 并注入中文字体。必须在创建任何 VisUI 组件之前调用。 */
        public static void load() {
                VisUI.load();
                Skin skin = VisUI.getSkin();

                generateCjkFonts();
                if (bodyFont == null) {
                        Log.debug("[UITheme] CJK font unavailable, VisUI keeps its Latin default font.");
                        return;
                }

                skin.add("default-font", bodyFont, BitmapFont.class);
                skin.add("default-font-large", titleFont, BitmapFont.class);
                restyle(skin);
                Log.debug("[UITheme] VisUI skin restyled with CJK fonts (body 40px, title 48px).");
        }

        /** 用 FreeType 生成两档中文字体：正文 40px、标题 48px。 */
        private static void generateCjkFonts() {
                try {
                        FileHandle cjk = Resources.getCjkFontFile();
                        if (!cjk.exists()) {
                                Log.debug("[UITheme] CJK font file not found: %s", new Object[] { cjk.path() });
                                return;
                        }
                        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(cjk);
                        String chars = Resources.buildFontCharacterSet();

                        FreeTypeFontParameter p = new FreeTypeFontParameter();
                        p.characters = chars;
                        p.size = 32;
                        p.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
                        p.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
                        p.incremental = true; // 运行时遇到缺字自动补绘（如玩家自定义文本）
                        bodyFont = gen.generateFont(p);

                        p.size = 40;
                        titleFont = gen.generateFont(p);
                        gen.dispose();
                } catch (Throwable t) {
                        Log.error("[UITheme] Failed to generate CJK fonts: %s", new Object[] { t.getMessage() });
                        bodyFont = null;
                        titleFont = null;
                }
        }

        /**
         * 逐个替换样式的字体字段。单个样式缺失不影响其他样式。
         *
         * <p>注意 VisUI 1.5.9 的皮肤 JSON 大多把样式注册为 scene2d 样式类
         * （WindowStyle/LabelStyle/...），只有 VisTextButton/VisCheckBox/VisTextField
         * 用自己的嵌套样式类。这里按皮肤 JSON 的实际注册类来改。
         */
        private static void restyle(Skin skin) {
                // 窗口标题（VisWindow 用 default，VisDialog 可用 dialog；一并列出全部窗口样式名）
                String[] windowStyles = {"default", "dialog", "resizable", "noborder"};
                for (String name : windowStyles) {
                        try {
                                Window.WindowStyle ws = skin.get(name, Window.WindowStyle.class);
                                ws.titleFont = titleFont;
                        } catch (Throwable t) {
                                Log.debug("[UITheme] Window style '%s' restyle skipped: %s",
                                                new Object[] { name, t.getMessage() });
                        }
                }

                // VisTextButton（嵌套样式类），兼容 scene2d TextButton
                String[] buttonStyles = {"default", "toggle", "blue"};
                for (String name : buttonStyles) {
                        try {
                                VisTextButton.VisTextButtonStyle ts =
                                                skin.get(name, VisTextButton.VisTextButtonStyle.class);
                                ts.font = bodyFont;
                        } catch (Throwable t) {
                                try {
                                        TextButton.TextButtonStyle ts =
                                                        skin.get(name, TextButton.TextButtonStyle.class);
                                        ts.font = bodyFont;
                                } catch (Throwable t2) {
                                        Log.debug("[UITheme] TextButton style '%s' restyle skipped: %s",
                                                        new Object[] { name, t2.getMessage() });
                                }
                        }
                }

                // VisCheckBox（嵌套样式类）
                try {
                        VisCheckBox.VisCheckBoxStyle cs = skin.get("default", VisCheckBox.VisCheckBoxStyle.class);
                        cs.font = bodyFont;
                } catch (Throwable t) {
                        Log.debug("[UITheme] VisCheckBox style restyle skipped: %s", new Object[] { t.getMessage() });
                }

                // VisTextField（嵌套样式类）
                try {
                        VisTextField.VisTextFieldStyle ts = skin.get("default", VisTextField.VisTextFieldStyle.class);
                        ts.font = bodyFont;
                        if (ts.messageFont != null)
                                ts.messageFont = bodyFont;
                } catch (Throwable t) {
                        Log.debug("[UITheme] VisTextField style restyle skipped: %s", new Object[] { t.getMessage() });
                }

                // scene2d 样式类：Label / List / SelectBox / Tree / CheckBox 兜底
                try {
                        Label.LabelStyle ls = skin.get("default", Label.LabelStyle.class);
                        ls.font = bodyFont;
                } catch (Throwable t) {
                        Log.debug("[UITheme] Label style restyle skipped: %s", new Object[] { t.getMessage() });
                }
                try {
                        List.ListStyle lls = skin.get("default", List.ListStyle.class);
                        lls.font = bodyFont;
                } catch (Throwable t) {
                        Log.debug("[UITheme] List style restyle skipped: %s", new Object[] { t.getMessage() });
                }
                try {
                        SelectBox.SelectBoxStyle ss = skin.get("default", SelectBox.SelectBoxStyle.class);
                        ss.font = bodyFont;
                        if (ss.listStyle != null)
                                ss.listStyle.font = bodyFont;
                } catch (Throwable t) {
                        Log.debug("[UITheme] SelectBox style restyle skipped: %s", new Object[] { t.getMessage() });
                }
                // 注：TreeStyle 无 font 字段（Tree 用 Label 渲染节点文本），Label 样式已覆盖。
        }

        /** 释放生成的字体与 VisUI 皮肤。 */
        public static void dispose() {
                if (bodyFont != null) {
                        bodyFont.dispose();
                        bodyFont = null;
                }
                if (titleFont != null) {
                        titleFont.dispose();
                        titleFont = null;
                }
                if (VisUI.isLoaded()) {
                        VisUI.dispose();
                }
        }
}

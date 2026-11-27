package com.ntk.darkmoor.engine.event;

import java.util.ArrayList;
import java.util.List;

import com.ntk.darkmoor.engine.script.gui.ScriptChoice;

/**
 * 随机事件封装 — 由各触发源（{@code EventSquare}、未来的 MazeZone 区域脚本、
 * 怪物触发器等）构造，统一提交给 {@link RandomEventBus} 分发。
 *
 * <p>一个事件包含可选的正文（text）、可选的配图（pictureName）以及可选的
 * 选项列表（choices）。总线的展示规则：
 * <ul>
 *   <li>choices 非空 → 显示为带选项按钮的模态弹窗，玩家点选后执行对应动作</li>
 *   <li>choices 为空 → 显示为带"确定"按钮的信息弹窗</li>
 * </ul>
 */
public class RandomEvent {

        /** 事件来源描述（仅用于日志定位，例如 "Forest@10,12 eventsquare"）。 */
        private String source;

        /** 弹窗正文，支持 \n 换行；数据中的 {@code <br />} 会在展示层转换。 */
        private String text;

        /** 配图文件名（相对 data/ 目录），可为 null。 */
        private String pictureName;

        /** 可选项，可为 null 或空（空表示纯信息弹窗）。 */
        private List<ScriptChoice> choices;

        public RandomEvent() {
                this.choices = new ArrayList<ScriptChoice>();
        }

        public RandomEvent(String source, String text, String pictureName, List<ScriptChoice> choices) {
                this.source = source;
                this.text = text;
                this.pictureName = pictureName;
                this.choices = (choices == null) ? new ArrayList<ScriptChoice>() : choices;
        }

        public boolean hasChoices() {
                return choices != null && !choices.isEmpty();
        }

        public String getSource() {
                return source;
        }

        public void setSource(String source) {
                this.source = source;
        }

        public String getText() {
                return text;
        }

        public void setText(String text) {
                this.text = text;
        }

        public String getPictureName() {
                return pictureName;
        }

        public void setPictureName(String pictureName) {
                this.pictureName = pictureName;
        }

        public List<ScriptChoice> getChoices() {
                return choices;
        }

        public void setChoices(List<ScriptChoice> choices) {
                this.choices = choices;
        }
}

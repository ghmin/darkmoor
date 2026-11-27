package com.ntk.darkmoor.engine.feedback;

/**
 * 玩家反馈层静态入口 — 引擎逻辑（Team / 脚本动作）通过它发出
 * "需要给玩家看到"的反馈，UI 侧（GameScreen）注册实现。
 *
 * <p><b>为什么经此中转而不是直接弹 UI</b>：Team 等引擎类位于 engine 包，
 * 不应反向依赖 scene2d / VisUI；且反馈的具体形式（toast、震动、音效）
 * 由 UI 层决定，引擎层只声明"发生了什么"。
 *
 * <p>设计文档：docs/v0.5/14-move-blocked-feedback.md
 */
public class Feedback {

        /** UI 侧反馈实现（由 GameScreen 注册）。 */
        public interface Listener {
                /** 移动被阻挡（撞墙 / 怪物挡路）。 */
                void onMoveBlocked();

                /** 一般短消息（英雄喊话、旁白、发现提示等）。 */
                void onMessage(String text);
        }

        private static Listener listener;

        /** 注册/替换反馈实现；传 null 注销。 */
        public static void setListener(Listener feedbackListener) {
                listener = feedbackListener;
        }

        /** 引擎侧：报告移动被阻挡。未注册 UI 时静默。 */
        public static void moveBlocked() {
                if (listener != null)
                        listener.onMoveBlocked();
        }

        /** 引擎侧：报告一条短消息。未注册 UI 时静默。 */
        public static void message(String text) {
                if (listener == null || text == null || text.isEmpty())
                        return;
                listener.onMessage(text);
        }
}

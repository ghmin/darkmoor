package com.ntk.darkmoor.engine.event;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.ntk.darkmoor.DarkmoorGame;
import com.ntk.darkmoor.config.Log;
import com.ntk.darkmoor.engine.ScriptedDialog;
import com.ntk.darkmoor.engine.feedback.Feedback;
import com.ntk.darkmoor.engine.script.actions.ActionBase;
import com.ntk.darkmoor.engine.script.actions.DisplayMessage;
import com.ntk.darkmoor.engine.script.gui.ScriptChoice;
import com.ntk.darkmoor.config.LanguagesManager;

/**
 * 统一随机事件总线 — 本项目所有随机事件的唯一入口与分发中心。
 *
 * <p><b>设计目标</b>（docs/v0.5/13-random-event-system.md）：随机事件的触发
 * 逻辑散落在各 SquareActor / 未来各系统之中，展示层（弹窗）与触发层耦合。
 * 本总线把"触发 → 排队 → 弹窗展示 → 选项执行 → 未完成功能提示"收拢为一条链路：
 * <ol>
 *   <li>触发源调用 {@link #post(RandomEvent)} 提交事件（任何线程安全考虑：
 *       当前触发均发生在 GL 线程，故不做同步）</li>
 *   <li>总线维护 FIFO 队列，一次只显示一个弹窗（VisUI ScriptedDialog），
 *       避免一次移动触发多个事件时弹窗互相覆盖</li>
 *   <li>玩家点选选项后 {@link #runChoice(ScriptChoice)} 逐个执行脚本动作：
 *       已实现的动作直接执行；未实现的动作弹出"功能尚在制作中"提示窗</li>
 *   <li>弹窗关闭（含 EndDialog/EndChoice 动作触发）后自动展示队列中的下一个事件</li>
 * </ol>
 *
 * <p><b>接入方式</b>：GameScreen.initScreenObjects() 中调用 {@link #bind(Stage)}，
 * unloadContent() 中调用 {@link #unbind()}。
 */
public class RandomEventBus {

        private static final String TAG = "[RandomEventBus]";

        /** 待展示事件队列（FIFO）。 */
        private static final ArrayDeque<RandomEvent> queue = new ArrayDeque<RandomEvent>();

        /** 事件弹窗挂载的 stage（GameScreen 的主 stage）。 */
        private static Stage stage;

        /** 当前是否有事件弹窗正在展示。 */
        private static boolean showing;

        /**
         * 已实现、可由总线直接执行的脚本动作白名单（类的 simpleName）。
         * 名单之外的动作一律视为"功能尚在制作中"。
         *
         * <p>依据 2026-11 解构结论（docs/v0.5/13-random-event-system.md §3.2）：
         * GiveItem/Healing/PlaySound/JoinCharacter/ChangePicture/ChangeText/
         * EnableChoice/DisableChoice 的 run() 均为空壳；EndChoice 有独立语义，
         * 由 runChoice() 特判处理，不在此列。
         */
        private static final Set<String> IMPLEMENTED_ACTIONS = new HashSet<String>(Arrays.asList(
                        "Teleport", "SpawnMonster", "GiveExperience", "DisplayMessage",
                        "ActivateTarget", "DeactivateTarget", "EnableTarget",
                        "DisableTarget", "ToggleTarget", "EndDialog"));

        /** 绑定 GameScreen 主 stage。重复绑定以最后一次为准。 */
        public static void bind(Stage gameStage) {
                stage = gameStage;
        }

        /** 解绑（切换屏幕时调用），丢弃未展示的事件。 */
        public static void unbind() {
                stage = null;
                showing = false;
                queue.clear();
        }

        public static boolean isBound() {
                return stage != null;
        }

        public static int getQueueSize() {
                return queue.size();
        }

        /** 提交一个随机事件；若当前没有弹窗在展示则立即弹出。 */
        public static void post(RandomEvent event) {
                if (event == null)
                        return;

                queue.addLast(event);
                Log.debug(TAG + " post() from %s, queue=%d",
                                new Object[] { event.getSource(), queue.size() });

                if (!showing)
                        showNext();
        }

        /** 事件弹窗关闭时由 ScriptedDialog 回调：展示队列中的下一个事件。 */
        public static void onDialogClosed() {
                showing = false;
                showNext();
        }

        /** 弹出队列中的下一个事件（内部）。 */
        private static void showNext() {
                if (stage == null) {
                        queue.clear();
                        showing = false;
                        return;
                }

                RandomEvent evt = queue.pollFirst();
                if (evt == null)
                        return;

                try {
                        ScriptedDialog dialog = new ScriptedDialog(evt);
                        DarkmoorGame.getInstance().getGameScreen().setDialog(dialog);
                        dialog.pack();
                        dialog.centerWindow();
                        stage.addActor(dialog);
                        showing = true;
                        Log.debug(TAG + " showing event from %s", new Object[] { evt.getSource() });
                } catch (Throwable t) {
                        t.printStackTrace();
                        // 配图缺失等异常不让事件系统卡死：记日志、跳过该事件
                        Log.error(TAG + " failed to show event (%s): %s",
                                        new Object[] { evt.getSource(), t.getMessage() });
                        showing = false;
                }
        }

        /**
         * 执行某个选项的全部脚本动作。
         * 已实现动作直接执行；DisplayMessage 转为 toast 消息（GameMessage
         * 渲染层尚未接入主循环）；其余弹出"功能尚在制作中"提示窗。
         */
        public static void runChoice(ScriptChoice choice) {
                if (choice == null)
                        return;

                for (ActionBase action : choice.getActions()) {
                        if (action == null)
                                continue;

                        String name = action.getClass().getSimpleName();

                        if ("EndChoice".equals(name)) {
                                // EndChoice 语义 = 结束当前选项分支：关闭当前事件弹窗
                                ScriptedDialog current = DarkmoorGame.getInstance().getGameScreen().getDialog();
                                if (current != null)
                                        current.exit();
                                continue;
                        }

                        if (isImplemented(name)) {
                                try {
                                        if (action instanceof DisplayMessage) {
                                                // GameMessage 系统尚未接入渲染循环，转 toast 保证可见
                                                String msg = ((DisplayMessage) action).getMessage();
                                                Feedback.message(msg);
                                        } else {
                                                action.run();
                                        }
                                } catch (Throwable t) {
                                        Log.error(TAG + " action %s failed: %s",
                                                        new Object[] { name, t.getMessage() });
                                }
                        } else {
                                Log.debug(TAG + " action not implemented yet: " + name);
                                notImplemented();
                        }
                }
        }

        /** 弹出"功能尚在制作中"模态提示窗（VisUI）。 */
        public static void notImplemented() {
                if (stage == null)
                        return;

                VisDialog dialog = new VisDialog(LanguagesManager.getString("titles", 3)); // "选项"
                dialog.text(LanguagesManager.getString("events", 1)); // "该功能尚在制作中……"
                dialog.button(LanguagesManager.getString("events", 3), null); // "确定"
                dialog.pack();
                dialog.centerWindow();
                stage.addActor(dialog);
        }

        private static boolean isImplemented(String actionSimpleName) {
                return IMPLEMENTED_ACTIONS.contains(actionSimpleName);
        }
}

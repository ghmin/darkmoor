package com.ntk.darkmoor.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.ntk.darkmoor.GameScreen;
import com.ntk.darkmoor.config.KeyBindings;
import com.ntk.darkmoor.config.KeyBindings.Action;
import com.ntk.darkmoor.config.Settings;
import com.ntk.darkmoor.engine.Compass.CompassRotation;

/**
 * 键盘输入处理器 — 把按键事件翻译成游戏动作。
 *
 * <p>通过 {@link InputMultiplexer} 注册在 stage 之前，使键盘事件优先被消费。
 * 鼠标事件（touchDown/moved/scrolled）一律返回 false，让 scene2d stage 处理。
 *
 * <p>设计文档：{@code docs/v0.5/09-input-system.md}
 *
 * @see KeyBindings
 * @see GameScreenBase#setupStage()
 */
public class KeyboardInputProcessor implements InputProcessor {

        private final Team team;
        private final KeyBindings bindings;
        private final GameScreen gameScreen;

        /** 上次触发时间戳（毫秒），用于防止 keyDown 高频重复触发。 */
        private final java.util.EnumMap<Action, Long> lastTrigger;
        private static final long MIN_INTERVAL_MS = 180;

        public KeyboardInputProcessor(Team team, KeyBindings bindings, GameScreen gameScreen) {
                this.team = team;
                this.bindings = bindings;
                this.gameScreen = gameScreen;
                this.lastTrigger = new java.util.EnumMap<Action, Long>(Action.class);
        }

        @Override
        public boolean keyDown(int keycode) {
                if (bindings == null || team == null) return false;
                Action action = bindings.lookup(keycode);
                if (action == null) return false;

                // 节流：同一动作在 MIN_INTERVAL_MS 内不重复触发
                long now = System.currentTimeMillis();
                Long last = lastTrigger.get(action);
                if (last != null && now - last < MIN_INTERVAL_MS) {
                        return true; // 消费事件但不执行
                }
                lastTrigger.put(action, now);

                return dispatch(action);
        }

        @Override
        public boolean keyUp(int keycode) {
                // 不处理；移动是"按一次走一格"模式
                return false;
        }

        @Override
        public boolean keyTyped(char ch) {
                return false;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                return false; // 让 stage 处理
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
                return false;
        }

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
                return false;
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
                return false;
        }

        /** libGDX 1.14+ 新增方法，处理触摸取消（如系统弹窗打断）。 */
        @Override
        public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
                return false;
        }

        /** 实际派发动作。 */
        private boolean dispatch(Action action) {
                switch (action) {
                        case MoveForward:
                                team.walk(0, -1);
                                return true;
                        case MoveBackward:
                                team.walk(0, 1);
                                return true;
                        case StrafeLeft:
                                team.walk(-1, 0);
                                return true;
                        case StrafeRight:
                                team.walk(1, 0);
                                return true;
                        case TurnLeft:
                                team.getLocation().setDirection(
                                        Compass.rotate(team.getLocation().getDirection(), CompassRotation.Rotate270));
                                return true;
                        case TurnRight:
                                team.getLocation().setDirection(
                                        Compass.rotate(team.getLocation().getDirection(), CompassRotation.Rotate90));
                                return true;
                        case Camp:
                                if (gameScreen != null) gameScreen.openCampDialog();
                                return true;
                        case Inventory:
                                if (gameScreen != null) gameScreen.setInterfaceType(GameScreen.TeamInterface.Inventory);
                                return true;
                        case Statistics:
                                if (gameScreen != null) gameScreen.setInterfaceType(GameScreen.TeamInterface.Statistic);
                                return true;
                        case SelectHero1: return selectHero(0);
                        case SelectHero2: return selectHero(1);
                        case SelectHero3: return selectHero(2);
                        case SelectHero4: return selectHero(3);
                        case SelectHero5: return selectHero(4);
                        case SelectHero6: return selectHero(5);
                        case MainMenu:
                                if (gameScreen != null) gameScreen.openInGameMenu();
                                return true;
                        case QuickSave:
                                if (gameScreen != null) {
                                        try { gameScreen.saveGameSlot(0); } catch (Exception e) { /* ignore */ }
                                }
                                return true;
                        case QuickLoad:
                                if (gameScreen != null) gameScreen.loadGameSlot(0);
                                return true;
                        case ToggleMinimap: {
                                Settings s = Settings.getLastLoadedInstance();
                                if (s != null) s.setShowMinimap(!s.isShowMinimap());
                                return true;
                        }
                        case ToggleNavButtons: {
                                Settings s = Settings.getLastLoadedInstance();
                                if (s != null) s.setShowNavButtons(!s.isShowNavButtons());
                                return true;
                        }
                        default:
                                return false;
                }
        }

        private boolean selectHero(int index) {
                if (team == null || team.getHeroes() == null) return false;
                Hero[] heroes = team.getHeroes();
                if (index < 0 || index >= heroes.length || heroes[index] == null) return false;
                team.setSelectedHero(heroes[index]);
                return true;
        }
}

package com.ntk.darkmoor.engine.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.ntk.darkmoor.config.Settings;
import com.ntk.darkmoor.engine.Compass.CardinalPoint;
import com.ntk.darkmoor.engine.DisplayCoordinates;
import com.ntk.darkmoor.engine.DungeonLocation;
import com.ntk.darkmoor.engine.Maze;
import com.ntk.darkmoor.engine.Square;
import com.ntk.darkmoor.engine.Square.SquareType;
import com.ntk.darkmoor.engine.Team;
import com.ntk.darkmoor.engine.actor.Door;
import com.ntk.darkmoor.engine.actor.Pit;
import com.ntk.darkmoor.engine.actor.Stair;
import com.ntk.darkmoor.engine.actor.Teleporter;

/**
 * 小地图模块 — 在主游戏界面左上角显示当前楼层的实时缩略图。
 *
 * <p>显示内容：
 * <ul>
 *   <li>墙体 / 幻象墙 / 地面（深灰 / 中灰 / 浅灰）</li>
 *   <li>门（黄）/ 楼梯（浅绿）/ 陷阱（深蓝）/ 传送器（紫）</li>
 *   <li>怪物（红）/ 地面物品（橙）</li>
 *   <li>玩家位置（蓝）+ 朝向箭头（白）</li>
 * </ul>
 *
 * <p>渲染技术：每帧用 {@link Pixmap} 在内存中重绘整个 minimap，
 * 通过 {@link Texture#draw(Pixmap, int, int)} 上传到 GPU，
 * 由一个 {@link Image} 渲染到 stage。
 *
 * <p>设计文档：{@code docs/v0.5/08-minimap-design.md}
 * <p>地图系统总览：{@code docs/v0.5/modules/12-maze-system.md}
 *
 * @see GameScreenGroup
 * @see Maze
 * @see Team
 */
public class MinimapGroup extends GameScreenGroup implements Disposable {

	/** minimap 显示区域宽度（像素） */
	public static final int MINIMAP_WIDTH = 220;
	/** minimap 显示区域高度（像素） */
	public static final int MINIMAP_HEIGHT = 220;
	/** minimap 左下角 X（stage 坐标） */
	public static final float MINIMAP_X = 8f;
	/** minimap 左下角 Y（stage 坐标）— 紧贴第一人称视图顶部 */
	public static final float MINIMAP_Y = DisplayCoordinates.PLAYABLE_WINDOW_HEIGHT - MINIMAP_HEIGHT - 8f;

	/** minimap 内边距（避免地图贴边） */
	private static final int PADDING = 4;

	// 颜色方案 — 与 docs/v0.5/08-minimap-design.md §8.5 一致
	private static final Color COLOR_BG = new Color(0f, 0f, 0f, 0.65f);
	private static final Color COLOR_BORDER = Color.LIGHT_GRAY;
	private static final Color COLOR_WALL = new Color(0.19f, 0.19f, 0.19f, 1f);
	private static final Color COLOR_ILLUSION = new Color(0.4f, 0.4f, 0.4f, 1f);
	private static final Color COLOR_GROUND = new Color(0.85f, 0.85f, 0.85f, 1f);
	private static final Color COLOR_DOOR = new Color(1f, 1f, 0.33f, 1f);
	private static final Color COLOR_STAIR = new Color(0.57f, 0.81f, 0.54f, 1f);
	private static final Color COLOR_PIT = new Color(0.17f, 0.19f, 0.54f, 1f);
	private static final Color COLOR_TELEPORTER = new Color(0.7f, 0.3f, 0.9f, 1f);
	private static final Color COLOR_MONSTER = new Color(1f, 0.33f, 0.33f, 1f);
	private static final Color COLOR_ITEM = new Color(1f, 0.65f, 0f, 1f);
	private static final Color COLOR_TEAM = new Color(0.4f, 0.41f, 0.71f, 1f);
	private static final Color COLOR_ARROW = Color.WHITE;

	public Team team;

	private Pixmap pixmap;
	private Texture texture;
	private Image image;

	@Override
	public void initialize() {
		pixmap = new Pixmap(MINIMAP_WIDTH, MINIMAP_HEIGHT, Pixmap.Format.RGBA8888);
		texture = new Texture(pixmap);
		texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

		image = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
		image.setBounds(MINIMAP_X, MINIMAP_Y, MINIMAP_WIDTH, MINIMAP_HEIGHT);
		addActor(image);
	}

	@Override
	public void update() {
		if (team == null || team.getMaze() == null)
			return;

		// 根据 Settings 决定可见性
		Settings settings = Settings.getLastLoadedInstance();
		setVisible(settings == null || settings.isShowMinimap());
		if (!isVisible())
			return;

		redraw();
	}

	/**
	 * 重绘整个 minimap。每帧调用。
	 *
	 * <p>步骤：
	 * <ol>
	 *   <li>清空 pixmap（半透明黑色背景 + 边框）</li>
	 *   <li>计算 tile 像素大小（保持长宽比，使地图居中填满）</li>
	 *   <li>遍历所有 tile，按优先级绘制颜色方块</li>
	 *   <li>绘制玩家位置（蓝方块 + 白色朝向箭头）</li>
	 *   <li>把 pixmap 上传到 texture</li>
	 * </ol>
	 */
	private void redraw() {
		Maze maze = team.getMaze();
		if (maze == null || maze.getSize() == null)
			return;

		int mazeW = (int) maze.getSize().x;
		int mazeH = (int) maze.getSize().y;
		if (mazeW <= 0 || mazeH <= 0)
			return;

		// 计算 tile 像素大小（保持长宽比，地图居中）
		float tilePxF = Math.min(
				(float) (MINIMAP_WIDTH - 2 * PADDING) / mazeW,
				(float) (MINIMAP_HEIGHT - 2 * PADDING) / mazeH);
		if (tilePxF < 1f)
			tilePxF = 1f;

		int tilePx = (int) Math.floor(tilePxF);
		if (tilePx < 1)
			tilePx = 1;

		int drawW = tilePx * mazeW;
		int drawH = tilePx * mazeH;
		int offsetX = (MINIMAP_WIDTH - drawW) / 2;
		int offsetY = (MINIMAP_HEIGHT - drawH) / 2;

		// 1. 清空 + 背景
		pixmap.setColor(COLOR_BG);
		pixmap.fill();

		// 边框
		pixmap.setColor(COLOR_BORDER);
		pixmap.drawRectangle(0, 0, MINIMAP_WIDTH, MINIMAP_HEIGHT);

		// 2. 遍历所有 tile
		DungeonLocation loc = team.getLocation();
		Vector2 coord = (loc != null) ? loc.getCoordinates() : null;
		String currentMazeName = maze.getName();

		Vector2 probe = new Vector2();
		for (int y = 0; y < mazeH; y++) {
			for (int x = 0; x < mazeW; x++) {
				probe.set(x, y);
				Square sq = maze.getSquare(probe);
				if (sq == null)
					continue;

				Color color = tileColor(sq);
				int px = offsetX + x * tilePx;
				int py = offsetY + y * tilePx;
				pixmap.setColor(color);
				pixmap.fillRectangle(px, py, tilePx, tilePx);
			}
		}

		// 3. 绘制玩家位置 + 朝向箭头
		if (loc != null && coord != null && currentMazeName != null
				&& currentMazeName.equals(loc.getMaze())) {
			int tx = (int) coord.x;
			int ty = (int) coord.y;
			if (tx >= 0 && tx < mazeW && ty >= 0 && ty < mazeH) {
				int px = offsetX + tx * tilePx;
				int py = offsetY + ty * tilePx;

				// 玩家方块（蓝色，比 tile 大 1px 边缘）
				pixmap.setColor(COLOR_TEAM);
				pixmap.fillRectangle(px, py, tilePx, tilePx);

				// 朝向箭头
				CardinalPoint dir = loc.getDirection();
				if (dir != null) {
					drawDirectionArrow(px, py, tilePx, dir);
				}
			}
		}

		// 4. 上传到 GPU
		texture.draw(pixmap, 0, 0);
	}

	/**
	 * 决定单个 tile 的颜色。优先级（从低到高）：
	 * <pre>
	 *   Wall/Illusion/Ground → Item → Door/Stair/Pit/Teleporter → Monster
	 * </pre>
	 * 玩家位置在 redraw() 末尾单独画，不进入此方法。
	 */
	private Color tileColor(Square sq) {
		// 基础颜色
		Color color;
		SquareType type = sq.getType();
		if (type == SquareType.Wall) {
			color = COLOR_WALL;
		} else if (type == SquareType.Illusion) {
			color = COLOR_ILLUSION;
		} else {
			color = COLOR_GROUND;
		}

		// 物品（覆盖地面）
		if (sq.getItemCount() > 0) {
			color = COLOR_ITEM;
		}

		// Actor（覆盖物品；按类型选择颜色）
		Object actor = sq.getActor();
		if (actor instanceof Door) {
			color = COLOR_DOOR;
		} else if (actor instanceof Stair) {
			color = COLOR_STAIR;
		} else if (actor instanceof Pit) {
			color = COLOR_PIT;
		} else if (actor instanceof Teleporter) {
			color = COLOR_TELEPORTER;
		}

		// 怪物（最高优先级，覆盖一切 — 威胁优先）
		if (sq.getMonsterCount() > 0) {
			color = COLOR_MONSTER;
		}

		return color;
	}

	/**
	 * 在玩家方块上画一个白色三角形箭头指示朝向。
	 *
	 * <p>注意 Pixmap 坐标系 Y 向下：
	 * <ul>
	 *   <li>North（迷宫 -Y）→ 箭头朝 pixmap 上方</li>
	 *   <li>South（迷宫 +Y）→ 箭头朝 pixmap 下方</li>
	 *   <li>East（迷宫 +X）→ 箭头朝 pixmap 右方</li>
	 *   <li>West（迷宫 -X）→ 箭头朝 pixmap 左方</li>
	 * </ul>
	 */
	private void drawDirectionArrow(int px, int py, int tilePx, CardinalPoint dir) {
		int cx = px + tilePx / 2;
		int cy = py + tilePx / 2;
		int armLen = Math.max(tilePx + 2, 4);
		int half = Math.max(tilePx / 2, 1);

		pixmap.setColor(COLOR_ARROW);
		switch (dir) {
			case North:
				// 顶点在玩家方块上方
				pixmap.fillTriangle(
						cx, cy - armLen,
						cx - half, cy,
						cx + half, cy);
				break;
			case South:
				pixmap.fillTriangle(
						cx, cy + armLen,
						cx - half, cy,
						cx + half, cy);
				break;
			case East:
				pixmap.fillTriangle(
						cx + armLen, cy,
						cx, cy - half,
						cx, cy + half);
				break;
			case West:
				pixmap.fillTriangle(
						cx - armLen, cy,
						cx, cy - half,
						cx, cy + half);
				break;
		}
	}

	@Override
	public void dispose() {
		if (texture != null) {
			texture.dispose();
			texture = null;
		}
		if (pixmap != null) {
			pixmap.dispose();
			pixmap = null;
		}
		image = null;
	}
}

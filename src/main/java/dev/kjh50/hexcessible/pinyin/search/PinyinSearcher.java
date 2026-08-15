package dev.kjh50.hexcessible.pinyin.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.tizu.hexcessible.Utils;
import dev.tizu.hexcessible.entries.PatternEntries.Entry;
import me.towdium.pinin.PinIn;
import net.minecraft.client.MinecraftClient;

/**
 * 拼音感知的搜索评分器。
 * <p>
 * 完整复刻原版 Hexcessible 的 {@code PatternEntries.get(String)} 评分逻辑，
 * 并叠加两个增强：
 * <ol>
 *   <li><b>contains 回退</b>：顺序匹配失败但整体包含时给予基础分，对 CJK 名称友好；</li>
 *   <li><b>PinIn 拼音匹配</b>：按当前游戏语言动态调整权重——
 *       中文环境拼音匹配优先（+5000），其他语言拼音辅助（+2000）。</li>
 * </ol>
 * 该实现不修改原版任何代码，原版的 {@code Utils.fluffySearch} 依旧直接复用。
 */
public final class PinyinSearcher {
	/** PinIn 拼音匹配引擎 — 懒加载，避免类初始化阶段实例化 */
	private static PinIn pinIn;

	private PinyinSearcher() {
	}

	private static PinIn getPinIn() {
		if (pinIn == null) {
			pinIn = new PinIn();
			pinIn.config().fSh2S(true).fZh2Z(true).fCh2C(true).commit();
		}
		return pinIn;
	}

	/**
	 * 检测当前游戏语言是否为中文，用于动态调整搜索权重。
	 * 中文环境：PinIn 拼音匹配优先；其他语言：英文名/ID 匹配优先。
	 */
	public static boolean isChineseLanguage() {
		try {
			var client = MinecraftClient.getInstance();
			if (client != null) {
				var lang = client.getLanguageManager().getLanguage();
				return lang != null && lang.startsWith("zh");
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	/**
	 * 执行拼音感知的模糊搜索，结果写入共享缓存（由原版 {@code invalidateCaches()} 统一管理）。
	 *
	 * @param query      用户输入（非空）
	 * @param entries    原版 {@code PatternEntries.entries}（@Shadow 注入）
	 * @param smartSigs  {@code SmartSigRegistry.get(query)} 返回的智能签名条目
	 * @param cache      原版 {@code PatternEntries.fuzzySearchCache}（@Shadow 注入）
	 * @return 按相关性降序排列的条目列表
	 */
	public static List<Entry> search(String query, List<Entry> entries,
			List<Entry> smartSigs, Map<String, List<Entry>> cache) {
		boolean chinese = isChineseLanguage();

		var pool = new ArrayList<Entry>(entries.size() + smartSigs.size());
		pool.addAll(entries);
		pool.addAll(smartSigs);

		var result = new ArrayList<>(pool.stream()
				.map(e -> {
					var score = e.z() * 10_000; // base score based on z index
					int nameMatch = fluffySearchWithFallback(query, e.name());
					int idMatch = fluffySearchWithFallback(query,
							e.id().replaceAll("[:_/]", " "));
					// 根据当前游戏语言调整搜索权重
					if (chinese) {
						// 中文环境：PinIn 拼音匹配优先，英文名/ID 辅助
						score += nameMatch * 3;
						score += idMatch * 1;
						if (!e.name().isEmpty() && getPinIn().contains(e.name(), query))
							score += 5000;
					} else {
						// 非中文环境：英文名/ID 匹配优先，PinIn 辅助
						score += nameMatch * 5;
						score += idMatch * 3;
						if (!e.name().isEmpty() && getPinIn().contains(e.name(), query))
							score += 2000;
					}
					return Map.entry(e, score);
				}).filter(e -> e.getValue() > 0)
				.sorted((a, b) -> b.getValue() - a.getValue())
				.map(Map.Entry::getKey)
				.toList());

		cache.put(query, result);
		return result;
	}

	/**
	 * 原版 {@code Utils.fluffySearch} 的 contains 回退包装。
	 * 顺序匹配失败但整体包含时返回基础分 10，帮助 CJK 名称按翻译字符命中。
	 */
	private static int fluffySearchWithFallback(String query, String candidate) {
		int score = Utils.fluffySearch(query, candidate);
		if (score > 0)
			return score;
		if (query == null || candidate == null || query.isEmpty())
			return 0;
		if (candidate.toLowerCase().contains(query.toLowerCase()))
			return 10;
		return 0;
	}
}

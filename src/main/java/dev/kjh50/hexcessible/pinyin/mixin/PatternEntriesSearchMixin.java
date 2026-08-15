package dev.kjh50.hexcessible.pinyin.mixin;

import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.kjh50.hexcessible.pinyin.search.PinyinSearcher;
import dev.tizu.hexcessible.entries.PatternEntries;
import dev.tizu.hexcessible.entries.PatternEntries.Entry;
import dev.tizu.hexcessible.smartsig.SmartSig.SmartSigRegistry;

/**
 * 在 {@code PatternEntries.get(String)} 入口处接管搜索，
 * 将原版英文评分替换为拼音感知评分，原版逻辑不做任何修改。
 * <p>
 * 复用原版私有字段：
 * <ul>
 *   <li>{@code entries} —— 已索引的全部法术条目；</li>
 *   <li>{@code fuzzySearchCache} —— 搜索缓存，原版 {@code invalidateCaches()}
 *       刷新时会一并清空本插件的缓存结果。</li>
 * </ul>
 */
// remap = false：hexcessible 发布 jar 自身成员已是 named（Yarn）名，
// 运行时方法/字段名与源码一致，无需（也无法）走 loom 映射表
@Mixin(value = PatternEntries.class, remap = false)
public abstract class PatternEntriesSearchMixin {
	@Shadow(remap = false)
	private List<Entry> entries;

	@Shadow(remap = false)
	@Final
	private Map<String, List<Entry>> fuzzySearchCache = null;

	@Inject(method = "get(Ljava/lang/String;)Ljava/util/List;",
			at = @At("HEAD"), cancellable = true, remap = false)
	private void hexcessiblePinyin$pinyinSearch(String query,
			CallbackInfoReturnable<List<Entry>> cir) {
		// 空查询交给原版逻辑（返回全量列表）
		if (query == null || query.isEmpty())
			return;

		// 命中缓存直接返回（缓存由原版 invalidateCaches 统一管理）
		if (fuzzySearchCache.containsKey(query)) {
			cir.setReturnValue(fuzzySearchCache.get(query));
			return;
		}

		cir.setReturnValue(PinyinSearcher.search(query, entries,
				SmartSigRegistry.get(query), fuzzySearchCache));
	}
}

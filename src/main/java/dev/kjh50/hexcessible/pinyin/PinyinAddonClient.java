package dev.kjh50.hexcessible.pinyin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;

/**
 * Hexcessible 中文拼音搜索附属插件入口。
 * <p>
 * 搜索逻辑通过 Mixin 在 {@code dev.tizu.hexcessible.entries.PatternEntries}
 * 中注入，本类仅负责打印加载信息。
 */
public class PinyinAddonClient implements ClientModInitializer {
	public static final String MOD_ID = "hexcessible-pinyin";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("[{}] 中文拼音搜索已启用：现在可以用拼音或中文模糊搜索法术图案", MOD_ID);
	}
}

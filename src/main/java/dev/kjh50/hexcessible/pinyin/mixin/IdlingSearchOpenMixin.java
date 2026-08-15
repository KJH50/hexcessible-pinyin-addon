package dev.kjh50.hexcessible.pinyin.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.tizu.hexcessible.Hexcessible;
import dev.tizu.hexcessible.drawstate.Idling;
import dev.tizu.hexcessible.drawstate.KeyboardDrawing;

/**
 * 在施法界面「空闲」状态下输入字符时直接打开法术搜索框。
 * <p>
 * 原版 hexcessible 用 Ctrl+Space 打开搜索框（{@code Idling.onKeyPress} 中
 * {@code key == 32 && (mods & 2) != 0}），但 Windows 中文输入法（微软拼音）默认把
 * Ctrl+Space 注册为「输入法开/关」系统热键，在 OS 层直接吞掉该按键，游戏永远收不到
 * ——于是搜索框只能靠点击出现，按键无法打开。
 * <p>
 * 本 Mixin 在 {@code Idling.onCharType} 入口接管：输入任意字母/数字/汉字时
 * （画符键 qweasd 除外，键盘画符行为保留），直接调用原版
 * {@code Idling.onKeyPress(GLFW_KEY_SPACE, GLFW_MOD_CONTROL)} 模拟 Ctrl+Space
 * 按键事件，以鼠标所在格为锚点打开搜索框，完全不依赖系统热键。
 */
@Mixin(value = Idling.class, remap = false)
public abstract class IdlingSearchOpenMixin {
	@Inject(method = "onCharType(C)V", at = @At("HEAD"), cancellable = true, remap = false)
	private void hexcessiblePinyin$openSearchOnTypedChar(char c, CallbackInfo ci) {
		// 自动补全未开启：保持原版行为（输入不做任何事）
		if (!Hexcessible.cfg().autoComplete.allow)
			return;
		// 键盘画符开启时，画符键（qweasd）仍归原版处理
		if (Hexcessible.cfg().keyboardDraw.allow && KeyboardDrawing.validSig.contains(c))
			return;
		// 只对可输入的字母/数字/汉字响应（空格、标点等忽略）
		if (!Character.isLetterOrDigit(c))
			return;

		// 模拟原版 Ctrl+Space：GLFW_KEY_SPACE = 32，GLFW_MOD_CONTROL = 2
		((Idling) (Object) this).onKeyPress(32, 2);
		ci.cancel();
	}
}

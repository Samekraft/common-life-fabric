package com.example.mixin;

import com.example.ExampleMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public class FoodDataMixin {
	@Unique
	private static final ThreadLocal<ServerPlayer> COMMON_LIFE_CURRENT_PLAYER = new ThreadLocal<>();

	@Inject(method = "tick", at = @At("HEAD"))
	private void commonLife$setCurrentPlayer(ServerPlayer player, CallbackInfo ci) {
		COMMON_LIFE_CURRENT_PLAYER.set(player);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void commonLife$clearCurrentPlayer(ServerPlayer player, CallbackInfo ci) {
		COMMON_LIFE_CURRENT_PLAYER.remove();
	}

	@Redirect(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V")
	)
	private void commonLife$gateHeal(ServerPlayer player, float amount) {
		if (ExampleMod.canNaturalRegen(player)) {
			player.heal(amount);
		}
	}

	@Redirect(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V")
	)
	private void commonLife$gateExhaustion(FoodData instance, float amount) {
		ServerPlayer player = COMMON_LIFE_CURRENT_PLAYER.get();
		if (player == null || ExampleMod.canNaturalRegen(player)) {
			instance.addExhaustion(amount);
		}
	}
}

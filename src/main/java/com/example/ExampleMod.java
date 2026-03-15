package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ExampleMod implements ModInitializer {
	public static final String MOD_ID = "modid";
	private static float sharedHealth = -1.0f;
	private static boolean deathWaveTriggered = false;

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Shared HP mod initialized");

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			sharedHealth = -1.0f;
			ensureNaturalRegenEnabled(server);
		});

		ServerTickEvents.END_SERVER_TICK.register(ExampleMod::tickSharedHealth);
	}

	private static void tickSharedHealth(MinecraftServer server) {
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		if (players.isEmpty()) {
			sharedHealth = -1.0f;
			return;
		}

		float minHealth = Float.MAX_VALUE;
		float maxHealth = Float.MIN_VALUE;
		float minMaxHealth = Float.MAX_VALUE;
		int aliveCount = 0;
		boolean anyDead = false;
		for (ServerPlayer player : players) {
			if (!player.isAlive()) {
				anyDead = true;
				continue;
			}
			float health = player.getHealth();
			minHealth = Math.min(minHealth, health);
			maxHealth = Math.max(maxHealth, health);
			minMaxHealth = Math.min(minMaxHealth, player.getMaxHealth());
			aliveCount++;
		}

		if (anyDead) {
			if (!deathWaveTriggered && aliveCount > 0) {
				for (ServerPlayer player : players) {
					if (player.isAlive()) {
						player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
					}
				}
				deathWaveTriggered = true;
			}
			sharedHealth = 0.0f;
			return;
		} else {
			deathWaveTriggered = false;
		}

		if (aliveCount == 0) {
			sharedHealth = -1.0f;
			return;
		}

		if (sharedHealth < 0.0f || (sharedHealth <= 0.0f && minHealth > 0.0f)) {
			sharedHealth = Math.min(minHealth, minMaxHealth);
		}

		if (minHealth < sharedHealth - 0.001f) {
			sharedHealth = minHealth;
		} else if (maxHealth > sharedHealth + 0.001f) {
			sharedHealth = maxHealth;
		}

		if (sharedHealth > minMaxHealth) {
			sharedHealth = minMaxHealth;
		}
		if (sharedHealth < 0.0f) {
			sharedHealth = 0.0f;
		}

		for (ServerPlayer player : players) {
			if (!player.isAlive()) {
				continue;
			}
			float target = Math.min(sharedHealth, player.getMaxHealth());
			if (player.getHealth() != target) {
				player.setHealth(target);
			}
		}
	}

	public static boolean canNaturalRegen(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return true;
		}
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		for (ServerPlayer p : players) {
			if (p.getFoodData().getFoodLevel() < 20) {
				return false;
			}
		}
		return true;
	}

	private static void ensureNaturalRegenEnabled(MinecraftServer server) {
		server.getAllLevels().forEach(level -> {
			level.getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION, true, server);
		});
	}
}

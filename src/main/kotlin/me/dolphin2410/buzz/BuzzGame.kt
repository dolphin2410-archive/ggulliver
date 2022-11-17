package me.dolphin2410.buzz

import io.github.monun.heartbeat.coroutines.HeartbeatScope
import io.github.monun.heartbeat.coroutines.Suspension
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title.title
import org.bukkit.*
import org.bukkit.entity.Bee
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import java.util.*
import kotlin.math.max


class BuzzGame: Listener {
    val ARR = arrayOf("red", "blue", "yellow", "purple", "orange", "aqua", "pink", "white")
    lateinit var scoreboard: Objective
    var valid = true
    fun start(plugin: BuzzPlugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        object: BukkitRunnable() {
            override fun run() {
                if (!valid) {
                    cancel()
                    return
                }
                if (Random().nextInt(100) < 15) {
                    spawnBee(randomLocation(BuzzPlugin.instance.arena!!), ARR.random())
                }
            }
        }.runTaskTimer(plugin, 0, 40L)


        for (player in Bukkit.getOnlinePlayers()) {
            player.inventory.clear()
            for (i in 1..35) {
                player.inventory.setItem(i, ItemStack(Material.BARRIER))
            }

            player.inventory.setItem(40, ItemStack(Material.BARRIER))
        }

        scoreboard = initScoreboard()
    }

    fun initScoreboard(): Objective {
        val manager = Bukkit.getScoreboardManager()
        val board = manager.newScoreboard
        val objective = board.registerNewObjective("buzz", Criteria.DUMMY, text("BUZZ"))
        objective.displaySlot = DisplaySlot.SIDEBAR
        objective.displayName(text("BUZZ"))
        for (player in Bukkit.getOnlinePlayers()) {
            val score = objective.getScoreFor(player)
            score.score = 0
            player.scoreboard = board
        }
        return objective
    }

    fun scoreAdd(player: Player) {
        scoreboard.getScoreFor(player).score += 1
    }

    fun removeScore(player: Player) {
        scoreboard.getScoreFor(player).resetScore()
    }

    fun stop(plugin: BuzzPlugin) {
        valid = false
        for (entity in plugin.server.entities) {
            entity.remove()
        }

        for (player in Bukkit.getOnlinePlayers()) {
            player.inventory.clear()
        }

        scoreboard.unregister()
    }

    fun randomLocation(arena: Pair<Location, Location>): Location {
        val y = max(arena.first.y, arena.second.y) + 2

        val x = random(arena.first.blockX, arena.second.blockX)
        val z = random(arena.first.blockZ, arena.second.blockZ)
        return Location(arena.first.world, x.toDouble(), y, z.toDouble())
    }

    fun random(x: Int, y: Int): Int {
        return if (x > y) {
            (y..x).random()
        } else {
            (x..y).random()
        }
    }

    fun convertFlower(type: Material): String {
        return when(type) {
            Material.POPPY -> { "red" }          // red
            Material.CORNFLOWER -> { "blue" }      // blue
            Material.DANDELION -> { "yellow" }       // yellow
            Material.ALLIUM -> { "purple" }          // purple
            Material.ORANGE_TULIP -> { "orange" }    // orange
            Material.BLUE_ORCHID -> { "aqua" }     // aqua
            Material.PINK_TULIP -> { "pink" }      // pink
            Material.OXEYE_DAISY -> { "white" }     // white
            else -> { "NONE" }
        }
    }

    @EventHandler
    fun onBlockBreak(e: BlockBreakEvent) {
        if (valid) {
            if (ARR.contains(convertFlower(e.block.type))) {
                val data = e.block.blockData.clone()
                HeartbeatScope().launch {
                    val suspension = Suspension()
                    suspension.delay(5000)
                    e.block.blockData = data
                }
            } else {
                e.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onEntityInteract(e: PlayerInteractAtEntityEvent) {
        if (valid) {
            val entity = e.rightClicked
            val flower = e.player.inventory.itemInMainHand.type
            if (entity.type == EntityType.BEE) {
                entity as Bee
                val convert = convertFlower(flower)
                if (convert == "NONE") return
                if ((entity.customName() as? TextComponent)?.content()?.removePrefix("[")?.removeSuffix("]") == convert) {
                    entity.customName(text("Owner: ${e.player.name}", NamedTextColor.GREEN))
                    scoreAdd(e.player)
                    HeartbeatScope().launch {
                        val suspension = Suspension()
                        suspension.delay(7000)
                        entity.remove()
                        val firework = entity.world.spawn(entity.location, Firework::class.java)
                        val meta = firework.fireworkMeta
                        meta.addEffects(FireworkEffect.builder().with(FireworkEffect.Type.STAR).withColor(Color.AQUA).build())
                        firework.fireworkMeta = meta
                        suspension.delay(500)
                        firework.detonate()
                    }
                } else if ((entity.customName() as? TextComponent)?.content()?.startsWith("[") == true) {
                    HeartbeatScope().launch {
                        val suspension = Suspension()
                        removeScore(e.player)
                        entity.target = e.player
                        freezePlayer(e.player)
                        suspension.delay(3000)
                        unfreeze(e.player)
                        killPlayer(e.player)
                    }
                }
            }
        }
    }

    @EventHandler
    fun breedEvent(e: EntityBreedEvent) {
        if (valid) {
            e.isCancelled = true
        }
    }

    fun killPlayer(p: Player) {
        p.gameMode = GameMode.SPECTATOR
        p.showTitle(title(text("You Lost", NamedTextColor.RED), text("looser")))
    }

    fun spawnBee(location: Location, color: String): Bee {
        val bee = location.world!!.spawn(location, Bee::class.java)
        bee.customName(text("[$color]"))
        return bee
    }

    fun freezePlayer(p: Player) {
        BuzzPlugin.instance.freezelist.add(p)
    }

    fun unfreeze(p: Player) {
        BuzzPlugin.instance.freezelist.remove(p)
    }
}
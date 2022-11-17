package me.dolphin2410.buzz

import io.github.monun.kommand.getValue
import io.github.monun.kommand.kommand
import io.github.monun.kommand.wrapper.BlockPosition3D
import io.github.monun.tap.fake.FakeEntityServer
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector

class BuzzPlugin: JavaPlugin(), Listener {
    companion object {
        lateinit var instance: BuzzPlugin
    }

    lateinit var server: FakeEntityServer
    private var game: BuzzGame? = null
    var arena: Pair<Location, Location>? = null
    val freezelist = ArrayList<Player>()

    override fun onEnable() {
        instance = this
        server = FakeEntityServer.create(this)
        Bukkit.getScheduler().runTaskTimer(this, server::update, 0L, 1L)
        Bukkit.getPluginManager().registerEvents(this, this)

        kommand {
            register("buzz") {
                then("setarena") {
                    then("start" to blockPosition()) {
                        then("end" to blockPosition()) {
                            executes {
                                val start: BlockPosition3D by it
                                val end: BlockPosition3D by it

                                if (game != null) {
                                    sender.sendMessage(text("The game has already started", NamedTextColor.RED))
                                } else {
                                    arena = start.toBlock(player.world).location to end.toBlock(player.world).location
                                    player.inventory.addItem(ItemStack(Material.POPPY))
                                    player.inventory.addItem(ItemStack(Material.DANDELION))
                                    player.inventory.addItem(ItemStack(Material.ORANGE_TULIP))
                                    player.inventory.addItem(ItemStack(Material.PINK_TULIP))
                                    player.inventory.addItem(ItemStack(Material.OXEYE_DAISY))
                                    player.inventory.addItem(ItemStack(Material.BLUE_ORCHID))
                                    player.inventory.addItem(ItemStack(Material.CORNFLOWER))
                                    player.inventory.addItem(ItemStack(Material.ALLIUM))
                                    player.sendMessage("Place the plants!")
                                }
                            }
                        }
                    }
                }
                then("start") {
                    executes {
                        if (game == null) {
                            if (arena == null) {
                                sender.sendMessage(text("The arena hasn't been specified", NamedTextColor.RED))
                                return@executes
                            }
                            game = BuzzGame()
                            game!!.start(this@BuzzPlugin)
                        } else {
                            sender.sendMessage(text("The game has already started", NamedTextColor.RED))
                        }
                    }
                }

                then("stop") {
                    executes {
                        if (game == null) {
                            sender.sendMessage(text("The game hasn't started yet", NamedTextColor.RED))
                        } else {
                            game!!.stop(this@BuzzPlugin)
                            freezelist.clear()
                            game = null
                            player.performCommand("kill @e[type=bee]")
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        server.addPlayer(e.player)
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        server.removePlayer(e.player)
    }

    @EventHandler
    fun onPlayerInv(e: InventoryClickEvent) {
        if (game != null) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerPlace(e: BlockPlaceEvent) {
        if (game != null) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onMove(e: PlayerMoveEvent) {
        if (game != null) {
            if (freezelist.contains(e.player)) {
                e.isCancelled = true
            }
        }
    }

    @EventHandler
    fun hit(e: EntityDamageByEntityEvent) {
        if (game != null) {
            if (e.entity is Player && e.damager is Player) {
                val dir = (e.damager as Player).eyeLocation.direction
                (e.entity as Player).knockback(1.5, dir.x, dir.z)
            }
        }
    }
}
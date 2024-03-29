package net.brian.heroesskills.bukkit.listeners;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.brian.heroesskills.HeroesSkills;
import net.brian.heroesskills.api.players.PlayerSkillProfile;
import net.brian.heroesskills.api.skills.casting.ClickSequence;
import net.brian.heroesskills.api.skills.casting.ClickType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.*;

public class CastingListener implements Listener {

    public static long REFRESH_TICKS = 60;

    private List<String> availableTypes = List.of(
            "MAGICWIND",
            "SPWEAPON",
            "SWORD"
    );

    private final HeroesSkills plugin;
    private Map<UUID,CastingProfile> castingMap = new HashMap<>();

    public CastingListener(HeroesSkills plugin){
        this.plugin = plugin;
    }


    @EventHandler
    public void onClickAirOrBlock(PlayerInteractEvent event){
        if(event.getHand() == null) return;
        if(!event.getHand().equals(EquipmentSlot.HAND)) return;
        if(!validCastingItem(event.getPlayer().getEquipment().getItemInMainHand())) return;

        CastingProfile castingProfile = castingMap.computeIfAbsent(event.getPlayer().getUniqueId(),k->new CastingProfile(event.getPlayer()));
        if(event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            castingProfile.updateClick(ClickType.RIGHT);
            return;
        }
        else if(!castingProfile.sequence.isEmpty() && (event.getAction().equals(Action.LEFT_CLICK_AIR) || event.getAction().equals(Action.LEFT_CLICK_BLOCK))) {
            castingProfile.updateClick(ClickType.LEFT);
            return;
        }
    }

    @EventHandler
    public void onClickEntity(PlayerInteractEntityEvent event){
        if(!event.getHand().equals(EquipmentSlot.HAND)) return;
        if(!validCastingItem(event.getPlayer().getEquipment().getItemInMainHand())) return;
        CastingProfile castingProfile = castingMap.computeIfAbsent(event.getPlayer().getUniqueId(),k->new CastingProfile(event.getPlayer()));
        castingProfile.updateClick(ClickType.RIGHT);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event){
        castingMap.remove(event.getPlayer().getUniqueId());
    }


    private static class CastingProfile{
        final Player player;
        final ArrayList<ClickType> sequence = new ArrayList<>();
        BukkitTask refreshTask = startRefreshTask();

        public CastingProfile(Player player){
            this.player = player;
        }

        void updateClick(ClickType clickType){
            if(sequence.size() >= 3) sequence.remove(0);
            sequence.add(clickType);
            if(refreshTask != null) refreshTask.cancel();;
            refreshTask = startRefreshTask();
            String display = "";
            for (ClickType type : sequence) {
                display += type.getDisplay();
            }
            player.sendTitle("   ",display);
            if(sequence.size() <3) player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK,1,2);
            else {
                player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER,1,2);

                Optional<PlayerSkillProfile> playerProfile = PlayerSkillProfile.get(player.getUniqueId());
                Optional<ClickSequence> clickSequence = ClickSequence.get(sequence);
                sequence.clear();
                if(playerProfile.isPresent() && clickSequence.isPresent()){
                    playerProfile.get().getButtonSkill(clickSequence.get()).ifPresentOrElse(skill->{
                        skill.cast(playerProfile.get(),playerProfile.get().getSkillData(skill.getSkillID()));
                    },()->{
                        player.sendMessage("該欄位未綁定技能 /hs castbutton 來進行綁定");
                    });
                }
            }
        }

        BukkitTask startRefreshTask(){
            return Bukkit.getScheduler().runTaskLater(HeroesSkills.getInstance(), sequence::clear,REFRESH_TICKS);
        }
    }

    private boolean validCastingItem(@Nullable ItemStack itemStack){
        if(itemStack == null) return false;
        String type = NBTItem.get(itemStack).getType();
        if(type  == null) return false;
        if(availableTypes.contains(type)){
            return true;
        }
        return false;
    }

}

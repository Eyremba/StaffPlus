package net.shortninja.staffplus;

import net.shortninja.staffplus.data.config.Messages;
import net.shortninja.staffplus.data.config.Options;
import net.shortninja.staffplus.player.User;
import net.shortninja.staffplus.player.UserManager;
import net.shortninja.staffplus.player.attribute.infraction.InfractionCoordinator;
import net.shortninja.staffplus.player.attribute.infraction.Warning;
import net.shortninja.staffplus.player.attribute.mode.handler.FreezeHandler;
import net.shortninja.staffplus.player.attribute.mode.handler.GadgetHandler;
import net.shortninja.staffplus.server.AlertCoordinator;
import net.shortninja.staffplus.util.Message;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

//TODO: Remove debug.

public class Tasks extends BukkitRunnable
{
	private Message message = StaffPlus.get().message;
	private Options options = StaffPlus.get().options;
	private Messages messages = StaffPlus.get().messages;
	private UserManager userManager = StaffPlus.get().userManager;
	private FreezeHandler freezeHandler = StaffPlus.get().freezeHandler;
	private GadgetHandler gadgetHandler = StaffPlus.get().gadgetHandler;
	private InfractionCoordinator infractionCoordinator = StaffPlus.get().infractionCoordinator;
	private AlertCoordinator alertCoordinator = StaffPlus.get().alertCoordinator;
	private int saveInterval;
	private int freezeInterval;
	private long now;
	private long later;
	
	public Tasks()
	{
		saveInterval = 0;
		freezeInterval = 0;
		now = System.currentTimeMillis();
		runTaskTimer(StaffPlus.get(), options.clock, options.clock);
	}
	
	@Override
	public void run()
	{
		checkWarnings();
		decideAutosave();
		freezeHandler.checkLocations();
		gadgetHandler.updateGadgets();
	}
	
	private void checkWarnings()
	{
		for(Warning warning : infractionCoordinator.getWarnings())
		{
			if(warning.shouldRemove())
			{
				User user = userManager.get(warning.getUuid());
				
				if(user == null)
				{
					continue;
				}
				
				user.removeWarning(warning.getUuid());
			}
		}
	}
	
	private void decideAutosave()
	{
		later = System.currentTimeMillis();
		
		if((later - now) >= 1000)
		{
			int addition = (int) ((later - now) / 1000);
			saveInterval += addition;
			freezeInterval += addition;
			now = System.currentTimeMillis();
		}
		
		if(saveInterval >= options.autoSave && saveInterval > 0)
		{
			StaffPlus.get().saveUsers();
			StaffPlus.get().message.sendConsoleMessage("Staff+ is now auto saving...", false);
			alertCoordinator.clearNotified();
			saveInterval = 0;
		}else if(options.autoSave <= 0 && saveInterval >= 1800)
		{
			alertCoordinator.clearNotified();
		}
		
		if(freezeInterval >= options.modeFreezeTimer && freezeInterval > 0)
		{
			for(Player player : Bukkit.getOnlinePlayers())
			{
				User user = userManager.get(player.getUniqueId());
				
				if(user.isFrozen())
				{
					options.modeFreezeSound.play(player);
					
					if(!options.modeFreezePrompt)
					{
						message.sendCollectedMessage(player, messages.freeze, messages.prefixGeneral);
					}
				}
			}
			
			freezeInterval = 0;
		}
	}
}
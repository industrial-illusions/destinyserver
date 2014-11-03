package org.destiny.server.messages;

import java.util.HashMap;
import java.util.Map;

import org.destiny.server.messages.events.AcceptRequestEvent;
import org.destiny.server.messages.events.AddFriendEvent;
import org.destiny.server.messages.events.AllowEvolutionEvent;
import org.destiny.server.messages.events.BattleRequestEvent;
import org.destiny.server.messages.events.BoxInfoEvent;
import org.destiny.server.messages.events.CanLearnMoveEvent;
import org.destiny.server.messages.events.CancelEvolutionEvent;
import org.destiny.server.messages.events.CancelTradeEvent;
import org.destiny.server.messages.events.ChangePasswordEvent;
import org.destiny.server.messages.events.ChatEvent;
import org.destiny.server.messages.events.DeclineRequestEvent;
import org.destiny.server.messages.events.DropItemEvent;
import org.destiny.server.messages.events.FinishBoxingEvent;
import org.destiny.server.messages.events.FinishTalkingEvent;
import org.destiny.server.messages.events.ForceLoginEvent;
import org.destiny.server.messages.events.GiveItemEvent;
import org.destiny.server.messages.events.LoginEvent;
import org.destiny.server.messages.events.LogoutRequestEvent;
import org.destiny.server.messages.events.MoveDownEvent;
import org.destiny.server.messages.events.MoveLearnRequestEvent;
import org.destiny.server.messages.events.MoveLeftEvent;
import org.destiny.server.messages.events.MoveRightEvent;
import org.destiny.server.messages.events.MoveUpEvent;
import org.destiny.server.messages.events.PartySwapEvent;
import org.destiny.server.messages.events.PlayerCommandEvent;
import org.destiny.server.messages.events.PokemonInfoRequestEvent;
import org.destiny.server.messages.events.PokemonSwitchEvent;
import org.destiny.server.messages.events.RegisterEvent;
import org.destiny.server.messages.events.ReleasePokemonEvent;
import org.destiny.server.messages.events.RemoveFriendEvent;
import org.destiny.server.messages.events.RunEvent;
import org.destiny.server.messages.events.SelectedMoveEvent;
import org.destiny.server.messages.events.ShopEvent;
import org.destiny.server.messages.events.SpriteEvent;
import org.destiny.server.messages.events.StartBattlefrontierEvent;
import org.destiny.server.messages.events.StartTalkingEvent;
import org.destiny.server.messages.events.SwapPokemonBoxPartyEvent;
import org.destiny.server.messages.events.TradeCancelOfferEvent;
import org.destiny.server.messages.events.TradeOfferEvent;
import org.destiny.server.messages.events.TradeReadyEvent;
import org.destiny.server.messages.events.TradeRequestEvent;
import org.destiny.server.messages.events.TravelEvent;
import org.destiny.server.messages.events.UnableLearnMoveEvent;
import org.destiny.server.messages.events.UseItemEvent;

public class MessageHandler
{
	private Map<Integer, MessageEvent> messages;

	public MessageHandler()
	{
		messages = new HashMap<Integer, MessageEvent>();
	}

	public boolean contains(int id)
	{
		return messages.containsKey(id);
	}

	public MessageEvent get(int id)
	{
		if(messages.containsKey(id))
			return messages.get(id);
		else
			return null;
	}

	public void register()
	{
		messages.put(1, new RegisterEvent());
		messages.put(2, new ChangePasswordEvent());
		messages.put(3, new ForceLoginEvent());
		messages.put(4, new MoveUpEvent());
		messages.put(5, new MoveDownEvent());
		messages.put(6, new MoveLeftEvent());
		messages.put(7, new MoveRightEvent());
		messages.put(8, new CanLearnMoveEvent());
		messages.put(9, new UnableLearnMoveEvent());
		messages.put(10, new CancelEvolutionEvent());
		messages.put(11, new AllowEvolutionEvent());
		messages.put(12, new PartySwapEvent());
		messages.put(13, new ShopEvent());
		messages.put(14, new SpriteEvent());
		messages.put(15, new BattleRequestEvent());
		messages.put(16, new TradeRequestEvent());
		messages.put(17, new AcceptRequestEvent());
		messages.put(18, new DeclineRequestEvent());
		messages.put(19, new BoxInfoEvent());
		messages.put(20, new ReleasePokemonEvent());
		messages.put(21, new SwapPokemonBoxPartyEvent());
		messages.put(22, new FinishBoxingEvent());
		messages.put(23, new PokemonInfoRequestEvent()); // TODO: request pokemon info (can be all kinds of things. request a summary, info on stats, movesets, etc ...)
		messages.put(24, new MoveLearnRequestEvent()); // only for movetutor/moverelearner
		// messages.put(25, new ());
		// messages.put(26, new ());
		// messages.put(27, new ());
		// messages.put(28, new ());
		// messages.put(29, new ());
		// messages.put(30, new ());
		// messages.put(31, new ());
		// messages.put(32, new ());
		messages.put(33, new PlayerCommandEvent());
		// messages.put(34, new ());
		messages.put(35, new SelectedMoveEvent());
		messages.put(36, new PokemonSwitchEvent());
		messages.put(37, new RunEvent());
		messages.put(38, new AddFriendEvent());
		messages.put(39, new RemoveFriendEvent());
		messages.put(40, new UseItemEvent());
		messages.put(41, new DropItemEvent());
		messages.put(42, new TradeOfferEvent());
		messages.put(43, new TradeReadyEvent());
		messages.put(44, new TradeCancelOfferEvent());
		messages.put(45, new CancelTradeEvent());
		messages.put(46, new ChatEvent());
		messages.put(47, new StartTalkingEvent());
		messages.put(48, new FinishTalkingEvent());
		messages.put(49, new LogoutRequestEvent());
		messages.put(50, new LoginEvent());
		messages.put(51, new GiveItemEvent());
		// messages.put(52, new ());
		messages.put(53, new TravelEvent());
		// messages.put(54, new ());
		// messages.put(55, new ());
		// messages.put(56, new ());
		// messages.put(57, new ());
		messages.put(58, new StartBattlefrontierEvent());
		/* TODO: Use commented numbers for future messages. */
	}
}

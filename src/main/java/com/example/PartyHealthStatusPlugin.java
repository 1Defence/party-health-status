/*
 * Copyright (c) 2022, Jamal <http://github.com/1Defence>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.example;

import com.google.inject.Provides;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;

import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;

import net.runelite.client.party.messages.PartyMemberMessage;
import net.runelite.client.party.messages.UserJoin;
import net.runelite.client.party.messages.UserPart;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.util.Text;

@PluginDescriptor(
		name = "Party Health Status",
		description = "Visual health display of party members"
)
@Slf4j
public class PartyHealthStatusPlugin extends Plugin
{

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PartyService partyService;

	@Inject
	private WSClient wsClient;

	@Inject
	private PartyHealthStatusOverlay partyHealthStatusOverlay;

	@Inject
	private PartyHealthStatusConfig config;

	@Getter(AccessLevel.PACKAGE)
	private final Map<String, PartyHealthStatusMember> members = new ConcurrentHashMap<>();

	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private int lastKnownHP = -1;

	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private boolean queuedUpdate = false;

	/**
	 * Visible players from the configuration (Strings)
	 */
	private List<String> visiblePlayers = new ArrayList<>();

	private final String DEFAULT_MEMBER_NAME = "<unknown>";


	@Provides
	PartyHealthStatusConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PartyHealthStatusConfig.class);
	}

	@Override
	protected void startUp()
	{
		visiblePlayers = getVisiblePlayers();
		overlayManager.add(partyHealthStatusOverlay);
		wsClient.registerMessage(PartyHealthStatusUpdate.class);
	}

	@Override
	protected void shutDown()
	{
		wsClient.unregisterMessage(PartyHealthStatusUpdate.class);
		overlayManager.remove(partyHealthStatusOverlay);
		members.clear();
	}

	@Subscribe
	public void onPartyChanged(PartyChanged partyChanged)
	{
		members.clear();
	}

	@Subscribe(priority = 1)
	public void onUserJoin(final UserJoin message)
	{
		if (!partyService.getPartyId().equals(message.getPartyId()))
		{
			// This can happen when a session is resumed server side after the client party
			// changes when disconnected.
			return;
		}

		//when a user joins, request an update for the next registered game tick
		queuedUpdate = true;
	}

	@Subscribe(priority = 1000) // run prior to the actual leave so we can still grab the name of the leaving player
	public void onUserPart(final UserPart message)
	{
		members.remove(partyService.getMemberById(message.getMemberId()).getDisplayName());
	}

	@Subscribe
	public void onGameTick(GameTick event){
		//an update has been requested, resync party members hp data
		if(queuedUpdate && client.getLocalPlayer() != null && partyService.isInParty()){
			String name = partyService.getMemberById(partyService.getLocalMember().getMemberId()).getDisplayName();
			if(!name.equals(DEFAULT_MEMBER_NAME)){
				queuedUpdate = false;
				SendUpdate(client.getBoostedSkillLevel(Skill.HITPOINTS), client.getRealSkillLevel(Skill.HITPOINTS));
			}
		}
	}

	public boolean LocalMemberIsValid(){
		//validate local member is in a party
		PartyMember localMember = partyService.getLocalMember();
		return (localMember != null);
	}

	public boolean MemberIsValid(PartyMemberMessage message, boolean allowSelf){

		if(!allowSelf) {
			if(partyService.getLocalMember().getMemberId().equals(message.getMemberId())){
				return false;
			}
		}

		String name = partyService.getMemberById(message.getMemberId()).getDisplayName();
		if (name == null)
		{
			return false;
		}

		return true;
	}


	public void SendUpdate(int currentHP, int maxHP){


		if(LocalMemberIsValid()){
			UUID localID = partyService.getLocalMember().getMemberId();
			String name = partyService.getMemberById(partyService.getLocalMember().getMemberId()).getDisplayName();

			partyService.send(new PartyHealthStatusUpdate(currentHP, maxHP, localID));
			//handle self locally.
			PartyHealthStatusMember partyHealthStatusMember = members.computeIfAbsent(name, PartyHealthStatusMember::new);
			partyHealthStatusMember.setCurrentHP(currentHP);
			partyHealthStatusMember.setMaxHP(maxHP);
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{

		if(queuedUpdate){
			return;
		}

		Skill skill = statChanged.getSkill();
		if(skill != Skill.HITPOINTS){
			return;
		}

		int maxHP = client.getRealSkillLevel(skill);
		int currentHP = client.getBoostedSkillLevel(skill);

		if(currentHP != lastKnownHP){
			SendUpdate(currentHP,maxHP);
		}

		lastKnownHP = currentHP;

	}

	@Subscribe
	public void onPartyHealthStatusUpdate(PartyHealthStatusUpdate partyHealthStatusUpdate)
	{

		if(!MemberIsValid(partyHealthStatusUpdate,false)){
			return;
		}

		PartyHealthStatusMember partyHealthStatusMember = members.computeIfAbsent(partyService.getMemberById(partyHealthStatusUpdate.getMemberId()).getDisplayName(), PartyHealthStatusMember::new);
		partyHealthStatusMember.setCurrentHP(partyHealthStatusUpdate.getCurrentHealth());
		partyHealthStatusMember.setMaxHP(partyHealthStatusUpdate.getMaxHealth());

	}

	public List<String> getVisiblePlayers()
	{
		final String configPlayers = config.getVisiblePlayers().toLowerCase();

		if (configPlayers.isEmpty())
		{
			return Collections.emptyList();
		}

		return Text.fromCSV(configPlayers);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals("partyhealthstatus"))
		{
			return;
		}

		visiblePlayers = getVisiblePlayers();

	}


}

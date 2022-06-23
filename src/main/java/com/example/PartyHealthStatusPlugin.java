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
import java.util.List;
import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;

import net.runelite.client.party.PartyMember;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.party.PartyPlugin;
import net.runelite.client.plugins.party.PartyPluginService;
import net.runelite.client.plugins.party.messages.CharacterNameUpdate;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.party.PartyService;
import net.runelite.client.util.Text;

@PluginDescriptor(
		name = "Party Health Status",
		description = "Visual health display of party members"
)

@PluginDependency(PartyPlugin.class)
@Slf4j
public class PartyHealthStatusPlugin extends Plugin
{

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PartyService partyService;

	@Getter(AccessLevel.PACKAGE)
	@Inject
	private PartyPluginService partyPluginService;

	@Inject
	private PartyHealthStatusOverlay partyHealthStatusOverlay;

	@Inject
	private PartyHealthStatusConfig config;

	@Getter(AccessLevel.PACKAGE)
	private final Map<String, Long> members = new HashMap<>();

	/**
	 * Visible players from the configuration (Strings)
	 */
	@Getter(AccessLevel.PACKAGE)
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

		//handle startup while already in a party as party syncing events won't be fired.
		if(partyService != null && partyService.isInParty()){
			for (PartyMember member : partyService.getMembers()) {
				if(member.getDisplayName().equals(DEFAULT_MEMBER_NAME)){
					continue;//skip logged out players, they're updated via nameupdate
				}
				RegisterMember(member.getMemberId(),member.getDisplayName());
			}
		}

	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(partyHealthStatusOverlay);
		members.clear();
	}

	@Subscribe
	public void onPartyChanged(PartyChanged partyChanged)
	{
		members.clear();
	}


	@Subscribe
	public void onUserPart(final UserPart message) {
		//name not always present, find by id
		String name = "";
		for (Map.Entry<String, Long> entry: members.entrySet()) {
			if(entry.getValue() == message.getMemberId()){
				name = entry.getKey();
			}
		}
		if(!name.isEmpty()) {
			members.remove(name);
		}
	}

	void RegisterMember(long memberID, String memberName){
		members.putIfAbsent(memberName,memberID);
	}


	public List<String> parseVisiblePlayers()
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

		visiblePlayers = parseVisiblePlayers();
	}

	//only register member once their name has been set.
	@Subscribe
	public void onCharacterNameUpdate(final CharacterNameUpdate event){
	String name = event.getCharacterName();
		if(!name.isEmpty()){
			RegisterMember(event.getMemberId(),event.getCharacterName());
		}
	}

}

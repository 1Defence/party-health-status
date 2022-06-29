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

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.MenuEntryAdded;
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
import net.runelite.client.plugins.party.data.PartyData;
import net.runelite.client.plugins.party.messages.StatusUpdate;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.party.PartyService;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

import static com.example.PartyHealthStatusConfig.TextRenderType;
import static com.example.PartyHealthStatusConfig.ColorType;

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

	@Inject
	private Client client;

	@Getter(AccessLevel.PACKAGE)
	private final Map<String, Long> members = new HashMap<>();

	/**
	 * Visible players from the configuration (Strings)
	 */
	@Getter(AccessLevel.PACKAGE)
	private List<String> visiblePlayers = new ArrayList<>();

	private final String DEFAULT_MEMBER_NAME = "<unknown>";

	/*<|Cached Configs*/

	int healthyOffSet,
			hullOpacity,
			hitPointsMinimum,
			mediumHP,
			lowHP,
			offSetTextHorizontal,
			offSetTextVertical,
			offSetTextZ,
			offSetStackVertical,
			fontSize;


	Color healthyColor,
			highColor,
			mediumColor,
			lowColor;


	boolean renderPlayerHull,
			recolorHealOther,
			drawPercentByName,
			drawParentheses,
			boldFont;

	TextRenderType nameRender,
			hpRender;

	ColorType colorType;
	/*Cached Configs|>*/

	@Provides
	PartyHealthStatusConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PartyHealthStatusConfig.class);
	}

	@Override
	protected void startUp()
	{
		CacheConfigs();
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
		members.put(memberName,memberID);
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

		CacheConfigs();
	}

	public void CacheConfigs(){

		healthyOffSet = config.healthyOffset();
				hullOpacity = config.hullOpacity();
				hitPointsMinimum = config.getHitpointsMinimum();
				mediumHP = config.getMediumHP();
				lowHP = config.getLowHP();
				offSetTextHorizontal = config.offSetTextHorizontal();
				offSetTextVertical = config.offSetTextVertial();
				offSetTextZ = config.offSetTextZ();
				offSetStackVertical = config.offSetStackVertical();
				fontSize = config.fontSize();

		healthyColor = config.getHealthyColor();
				highColor = config.getHighColor();
				mediumColor = config.getMediumColor();
				lowColor = config.getLowColor();


		renderPlayerHull = config.renderPlayerHull();
				recolorHealOther = config.recolorHealOther();
				drawPercentByName = config.drawPercentByName();
				drawParentheses = config.drawParentheses();
				boldFont = config.boldFont();

		colorType = config.getColorType();

		nameRender = config.nameRender();
				hpRender = config.hpRender();


		visiblePlayers = parseVisiblePlayers();
	}


	//only register member once their name has been set.
	@Subscribe
	public void onStatusUpdate(final StatusUpdate event){
		String name = event.getCharacterName();
		if(name != null && !name.isEmpty()){
			RegisterMember(event.getMemberId(),event.getCharacterName());
		}
	}


	public boolean RenderText(TextRenderType textRenderType, boolean healthy){
		if(textRenderType == TextRenderType.NEVER)
			return false;
		return textRenderType == TextRenderType.ALWAYS
				|| (textRenderType == TextRenderType.WHEN_MISSING_HP && !healthy);
	}

	public int ClampMax(float val, float max){
		return val > max ? (int)max : (int)val;
	}

	public float ClampMinf(float val, float min){
		return val < min ? min : val;
	}

	public boolean IsHealthy(int currentHP,int maxHP){
		return currentHP == -1 || currentHP >= (maxHP-healthyOffSet);
	}

	public Color GetHitPointsColor(int currentHP, int maxHP){
		Color color = healthyColor;

		switch (colorType){

			case LERP_2D:
			{
				float hpThreshold = hitPointsMinimum;
				float currentRatio = (currentHP - hpThreshold <= 0) ? 0 : ClampMinf(((float) currentHP - hpThreshold) / maxHP, 0);
				int r = ClampMax((1 - currentRatio) * 255, 255);
				int g = ClampMax(currentRatio * 255, 255);
				color = new Color(r, g, 0, hullOpacity);
			}
			break;
			case LERP_3D:
			{
				float halfHP = (float)maxHP/2f;
				if(currentHP >= halfHP){
					color = ColorUtil.colorLerp(Color.orange, Color.green, (((float)currentHP-halfHP)/halfHP));
				}else{
					color = ColorUtil.colorLerp(Color.red, Color.orange, (float)currentHP/halfHP);
				}
			}
			break;
			case COLOR_THRESHOLDS:
			{
				float hpPerc = ((float)currentHP/(float)maxHP)*maxHP;
				color = hpPerc <= lowHP ? lowColor
						: hpPerc <= mediumHP ? mediumColor
						: hpPerc < maxHP ? highColor : healthyColor;
			}
			break;
		}
		return color;
	}

	String GenerateTargetText(Player player){
		String name = player.getName();
		long memberID = getMembers().getOrDefault(name, -1L);
		PartyData partyData = getPartyPluginService().getPartyData(memberID);
		boolean validMember = partyData != null;

		int currentHP = validMember ? partyData.getHitpoints() : -1;
		int maxHP = validMember ? partyData.getMaxHitpoints() : -1;
		boolean healthy = IsHealthy(currentHP,maxHP);

		Color greyedOut = new Color(128,128,128);
		Color color = GetHitPointsColor(currentHP,maxHP);

		return ColorUtil.wrapWithColorTag("Heal Other", healthy ? greyedOut : Color.green) +
				ColorUtil.wrapWithColorTag(" -> ", healthy ? greyedOut : Color.white) +
				ColorUtil.wrapWithColorTag(name, healthy ? greyedOut : color) +
				ColorUtil.wrapWithColorTag(healthy ? "" : ("  (HP-" + currentHP + ")"), healthy ? greyedOut : color);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if(!recolorHealOther)
			return;

		int type = event.getType();
		final MenuAction menuAction = MenuAction.of(type);

		if(menuAction.equals(MenuAction.WIDGET_TARGET_ON_PLAYER)){
			String option = event.getMenuEntry().getOption();
			String target = Text.removeTags(event.getMenuEntry().getTarget());

			if(option.equals("Cast") && target.startsWith("Heal Other")){

				Player player = client.getCachedPlayers()[event.getIdentifier()];

				MenuEntry[] menuEntries = client.getMenuEntries();
				final MenuEntry menuEntry = menuEntries[menuEntries.length - 1];

				menuEntry.setTarget(GenerateTargetText(player));
				client.setMenuEntries(menuEntries);

			}
		}
	}

}

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
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;

import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserJoin;
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

	@Inject
	private ClientThread clientThread;

	@Inject
	private WSClient wsClient;

	@Getter(AccessLevel.PACKAGE)
	private final Map<String, PartyHealthStatusMember> members = new ConcurrentHashMap<>();

	@Getter(AccessLevel.PACKAGE)
	private final Map<String, PartyHealthStatusMember> genericPartyMembers = new ConcurrentHashMap<>();

	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private int lastKnownHP = -1;

	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private boolean queuedUpdate = false;

	/**
	 * Visible players from the configuration (Strings)
	 */
	@Getter(AccessLevel.PACKAGE)
	private List<String> visiblePlayers = new ArrayList<>();

	/**
	 * Hidden players from the configuration (Strings)
	 */
	@Getter(AccessLevel.PACKAGE)
	private List<String> hiddenPlayers = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private List<String> hiddenPartyMembers = new ArrayList<>();

	private final String DEFAULT_MEMBER_NAME = "<unknown>";
	private String currentLocalUsername;

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


	boolean hideAllPlayers,
			hideSelf,
			renderPlayerHull,
			recolorHealOther,
			drawPercentByName,
			drawParentheses,
			boldFont;

	TextRenderType nameRender,
			hpRender;

	ColorType colorType;
	/*Cached Configs|>*/

	@Inject
	private RenderCallbackManager renderCallbackManager;

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
		lastKnownHP = -1;
		queuedUpdate = true;
		wsClient.registerMessage(PartyHealthStatusUpdate.class);
		clientThread.invokeLater(() ->
		{
			renderCallbackManager.register(renderCallback);
		});
	}

	@Override
	protected void shutDown()
	{
		wsClient.unregisterMessage(PartyHealthStatusUpdate.class);
		overlayManager.remove(partyHealthStatusOverlay);
		members.clear();
		clientThread.invoke(() ->
		{
			renderCallbackManager.unregister(renderCallback);
		});
	}

	@Subscribe
	public void onPartyChanged(PartyChanged partyChanged)
	{
		members.clear();
	}

	@Subscribe
	public void onUserJoin(final UserJoin message)
	{
		//when a user joins, request an update for the next registered game tick
		queuedUpdate = true;
	}

	/**
	 * genericPartyMembers acts as a fallback for users who don't have this plugin, or are dropping these packets.
	 */
	@Subscribe(priority = -100)
	public void onStatusUpdate(final StatusUpdate message) {
		long updateId = message.getMemberId();
		if (partyService.getLocalMember().getMemberId() == updateId) {
			return;
		}

		String name = partyService.getMemberById(updateId).getDisplayName();
		if (name.equals(DEFAULT_MEMBER_NAME)) {
			return;
		}

		PartyHealthStatusMember member = genericPartyMembers.computeIfAbsent(name, PartyHealthStatusMember::new);
		UpdateMember(member, name, updateId);
	}

	void UpdateMember(PartyHealthStatusMember member, String name, long memberId){
		PartyData partyData = partyPluginService.getPartyData(memberId);
		member.setName(name);
		member.setMemberID(memberId);
		member.setCurrentHP(partyData.getHitpoints());
		member.setMaxHP(partyData.getMaxHitpoints());
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged runeScapeProfileChanged)
	{
		queuedUpdate = true;
	}

	@Subscribe
	public void onUserPart(final UserPart message) {
		//name not always present, find by id
		String name = "";
		for (Map.Entry<String, PartyHealthStatusMember> entry: members.entrySet()) {
			if(entry.getValue().getMemberID() == message.getMemberId()){
				name = entry.getKey();
			}
		}
		if(!name.isEmpty()) {
			members.remove(name);
			genericPartyMembers.remove(name);
		}
	}

	void RegisterMember(long memberID, String memberName, int currentHP, int maxHP){
		if(memberName.equals(DEFAULT_MEMBER_NAME)){
			return;
		}
		PartyHealthStatusMember member = members.computeIfAbsent(memberName, PartyHealthStatusMember::new);
		member.setName(memberName);
		member.setMemberID(memberID);
		member.setCurrentHP(currentHP);
		member.setMaxHP(maxHP);
	}


	public List<String> parsePlayerList(String playerList)
	{
		final String configPlayers = playerList.toLowerCase();

		if (configPlayers.isEmpty())
		{
			return Collections.emptyList();
		}

		return Text.fromCSV(configPlayers);
	}

	public List<String> parseVisiblePlayers(){
		return parsePlayerList(config.getVisiblePlayers());
	}

	public List<String> parseHiddenPlayers(){
		return parsePlayerList(config.getHiddenPlayers());
	}
	public List<String> parseHiddenPartyMembers(){
		return parsePlayerList(config.getHiddenPartyMembers());
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


		hideAllPlayers = config.hideAllPlayers();
				hideSelf = config.hideSelf();
				renderPlayerHull = config.renderPlayerHull();
				recolorHealOther = config.recolorHealOther();
				drawPercentByName = config.drawPercentByName();
				drawParentheses = config.drawParentheses();
				boldFont = config.boldFont();

		colorType = config.getColorType();

		nameRender = config.nameRender();
				hpRender = config.hpRender();


		visiblePlayers = parseVisiblePlayers();
		hiddenPlayers = parseHiddenPlayers();
		hiddenPartyMembers = parseHiddenPartyMembers();
	}


	@Subscribe
	public void onPartyHealthStatusUpdate(PartyHealthStatusUpdate update)
	{

		if (partyService.getLocalMember().getMemberId() == update.getMemberId())
		{
			return;
		}

		String name = partyService.getMemberById(update.getMemberId()).getDisplayName();
		if (name == null)
		{
			return;
		}

		RegisterMember(update.getMemberId(),name,update.getCurrentHealth(), update.getMaxHealth());
	}


	@Subscribe
	public void onGameTick(GameTick event)
	{
		//an update has been requested, resync party members hp data
		if (queuedUpdate && client.getLocalPlayer() != null && partyService.isInParty() && partyService.getLocalMember() != null)
		{
			currentLocalUsername = SanitizeName(client.getLocalPlayer().getName());
			String partyName = partyService.getMemberById(partyService.getLocalMember().getMemberId()).getDisplayName();
			//dont send unless the partyname has updated to the local name
			if (currentLocalUsername != null && currentLocalUsername.equals(partyName))
			{
				queuedUpdate = false;
				SendUpdate(currentLocalUsername, client.getBoostedSkillLevel(Skill.HITPOINTS), client.getRealSkillLevel(Skill.HITPOINTS));
			}
		}
	}

	private final RenderCallback renderCallback = new RenderCallback()
	{
		@Override
		public boolean addEntity(Renderable renderable, boolean ui)
		{
			if (hiddenPartyMembers.isEmpty())
				return true;

			if (renderable instanceof Player)
			{
				Player player = (Player) renderable;
				String name = SanitizeName(player.getName());

				//Only allowed to hide if party member
				boolean validPartyMember = partyService.getMemberByDisplayName(name) != null;

				boolean hidden = hiddenPartyMembers.contains(name.toLowerCase());

				//dont parse if not in list
				if(!hidden)
					return true;

				if(validPartyMember)
					return false;
			}
			return true;
		}
	};



	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		Skill skill = statChanged.getSkill();
		if (skill != Skill.HITPOINTS)
		{
			return;
		}

		int currentHP = client.getBoostedSkillLevel(skill);

		if (currentHP != lastKnownHP)
		{
			queuedUpdate = true;
		}

		lastKnownHP = currentHP;
	}
	public void SendUpdate(String name, int currentHP, int maxHP){
		if(partyService.getLocalMember() != null) {
			partyService.send(new PartyHealthStatusUpdate(currentHP, maxHP));
			//handle self locally.
			RegisterMember(partyService.getLocalMember().getMemberId(),name,currentHP,maxHP);
		}
	}

	String SanitizeName(String name){
		return Text.removeTags(Text.toJagexName(name));
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

	public int GetHitpoints(String name, boolean current){
		PartyHealthStatusMember member = getMembers().get(name);
		if(member == null){
			member = getGenericPartyMembers().get(name);
		}

		if(member == null)
			return 99;

		return current ? member.getCurrentHP() : member.getMaxHP();
	}

	public Color GetHitPointsColor(int currentHP, int maxHP){
		Color color = healthyColor;

		if(currentHP == -1) {
			return color;
		}

		if(currentHP > maxHP){
			currentHP = maxHP;
		}

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


	boolean RenderPlayer(String sanitizedName){
		if(!members.containsKey(sanitizedName) && !genericPartyMembers.containsKey(sanitizedName))
			return false;
		if(!visiblePlayers.isEmpty() && !visiblePlayers.contains(sanitizedName.toLowerCase()))
			return false;
		if(hiddenPlayers.contains(sanitizedName.toLowerCase()))
			return false;
		if(hideSelf && currentLocalUsername.equals(sanitizedName)){
			return false;
		}
		return true;
	}

	String GenerateTargetText(Player player){
		String rawName = player.getName();
		String sanitizedName = SanitizeName(rawName);
		boolean validMember = RenderPlayer(sanitizedName);
		int currentHP = validMember ? GetHitpoints(sanitizedName,true) : -1;
		int maxHP = validMember ? GetHitpoints(sanitizedName,false) : -1;
		boolean healthy = IsHealthy(currentHP,maxHP);

		Color greyedOut = new Color(128,128,128);
		Color color = GetHitPointsColor(currentHP,maxHP);

		return ColorUtil.wrapWithColorTag("Heal Other", healthy ? greyedOut : Color.green) +
				ColorUtil.wrapWithColorTag(" -> ", healthy ? greyedOut : Color.white) +
				ColorUtil.wrapWithColorTag(rawName, healthy ? greyedOut : color) +
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

				Player player = client.getTopLevelWorldView().players().byIndex(event.getIdentifier());

				MenuEntry[] menuEntries = client.getMenuEntries();
				final MenuEntry menuEntry = menuEntries[menuEntries.length - 1];

				menuEntry.setTarget(GenerateTargetText(player));
				client.setMenuEntries(menuEntries);

			}
		}
	}

}

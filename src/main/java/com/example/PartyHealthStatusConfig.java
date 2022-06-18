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

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("partyhealthstatus")
public interface PartyHealthStatusConfig extends Config
{

	@ConfigSection(name="Visual Overlay", description="visual overlay settings", position=1, closedByDefault=false)
	String visualOverlay = "visualOverlay";
	@ConfigSection(name="Text Overlay", description="text overlay settings", position=2, closedByDefault=true)
	String textOverlay = "textOverlay";


	@ConfigItem(
			position = 1,
			keyName = "visiblePlayers",
			name = "Visible Players",
			description = "Only names listed will have visuals shown, if list is empty all connected party members will show up",
			section = visualOverlay
	)
	default String getVisiblePlayers()
	{
		return "";
	}

	@ConfigItem(
			position = 2,
			keyName = "hitpointsThreshold",
			name = "Hitpoints Threshold",
			description = "The amount of hitpoints the player should be highlighted fully red at(1-99), 0 to disable color changing, 20 recommended",
			section = visualOverlay
	)
	default int getHitpointsThreshold()
	{
		return 20;
	}

	@Alpha
	@ConfigItem(
			position = 3,
			keyName = "healthyColor",
			name = "Healthy Color",
			description = "The default color of a healthy full-HP player",
			section = visualOverlay
	)
	default Color getHealthyColor()
	{
		return new Color(255,255,255,50);
	}

	@ConfigItem(
			position = 4,
			keyName = "hullOpacity",
			name = "Hull Opacity",
			description = "hull opcacity, 30 recommended",
			section = visualOverlay)
	default int hullOpacity() { return 30; }

	@ConfigItem(
			position = 8,
			keyName = "renderPlayerHull",
			name = "Render Player Hull",
			description = "Render the hull of visible party members",
			section = visualOverlay)
	default boolean renderPlayerHull()
	{
		return false;
	}

	@ConfigItem(
			position = 4,
			keyName = "drawNames",
			name = "Draw Name Above players",
			description = "Configures whether or not player names should render",
			section = textOverlay
	)
	default boolean drawNames()
	{
		return false;
	}

	@ConfigItem(
			position = 5,
			keyName = "drawPercentByName",
			name = "Draw Percent By Name",
			description = "Draw a % beside the numeral value of remaining hp",
			section = textOverlay)
	default boolean drawPercentByName() { return false; }

	@ConfigItem(
			position = 6,
			keyName = "drawParentheses",
			name = "Draw Parentheses By Name",
			description = "Draw parentheses surrounding hp number",
			section = textOverlay)
	default boolean drawParentheses() { return false; }

	@Range(max=16, min=8)
	@ConfigItem(
			position=6,
			keyName="fontSize",
			name="Font Size",
			description="font size",
			section = textOverlay)
	default int fontSize() {
		return 12;
	}


	@ConfigItem(
			position = 7,
			keyName = "offSetTextHorizontal",
			name = "OffSet Text Horizontal",
			description = "OffSet the text horizontally",
			section = textOverlay)
	default int offSetTextHorizontal() { return 0; }

	@ConfigItem(
			position = 8,
			keyName = "offSetTextVertical",
			name = "OffSet Text Vertical",
			description = "OffSet the text vertically",
			section = textOverlay)
	default int offSetTextVertial() { return 0; }

	@ConfigItem(
			position = 9,
			keyName = "offSetTextZ",
			name = "OffSet Text Z",
			description = "OffSet the text Z",
			section = textOverlay)
	default int offSetTextZ() { return 65; }

}
